package com.kairokk.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Downloads pinned, compatible client add-ons from Modrinth's public API. */
final class KairokkModrinthDownloader {
	private static final String API = "https://api.modrinth.com/v2";
	private static final String USER_AGENT = "Kairokk/0.1.0 (client add-ons)";
	private static final String GAME_VERSION = "26.1.2";
	private static final String LOADER = "fabric";
	private static final String PACK_DISABLER = "packdisabler-for-hypixel-skyblock";
	private static final long MAX_FILE_BYTES = 100L * 1024L * 1024L;

	private KairokkModrinthDownloader() { }

	static boolean isPackDisablerInstalled(Path gameDirectory) {
		return hasJar(gameDirectory.resolve("mods"), "packdisabler-");
	}

	static CompletableFuture<DownloadResult> downloadPackDisabler(Path gameDirectory) {
		return downloadPackDisabler(gameDirectory, progress -> { });
	}

	static CompletableFuture<DownloadResult> downloadPackDisabler(Path gameDirectory, Consumer<DownloadProgress> progress) {
		return CompletableFuture.supplyAsync(() -> {
			try {
				Path modsDirectory = gameDirectory.resolve("mods");
				Files.createDirectories(modsDirectory);
				HttpClient client = HttpClient.newBuilder()
						.followRedirects(HttpClient.Redirect.NORMAL)
						.connectTimeout(Duration.ofSeconds(15))
						.build();

				JsonObject packVersion = latestVersion(client, PACK_DISABLER);
				List<String> installed = new ArrayList<>();
				installFile(client, packVersion, modsDirectory, installed, progress);
				JsonArray dependencies = packVersion.getAsJsonArray("dependencies");
				for (JsonElement dependencyElement : dependencies) {
					JsonObject dependency = dependencyElement.getAsJsonObject();
					if (!"required".equals(dependency.get("dependency_type").getAsString())) continue;
					String projectId = dependency.get("project_id").getAsString();
					JsonObject dependencyVersion = latestVersion(client, projectId);
					String slug = dependencyVersion.get("project_id").getAsString();
					if (hasProjectJar(modsDirectory, projectId, slug)) continue;
					installFile(client, dependencyVersion, modsDirectory, installed, progress);
				}
				return new DownloadResult(true, installed.isEmpty() ? "PackDisabler is already installed." : "Downloaded " + String.join(", ", installed) + ". Restart required.");
			} catch (Exception exception) {
				return new DownloadResult(false, userMessage(exception));
			}
		});
	}

	private static JsonObject latestVersion(HttpClient client, String project) throws IOException, InterruptedException {
		String query = "/project/" + project + "/version?loaders=%5B%22fabric%22%5D&game_versions=%5B%22" + GAME_VERSION + "%22%5D&include_changelog=false";
		JsonArray versions = get(client, API + query).getAsJsonArray();
		for (JsonElement element : versions) {
			JsonObject version = element.getAsJsonObject();
			if ("release".equals(version.get("version_type").getAsString()) && "listed".equals(version.get("status").getAsString())) return version;
		}
		throw new IOException("No compatible Fabric " + GAME_VERSION + " release was found on Modrinth for " + project);
	}

