package com.kairokk.client;

import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.mc.SimpleScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;


/** A presentational first pass of the Kairokk in-game client GUI. */
public final class KairokkClickGuiScreen extends SimpleScreen {
	private final Screen parent;
	private final KairokkClickGuiFragment clickGui;
	private PlayerModel wideModel;
	private PlayerModel slimModel;
	private volatile PlayerSkin accountSkin;
	private boolean previewDragging;
	private float previewRotation = 0.0F;
	private float previewZoom = 1.0F;

	public KairokkClickGuiScreen(Screen parent) {
		this(new KairokkClickGuiFragment(parent), parent);
	}

	private KairokkClickGuiScreen(KairokkClickGuiFragment fragment, Screen parent) {
		super(fragment, new Callback(), null, Component.empty());
		this.clickGui = fragment;
		this.parent = parent;
		Minecraft minecraft = Minecraft.getInstance();
		accountSkin = DefaultPlayerSkin.get(minecraft.getGameProfile());
		minecraft.getSkinManager().get(minecraft.getGameProfile()).thenAccept(skin -> skin.ifPresent(found -> accountSkin = found));
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTick) {
		// Screens are cleared to black before their UI is composed. Render the same
		// ambient title backdrop here so the bar remains the only GUI surface.
		TitleBackgroundRenderer.render(graphics, width, height);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float deltaTick) {
		super.extractRenderState(graphics, mouseX, mouseY, deltaTick);
		KairokkClickGuiFragment.PreviewBounds bounds = clickGui.previewBounds();
		if (bounds == null || bounds.uiWidth() <= 0 || bounds.height() <= 0) return;

		float uiToGui = (float) width / bounds.uiWidth();
		int left = Math.round(bounds.x() * uiToGui);
		int top = Math.round(bounds.y() * uiToGui);
		int stageWidth = Math.round(bounds.width() * uiToGui);
		int stageHeight = Math.round(bounds.height() * uiToGui);
		if (stageWidth < 20 || stageHeight < 20) return;

		Minecraft minecraft = Minecraft.getInstance();
		PlayerSkin skin = minecraft.player != null ? minecraft.player.getSkin() : accountSkin;
		if (skin == null) return;
		if (wideModel == null) {
			wideModel = new PlayerModel(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER), false);
			slimModel = new PlayerModel(minecraft.getEntityModels().bakeLayer(ModelLayers.PLAYER_SLIM), true);
		}
		PlayerModel model = skin.model() == PlayerModelType.SLIM ? slimModel : wideModel;
		int cx = left + stageWidth / 2;
		int cy = top + stageHeight / 2;
		// The skin renderer uses both scale and its render bounds. Keeping both
		// tied to previewZoom stops a zoomed-out model being clipped or drifting
		// away from its ESP frame.
		float baseScale = Math.min(stageWidth * 0.25F, stageHeight * 0.29F);
		float scale = baseScale * previewZoom;
		int renderHalfWidth = Math.max(24, Math.round(scale * 1.18F));
		int renderHalfHeight = Math.max(38, Math.round(scale * 1.72F));
		int modelCenterY = cy - Math.round(scale * 0.66F);
		int renderLeft = cx - renderHalfWidth;
		int renderRight = cx + renderHalfWidth;
		int renderTop = modelCenterY - renderHalfHeight;
		int renderBottom = modelCenterY + renderHalfHeight;
		// Match vanilla's PlayerSkinWidget pivot so the model is vertically
		// centered in the same bounds used by the ESP preview frame.
		graphics.skin(model, skin.body().texturePath(), scale, -5.0F, previewRotation, -1.0625F,
			renderLeft, renderTop, renderRight, renderBottom);

