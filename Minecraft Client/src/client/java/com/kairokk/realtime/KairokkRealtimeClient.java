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

import java.net.URI;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** Authenticated website bridge. All game mutations run on Minecraft's main thread. */
public final class KairokkRealtimeClient implements WebSocket.Listener {
	private static final Gson GSON = new Gson();
	private static final List<String> CAPABILITIES = List.of("TOGGLE_MODULE", "SEND_CHAT", "TOGGLE_HUD", "PATHFINDER_START", "PATHFINDER_STOP", "WORLD_SUBSCRIBE", "WORLD_UNSUBSCRIBE", "WORLD_RESYNC_REQUEST", "PATHFINDER_SET_GOAL");
	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> { Thread t = new Thread(r, "kairokk-realtime"); t.setDaemon(true); return t; });
	private final AtomicBoolean reconnectScheduled = new AtomicBoolean();
	private final AtomicBoolean heartbeatStarted = new AtomicBoolean();
	private WebSocket socket;
	private BlockPos pathfinderGoal;
	private String backend, token, fingerprint, deviceId;
	private volatile String connectionStatus = "Not linked";
	private final WorldViewService worldView = new WorldViewService(this::sendEvent);
	public void tick(Minecraft client) { worldView.tick(client); }

	public void connectFromLaunchArguments() {
		backend = System.getProperty("kairokk.backend"); token = System.getProperty("kairokk.clientAccessToken"); fingerprint = System.getProperty("kairokk.deviceFingerprintHash"); deviceId = System.getProperty("kairokk.deviceId");
		// A development launch can carry an old 15-minute access token. Prefer the
		// durable local refresh-link whenever it exists, so normal restarts are safe.
		loadSavedLink();
		if (blank(backend) || blank(token) || blank(fingerprint) || blank(deviceId)) {
			connectionStatus = "Not linked";
			KairokkClient.LOGGER.info("Dashboard is not linked. Set KAIROKK_DASHBOARD_EMAIL and KAIROKK_DASHBOARD_PASSWORD for one authenticated launch.");
			return;
		}
		connect();
	}
	public String connectionStatus() { return connectionStatus; }
	public boolean isConnected() { return socket != null && "Connected".equals(connectionStatus); }
	public void reconnect() { scheduler.execute(() -> { connectionStatus = "Reconnecting…"; socket = null; loadSavedLink(); if (!blank(backend) && !blank(token) && !blank(fingerprint) && !blank(deviceId)) connect(); }); }

	private void loadSavedLink() {
		try {
			Path path = Path.of(System.getenv("LOCALAPPDATA"), "Kairokk", "dashboard-link.json"); if (!Files.isRegularFile(path)) return;
			JsonObject saved = JsonParser.parseString(Files.readString(path)).getAsJsonObject(); backend = saved.get("backend").getAsString(); fingerprint = saved.get("deviceFingerprintHash").getAsString(); deviceId = saved.get("deviceId").getAsString();
			JsonObject body = new JsonObject(); body.addProperty("refreshToken", saved.get("refreshToken").getAsString());
			HttpURLConnection connection = (HttpURLConnection) URI.create(backend + "/auth/refresh").toURL().openConnection(); connection.setRequestMethod("POST"); connection.setConnectTimeout(8000); connection.setReadTimeout(12000); connection.setRequestProperty("Content-Type", "application/json"); connection.setDoOutput(true); byte[] bytes = GSON.toJson(body).getBytes(StandardCharsets.UTF_8); connection.getOutputStream().write(bytes);
			if (connection.getResponseCode() / 100 != 2) throw new IllegalStateException("refresh rejected"); JsonObject refreshed = JsonParser.parseString(new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject(); token = refreshed.get("accessToken").getAsString(); saved.addProperty("refreshToken", refreshed.get("refreshToken").getAsString()); Files.writeString(path, GSON.toJson(saved));
		} catch (Exception error) { connectionStatus = "Link expired — reconnect from launcher"; KairokkClient.LOGGER.warn("Could not refresh dashboard link", error); }
	}

	private void connect() {
		connectionStatus = "Connecting…";
		URI uri = URI.create(backend.replaceFirst("^http", "ws") + "/client/connect?deviceFingerprintHash=" + URLEncoder.encode(fingerprint, StandardCharsets.UTF_8));
		HttpClient.newHttpClient().newWebSocketBuilder().header("Authorization", "Bearer " + token).buildAsync(uri, this)
				.thenAccept(ws -> {
					JsonObject hello = new JsonObject();
					hello.addProperty("protocolVersion", 1); hello.addProperty("clientVersion", "1.0.0"); hello.addProperty("minecraftVersion", "26.1.2"); hello.addProperty("deviceId", deviceId); hello.add("capabilities", GSON.toJsonTree(CAPABILITIES));
					send(ws, "HELLO", UUID.randomUUID().toString(), hello).thenRun(() -> {
						reconnectScheduled.set(false);
					});
				}).exceptionally(error -> { connectionStatus = "Connection failed — retrying"; KairokkClient.LOGGER.warn("Dashboard connection failed; retrying", error); scheduleReconnect(); return null; });
	}

	private void scheduleReconnect() { if (reconnectScheduled.compareAndSet(false, true)) scheduler.schedule(() -> { reconnectScheduled.set(false); if (socket == null) connect(); }, 5, TimeUnit.SECONDS); }

	private void heartbeat() {
		Minecraft client = Minecraft.getInstance();
		client.execute(() -> { JsonObject payload = new JsonObject(); if (client.getCurrentServer() != null) payload.addProperty("server", client.getCurrentServer().ip); else payload.add("server", null); send("HEARTBEAT", UUID.randomUUID().toString(), payload); });
	}

	@Override public CompletionStage<?> onText(WebSocket ws, CharSequence data, boolean last) {
		try { JsonObject message = JsonParser.parseString(data.toString()).getAsJsonObject(); if ("HELLO_ACK".equals(string(message, "type"))) { socket = ws; connectionStatus = "Connected"; if (heartbeatStarted.compareAndSet(false, true)) scheduler.scheduleAtFixedRate(this::heartbeat, 2, 15, TimeUnit.SECONDS); KairokkClient.LOGGER.info("Connected Minecraft client to the Kairokk dashboard"); } else if ("COMMAND".equals(string(message, "type"))) handleCommand(message); }
		catch (Exception error) { KairokkClient.LOGGER.warn("Rejected malformed dashboard message", error); }
		ws.request(1); return null;
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
				case "PATHFINDER_START" -> { if (client.player == null || client.level == null) throw new IllegalStateException("Player is not in a world"); BlockPos target = data.has("x") ? new BlockPos(integer(data, "x"), integer(data, "y"), integer(data, "z")) : pathfinderGoal; if (target == null) throw new IllegalStateException("Set a pathfinding goal first"); pathfinderGoal = target; NavigationManager.getInstance().requestPath(client, target); value.addProperty("started", true); }
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
	private void sendEvent(String event, JsonObject data) { JsonObject payload=new JsonObject();payload.addProperty("event",event);payload.add("data",data);send("EVENT",UUID.randomUUID().toString(),payload); }
	private void send(String type, String requestId, JsonObject payload) { WebSocket current = socket; if (current != null) send(current, type, requestId, payload); }
	private CompletionStage<WebSocket> send(WebSocket target, String type, String requestId, JsonObject payload) { JsonObject envelope = new JsonObject(); envelope.addProperty("type", type); envelope.addProperty("requestId", requestId); envelope.addProperty("timestamp", Instant.now().toString()); envelope.add("payload", payload); return target.sendText(GSON.toJson(envelope), true); }
	private static String required(JsonObject object, String key) { String value = string(object, key); if (blank(value)) throw new IllegalArgumentException("Missing " + key); return value; }
	private static int integer(JsonObject object, String key) { if (!object.has(key) || !object.get(key).isJsonPrimitive()) throw new IllegalArgumentException("Missing " + key); return object.get(key).getAsInt(); }
	private static String string(JsonObject object, String key) { return object != null && object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : null; }
	private static boolean blank(String value) { return value == null || value.isBlank(); }
	@Override public void onOpen(WebSocket ws) { ws.request(1); }
	@Override public CompletionStage<?> onClose(WebSocket ws, int statusCode, String reason) { if (socket == ws) { socket = null; worldView.disconnected(); } connectionStatus = "Disconnected — retrying"; KairokkClient.LOGGER.info("Kairokk dashboard disconnected: {}; reconnecting", reason); scheduleReconnect(); return null; }
	@Override public void onError(WebSocket ws, Throwable error) { if (socket == ws) { socket = null; worldView.disconnected(); } connectionStatus = "Connection failed — retrying"; KairokkClient.LOGGER.warn("Kairokk dashboard socket error; reconnecting", error); scheduleReconnect(); }
}
