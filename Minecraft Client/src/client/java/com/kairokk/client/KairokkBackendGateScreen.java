package com.kairokk.client;

import com.kairokk.KairokkClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Startup gate that waits for the authenticated dashboard bridge before exposing the title menu. */
public final class KairokkBackendGateScreen extends Screen {
	private int retryLeft, retryTop, retryWidth = 178, retryHeight = 28;
	public KairokkBackendGateScreen() { super(Component.literal("Kairokk secure connection")); }
	@Override public boolean shouldCloseOnEsc() { return false; }
	@Override public boolean isPauseScreen() { return false; }
	@Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTick) { TitleBackgroundRenderer.render(graphics, width, height); }
	@Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTick) {
		int centerX = width / 2, centerY = height / 2; retryLeft = centerX - retryWidth / 2; retryTop = centerY + 66;
		graphics.fill(centerX - 228, centerY - 116, centerX + 228, centerY + 122, 0xE9111C2B);
		graphics.outline(centerX - 228, centerY - 116, 456, 238, 0xFF31547A);
		KairokkHudIconRenderer.draw(graphics, ShulkIcon.BRAND, centerX - 28, centerY - 89, 56, 0xFF4A94FF);
		graphics.centeredText(Minecraft.getInstance().font, "VERIFYING KAIROKK", centerX, centerY - 22, 0xFFF0F4FF);
		graphics.centeredText(Minecraft.getInstance().font, "A secure dashboard connection is required before continuing.", centerX, centerY + 3, 0xFFA8BBD4);
		String status = KairokkClient.realtime() == null ? "Starting secure bridge…" : KairokkClient.realtime().connectionStatus();
		graphics.centeredText(Minecraft.getInstance().font, status, centerX, centerY + 28, KairokkClient.realtime() != null && KairokkClient.realtime().isConnected() ? 0xFF5EE7A5 : 0xFF72AEFF);
		graphics.fill(retryLeft, retryTop, retryLeft + retryWidth, retryTop + retryHeight, 0xFF173C68);
		graphics.outline(retryLeft, retryTop, retryWidth, retryHeight, 0xFF4A94FF);
		graphics.centeredText(Minecraft.getInstance().font, "Retry connection", centerX, retryTop + 10, 0xFFF4F8FF);
		graphics.centeredText(Minecraft.getInstance().font, "The client never stores your dashboard password.", centerX, centerY + 104, 0xFF7C93B0);
	}
	@Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && event.x() >= retryLeft && event.x() <= retryLeft + retryWidth && event.y() >= retryTop && event.y() <= retryTop + retryHeight && KairokkClient.realtime() != null) { KairokkClient.realtime().reconnect(); return true; }
		return true;
	}
	@Override public boolean keyPressed(KeyEvent event) { return true; }
}
