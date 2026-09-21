package com.kairokk.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Persisted preference used by the settings hub to choose Shulk or vanilla option pages. */
public final class ShulkUiState {
	private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("kairokk.properties");
	private static boolean customScreensEnabled = loadCustomScreensEnabled();

	private ShulkUiState() {}

	public static boolean customScreensEnabled() {
		return customScreensEnabled;
	}

	public static boolean toggleCustomScreens() {
		customScreensEnabled = !customScreensEnabled;
		saveCustomScreensEnabled();
		return customScreensEnabled;
	}

	private static boolean loadCustomScreensEnabled() {
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
			properties.load(reader);
			return Boolean.parseBoolean(properties.getProperty("customScreensEnabled", "true"));
		} catch (IOException ignored) {
			return true;
		}
	}

	private static void saveCustomScreensEnabled() {
		Properties properties = new Properties();
		properties.setProperty("customScreensEnabled", Boolean.toString(customScreensEnabled));
		try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
			properties.store(writer, "Kairokk client preferences");
		} catch (IOException ignored) {
			// The UI remains usable even if the local preference cannot be written.
		}
	}
}
