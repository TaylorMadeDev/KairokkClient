package com.kairokk.client;

import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.mc.SimpleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ShulkMultiplayerScreen extends SimpleScreen {
	private final Screen parent;
	private final ShulkMultiplayerFragment multiplayerFragment;

	public ShulkMultiplayerScreen(Screen parent) {
		this(parent, new ShulkMultiplayerFragment(parent));
	}

	private ShulkMultiplayerScreen(Screen parent, ShulkMultiplayerFragment fragment) {
		super(fragment, new Callback(), null, Component.empty());
		this.parent = parent;
		this.multiplayerFragment = fragment;
	}

	@Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTick) {
		TitleBackgroundRenderer.render(graphics, width, height);
	}

	@Override public void tick() {
		super.tick();
		multiplayerFragment.tickPinger();
	}

	@Override public void removed() {
		multiplayerFragment.stopPinger();
		super.removed();
	}

	@Override public void onClose() { Minecraft.getInstance().setScreen(parent); }
	@Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) { if (event.key() == 256) { onClose(); return true; } return super.keyPressed(event); }

	private static final class Callback implements ScreenCallback {
		@Override public boolean hasDefaultBackground() { return false; }
		@Override public boolean shouldClose() { return false; }
		@Override public boolean shouldBlurBackground() { return false; }
	}
}
