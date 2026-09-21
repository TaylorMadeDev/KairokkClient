package com.kairokk;

import com.kairokk.client.ShulkTitleScreen;
import com.kairokk.client.KairokkBackendGateScreen;
import com.kairokk.client.TitleOrbRenderer;
import com.kairokk.client.KairokkUiSounds;
import com.kairokk.client.KairokkClickGuiScreen;
import com.kairokk.client.KairokkCustomizationState;
import com.kairokk.client.pathfinder.commands.PathfindCommand;
import com.kairokk.client.pathfinder.pathfinding.NavigationManager;
import com.kairokk.client.pathfinder.pathfinding.render.PathRenderer;
import com.kairokk.realtime.KairokkRealtimeClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Main client entrypoint for Kairokk. */
public final class KairokkClient implements ClientModInitializer {
	public static final String MOD_ID = "kairokk";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private String appliedWindowTitle;
	private KeyMapping clickGuiKey;
	private static KairokkRealtimeClient realtime;
	private static final Identifier OVERLAY_ID = Identifier.fromNamespaceAndPath(MOD_ID, "overlay");

	@Override
	public void onInitializeClient() {
		TitleOrbRenderer.initialize();
		KairokkUiSounds.initialize();
		HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT, OVERLAY_ID, com.kairokk.client.KairokkOverlayRenderer::render);
		PathfindCommand.register();
		ClientTickEvents.END_CLIENT_TICK.register(NavigationManager.getInstance()::tick);
		PathRenderer pathRenderer = PathRenderer.getInstance();
		LevelRenderEvents.END_EXTRACTION.register(pathRenderer::extract);
		LevelRenderEvents.AFTER_TRANSLUCENT_TERRAIN.register(pathRenderer::draw);
		clickGuiKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.kairokk.click_gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_U, KeyMapping.Category.MISC));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.getWindow() != null) {
				String desiredTitle = KairokkCustomizationState.nativeTitleBar() ? "Minecraft" : "Kairokk Client";
				if (!desiredTitle.equals(appliedWindowTitle)) {
					client.getWindow().setTitle(desiredTitle);
					appliedWindowTitle = desiredTitle;
				}
			}
			if (client.screen instanceof TitleScreen && (realtime == null || !realtime.isConnected())) client.setScreen(new KairokkBackendGateScreen());
			else if (client.screen instanceof TitleScreen || client.screen instanceof KairokkBackendGateScreen) {
				if (realtime != null && realtime.isConnected()) client.setScreen(new ShulkTitleScreen());
			}
			while (clickGuiKey.consumeClick()) {
				if (!(client.screen instanceof KairokkClickGuiScreen)) client.setScreen(new KairokkClickGuiScreen(client.screen));
			}
		});
		LOGGER.info("Kairokk client initialized");
		realtime = new KairokkRealtimeClient();
		ClientTickEvents.END_CLIENT_TICK.register(realtime::tick);
		realtime.connectFromLaunchArguments();
	}
	public static KairokkRealtimeClient realtime() { return realtime; }
}