	private static void installFile(HttpClient client, JsonObject version, Path modsDirectory, List<String> installed,
			Consumer<DownloadProgress> progress) throws IOException, InterruptedException {
		JsonObject file = null;
		for (JsonElement element : version.getAsJsonArray("files")) {
			JsonObject candidate = element.getAsJsonObject();
			if (candidate.get("primary").getAsBoolean()) { file = candidate; break; }
			if (file == null) file = candidate;
		}
		if (file == null) throw new IOException("Modrinth returned no download file for " + version.get("name").getAsString());
		String filename = file.get("filename").getAsString();
		Path destination = modsDirectory.resolve(filename).normalize();
		if (!destination.getParent().equals(modsDirectory)) throw new IOException("Unsafe Modrinth filename: " + filename);
		if (Files.isRegularFile(destination)) return;
		Path temporary = Files.createTempFile(modsDirectory, ".kairokk-download-", ".part");
		try {
			HttpResponse<InputStream> response = client.send(HttpRequest.newBuilder(URI.create(file.get("url").getAsString()))
					.timeout(Duration.ofMinutes(2)).header("User-Agent", USER_AGENT).build(), HttpResponse.BodyHandlers.ofInputStream());
			if (response.statusCode() != 200) throw new IOException("Modrinth download failed (HTTP " + response.statusCode() + ")");
			try (InputStream input = response.body(); var output = Files.newOutputStream(temporary, StandardOpenOption.TRUNCATE_EXISTING)) {
				byte[] buffer = new byte[8192];
				long total = 0;
				long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
				int read;
				progress.accept(new DownloadProgress(filename, 0L, contentLength));
				while ((read = input.read(buffer)) != -1) {
					total += read;
					if (total > MAX_FILE_BYTES) throw new IOException("Modrinth file is unexpectedly large");
					output.write(buffer, 0, read);
					if (total == read || total % (64 * 1024) < read || (contentLength > 0 && total >= contentLength)) {
						progress.accept(new DownloadProgress(filename, total, contentLength));
					}
				}
			}
			progress.accept(new DownloadProgress(filename, Files.size(temporary), Files.size(temporary)));
			String expectedSha512 = file.getAsJsonObject("hashes").get("sha512").getAsString();
			String actualSha512 = sha512(temporary);
			if (!expectedSha512.equalsIgnoreCase(actualSha512)) throw new IOException("Modrinth checksum verification failed for " + filename);
			try {
				Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
				Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING);
			}
			installed.add(filename);
		} finally {
			Files.deleteIfExists(temporary);
		}
	}

	private static JsonElement get(HttpClient client, String url) throws IOException, InterruptedException {
		HttpResponse<String> response = client.send(HttpRequest.newBuilder(URI.create(url))
				.timeout(Duration.ofSeconds(30)).header("User-Agent", USER_AGENT).header("Accept", "application/json").build(), HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) throw new IOException("Modrinth API request failed (HTTP " + response.statusCode() + ")");
		return JsonParser.parseString(response.body());
	}

	private static boolean hasProjectJar(Path modsDirectory, String projectId, String ignoredProjectSlug) throws IOException {
		if (!Files.isDirectory(modsDirectory)) return false;
		String marker = switch (projectId) {
			case "P7dR8mSH" -> "fabric-api";
			case "Ha28R6CL" -> "fabric-language-kotlin";
			default -> ignoredProjectSlug;
		};
		try (var files = Files.list(modsDirectory)) {
			return files.anyMatch(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".jar")
					&& path.getFileName().toString().toLowerCase(Locale.ROOT).contains(marker.toLowerCase(Locale.ROOT)));
		}
	}

	private static boolean hasJar(Path modsDirectory, String prefix) {
		if (!Files.isDirectory(modsDirectory)) return false;
		try (var files = Files.list(modsDirectory)) {
			return files.anyMatch(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).startsWith(prefix));
		} catch (IOException ignored) {
			return false;
		}
	}

	private static String sha512(Path file) throws IOException {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-512");
			try (InputStream input = Files.newInputStream(file)) {
				byte[] buffer = new byte[8192];
				int read;
				while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
			}
			return HexFormat.of().formatHex(digest.digest());
		} catch (java.security.NoSuchAlgorithmException exception) {
			throw new IOException("SHA-512 is unavailable", exception);
		}
	}

	private static String userMessage(Exception exception) {
		String message = exception.getMessage();
		return message == null || message.isBlank() ? "Could not download the add-on." : message;
	}

	record DownloadResult(boolean success, String message) { }
	record DownloadProgress(String filename, long downloadedBytes, long totalBytes) {
		double fraction() {
			return totalBytes <= 0L ? 0D : Math.max(0D, Math.min(1D, downloadedBytes / (double) totalBytes));
		}
	}
}
