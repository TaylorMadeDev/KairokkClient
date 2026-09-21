package com.kairokk.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Kairokk's persistent overlay model, adapted from Shulk's Overlay Studio. */
public final class KairokkOverlayManager {
	public static final List<String> PRESETS = List.of("Default", "Minimal", "Combat", "Building", "Debugging", "Streaming", "Custom");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final int EDGE = 4;
	private static final Map<String, Type> TYPES = new LinkedHashMap<>();
	private final List<Widget> widgets = new ArrayList<>();
	private List<Widget> customLayout = new ArrayList<>();
	private boolean rendererEnabled = true;
	private String selectedId;
	private String currentPreset = "Default";
	private int gridSpacing = 4;
	private int layoutWidth = 854;
	private int layoutHeight = 480;
	private boolean dragging;
	private int dragOffsetX;
	private int dragOffsetY;

	private KairokkOverlayManager() { load(); }
	public static KairokkOverlayManager get() { return INSTANCE; }

	public synchronized List<Widget> widgets() { return widgets.stream().sorted(Comparator.comparingInt(w -> w.order)).map(Widget::copy).toList(); }
	public synchronized Widget selected() { Widget w = find(selectedId); return w == null ? null : w.copy(); }
	public synchronized String selectedId() { return selectedId; }
	public synchronized boolean rendererEnabled() { return rendererEnabled; }
	public synchronized void setRendererEnabled(boolean value) { rendererEnabled = value; save(); }
	public synchronized String currentPreset() { return currentPreset; }
	public synchronized int gridSpacing() { return gridSpacing; }
	public synchronized int layoutWidth() { return layoutWidth; }
	public synchronized int layoutHeight() { return layoutHeight; }
	public synchronized void select(String id) { if (find(id) != null) selectedId = id; }

	public synchronized String addWidget(String typeId) {
		Type type = type(typeId);
		if (type == null) return null;
		Widget widget = new Widget(type, widgets.size());
		int copy = 1;
		String base = type.name;
		while (widgets.stream().anyMatch(existing -> existing.name.equalsIgnoreCase(widget.name))) widget.name = base + " " + (++copy);
		int offset = widgets.size() % 8 * gridSpacing;
		widget.x += offset;
		widget.y += offset;
		clamp(widget, layoutWidth, layoutHeight);
		widgets.add(widget);
		selectedId = widget.id;
		currentPreset = "Custom";
		save();
		return widget.id;
	}

	public synchronized void removeSelected() {
		if (selectedId == null) return;
		widgets.removeIf(widget -> selectedId.equals(widget.id));
		selectedId = widgets.isEmpty() ? null : widgets.get(widgets.size() - 1).id;
		currentPreset = "Custom";
		save();
	}

	public synchronized void toggleVisible(String id) { Widget widget = find(id); if (widget != null) { widget.visible = !widget.visible; selectedId = id; touch(); } }
	public synchronized void toggleSelectedBackground() { mutateBoolean("background"); }
	public synchronized void toggleSelectedBorder() { mutateBoolean("border"); }
	public synchronized void toggleSelectedCompact() { mutateBoolean("compact"); }
	public synchronized void cycleGridSpacing() { gridSpacing = switch (gridSpacing) { case 2 -> 4; case 4 -> 8; case 8 -> 12; default -> 2; }; save(); }
	public synchronized void cycleSelectedScale() { Widget w = find(selectedId); if (w != null) { w.scale = w.scale >= 1.5F ? .75F : Math.round((w.scale + .25F) * 100F) / 100F; clamp(w, layoutWidth, layoutHeight); touch(); } }
	public synchronized void cycleSelectedOpacity() { Widget w = find(selectedId); if (w != null) { w.opacity = w.opacity >= 100 ? 40 : w.opacity + 15; touch(); } }
	public synchronized void cycleSelectedAccent() { Widget w = find(selectedId); if (w != null) { int[] colors = {0xFF4A94FF, 0xFF70AEFF, 0xFF9D8BFF, 0xFF6BD5FF, 0xFF63D79C, 0xFFFFC56B}; w.accentColor = nextColor(w.accentColor, colors); touch(); } }
	public synchronized void cycleSelectedTextColor() { Widget w = find(selectedId); if (w != null) { int[] colors = {0xFFF0F4FF, 0xFFD5E3F7, 0xFFFFFFFF}; w.textColor = nextColor(w.textColor, colors); touch(); } }
	public synchronized void cycleSelectedCornerRadius() { Widget w = find(selectedId); if (w != null) { w.cornerRadius = w.cornerRadius >= 14 ? 0 : w.cornerRadius + 2; touch(); } }
	public synchronized void cycleSelectedPadding() { Widget w = find(selectedId); if (w != null) { w.padding = w.padding >= 14 ? 3 : w.padding + 1; touch(); } }
	public synchronized void cycleSelectedAlignment() { Widget w = find(selectedId); if (w != null) { w.alignment = switch (w.alignment) { case "left" -> "center"; case "center" -> "right"; default -> "left"; }; touch(); } }
	public synchronized void toggleOption(String key) { Widget w = find(selectedId); if (w != null) { w.options.put(key, Boolean.toString(!Boolean.parseBoolean(w.options.getOrDefault(key, "false")))); touch(); } }
	public synchronized void saveCurrentLayout() { customLayout = copy(widgets); currentPreset = "Custom"; save(); }
	public synchronized void resetCurrentLayout() { applyPreset(currentPreset); }
	public synchronized void restoreDefaults() { applyPreset("Default"); }

