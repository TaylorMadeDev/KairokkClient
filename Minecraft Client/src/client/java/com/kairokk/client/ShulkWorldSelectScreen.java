package com.kairokk.client;

import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.mc.SimpleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** ModernUI-backed saved-world browser. */
public final class ShulkWorldSelectScreen extends SimpleScreen {
	private final Screen parent;

	public ShulkWorldSelectScreen(Screen parent) {
		super(new ShulkWorldSelectFragment(parent), new Callback(), null, Component.empty());
		this.parent = parent;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTick) {
		TitleBackgroundRenderer.render(graphics, width, height);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(parent);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (event.key() == 256) { onClose(); return true; }
		return super.keyPressed(event);
	}

	private static final class Callback implements ScreenCallback {
		@Override public boolean hasDefaultBackground() { return false; }
		@Override public boolean shouldClose() { return false; }
		@Override public boolean shouldBlurBackground() { return false; }
	}
}
