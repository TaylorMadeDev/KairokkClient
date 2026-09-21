package com.kairokk.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.levelgen.presets.WorldPresets;

/** Bridges the custom setup UI to Minecraft's complete world creation pipeline. */
public final class ShulkCreateWorldRequest {
	private static Request pending;

	private ShulkCreateWorldRequest() {
	}

	public static void create(Screen returnScreen, String name, String seed,
			WorldCreationUiState.SelectedGameMode gameMode, Difficulty difficulty,
			boolean commands, boolean structures, boolean bonusChest, WorldType worldType) {
		pending = new Request(name, seed, gameMode, difficulty, commands, structures, bonusChest, worldType);
		Minecraft minecraft = Minecraft.getInstance();
		CreateWorldScreen.openFresh(minecraft, () -> {
			pending = null;
			minecraft.setScreen(returnScreen);
		});
	}

	public static Request take() {
		Request request = pending;
		pending = null;
		return request;
	}

	public record Request(String name, String seed, WorldCreationUiState.SelectedGameMode gameMode,
		Difficulty difficulty, boolean commands, boolean structures, boolean bonusChest, WorldType worldType) {
	}

	public enum WorldType {
		NORMAL("Default", "Normal terrain", WorldPresets.NORMAL),
		FLAT("Superflat", "A level world built from flat layers", WorldPresets.FLAT),
		LARGE_BIOMES("Large Biomes", "Biomes generate at a much larger scale", WorldPresets.LARGE_BIOMES),
		AMPLIFIED("Amplified", "Extreme terrain with towering mountains", WorldPresets.AMPLIFIED),
		SINGLE_BIOME_SURFACE("Single Biome", "Generate the world with one surface biome", WorldPresets.SINGLE_BIOME_SURFACE);

		private final String displayName;
		private final String description;
		private final net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.presets.WorldPreset> preset;

		WorldType(String displayName, String description,
				net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.presets.WorldPreset> preset) {
			this.displayName = displayName;
			this.description = description;
			this.preset = preset;
		}

		public String displayName() {
			return displayName;
		}

		public String description() {
			return description;
		}

		public net.minecraft.resources.ResourceKey<net.minecraft.world.level.levelgen.presets.WorldPreset> preset() {
			return preset;
		}
	}
}