	public synchronized void applyPreset(String preset) {
		String normalized = PRESETS.contains(preset) ? preset : "Default";
		if ("Custom".equals(normalized) && !customLayout.isEmpty()) {
			widgets.clear(); widgets.addAll(copy(customLayout));
		} else {
			List<Widget> replacement = new ArrayList<>();
			for (Type type : TYPES.values()) replacement.add(new Widget(type, replacement.size()));
			List<String> visible = switch (normalized) {
				case "Minimal" -> List.of("player_info", "movement_info");
				case "Combat" -> List.of("performance", "target_hud", "player_vitals");
				case "Building" -> List.of("coordinates", "crosshair_inspection", "compass");
				case "Debugging" -> List.of("performance", "coordinates", "crosshair_inspection", "world_info");
				case "Streaming" -> List.of("clock", "target_hud", "player_vitals");
				default -> List.of("player_info", "movement_info");
			};
			for (Widget widget : replacement) widget.visible = visible.contains(widget.typeId);
			widgets.clear(); widgets.addAll(replacement);
		}
		currentPreset = normalized;
		Widget firstVisible = widgets.stream().filter(w -> w.visible).findFirst().orElse(null);
		selectedId = firstVisible != null ? firstVisible.id : (widgets.isEmpty() ? null : widgets.get(0).id);
		for (Widget widget : widgets) { widget.ensureAnchorDefaults(); placeAnchored(widget, layoutWidth, layoutHeight); clamp(widget, layoutWidth, layoutHeight); }
		save();
	}

	public synchronized void syncLayout(int width, int height) {
		if (width <= 0 || height <= 0 || (width == layoutWidth && height == layoutHeight)) return;
		float sx = width / (float) layoutWidth, sy = height / (float) layoutHeight;
		for (Widget widget : widgets) {
			// Edge-anchored widgets retain their exact corner on resize instead of
			// being scaled away from it. Free widgets keep their proportional position.
			if ("free".equals(widget.horizontalAnchor) || widget.horizontalAnchor == null) widget.x = Math.round(widget.x * sx);
			if ("free".equals(widget.verticalAnchor) || widget.verticalAnchor == null) widget.y = Math.round(widget.y * sy);
			placeAnchored(widget, width, height);
			clamp(widget, width, height);
		}
		layoutWidth = width; layoutHeight = height; save();
	}

	public synchronized boolean beginDrag(double mouseX, double mouseY) {
		Widget hit = hit((int) mouseX, (int) mouseY);
		if (hit == null) return false;
		selectedId = hit.id; dragging = true; dragOffsetX = (int) mouseX - hit.x; dragOffsetY = (int) mouseY - hit.y; return true;
	}
	public synchronized boolean dragTo(double mouseX, double mouseY, int width, int height) {
		Widget moving = find(selectedId);
		if (!dragging || moving == null) return false;
		moving.x = Math.round(((int) mouseX - dragOffsetX) / (float) gridSpacing) * gridSpacing;
		moving.y = Math.round(((int) mouseY - dragOffsetY) / (float) gridSpacing) * gridSpacing;
		moving.horizontalAnchor = "free";
		moving.verticalAnchor = "free";
		clamp(moving, width, height); currentPreset = "Custom"; return true;
	}
	public synchronized void endDrag() { if (dragging) { dragging = false; save(); } }

