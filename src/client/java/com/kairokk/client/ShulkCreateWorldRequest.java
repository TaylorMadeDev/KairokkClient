package com.kairokk.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.Difficulty;

/** Bridges the custom setup UI to Minecraft's complete world creation pipeline. */
public final class ShulkCreateWorldRequest {
	private static Request pending;

	private ShulkCreateWorldRequest() {
	}

	public static void create(Screen returnScreen, String name, String seed,
			WorldCreationUiState.SelectedGameMode gameMode, Difficulty difficulty,
			boolean commands, boolean structures, boolean bonusChest) {
		pending = new Request(name, seed, gameMode, difficulty, commands, structures, bonusChest);
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
		Difficulty difficulty, boolean commands, boolean structures, boolean bonusChest) {
	}
}