		PreviewFrame frame = new PreviewFrame(cx, cy, scale);
		KairokkClickGuiFragment.PreviewStyle style = clickGui.previewStyle();
		if (style != null) drawPreviewOverlays(graphics, minecraft, style, frame, top, stageHeight);
	}

	private void drawPreviewOverlays(GuiGraphicsExtractor graphics, Minecraft minecraft,
			KairokkClickGuiFragment.PreviewStyle style, PreviewFrame frame, int stageTop, int stageHeight) {
		if (style.esp()) {
			if (style.chams()) drawChamsOverlay(graphics, frame, style.color());
			if (style.tracers()) graphics.fill(frame.centerX - 1, frame.boxBottom, frame.centerX + 1,
				Math.min(stageTop + stageHeight - 10, frame.boxBottom + Math.round(frame.scale * .42F)), style.color());
			if (!"None".equals(style.boxStyle())) drawEspBox(graphics, frame, style.boxStyle(), style.color());
			if (style.health()) drawHealthBar(graphics, frame);
		}
		int labelY = frame.boxTop - (style.distance() ? 30 : 16);
		if (style.nameTags()) graphics.centeredText(minecraft.font, minecraft.getUser().getName(), frame.centerX, labelY, style.color());
		if (style.distance()) graphics.centeredText(minecraft.font, "[12m]", frame.centerX, labelY + 13, style.color());
	}

	private static void drawEspBox(GuiGraphicsExtractor graphics, PreviewFrame frame, String boxStyle, int color) {
		int corner = "Corner".equals(boxStyle) ? Math.max(6, frame.width() / 4) : frame.width();
		graphics.fill(frame.boxLeft, frame.boxTop, frame.boxLeft + corner, frame.boxTop + 1, color);
		graphics.fill(frame.boxRight - corner, frame.boxTop, frame.boxRight, frame.boxTop + 1, color);
		graphics.fill(frame.boxLeft, frame.boxBottom - 1, frame.boxLeft + corner, frame.boxBottom, color);
		graphics.fill(frame.boxRight - corner, frame.boxBottom - 1, frame.boxRight, frame.boxBottom, color);
		graphics.fill(frame.boxLeft, frame.boxTop, frame.boxLeft + 1, frame.boxTop + corner, color);
		graphics.fill(frame.boxRight - 1, frame.boxTop, frame.boxRight, frame.boxTop + corner, color);
		graphics.fill(frame.boxLeft, frame.boxBottom - corner, frame.boxLeft + 1, frame.boxBottom, color);
		graphics.fill(frame.boxRight - 1, frame.boxBottom - corner, frame.boxRight, frame.boxBottom, color);
	}

	private static void drawHealthBar(GuiGraphicsExtractor graphics, PreviewFrame frame) {
		int barWidth = Math.max(3, Math.round(frame.scale * .055F));
		int barLeft = frame.boxRight + Math.max(5, Math.round(frame.scale * .08F));
		graphics.fill(barLeft, frame.boxTop, barLeft + barWidth, frame.boxBottom, 0xCC192733);
		int healthTop = frame.boxBottom - Math.round((frame.boxBottom - frame.boxTop) * .78F);
		graphics.fill(barLeft, healthTop, barLeft + barWidth, frame.boxBottom, 0xFF48D878);
	}

	private static void drawChamsOverlay(GuiGraphicsExtractor graphics, PreviewFrame frame, int color) {
		int overlay = (color & 0x00FFFFFF) | 0x62000000;
		int width = frame.width();
		int headWidth = Math.round(width * .54F);
		int headHeight = Math.round((frame.boxBottom - frame.boxTop) * .23F);
		int torsoTop = frame.boxTop + headHeight;
		int torsoBottom = frame.boxTop + Math.round((frame.boxBottom - frame.boxTop) * .66F);
		int legTop = torsoBottom;
		int armWidth = Math.max(4, Math.round(width * .20F));
		int bodyWidth = Math.round(width * .48F);
		graphics.fill(frame.centerX - headWidth / 2, frame.boxTop, frame.centerX + headWidth / 2, torsoTop, overlay);
		graphics.fill(frame.centerX - bodyWidth / 2, torsoTop, frame.centerX + bodyWidth / 2, torsoBottom, overlay);
		graphics.fill(frame.boxLeft, torsoTop, frame.boxLeft + armWidth, torsoBottom, overlay);
		graphics.fill(frame.boxRight - armWidth, torsoTop, frame.boxRight, torsoBottom, overlay);
		graphics.fill(frame.centerX - bodyWidth / 2, legTop, frame.centerX - 1, frame.boxBottom, overlay);
		graphics.fill(frame.centerX + 1, legTop, frame.centerX + bodyWidth / 2, frame.boxBottom, overlay);
	}

	private record PreviewFrame(int centerX, int centerY, float scale, int boxLeft, int boxTop, int boxRight, int boxBottom) {
		private PreviewFrame(int centerX, int centerY, float scale) {
			this(centerX, centerY, scale, centerX - Math.max(12, Math.round(scale * .57F)),
				centerY - Math.round(scale * 1.05F), centerX + Math.max(12, Math.round(scale * .57F)),
				centerY + Math.round(scale * .96F));
		}
		private int width() { return boxRight - boxLeft; }
	}

	private boolean inPreview(double mouseX, double mouseY) {
		KairokkClickGuiFragment.PreviewBounds bounds = clickGui.previewBounds();
		if (bounds == null || bounds.uiWidth() <= 0) return false;
		float factor = (float) width / bounds.uiWidth();
		return mouseX >= bounds.x() * factor && mouseX < (bounds.x() + bounds.width()) * factor
			&& mouseY >= bounds.y() * factor && mouseY < (bounds.y() + bounds.height()) * factor;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 0 && inPreview(event.x(), event.y())) {
			previewDragging = true;
			return true;
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if (previewDragging) {
			previewRotation += (float) dragX * 1.2F;
			return true;
		}
		return super.mouseDragged(event, dragX, dragY);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (previewDragging && event.button() == 0) {
			previewDragging = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
		if (inPreview(mouseX, mouseY)) {
			previewZoom = Math.max(0.65F, Math.min(1.5F, previewZoom + (float) vertical * 0.08F));
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
	}

	@Override
	public void onClose() {
		Minecraft.getInstance().setScreen(parent);
	}

	@Override
	public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
		if (event.key() == 256 || event.key() == 344) {
			onClose();
			return true;
		}
		return super.keyPressed(event);
	}

	private static final class Callback implements ScreenCallback {
		@Override public boolean hasDefaultBackground() { return false; }
		@Override public boolean shouldClose() { return false; }
		@Override public boolean shouldBlurBackground() { return KairokkCustomizationState.backgroundBlur(); }
	}
}
