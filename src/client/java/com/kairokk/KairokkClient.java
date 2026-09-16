package com.kairokk;

import com.kairokk.client.ShulkTitleScreen;
import com.kairokk.client.TitleOrbRenderer;
import com.kairokk.client.KairokkUiSounds;
import com.kairokk.client.KairokkClickGuiScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.TitleScreen;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main client entrypoint for Kairokk. */
public final class KairokkClient implements ClientModInitializer {
	public static final String MOD_ID = "kairokk";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private boolean windowTitleSet;
	private KeyMapping clickGuiKey;

	@Override
	public void onInitializeClient() {
		TitleOrbRenderer.initialize();
		KairokkUiSounds.initialize();
		clickGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.kairokk.client_gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_LEFT_SHIFT, KeyMapping.Category.MISC));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (!windowTitleSet && client.getWindow() != null) {
				client.getWindow().setTitle("Kairokk Client");
				windowTitleSet = true;
			}
			if (client.screen instanceof TitleScreen) {
				client.setScreen(new ShulkTitleScreen());
			}
			while (clickGuiKey.consumeClick()) {
				if (!(client.screen instanceof KairokkClickGuiScreen)) client.setScreen(new KairokkClickGuiScreen(client.screen));
			}
		});
		LOGGER.info("Kairokk client initialized");
	}
}
