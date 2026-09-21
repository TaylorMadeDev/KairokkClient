package com.kairokk.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Transparent, non-pausing in-game editor with grid snapping and screen clamping. */
public final class KairokkOverlayEditScreen extends Screen {
	private final Screen returnScreen;
	private boolean finishing;

	public KairokkOverlayEditScreen(Screen returnScreen) { super(Component.literal("Kairokk Overlay Editor")); this.returnScreen = returnScreen; }
	@Override public boolean isPauseScreen() { return false; }
	@Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTick) { }

	@Override public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTick) {
		KairokkOverlayManager manager = KairokkOverlayManager.get();
		int grid = manager.gridSpacing();
		for (int x = grid; x < width; x += grid) graphics.fill(x, 0, x + 1, height, 0x142F4C70);
		for (int y = grid; y < height; y += grid) graphics.fill(0, y, width, y + 1, 0x142F4C70);
		for (KairokkOverlayManager.Widget widget : manager.widgets()) if (widget.visible) KairokkOverlayRenderer.renderCard(graphics, Minecraft.getInstance().font, Minecraft.getInstance(), widget, widget.id.equals(manager.selectedId()));
		KairokkOverlayRenderer.renderEditHint(graphics, Minecraft.getInstance().font, width, height, grid);
	}

	@Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) { return event.button() == InputConstants.MOUSE_BUTTON_LEFT && KairokkOverlayManager.get().beginDrag(event.x(), event.y()); }
	@Override public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) { return event.button() == InputConstants.MOUSE_BUTTON_LEFT && KairokkOverlayManager.get().dragTo(event.x(), event.y(), width, height); }
	@Override public boolean mouseReleased(MouseButtonEvent event) { if (event.button() != InputConstants.MOUSE_BUTTON_LEFT) return false; KairokkOverlayManager.get().endDrag(); return true; }
	@Override public boolean keyPressed(KeyEvent event) { if (event.key() == InputConstants.KEY_ESCAPE || event.key() == InputConstants.KEY_U) { finishEditing(); return true; } return true; }
	@Override public void onClose() { finishEditing(); }
	private void finishEditing() { if (finishing) return; finishing = true; KairokkOverlayManager.get().endDrag(); Minecraft.getInstance().setScreen(returnScreen); }
}
