package com.kairokk.client;

import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.mc.SimpleScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Branded settings shell used for the settings hub and its category pages. */
public final class ShulkSettingsScreen extends SimpleScreen {
	private final ShulkSettingsFragment fragment;

	public ShulkSettingsScreen(Screen parent) {
		this(parent, "Settings");
	}

	public ShulkSettingsScreen(Screen parent, String pageTitle) {
		this(new ShulkSettingsFragment(parent, pageTitle));
	}

	private ShulkSettingsScreen(ShulkSettingsFragment fragment) {
		super(fragment, new Callback(), null, Component.empty());
		this.fragment = fragment;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTick) {
		TitleBackgroundRenderer.render(graphics, width, height);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (ShulkSettingsFragment.acceptPendingKey(event)) return true;
		if (event.key() == 256) {
			fragment.navigateBack();
			return true;
		}
		return super.keyPressed(event);
	}

	private static final class Callback implements ScreenCallback {
		@Override public boolean hasDefaultBackground() { return false; }
		@Override public boolean shouldClose() { return false; }
		@Override public boolean shouldBlurBackground() { return false; }
	}
}
