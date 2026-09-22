package com.kairokk.realtime;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kairokk.KairokkClient;
import com.kairokk.client.KairokkItemEspState;
import com.kairokk.client.KairokkOverlayManager;
import com.kairokk.client.pathfinder.pathfinding.NavigationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** Authenticated website bridge. All game mutations run on Minecraft's main thread. */
public final class KairokkRealtimeClient implements WebSocket.Listener {
	private static final Gson GSON = new Gson();
	private static final List<String> CAPABILITIES = List.of("TOGGLE_MODULE", "SEND_CHAT", "TOGGLE_HUD", "PATHFINDER_START", "PATHFINDER_STOP", "WORLD_SUBSCRIBE", "WORLD_UNSUBSCRIBE", "WORLD_RESYNC_REQUEST", "PATHFINDER_SET_GOAL");
	private static final long ACCESS_TOKEN_REFRESH_AFTER_MS = TimeUnit.MINUTES.toMillis(10);
	private static final long HANDSHAKE_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(12);
	private static final long MAX_RETRY_DELAY_MS = TimeUnit.SECONDS.toMillis(30);

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "kairokk-realtime"); t.setDaemon(true); return t; });
	private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
	private final AtomicBoolean reconnectScheduled = new AtomicBoolean();
	private final AtomicBoolean heartbeatStarted = new AtomicBoolean();
	private final AtomicLong connectionGeneration = new AtomicLong();
	private final Object sendLock = new Object();
	private CompletableFuture<Void> sendTail = CompletableFuture.completedFuture(null);
	private final WorldViewService worldView = new WorldViewService(this::sendEvent);
	private volatile WebSocket socket;
	private volatile WebSocket pendingSocket;
	private BlockPos pathfinderGoal;
	private String backend, token, fingerprint, deviceId;
	private volatile String connectionStatus = "Not linked — setup required";
	private volatile boolean savedLinkAvailable;
	private volatile long tokenIssuedAt;
	private int retryAttempt;

	public void tick(Minecraft client) { worldView.tick(client); }

	public void connectFromLaunchArguments() {
		backend = System.getProperty("kairokk.backend");
		token = System.getProperty("kairokk.clientAccessToken");
		fingerprint = System.getProperty("kairokk.deviceFingerprintHash");
		deviceId = System.getProperty("kairokk.deviceId");
		tokenIssuedAt = blank(token) ? 0L : System.currentTimeMillis();
		scheduler.execute(() -> {
			if (readSavedLink()) refreshSavedLink();
			if (!hasConnectionDetails()) {
				connectionStatus = "Not linked — run dashboard setup";
				KairokkClient.LOGGER.info("Dashboard is not linked. Run linkKairokkDashboard once or launch with KAIROKK_DASHBOARD_EMAIL and KAIROKK_DASHBOARD_PASSWORD.");
				return;
			}
			connect();
		});
	}

	public String connectionStatus() { return connectionStatus; }
	public boolean isConnected() { return socket != null && "Connected".equals(connectionStatus); }
	public boolean isLinkConfigured() { return savedLinkAvailable || hasConnectionDetails(); }

	/** Starts a clean connection attempt. It never reports a retry when no link exists. */
	public void reconnect() {
		scheduler.execute(() -> {
			connectionGeneration.incrementAndGet();
			reconnectScheduled.set(false);
			retryAttempt = 0;
			closeCurrentSockets();
			if (readSavedLink()) refreshSavedLink();
			if (!hasConnectionDetails()) {
				connectionStatus = "Not linked — run dashboard setup";
				return;
			}
			connect();
		});
	}

	private void connect() {
		if (!hasConnectionDetails() || socket != null || pendingSocket != null) return;
		if (savedLinkAvailable && System.currentTimeMillis() - tokenIssuedAt >= ACCESS_TOKEN_REFRESH_AFTER_MS) {
			RefreshResult refreshed = refreshSavedLink();
			if (refreshed != RefreshResult.SUCCESS) {
				handleRefreshFailure(refreshed);
				return;
			}
		}
		long generation = connectionGeneration.get();
		connectionStatus = retryAttempt == 0 ? "Connecting…" : "Reconnecting…";
		URI uri;
		try {
			uri = URI.create(backend.replaceFirst("^http", "ws") + "/client/connect?deviceFingerprintHash=" + URLEncoder.encode(fingerprint, StandardCharsets.UTF_8));
		} catch (Exception error) {
			connectionStatus = "Invalid dashboard address";
			KairokkClient.LOGGER.warn("Kairokk dashboard address is invalid", error);
			return;
		}
		httpClient.newWebSocketBuilder().header("Authorization", "Bearer " + token).buildAsync(uri, this)
			.thenAccept(ws -> scheduler.execute(() -> beginHandshake(ws, generation)))
			.exceptionally(error -> { scheduler.execute(() -> connectionFailed(generation, "Dashboard unreachable", error)); return null; });
	}

	private void beginHandshake(WebSocket ws, long generation) {
		if (generation != connectionGeneration.get()) { ws.abort(); return; }
		pendingSocket = ws;
		JsonObject hello = new JsonObject();
		hello.addProperty("protocolVersion", 1);
		hello.addProperty("clientVersion", "1.0.0");
		hello.addProperty("minecraftVersion", "26.1.2");
		hello.addProperty("deviceId", deviceId);
		hello.add("capabilities", GSON.toJsonTree(CAPABILITIES));
		send(ws, "HELLO", UUID.randomUUID().toString(), hello).whenComplete((ignored, error) -> {
			if (error != null) scheduler.execute(() -> connectionFailed(generation, "Handshake failed", error));
		});
		scheduler.schedule(() -> {
			if (generation == connectionGeneration.get() && pendingSocket == ws && socket == null) {
				pendingSocket = null;
				ws.abort();
				scheduleReconnect(generation, "Dashboard did not confirm the connection");
			}
		}, HANDSHAKE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
	}

	private void connectionFailed(long generation, String summary, Throwable error) {
		if (generation != connectionGeneration.get()) return;
		pendingSocket = null;
		if (looksUnauthorized(error) && savedLinkAvailable) {
			RefreshResult refreshed = refreshSavedLink();
			if (refreshed == RefreshResult.SUCCESS) {
				scheduleReconnect(generation, "Refreshing secure link");
				return;
			}
			handleRefreshFailure(refreshed);
			return;
		}
		KairokkClient.LOGGER.warn("Kairokk dashboard connection failed", error);
		scheduleReconnect(generation, summary);
	}

	private void scheduleReconnect(long generation, String summary) {
		if (generation != connectionGeneration.get() || !hasConnectionDetails() || !reconnectScheduled.compareAndSet(false, true)) return;
		retryAttempt = Math.min(retryAttempt + 1, 8);
		long baseDelay = Math.min(MAX_RETRY_DELAY_MS, TimeUnit.SECONDS.toMillis(1L << Math.min(retryAttempt - 1, 5)));
		long delay = Math.max(750L, (long) (baseDelay * ThreadLocalRandom.current().nextDouble(0.85D, 1.15D)));
		connectionStatus = summary + " — retrying in " + Math.max(1L, Math.round(delay / 1000.0D)) + "s";
		scheduler.schedule(() -> {
			reconnectScheduled.set(false);
			if (generation == connectionGeneration.get() && socket == null && pendingSocket == null) connect();
		}, delay, TimeUnit.MILLISECONDS);
	}

	private void handleRefreshFailure(RefreshResult result) {
		if (result == RefreshResult.REJECTED || result == RefreshResult.NO_LINK) {
			connectionStatus = "Link expired — run dashboard setup";
			return;
		}
		scheduleReconnect(connectionGeneration.get(), "Dashboard unavailable");
	}

	private boolean readSavedLink() {
		try {
			Path path = dashboardLinkPath();
			if (!Files.isRegularFile(path)) { savedLinkAvailable = false; return false; }
			JsonObject saved = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
			String savedBackend = string(saved, "backend");
			String savedFingerprint = string(saved, "deviceFingerprintHash");
			String savedDeviceId = string(saved, "deviceId");
			String savedRefresh = string(saved, "refreshToken");
			if (blank(savedBackend) || blank(savedFingerprint) || blank(savedDeviceId) || blank(savedRefresh)) throw new IllegalStateException("link file is incomplete");
			backend = savedBackend.replaceAll("/+$", "");
			fingerprint = savedFingerprint;
			deviceId = savedDeviceId;
			savedLinkAvailable = true;
			return true;
		} catch (Exception error) {
			savedLinkAvailable = false;
			connectionStatus = "Link file is invalid — run dashboard setup";
			KairokkClient.LOGGER.warn("Could not read Kairokk dashboard link", error);
			return false;
		}
	}

	private RefreshResult refreshSavedLink() {
		if (!readSavedLink()) return RefreshResult.NO_LINK;
		try {
			Path path = dashboardLinkPath();
			JsonObject saved = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
			JsonObject body = new JsonObject();
			body.addProperty("refreshToken", string(saved, "refreshToken"));
			HttpURLConnection connection = (HttpURLConnection) URI.create(backend + "/auth/refresh").toURL().openConnection();
			connection.setRequestMethod("POST");
			connection.setConnectTimeout((int) Duration.ofSeconds(8).toMillis());
			connection.setReadTimeout((int) Duration.ofSeconds(12).toMillis());
			connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
			connection.setDoOutput(true);
			byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
			try (var output = connection.getOutputStream()) { output.write(bytes); }
			int status = connection.getResponseCode();
			if (status == 401 || status == 403) return RefreshResult.REJECTED;
			if (status < 200 || status >= 300) return status >= 500 ? RefreshResult.UNAVAILABLE : RefreshResult.REJECTED;
			JsonObject refreshed = JsonParser.parseString(new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			token = string(refreshed, "accessToken");
			String refreshToken = string(refreshed, "refreshToken");
			if (blank(token) || blank(refreshToken)) return RefreshResult.REJECTED;
			saved.addProperty("refreshToken", refreshToken);
			writeSavedLink(path, saved);
			tokenIssuedAt = System.currentTimeMillis();
			return RefreshResult.SUCCESS;
		} catch (IOException error) {
			KairokkClient.LOGGER.debug("Kairokk dashboard is temporarily unavailable while refreshing the link", error);
			return RefreshResult.UNAVAILABLE;
		} catch (Exception error) {
			KairokkClient.LOGGER.warn("Could not refresh Kairokk dashboard link", error);
			return RefreshResult.REJECTED;
		}
	}

	private static void writeSavedLink(Path path, JsonObject saved) throws IOException {
		Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
		Files.writeString(temporary, GSON.toJson(saved), StandardCharsets.UTF_8);
		try { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
		catch (java.nio.file.AtomicMoveNotSupportedException ignored) { Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING); }
	}

	private void heartbeat() {
		Minecraft client = Minecraft.getInstance();
		client.execute(() -> {
			JsonObject payload = new JsonObject();
			if (client.getCurrentServer() != null) payload.addProperty("server", client.getCurrentServer().ip); else payload.add("server", null);
			send("HEARTBEAT", UUID.randomUUID().toString(), payload);
		});
	}

	@Override public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
		try {
			JsonObject message = JsonParser.parseString(data.toString()).getAsJsonObject();
			if ("HELLO_ACK".equals(string(message, "type")) && ws == pendingSocket) {
				socket = ws;
				pendingSocket = null;
				retryAttempt = 0;
				reconnectScheduled.set(false);
				connectionStatus = "Connected";
				if (heartbeatStarted.compareAndSet(false, true)) scheduler.scheduleAtFixedRate(this::heartbeat, 2, 15, TimeUnit.SECONDS);
				KairokkClient.LOGGER.info("Connected Minecraft client to the Kairokk dashboard");
			} else if ("WORLD_CHUNK_ACK".equals(string(message, "type")) && ws == socket) {
				JsonObject payload = message.getAsJsonObject("payload");
				String key = string(payload, "key");
				long revision = payload.get("revision").getAsLong();
				Minecraft.getInstance().execute(() -> worldView.acknowledgeChunk(key, revision));
			} else if ("COMMAND".equals(string(message, "type")) && ws == socket) {
				handleCommand(message);
			}
		} catch (Exception error) {
			KairokkClient.LOGGER.warn("Rejected malformed dashboard message", error);
		}
		ws.request(1);
		return null;
	}

	private void handleCommand(JsonObject message) {
		String requestId = string(message, "requestId"); JsonObject payload = message.getAsJsonObject("payload"); String command = string(payload, "command");
		JsonObject data = payload.has("data") && payload.get("data").isJsonObject() ? payload.getAsJsonObject("data") : new JsonObject();
		if (!CAPABILITIES.contains(command)) { result(requestId, false, null, "COMMAND_NOT_SUPPORTED"); return; }
		Minecraft.getInstance().execute(() -> execute(requestId, command, data));
	}

	private void execute(String requestId, String command, JsonObject data) {
		try {
			Minecraft client = Minecraft.getInstance(); JsonObject value = new JsonObject();
			switch (command) {
				case "TOGGLE_HUD" -> { boolean enabled = !KairokkOverlayManager.get().rendererEnabled(); KairokkOverlayManager.get().setRendererEnabled(enabled); value.addProperty("enabled", enabled); }
				case "TOGGLE_MODULE" -> { String module = required(data, "module"); boolean enabled = data.has("enabled") && data.get("enabled").getAsBoolean(); if (!module.equalsIgnoreCase("ESP") && !module.equalsIgnoreCase("Item ESP")) throw new IllegalArgumentException("Only ESP is available in this client build"); KairokkItemEspState.setBoolean("itemEsp", enabled); value.addProperty("module", "ESP"); value.addProperty("enabled", enabled); }
				case "SEND_CHAT" -> { String text = required(data, "message").strip(); if (text.isEmpty() || text.length() > 256) throw new IllegalArgumentException("Chat message must be 1-256 characters"); if (client.player == null) throw new IllegalStateException("Player is not in a world"); client.player.connection.sendChat(text); value.addProperty("sent", true); }
				case "PATHFINDER_START" -> { if (client.player == null || client.level == null) throw new IllegalStateException("Player is not in a world"); if (client.player.isInWater()) throw new IllegalStateException("Cannot start pathfinding while touching water"); BlockPos target = data.has("x") ? new BlockPos(integer(data, "x"), integer(data, "y"), integer(data, "z")) : pathfinderGoal; if (target == null) throw new IllegalStateException("Set a pathfinding goal first"); pathfinderGoal = target; NavigationManager.getInstance().requestPathAndFollow(client, target); value.addProperty("started", true); }
				case "PATHFINDER_STOP" -> { NavigationManager.getInstance().stop(client); value.addProperty("stopped", true); }
				case "WORLD_SUBSCRIBE" -> { worldView.subscribe(data.has("radius") ? data.get("radius").getAsInt() : 4); value.addProperty("subscribed", true); }
				case "WORLD_UNSUBSCRIBE" -> { worldView.unsubscribe(); value.addProperty("subscribed", false); }
				case "WORLD_RESYNC_REQUEST" -> { worldView.subscribe(data.has("radius") ? data.get("radius").getAsInt() : 4); value.addProperty("resync", true); }
				case "PATHFINDER_SET_GOAL" -> { if (client.player == null || client.level == null) throw new IllegalStateException("Player is not in a world"); pathfinderGoal = new BlockPos(integer(data, "x"), integer(data, "y"), integer(data, "z")); value.addProperty("goalSet", true); }
				default -> throw new IllegalArgumentException("Unsupported command");
			}
			result(requestId, true, value, null);
		} catch (Exception error) { result(requestId, false, null, error.getMessage() == null ? "COMMAND_FAILED" : error.getMessage()); }
	}

	private void result(String requestId, boolean success, JsonObject value, String error) { JsonObject payload = new JsonObject(); payload.addProperty("success", success); if (value != null) payload.add("result", value); if (error != null) payload.addProperty("error", error); send("COMMAND_RESULT", requestId, payload); }
	private void sendEvent(String event, JsonObject data) { JsonObject payload = new JsonObject(); payload.addProperty("event", event); payload.add("data", data); send("EVENT", UUID.randomUUID().toString(), payload); }
	private void send(String type, String requestId, JsonObject payload) { WebSocket current = socket; if (current != null) send(current, type, requestId, payload).whenComplete((ignored, error) -> { if (error != null) current.abort(); }); }
	private CompletionStage<WebSocket> send(WebSocket target, String type, String requestId, JsonObject payload) {
		JsonObject envelope = new JsonObject(); envelope.addProperty("type", type); envelope.addProperty("requestId", requestId); envelope.addProperty("timestamp", Instant.now().toString()); envelope.add("payload", payload);
		String text = GSON.toJson(envelope);
		synchronized (sendLock) {
			CompletableFuture<WebSocket> next = sendTail.handle((ignored, error) -> null).thenCompose(ignored -> target.sendText(text, true)).toCompletableFuture();
			sendTail = next.handle((ignored, error) -> null);
			return next;
		}
	}
	private void closeCurrentSockets() { WebSocket active = socket; WebSocket pending = pendingSocket; socket = null; pendingSocket = null; Minecraft.getInstance().execute(worldView::disconnected); if (active != null) active.sendClose(WebSocket.NORMAL_CLOSURE, "reconnecting"); if (pending != null && pending != active) pending.abort(); }
	private boolean hasConnectionDetails() { return !blank(backend) && !blank(token) && !blank(fingerprint) && !blank(deviceId); }
	private static boolean looksUnauthorized(Throwable error) { String message = error == null ? "" : String.valueOf(error.getMessage()); return message.contains("401") || message.contains("403") || message.toLowerCase().contains("unauthorized"); }
	private static String required(JsonObject object, String key) { String value = string(object, key); if (blank(value)) throw new IllegalArgumentException("Missing " + key); return value; }
	private static int integer(JsonObject object, String key) { if (!object.has(key) || !object.get(key).isJsonPrimitive()) throw new IllegalArgumentException("Missing " + key); return object.get(key).getAsInt(); }
	private static String string(JsonObject object, String key) { return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null; }
	private static boolean blank(String value) { return value == null || value.isBlank(); }
	private static Path dashboardLinkPath() { String base = System.getenv("LOCALAPPDATA"); if (blank(base)) base = Path.of(System.getProperty("user.home"), "AppData", "Local").toString(); return Path.of(base, "Kairokk", "dashboard-link.json"); }
	@Override public void onOpen(WebSocket ws) { ws.request(1); }
	@Override public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) { if (ws == socket || ws == pendingSocket) { socket = null; pendingSocket = null; Minecraft.getInstance().execute(worldView::disconnected); long generation = connectionGeneration.get(); scheduler.execute(() -> scheduleReconnect(generation, "Dashboard disconnected")); } return null; }
	@Override public void onError(WebSocket ws, Throwable error) { if (ws == socket || ws == pendingSocket) { socket = null; pendingSocket = null; Minecraft.getInstance().execute(worldView::disconnected); long generation = connectionGeneration.get(); scheduler.execute(() -> connectionFailed(generation, "Dashboard connection failed", error)); } }

	private enum RefreshResult { SUCCESS, UNAVAILABLE, REJECTED, NO_LINK }
}
