package com.kairokk.auth;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.InetAddress;
import java.net.URI;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Properties;

/** Links the development client to a Kairokk web account without storing its password. */
public final class KairokkDashboardLinkLauncher {
	private static final Gson GSON = new Gson();

	private KairokkDashboardLinkLauncher() { }

	public static void main(String[] args) throws Exception {
		if (args.length != 1) throw new IllegalArgumentException("Expected the launch-property output path");
		Path output = Path.of(args[0]);
		Path savedPath = savedLinkPath();
		Files.createDirectories(savedPath.getParent());
		SavedLink saved = load(savedPath);
		String backend = env("KAIROKK_BACKEND", saved == null ? "http://127.0.0.1:3000" : saved.backend);
		String fingerprint = saved == null ? randomFingerprint() : saved.deviceFingerprintHash;

		Tokens tokens = saved == null ? null : refresh(backend, saved.refreshToken);
		if (tokens == null) {
			String email = System.getenv("KAIROKK_DASHBOARD_EMAIL");
			String password = System.getenv("KAIROKK_DASHBOARD_PASSWORD");
			if (blank(email) || blank(password)) {
				Files.deleteIfExists(output);
				System.out.println("Dashboard link skipped. Set KAIROKK_DASHBOARD_EMAIL and KAIROKK_DASHBOARD_PASSWORD once, then launch again.");
				return;
			}
			tokens = login(backend, email, password);
		}

		JsonObject device = registerDevice(backend, tokens.accessToken, fingerprint);
		String deviceId = device.get("id").getAsString();
		SavedLink updated = new SavedLink(backend, tokens.refreshToken, fingerprint, deviceId);
		Files.writeString(savedPath, GSON.toJson(updated), StandardCharsets.UTF_8,
				StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

		Properties properties = new Properties();
		properties.setProperty("backend", backend);
		properties.setProperty("clientAccessToken", tokens.accessToken);
		properties.setProperty("deviceFingerprintHash", fingerprint);
		properties.setProperty("deviceId", deviceId);
		Files.createDirectories(output.toAbsolutePath().getParent());
		try (var stream = Files.newOutputStream(output, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			properties.store(stream, "Short-lived Kairokk dashboard link");
		}
		System.out.println("Linked Minecraft client device " + deviceId + " to the Kairokk dashboard.");
	}

	private static Tokens login(String backend, String email, String password) throws Exception {
		JsonObject body = new JsonObject(); body.addProperty("email", email); body.addProperty("password", password);
		return tokens(post(backend + "/auth/login", body, null));
	}

	private static Tokens refresh(String backend, String refreshToken) {
		if (blank(refreshToken)) return null;
		try {
			JsonObject body = new JsonObject(); body.addProperty("refreshToken", refreshToken);
			return tokens(post(backend + "/auth/refresh", body, null));
		} catch (Exception ignored) { return null; }
	}

	private static JsonObject registerDevice(String backend, String accessToken, String fingerprint) throws Exception {
		JsonObject body = new JsonObject();
		body.addProperty("deviceFingerprintHash", fingerprint);
		body.addProperty("deviceName", hostName() + " Minecraft Client");
		body.addProperty("platform", System.getProperty("os.name", "Desktop"));
		return post(backend + "/devices/register", body, accessToken).getAsJsonObject("device");
	}

	private static JsonObject post(String url, JsonObject body, String bearer) throws Exception {
		byte[] encodedBody = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
		HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
		connection.setConnectTimeout((int) Duration.ofSeconds(8).toMillis()); connection.setReadTimeout((int) Duration.ofSeconds(12).toMillis());
		connection.setRequestMethod("POST"); connection.setRequestProperty("Content-Type", "application/json; charset=utf-8"); connection.setFixedLengthStreamingMode(encodedBody.length); connection.setDoOutput(true);
		if (!blank(bearer)) connection.setRequestProperty("Authorization", "Bearer " + bearer);
		try (var output = connection.getOutputStream()) { output.write(encodedBody); }
		int status = connection.getResponseCode(); var stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream(); String response = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		if (status < 200 || status >= 300) throw new IllegalStateException("Kairokk server returned " + status + ": " + response);
		return JsonParser.parseString(response).getAsJsonObject();
	}

	private static Tokens tokens(JsonObject json) { return new Tokens(json.get("accessToken").getAsString(), json.get("refreshToken").getAsString()); }
	private static SavedLink load(Path path) { try { return Files.isRegularFile(path) ? GSON.fromJson(Files.readString(path), SavedLink.class) : null; } catch (Exception ignored) { return null; } }
	private static String randomFingerprint() { byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes); return HexFormat.of().formatHex(bytes); }
	private static String hostName() { try { return InetAddress.getLocalHost().getHostName(); } catch (Exception ignored) { return "Kairokk"; } }
	private static String env(String key, String fallback) { String value = System.getenv(key); return blank(value) ? fallback : value.strip().replaceAll("/+$", ""); }
	private static boolean blank(String value) { return value == null || value.isBlank(); }
	private static Path savedLinkPath() { String base = System.getenv("LOCALAPPDATA"); if (blank(base)) throw new IllegalStateException("LOCALAPPDATA is required"); return Path.of(base, "Kairokk", "dashboard-link.json"); }
	private record Tokens(String accessToken, String refreshToken) { }
	private record SavedLink(String backend, String refreshToken, String deviceFingerprintHash, String deviceId) { }
}
