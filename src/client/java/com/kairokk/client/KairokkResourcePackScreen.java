package com.kairokk.client;

import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.mc.SimpleScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Kairokk's resource-pack manager screen. */
public final class KairokkResourcePackScreen extends SimpleScreen {
	private final KairokkResourcePackFragment fragment;

	public KairokkResourcePackScreen(Screen parent) {
		this(new KairokkResourcePackFragment(parent));
	}

	private KairokkResourcePackScreen(KairokkResourcePackFragment fragment) {
		super(fragment, new Callback(), null, Component.empty());
		this.fragment = fragment;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTick) {
		TitleBackgroundRenderer.render(graphics, width, height);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (event.key() == 256) {
			fragment.cancel();
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
