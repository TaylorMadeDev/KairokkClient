package com.kairokk.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Draws the configured overlay in-game and exposes the same card geometry to the editor. */
public final class KairokkOverlayRenderer {
	private KairokkOverlayRenderer() { }

	public static void render(GuiGraphicsExtractor graphics, DeltaTracker ignored) {
		KairokkOverlayManager manager = KairokkOverlayManager.get();
		if (!manager.rendererEnabled()) return;
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.font == null || minecraft.getWindow() == null) return;
		int width = minecraft.getWindow().getGuiScaledWidth();
		int height = minecraft.getWindow().getGuiScaledHeight();
		manager.syncLayout(width, height);
		for (KairokkOverlayManager.Widget widget : manager.widgets()) {
			if (widget.visible) renderCard(graphics, minecraft.font, minecraft, widget, false);
		}
	}

	static void renderCard(GuiGraphicsExtractor graphics, Font font, Minecraft minecraft,
			KairokkOverlayManager.Widget widget, boolean selected) {
		int width = width(widget), height = height(widget);
		int padding = Math.max(3, Math.round(widget.padding * widget.scale));
		String[] content = content(minecraft, widget);
		String title = title(widget);
		int titleWidth = font.width(title);
		int titleX = "center".equals(widget.alignment) ? widget.x + (width - titleWidth) / 2 : "right".equals(widget.alignment) ? widget.x + width - padding - titleWidth : widget.x + padding;
		graphics.text(font, title, titleX, widget.y + padding - 1, withAlpha(widget.accentColor, 250), true);
		int y = widget.y + padding + 11;
		int maxLines = widget.compact ? 1 : 6;
		for (int index = 0; index < content.length && index < maxLines && y + 8 <= widget.y + height; index++) {
			String value = content[index];
			int drawX = "center".equals(widget.alignment) ? widget.x + (width - font.width(value)) / 2 : "right".equals(widget.alignment) ? widget.x + width - padding - font.width(value) : widget.x + padding;
			graphics.text(font, value, drawX, y, withAlpha(widget.textColor, 245), false);
			y += 10;
		}
	}

	static void renderEditHint(GuiGraphicsExtractor graphics, Font font, int width, int height, int grid) {
		String hint = "Overlay edit  •  drag to move  •  " + grid + "px grid  •  Esc or U to finish";
		int panelWidth = font.width(hint) + 20, x = Math.max(4, (width - panelWidth) / 2), y = height - 30;
		fillRounded(graphics, x, y, x + panelWidth, y + 20, 6, 0xE8142233);
		outline(graphics, x, y, panelWidth, 20, 0xCC4A94FF);
		graphics.text(font, hint, x + 10, y + 6, 0xFFF0F4FF, false);
	}

	static int width(KairokkOverlayManager.Widget widget) { return Math.max(36, Math.round(widget.width * widget.scale)); }
	static int height(KairokkOverlayManager.Widget widget) { return Math.max(18, Math.round(widget.height * widget.scale)); }

	private static String title(KairokkOverlayManager.Widget widget) { return switch (widget.typeId) { case "performance" -> "FPS"; case "coordinates" -> "COORDINATES"; case "target_hud" -> "TARGET"; case "player_vitals" -> "PLAYER VITALS"; case "clock" -> "TIME"; case "compass" -> "COMPASS"; case "world_info" -> "WORLD"; case "player_info" -> "PLAYER INFO"; case "movement_info" -> "MOVEMENT"; default -> widget.name.toUpperCase(Locale.ROOT); }; }
	private static String[] content(Minecraft minecraft, KairokkOverlayManager.Widget widget) {
		if ("player_info".equals(widget.typeId)) return playerInfo(minecraft);
		if ("movement_info".equals(widget.typeId)) return movementInfo(minecraft);
		if ("performance".equals(widget.typeId)) return new String[]{"FPS  " + minecraft.getFps()};
		if ("coordinates".equals(widget.typeId)) { if (minecraft.player == null) return new String[]{"No player"}; return new String[]{String.format(Locale.ROOT, "XYZ  %.1f  %.1f  %.1f", minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ())}; }
		if ("clock".equals(widget.typeId)) return new String[]{LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT))};
		if ("compass".equals(widget.typeId) && minecraft.player != null) return new String[]{String.format(Locale.ROOT, "%03d°", Math.round((minecraft.player.getYRot() % 360F + 360F) % 360F))};
		if ("player_vitals".equals(widget.typeId) && minecraft.player != null) return new String[]{"Health  " + Math.round(minecraft.player.getHealth()) + " / " + Math.round(minecraft.player.getMaxHealth())};
		if ("world_info".equals(widget.typeId)) return new String[]{minecraft.level == null ? "Not in a world" : (minecraft.getCurrentServer() == null ? "Singleplayer" : minecraft.getCurrentServer().ip)};
		if ("target_hud".equals(widget.typeId)) return new String[]{minecraft.hitResult == null ? "No target" : "Target detected"};
		return new String[]{widget.options.getOrDefault("text", "Your text here")};
	}
	private static String[] playerInfo(Minecraft minecraft) {
		if (minecraft.player == null) return new String[]{"FPS  " + minecraft.getFps(), "Ping  --", "Hunger  --", "Saturation  --", "Durability  None"};
		int ping = 0;
		if (minecraft.getConnection() != null && minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID()) != null) {
			ping = minecraft.getConnection().getPlayerInfo(minecraft.player.getUUID()).getLatency();
		}
		var stack = minecraft.player.getMainHandItem();
		String durability = !stack.isEmpty() && stack.isDamageableItem()
			? (stack.getMaxDamage() - stack.getDamageValue()) + " / " + stack.getMaxDamage() : "None";
		return new String[]{"FPS  " + minecraft.getFps(), "Ping  " + ping + " ms", "Hunger  " + minecraft.player.getFoodData().getFoodLevel(), String.format(Locale.ROOT, "Saturation  %.1f", minecraft.player.getFoodData().getSaturationLevel()), "Durability  " + durability};
	}
	private static String[] movementInfo(Minecraft minecraft) {
		if (minecraft.player == null) return new String[]{"Speed  0.00 b/s", "Direction  --", "Position  --"};
		var velocity = minecraft.player.getDeltaMovement();
		double speed = Math.sqrt(velocity.x() * velocity.x() + velocity.z() * velocity.z()) * 20.0D;
		String direction = minecraft.player.getDirection().getName();
		direction = direction.substring(0, 1).toUpperCase(Locale.ROOT) + direction.substring(1);
		return new String[]{String.format(Locale.ROOT, "Speed  %.2f b/s", speed), "Direction  " + direction, String.format(Locale.ROOT, "Position  %.1f, %.1f, %.1f", minecraft.player.getX(), minecraft.player.getY(), minecraft.player.getZ())};
	}
	private static int withAlpha(int color, int alpha) { return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0xFFFFFF); }
	private static void outline(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int color) { graphics.fill(x, y, x + width, y + 1, color); graphics.fill(x, y + height - 1, x + width, y + height, color); graphics.fill(x, y, x + 1, y + height, color); graphics.fill(x + width - 1, y, x + width, y + height, color); }
	private static void fillRounded(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int radius, int color) { int r = Math.max(0, Math.min(radius, Math.min((x2 - x1) / 2, (y2 - y1) / 2))); if (r == 0) { graphics.fill(x1, y1, x2, y2, color); return; } graphics.fill(x1, y1 + r, x2, y2 - r, color); for (int dy = 0; dy < r; dy++) { double py = r - dy - .5; int inset = Math.max(0, r - (int) Math.sqrt(Math.max(0, r * r - py * py))); graphics.fill(x1 + inset, y1 + dy, x2 - inset, y1 + dy + 1, color); graphics.fill(x1 + inset, y2 - dy - 1, x2 - inset, y2 - dy, color); } }
}
