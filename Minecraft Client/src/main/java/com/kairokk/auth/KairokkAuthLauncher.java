package com.kairokk.auth;

import com.google.gson.JsonParser;
import net.raphimc.minecraftauth.MinecraftAuth;
import net.raphimc.minecraftauth.java.JavaAuthManager;
import net.raphimc.minecraftauth.java.model.MinecraftProfile;
import net.raphimc.minecraftauth.java.model.MinecraftToken;
import net.raphimc.minecraftauth.msa.model.MsaDeviceCode;
import net.raphimc.minecraftauth.msa.service.impl.DeviceCodeMsaAuthService;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;
import java.util.function.Consumer;

/** Performs Microsoft device login for the authenticated development launcher. */
public final class KairokkAuthLauncher {
	private KairokkAuthLauncher() { }

	public static void main(String[] args) throws Exception {
		if (args.length != 1) throw new IllegalArgumentException("Expected the launch-argument output path");
		Path launchArgs = Path.of(args[0]);
		Path tokenFile = tokenFile();
		Files.createDirectories(tokenFile.getParent());

		var httpClient = MinecraftAuth.createHttpClient("Kairokk Development Client");
		JavaAuthManager authManager = load(httpClient, tokenFile);
		if (authManager == null) authManager = login(httpClient);
		attachPersistence(authManager, tokenFile);

		MinecraftProfile profile = authManager.getMinecraftProfile().getUpToDate();
		MinecraftToken token = authManager.getMinecraftToken().getUpToDate();
		Properties properties = new Properties();
		properties.setProperty("username", profile.getName());
		properties.setProperty("uuid", profile.getId().toString());
		properties.setProperty("accessToken", token.getToken());
		properties.setProperty("version", "26.1.2");
		if (authManager.getXboxUserProfile().hasValue()) {
			String xuid = authManager.getXboxUserProfile().getUpToDate().getId();
			if (xuid != null && !xuid.isBlank()) properties.setProperty("xuid", xuid);
		}
		Files.createDirectories(launchArgs.toAbsolutePath().getParent());
		try (var output = Files.newOutputStream(launchArgs, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
			properties.store(output, "Kairokk authenticated development profile");
		}
		System.out.println("Authenticated Minecraft profile: " + profile.getName());
	}

	private static JavaAuthManager load(net.lenni0451.commons.httpclient.HttpClient httpClient, Path tokenFile) {
		if (!Files.isRegularFile(tokenFile)) return null;
		try {
			JavaAuthManager manager = JavaAuthManager.fromJson(httpClient,
				JsonParser.parseString(Files.readString(tokenFile, StandardCharsets.UTF_8)).getAsJsonObject());
			// Validate the Java profile as well as the refresh token. This makes it
			// possible to switch accounts if the cached Microsoft account has no
			// Minecraft Java entitlement.
			manager.getMinecraftToken().getUpToDate();
			manager.getMinecraftProfile().getUpToDate();
			return manager;
		} catch (Exception ignored) {
			System.out.println("Saved Minecraft login expired; starting device login again.");
			return null;
		}
	}

	private static JavaAuthManager login(net.lenni0451.commons.httpclient.HttpClient httpClient) throws Exception {
		System.out.println("Opening Microsoft device login...");
		Consumer<MsaDeviceCode> callback = code -> {
			System.out.println("Microsoft login URL: " + code.getDirectVerificationUri());
			System.out.println("If it did not open automatically, visit " + code.getVerificationUri() + " and enter " + code.getUserCode());
			try {
				if (Desktop.isDesktopSupported()) Desktop.getDesktop().browse(URI.create(code.getDirectVerificationUri()));
			} catch (Exception ignored) { }
		};
		return JavaAuthManager.create(httpClient).login(
			(http, config, consumer) -> new DeviceCodeMsaAuthService(http, config, consumer), callback);
	}

	private static void attachPersistence(JavaAuthManager manager, Path tokenFile) {
		manager.getChangeListeners().add(() -> {
			try {
				Files.writeString(tokenFile, JavaAuthManager.toJson(manager).toString(), StandardCharsets.UTF_8,
						StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
			} catch (IOException exception) {
				throw new RuntimeException("Could not save the Kairokk Microsoft login", exception);
			}
		});
		try {
			Files.writeString(tokenFile, JavaAuthManager.toJson(manager).toString(), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException exception) {
			throw new RuntimeException("Could not save the Kairokk Microsoft login", exception);
		}
	}

	private static Path tokenFile() {
		String localAppData = System.getenv("LOCALAPPDATA");
		if (localAppData == null || localAppData.isBlank()) {
			throw new IllegalStateException("LOCALAPPDATA is not available; refusing to store account credentials in the project");
		}
		return Path.of(localAppData, "Kairokk", "minecraft-auth.json");
	}
}
