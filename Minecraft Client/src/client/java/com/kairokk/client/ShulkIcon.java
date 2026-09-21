package com.kairokk.client;

import net.minecraft.resources.Identifier;

/**
 * Single source of truth for every pictogram used by the Shulk UI and HUD.
 * File variants are explicit so call sites never depend on a FontAwesome
 * filename or resource path.
 */
public enum ShulkIcon {
	BRAND("cube", Variant.SOLID),
	HOME("house", Variant.SOLID),
	EDITOR("file-pen", Variant.SOLID),
	SCRIPTS("code", Variant.SOLID),
	OVERLAY("layer-group", Variant.SOLID),
	CONFIG("sliders", Variant.SOLID),
	INFO("circle-info", Variant.SOLID),
	HELP("circle-question", Variant.REGULAR),
	SETTINGS("gear", Variant.SOLID),
	SEARCH("magnifying-glass", Variant.SOLID),
	CLOSE("xmark", Variant.SOLID),
	BACK("chevron-left", Variant.SOLID),
	FORWARD("chevron-right", Variant.SOLID),
	EXPAND("chevron-down", Variant.SOLID),
	COLLAPSE("chevron-right", Variant.SOLID),
	ADD("plus", Variant.SOLID),
	RESET("arrows-rotate", Variant.SOLID),
	VISIBLE("eye", Variant.REGULAR),
	HIDDEN("eye-slash", Variant.SOLID),
	MODULES("puzzle-piece", Variant.SOLID),
	PERFORMANCE("chart-line", Variant.SOLID),
	GAUGE("gauge-high", Variant.SOLID),
	COORDINATES("location-dot", Variant.SOLID),
	TARGET("crosshairs", Variant.SOLID),
	PLAYER("user", Variant.SOLID),
	FOLDER("folder", Variant.REGULAR),
	FOLDER_OPEN("folder-open", Variant.SOLID),
	FOLDER_ADD("folder-plus", Variant.SOLID),
	FILE("file-code", Variant.REGULAR),
	SAVE("floppy-disk", Variant.REGULAR),
	DOWNLOAD("download", Variant.SOLID),
	WARNING("circle-exclamation", Variant.SOLID),
	SUCCESS("circle-check", Variant.REGULAR),
	HEART("heart", Variant.SOLID),
	CLOCK("clock", Variant.REGULAR),
	COMPASS("compass", Variant.REGULAR),
	SERVER("server", Variant.SOLID),
	GLOBE("globe", Variant.SOLID),
	PLAY("play", Variant.SOLID),
	STOP("stop", Variant.SOLID),
	MENU("bars", Variant.SOLID),
	MORE("ellipsis", Variant.SOLID),
	EXIT("arrow-right-from-bracket", Variant.SOLID),
	PALETTE("palette", Variant.SOLID),
	LOCK("lock", Variant.SOLID),
	SHIELD("shield", Variant.SOLID),
	CUSTOMIZE("wand-magic-sparkles", Variant.SOLID),
	LAYOUT("table-columns", Variant.SOLID),
	LIST("list", Variant.SOLID),
	TERMINAL("terminal", Variant.SOLID),
	COPY("copy", Variant.REGULAR),
	RENAME("pen-to-square", Variant.REGULAR),
	CHECK("check", Variant.SOLID),
	STATUS("circle", Variant.SOLID),
	NEXT("arrow-right", Variant.SOLID),
	EXTERNAL("arrow-up-right-from-square", Variant.SOLID),
	MOVE("arrow-right-arrow-left", Variant.SOLID),
	DELETE("trash-can", Variant.REGULAR),
	LANGUAGE("language", Variant.SOLID),
	SOUND("volume-high", Variant.SOLID),
	CONTROLS("gamepad", Variant.SOLID),
	APPEARANCE("shirt", Variant.SOLID),
	CHAT("comments", Variant.REGULAR),
	RESOURCE_PACK("box-open", Variant.SOLID),
	ACCESSIBILITY("universal-access", Variant.SOLID);

	public enum Variant {
		SOLID("solid"), REGULAR("regular");

		private final String suffix;
		Variant(String suffix) { this.suffix = suffix; }
	}

	private final String fileName;
	private final Identifier resource;

	ShulkIcon(String name, Variant variant) {
		this.fileName = name + "-" + variant.suffix + ".png";
		this.resource = Identifier.fromNamespaceAndPath("kairokk", "textures/icons/" + fileName);
	}

	public String fileName() { return fileName; }
	public Identifier resource() { return resource; }

	public static ShulkIcon forPage(String page) {
		return switch (page) {
			case "Home" -> HOME;
			case "Editor" -> EDITOR;
			case "Scripts" -> SCRIPTS;
			case "Overlay" -> OVERLAY;
			case "Configs" -> CONFIG;
			case "Info" -> INFO;
			case "Settings" -> SETTINGS;
			default -> INFO;
		};
	}

	public static ShulkIcon forOverlayWidget(String typeId) {
		return switch (typeId) {
			case "performance" -> GAUGE;
			case "coordinates" -> COORDINATES;
			case "module_list" -> MODULES;
			case "target_hud" -> TARGET;
			case "player_vitals" -> HEART;
			case "script_status" -> SCRIPTS;
			case "crosshair_inspection" -> SEARCH;
			case "clock" -> CLOCK;
			case "compass" -> COMPASS;
			case "world_info" -> GLOBE;
			case "custom_text" -> FILE;
			default -> OVERLAY;
		};
	}

	public static ShulkIcon forAction(String label) {
		if (label == null) return null;
		String value = label.toLowerCase(java.util.Locale.ROOT);
		if (value.contains("singleplayer") || value.contains("world")) return GLOBE;
		if (value.contains("multiplayer") || value.contains("server") || value.contains("hypixel")) return SERVER;
		if (value.contains("skin")) return APPEARANCE;
		if (value.contains("sound")) return SOUND;
		if (value.contains("control")) return CONTROLS;
		if (value.contains("chat")) return CHAT;
		if (value.contains("resource pack")) return RESOURCE_PACK;
		if (value.contains("accessibility")) return ACCESSIBILITY;
		if (value.contains("telemetry")) return PERFORMANCE;
		if (value.equals("credits")) return HEART;
		if (value.contains("delete") || value.contains("remove")) return DELETE;
		if (value.contains("reset") || value.contains("refresh") || value.contains("re-create") || value.contains("defaults")) return RESET;
		if (value.contains("save")) return SAVE;
		if (value.contains("add") || value.contains("new ") || value.startsWith("create")) return ADD;
		if (value.contains("back")) return BACK;
		if (value.contains("cancel") || value.contains("close")) return CLOSE;
		if (value.contains("done") || value.contains("apply")) return CHECK;
		if (value.equals("run") || value.contains("connect") || value.startsWith("play ")) return PLAY;
		if (value.equals("edit") || value.startsWith("edit ") || value.contains("rename")) return RENAME;
		if (value.contains("option") || value.contains("setting") || value.contains("manage")) return SETTINGS;
		if (value.contains("quit") || value.contains("exit")) return EXIT;
		if (value.contains("language")) return LANGUAGE;
		if (value.contains("account")) return PLAYER;
		if (value.contains("proxy")) return GLOBE;
		if (value.contains("clickgui") || value.contains("client menu")) return MENU;
		return null;
	}
}