	private void mutateBoolean(String key) { Widget w = find(selectedId); if (w == null) return; if ("background".equals(key)) w.backgroundVisible = !w.backgroundVisible; if ("border".equals(key)) w.borderVisible = !w.borderVisible; if ("compact".equals(key)) w.compact = !w.compact; touch(); }
	private void touch() { currentPreset = "Custom"; save(); }
	private Widget find(String id) { if (id == null) return null; return widgets.stream().filter(w -> id.equals(w.id)).findFirst().orElse(null); }
	private Widget hit(int x, int y) { return widgets.stream().filter(w -> w.visible).sorted(Comparator.comparingInt((Widget w) -> w.order).reversed()).filter(w -> x >= w.x && x <= w.x + width(w) && y >= w.y && y <= w.y + height(w)).findFirst().orElse(null); }
	private static int width(Widget w) { return Math.max(36, Math.round(w.width * w.scale)); }
	private static int height(Widget w) { return Math.max(18, Math.round(w.height * w.scale)); }
	private static void placeAnchored(Widget widget, int width, int height) {
		if ("right".equals(widget.horizontalAnchor)) widget.x = width - width(widget) - EDGE;
		else if ("left".equals(widget.horizontalAnchor)) widget.x = EDGE;
		if ("bottom".equals(widget.verticalAnchor)) widget.y = height - height(widget) - EDGE;
		else if ("top".equals(widget.verticalAnchor)) widget.y = EDGE;
	}
	private static void clamp(Widget w, int width, int height) { w.x = Math.max(EDGE, Math.min(Math.max(EDGE, width - KairokkOverlayManager.width(w) - EDGE), w.x)); w.y = Math.max(EDGE, Math.min(Math.max(EDGE, height - KairokkOverlayManager.height(w) - EDGE), w.y)); }
	private static int nextColor(int current, int[] colors) { for (int i = 0; i < colors.length; i++) if ((current & 0xFFFFFF) == (colors[i] & 0xFFFFFF)) return colors[(i + 1) % colors.length]; return colors[0]; }
	private int nextOrder() { return widgets.stream().mapToInt(w -> w.order).max().orElse(-1) + 1; }
	private static List<Widget> copy(List<Widget> source) { return source.stream().map(Widget::copy).collect(java.util.stream.Collectors.toCollection(ArrayList::new)); }

