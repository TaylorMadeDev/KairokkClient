package com.kairokk.client;

import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.mc.SimpleScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Hosts Kairokk's concise credits view. */
public final class KairokkCreditsScreen extends SimpleScreen {
	private final KairokkCreditsFragment fragment;
	public KairokkCreditsScreen(Screen parent) { this(new KairokkCreditsFragment(parent)); }
	private KairokkCreditsScreen(KairokkCreditsFragment fragment) { super(fragment, new Callback(), null, Component.empty()); this.fragment = fragment; }
	@Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTick) { TitleBackgroundRenderer.render(graphics, width, height); }
	@Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) { if (event.key() == 256) { fragment.close(); return true; } return super.keyPressed(event); }
	private static final class Callback implements ScreenCallback { @Override public boolean hasDefaultBackground() { return false; } @Override public boolean shouldClose() { return false; } @Override public boolean shouldBlurBackground() { return false; } }
}
