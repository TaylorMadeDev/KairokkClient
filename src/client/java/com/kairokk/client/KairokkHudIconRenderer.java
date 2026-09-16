package com.kairokk.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/** Draws Kairokk's mapped icons in Minecraft render layers such as loading overlays. */
public final class KairokkHudIconRenderer {
	private static final int SOURCE_SIZE = 640;

	private KairokkHudIconRenderer() {
	}

	public static void draw(GuiGraphicsExtractor graphics, ShulkIcon icon, int x, int y, int size, int argbTint) {
		if (graphics == null || icon == null || size <= 0) return;
		graphics.blit(RenderPipelines.GUI_TEXTURED, icon.resource(), x, y, 0, 0,
			size, size, SOURCE_SIZE, SOURCE_SIZE, SOURCE_SIZE, SOURCE_SIZE, argbTint);
	}
}