	private void load() {
		try {
			Path path = configPath();
			if (Files.exists(path)) try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
				JsonElement element = GSON.fromJson(reader, JsonElement.class);
				if (element != null && element.isJsonObject()) {
					Config config = GSON.fromJson(element, Config.class);
					if (config.widgets != null) widgets.addAll(config.widgets);
					customLayout = config.customLayout == null ? new ArrayList<>() : copy(config.customLayout);
					rendererEnabled = config.rendererEnabled; currentPreset = PRESETS.contains(config.currentPreset) ? config.currentPreset : "Custom";
					gridSpacing = List.of(2, 4, 8, 12).contains(config.gridSpacing) ? config.gridSpacing : 4;
					layoutWidth = config.layoutWidth > 0 ? config.layoutWidth : 854; layoutHeight = config.layoutHeight > 0 ? config.layoutHeight : 480;
					selectedId = find(config.selectedId) == null ? (widgets.isEmpty() ? null : widgets.get(0).id) : config.selectedId;
				}
			}
		} catch (Exception ignored) { widgets.clear(); }
		if (widgets.isEmpty()) applyPreset("Default");
		else ensureBuiltInWidgets();
	}

	private void ensureBuiltInWidgets() {
		boolean missing = TYPES.values().stream().anyMatch(type -> widgets.stream().noneMatch(widget -> type.id.equals(widget.typeId)));
		if (missing && "Default".equals(currentPreset)) {
			applyPreset("Default");
			return;
		}
		for (Type type : TYPES.values()) {
			if (widgets.stream().noneMatch(widget -> type.id.equals(widget.typeId))) {
				widgets.add(new Widget(type, nextOrder()));
			}
		}
		for (Widget widget : widgets) { widget.ensureAnchorDefaults(); placeAnchored(widget, layoutWidth, layoutHeight); clamp(widget, layoutWidth, layoutHeight); }
		save();
	}

	private void save() {
		try {
			Path path = configPath(); Files.createDirectories(path.getParent()); Path temp = path.resolveSibling(path.getFileName() + ".tmp");
			Config config = new Config(); config.rendererEnabled = rendererEnabled; config.currentPreset = currentPreset; config.gridSpacing = gridSpacing; config.layoutWidth = layoutWidth; config.layoutHeight = layoutHeight; config.selectedId = selectedId; config.widgets = copy(widgets); config.customLayout = copy(customLayout);
			Files.writeString(temp, GSON.toJson(config) + System.lineSeparator(), StandardCharsets.UTF_8);
			try { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); } catch (Exception ignored) { Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING); }
		} catch (Exception ignored) { }
	}

	public static Path configPath() { return FabricLoader.getInstance().getConfigDir().resolve("kairokk-overlay.json"); }

	public static Type type(String id) { return TYPES.get(id); }
	public static List<Type> types() { return List.copyOf(TYPES.values()); }
	static {
	register("performance", "FPS", "Shows your frame rate.", 112, 30, 12, 12, 0xFF4A94FF);
	register("coordinates", "Coordinates", "Your current position (XYZ).", 170, 44, 12, 52, 0xFF70AEFF);
		register("clock", "Time", "In-game time.", 112, 34, 690, 12, 0xFF9D8BFF);
		register("compass", "Direction", "Your facing direction.", 126, 40, 690, 54, 0xFF6BD5FF);
		register("target_hud", "Target HUD", "Crosshair target information.", 174, 54, 12, 106, 0xFF70AEFF);
		register("player_vitals", "Player Vitals", "Health, hunger and armor.", 166, 58, 12, 170, 0xFF63D79C);
	register("world_info", "Server / World", "Server and dimension information.", 178, 48, 690, 110, 0xFF6BD5FF);
	register("custom_text", "Custom Text", "A reusable text panel.", 164, 42, 350, 12, 0xFF9D8BFF);
	register("player_info", "Player Info", "FPS, ping, hunger, saturation and durability.", 190, 84, 12, 384, 0xFF70AEFF);
	register("movement_info", "Movement", "Speed, direction and position.", 204, 64, 638, 400, 0xFF6BD5FF);
	}
	private static final KairokkOverlayManager INSTANCE = new KairokkOverlayManager();
	private static void register(String id, String name, String desc, int width, int height, int x, int y, int accent) { TYPES.put(id, new Type(id, name, desc, width, height, x, y, accent)); }

	public record Type(String id, String name, String description, int width, int height, int x, int y, int accent) { }
	public static final class Widget {
		public String id = UUID.randomUUID().toString(); public String typeId; public String name; public int x, y, width, height, order; public boolean visible = true; public float scale = 1F; public int opacity = 88; public int accentColor; public int textColor = 0xFFF0F4FF; public boolean backgroundVisible = true, borderVisible = true, compact = true; public int cornerRadius = 7, padding = 7; public String alignment = "left"; public String horizontalAnchor; public String verticalAnchor; public Map<String, String> options = new LinkedHashMap<>();
		public Widget() { }
		private Widget(Type type, int order) { typeId = type.id; name = type.name; x = type.x; y = type.y; width = type.width; height = type.height; this.order = order; accentColor = type.accent; compact = !("player_info".equals(type.id) || "movement_info".equals(type.id)); ensureAnchorDefaults(); if ("coordinates".equals(type.id)) options.put("showDimension", "false"); }
		private void ensureAnchorDefaults() { if (horizontalAnchor == null || verticalAnchor == null) { if ("player_info".equals(typeId)) { horizontalAnchor = "left"; verticalAnchor = "bottom"; } else if ("movement_info".equals(typeId)) { horizontalAnchor = "right"; verticalAnchor = "bottom"; } else { horizontalAnchor = "free"; verticalAnchor = "free"; } } if ("movement_info".equals(typeId) && "left".equals(alignment)) alignment = "right"; }
		private Widget copy() { Widget w = new Widget(); w.id=id; w.typeId=typeId; w.name=name; w.x=x; w.y=y; w.width=width; w.height=height; w.order=order; w.visible=visible; w.scale=scale; w.opacity=opacity; w.accentColor=accentColor; w.textColor=textColor; w.backgroundVisible=backgroundVisible; w.borderVisible=borderVisible; w.compact=compact; w.cornerRadius=cornerRadius; w.padding=padding; w.alignment=alignment; w.horizontalAnchor=horizontalAnchor; w.verticalAnchor=verticalAnchor; w.options=options == null ? new LinkedHashMap<>() : new LinkedHashMap<>(options); return w; }
	}
	private static final class Config { boolean rendererEnabled = true; String currentPreset = "Default"; int gridSpacing = 4, layoutWidth = 854, layoutHeight = 480; String selectedId; List<Widget> widgets = new ArrayList<>(), customLayout = new ArrayList<>(); }
}
