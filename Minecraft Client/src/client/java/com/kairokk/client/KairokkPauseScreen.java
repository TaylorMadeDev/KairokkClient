package com.kairokk.client;

import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.mc.SimpleScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;

/** The in-game pause menu, rendered with the same controls as the rest of Kairokk. */
public final class KairokkPauseScreen extends SimpleScreen {
	public KairokkPauseScreen() {
		super(new KairokkPauseFragment(), new Callback(), null, Component.empty());
	}

	@Override
	public boolean isPauseScreen() {
		return true;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTick) {
		// Blur is supplied by the ModernUI screen callback. The tint keeps the game
		// readable but puts visual focus on the floating menu controls.
		graphics.fill(0, 0, width, height, 0x7A000000);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (event.key() == 256) {
			KairokkPauseFragment.resumeGame();
			return true;
		}
		return super.keyPressed(event);
	}

	private static final class Callback implements ScreenCallback {
		@Override public boolean hasDefaultBackground() { return false; }
		@Override public boolean shouldClose() { return false; }
		@Override public boolean shouldBlurBackground() { return true; }
	}
}
