package com.kairokk.client;

import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.mc.SimpleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Kairokk world-generation preset selector. */
public final class ShulkWorldTypeScreen extends SimpleScreen {
	private final Screen returnScreen;

	public ShulkWorldTypeScreen(Screen returnScreen, ShulkCreateWorldFragment owner) {
		super(new ShulkWorldTypeFragment(returnScreen, owner), new Callback(), null, Component.empty());
		this.returnScreen = returnScreen;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTick) {
		TitleBackgroundRenderer.render(graphics, width, height);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(returnScreen);
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
