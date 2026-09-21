package com.kairokk.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Kairokk's branded initial resource-loading overlay. */
public final class KairokkLoadingRenderer {
	private static final int ACCENT = 0xFF3D8BFF;

	private KairokkLoadingRenderer() {
	}

	public static void render(GuiGraphicsExtractor graphics, float progress, String status) {
		int width = graphics.guiWidth();
		int height = graphics.guiHeight();
		int centerX = width / 2;
		int centerY = height / 2;
		TitleBackgroundRenderer.render(graphics, width, height);

		var pose = graphics.pose();
		KairokkHudIconRenderer.draw(graphics, ShulkIcon.BRAND,
			centerX - 34, centerY - 84, 68, ACCENT);
		pose.pushMatrix();
		pose.translate(centerX, centerY + 24);
		pose.scale(1.65F, 1.65F);
		graphics.centeredText(Minecraft.getInstance().font, "Welcome to Kairokk", 0, 0, 0xFFF4F0F2);
		pose.popMatrix();
		graphics.centeredText(Minecraft.getInstance().font, status, centerX, centerY + 62, 0xFFAAA5AE);

		int barWidth = Math.min(Math.max(220, width / 3), 400);
		int barHeight = 6;
		int left = centerX - barWidth / 2;
		int top = Math.round(height * 0.83F);
		graphics.fill(left, top, left + barWidth, top + barHeight, 0xFF37343A);
		graphics.fill(left, top, left + Math.round(barWidth * progress), top + barHeight, ACCENT);
		graphics.outline(left - 1, top - 1, barWidth + 2, barHeight + 2, 0xFF111827);
		graphics.text(Minecraft.getInstance().font, "kairokk v0.1.0",
			Math.max(12, width - 104), height - 18, 0xFF807781);
	}
}
