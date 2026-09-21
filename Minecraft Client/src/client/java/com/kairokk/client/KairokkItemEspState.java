package com.kairokk.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Persisted controls for the item visualisation page and its in-menu preview. */
public final class KairokkItemEspState {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("kairokk-item-esp.properties");

    private static boolean itemEsp = true;
    private static boolean itemNames = true;
    private static boolean itemBoxes = true;
    private static boolean itemTracers;
    private static boolean itemGlow = true;
    private static boolean itemIcon = true;
    private static boolean itemDistance = true;
    private static boolean filterByRarity = true;
    private static boolean showCommon = true;
    private static boolean showUncommon = true;
    private static boolean showRare = true;
    private static boolean showEpic = true;
    private static boolean showLegendary = true;
    private static boolean showMythic = true;
    private static boolean showXpOrbs = true;
    private static boolean showOtherEntities = true;
    private static String boxStyle = "Cornered";
    private static String tracerStyle = "Solid";
    private static int lineWidth = 15;
    private static int cornerRadius = 2;
    private static int glowStrength = 60;
    private static int tracerThickness = 1;
    private static int common = 0xFFFFFFFF;
    private static int uncommon = 0xFF55FF88;
    private static int rare = 0xFF4A94FF;
    private static int epic = 0xFFAA44FF;
    private static int legendary = 0xFFFFCC33;
    private static int mythic = 0xFFFF4455;
    private static int text = 0xFFFFFFFF;
    private static int tracer = 0xFF22D3EE;

    static {
        load();
    }

    private KairokkItemEspState() {
    }

    public static synchronized boolean itemEsp() { return itemEsp; }
    public static synchronized boolean itemNames() { return itemNames; }
    public static synchronized boolean itemBoxes() { return itemBoxes; }
    public static synchronized boolean itemTracers() { return itemTracers; }
    public static synchronized boolean itemGlow() { return itemGlow; }
    public static synchronized boolean itemIcon() { return itemIcon; }
    public static synchronized boolean itemDistance() { return itemDistance; }
    public static synchronized boolean filterByRarity() { return filterByRarity; }
    public static synchronized boolean showCommon() { return showCommon; }
    public static synchronized boolean showUncommon() { return showUncommon; }
    public static synchronized boolean showRare() { return showRare; }
    public static synchronized boolean showEpic() { return showEpic; }
    public static synchronized boolean showLegendary() { return showLegendary; }
    public static synchronized boolean showMythic() { return showMythic; }
    public static synchronized boolean showXpOrbs() { return showXpOrbs; }
    public static synchronized boolean showOtherEntities() { return showOtherEntities; }
    public static synchronized String boxStyle() { return boxStyle; }
    public static synchronized String tracerStyle() { return tracerStyle; }
    public static synchronized int lineWidth() { return lineWidth; }
    public static synchronized int cornerRadius() { return cornerRadius; }
    public static synchronized int glowStrength() { return glowStrength; }
    public static synchronized int tracerThickness() { return tracerThickness; }

    public static synchronized int color(String slot) {
        return switch (slot) {
            case "common" -> common;
            case "uncommon" -> uncommon;
            case "rare" -> rare;
            case "epic" -> epic;
            case "legendary" -> legendary;
            case "mythic" -> mythic;
            case "text" -> text;
            case "tracer" -> tracer;
            default -> text;
        };
    }

    public static synchronized boolean enabled(String key) {
        return switch (key) {
            case "itemEsp" -> itemEsp;
            case "itemNames" -> itemNames;
            case "itemBoxes" -> itemBoxes;
            case "itemTracers" -> itemTracers;
            case "itemGlow" -> itemGlow;
            case "itemIcon" -> itemIcon;
            case "itemDistance" -> itemDistance;
            case "filterByRarity" -> filterByRarity;
            case "showCommon" -> showCommon;
            case "showUncommon" -> showUncommon;
            case "showRare" -> showRare;
            case "showEpic" -> showEpic;
            case "showLegendary" -> showLegendary;
            case "showMythic" -> showMythic;
            case "showXpOrbs" -> showXpOrbs;
            case "showOtherEntities" -> showOtherEntities;
            default -> false;
        };
    }

    public static synchronized void setBoolean(String key, boolean value) {
        switch (key) {
            case "itemEsp" -> itemEsp = value;
            case "itemNames" -> itemNames = value;
            case "itemBoxes" -> itemBoxes = value;
            case "itemTracers" -> itemTracers = value;
            case "itemGlow" -> itemGlow = value;
            case "itemIcon" -> itemIcon = value;
            case "itemDistance" -> itemDistance = value;
            case "filterByRarity" -> filterByRarity = value;
            case "showCommon" -> showCommon = value;
            case "showUncommon" -> showUncommon = value;
            case "showRare" -> showRare = value;
            case "showEpic" -> showEpic = value;
            case "showLegendary" -> showLegendary = value;
            case "showMythic" -> showMythic = value;
            case "showXpOrbs" -> showXpOrbs = value;
            case "showOtherEntities" -> showOtherEntities = value;
            default -> { return; }
        }
        save();
    }

    public static synchronized void setBoxStyle(String value) { boxStyle = safe(value, "Cornered"); save(); }
    public static synchronized void setTracerStyle(String value) { tracerStyle = safe(value, "Solid"); save(); }
    public static synchronized void setLineWidth(int value) { lineWidth = clamp(value, 5, 30); save(); }
    public static synchronized void setCornerRadius(int value) { cornerRadius = clamp(value, 0, 8); save(); }
    public static synchronized void setGlowStrength(int value) { glowStrength = clamp(value, 0, 100); save(); }
    public static synchronized void setTracerThickness(int value) { tracerThickness = clamp(value, 1, 4); save(); }

    public static synchronized void setColor(String slot, int value) {
        int opaque = value | 0xFF000000;
        switch (slot) {
            case "common" -> common = opaque;
            case "uncommon" -> uncommon = opaque;
            case "rare" -> rare = opaque;
            case "epic" -> epic = opaque;
            case "legendary" -> legendary = opaque;
            case "mythic" -> mythic = opaque;
            case "text" -> text = opaque;
            case "tracer" -> tracer = opaque;
            default -> { return; }
        }
        save();
    }

    public static synchronized void cycleColor(String slot) {
        int[] choices = switch (slot) {
            case "common" -> new int[]{0xFFFFFFFF, 0xFFE2E8F0, 0xFFFFE6B8};
            case "uncommon" -> new int[]{0xFF55FF88, 0xFF22C55E, 0xFF86EFAC};
            case "rare" -> new int[]{0xFF4A94FF, 0xFF38BDF8, 0xFF60A5FA};
            case "epic" -> new int[]{0xFFAA44FF, 0xFF8B5CF6, 0xFFC084FC};
            case "legendary" -> new int[]{0xFFFFCC33, 0xFFF59E0B, 0xFFFDE68A};
            case "mythic" -> new int[]{0xFFFF4455, 0xFFEF4444, 0xFFFF8A8A};
            case "tracer" -> new int[]{0xFF22D3EE, 0xFF06B6D4, 0xFF67E8F9};
            default -> new int[]{text};
        };
        int current = color(slot);
        int next = choices[0];
        for (int i = 0; i < choices.length; i++) if (choices[i] == current) next = choices[(i + 1) % choices.length];
        setColor(slot, next);
    }

    public static synchronized boolean rarityVisible(String rarity) {
        if (!filterByRarity) return true;
        return switch (rarity) {
            case "Common" -> showCommon;
            case "Uncommon" -> showUncommon;
            case "Rare" -> showRare;
            case "Epic" -> showEpic;
            case "Legendary" -> showLegendary;
            case "Mythic" -> showMythic;
            default -> true;
        };
    }

    public static synchronized void reset() {
        itemEsp = true;
        itemNames = true;
        itemBoxes = true;
        itemTracers = false;
        itemGlow = true;
        itemIcon = true;
        itemDistance = true;
        filterByRarity = true;
        showCommon = true;
        showUncommon = true;
        showRare = true;
        showEpic = true;
        showLegendary = true;
        showMythic = true;
        showXpOrbs = true;
        showOtherEntities = true;
        boxStyle = "Cornered";
        tracerStyle = "Solid";
        lineWidth = 15;
        cornerRadius = 2;
        glowStrength = 60;
        tracerThickness = 1;
        common = 0xFFFFFFFF;
        uncommon = 0xFF55FF88;
        rare = 0xFF4A94FF;
        epic = 0xFFAA44FF;
        legendary = 0xFFFFCC33;
        mythic = 0xFFFF4455;
        text = 0xFFFFFFFF;
        tracer = 0xFF22D3EE;
        save();
    }

    private static String safe(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }

    private static void load() {
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            p.load(reader);
        } catch (IOException ignored) {
            return;
        }
        itemEsp = bool(p, "itemEsp", itemEsp);
        itemNames = bool(p, "itemNames", itemNames);
        itemBoxes = bool(p, "itemBoxes", itemBoxes);
        itemTracers = bool(p, "itemTracers", itemTracers);
        itemGlow = bool(p, "itemGlow", itemGlow);
        itemIcon = bool(p, "itemIcon", itemIcon);
        itemDistance = bool(p, "itemDistance", itemDistance);
        filterByRarity = bool(p, "filterByRarity", filterByRarity);
        showCommon = bool(p, "showCommon", showCommon);
        showUncommon = bool(p, "showUncommon", showUncommon);
        showRare = bool(p, "showRare", showRare);
        showEpic = bool(p, "showEpic", showEpic);
        showLegendary = bool(p, "showLegendary", showLegendary);
        showMythic = bool(p, "showMythic", showMythic);
        showXpOrbs = bool(p, "showXpOrbs", showXpOrbs);
        showOtherEntities = bool(p, "showOtherEntities", showOtherEntities);
        boxStyle = p.getProperty("boxStyle", boxStyle);
        tracerStyle = p.getProperty("tracerStyle", tracerStyle);
        lineWidth = integer(p, "lineWidth", lineWidth, 5, 30);
        cornerRadius = integer(p, "cornerRadius", cornerRadius, 0, 8);
        glowStrength = integer(p, "glowStrength", glowStrength, 0, 100);
        tracerThickness = integer(p, "tracerThickness", tracerThickness, 1, 4);
        common = color(p, "common", common);
        uncommon = color(p, "uncommon", uncommon);
        rare = color(p, "rare", rare);
        epic = color(p, "epic", epic);
        legendary = color(p, "legendary", legendary);
        mythic = color(p, "mythic", mythic);
        text = color(p, "text", text);
        tracer = color(p, "tracer", tracer);
    }

    private static int color(Properties p, String key, int fallback) {
        try { return Integer.decode(p.getProperty(key, Integer.toString(fallback))) | 0xFF000000; }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static int integer(Properties p, String key, int fallback, int min, int max) {
        try { return clamp(Integer.parseInt(p.getProperty(key, Integer.toString(fallback))), min, max); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static boolean bool(Properties p, String key, boolean fallback) { return Boolean.parseBoolean(p.getProperty(key, Boolean.toString(fallback))); }

    private static void save() {
        Properties p = new Properties();
        p.setProperty("itemEsp", Boolean.toString(itemEsp));
        p.setProperty("itemNames", Boolean.toString(itemNames));
        p.setProperty("itemBoxes", Boolean.toString(itemBoxes));
        p.setProperty("itemTracers", Boolean.toString(itemTracers));
        p.setProperty("itemGlow", Boolean.toString(itemGlow));
        p.setProperty("itemIcon", Boolean.toString(itemIcon));
        p.setProperty("itemDistance", Boolean.toString(itemDistance));
        p.setProperty("filterByRarity", Boolean.toString(filterByRarity));
        p.setProperty("showCommon", Boolean.toString(showCommon));
        p.setProperty("showUncommon", Boolean.toString(showUncommon));
        p.setProperty("showRare", Boolean.toString(showRare));
        p.setProperty("showEpic", Boolean.toString(showEpic));
        p.setProperty("showLegendary", Boolean.toString(showLegendary));
        p.setProperty("showMythic", Boolean.toString(showMythic));
        p.setProperty("showXpOrbs", Boolean.toString(showXpOrbs));
        p.setProperty("showOtherEntities", Boolean.toString(showOtherEntities));
        p.setProperty("boxStyle", boxStyle);
        p.setProperty("tracerStyle", tracerStyle);
        p.setProperty("lineWidth", Integer.toString(lineWidth));
        p.setProperty("cornerRadius", Integer.toString(cornerRadius));
        p.setProperty("glowStrength", Integer.toString(glowStrength));
        p.setProperty("tracerThickness", Integer.toString(tracerThickness));
        p.setProperty("common", Integer.toString(common));
        p.setProperty("uncommon", Integer.toString(uncommon));
        p.setProperty("rare", Integer.toString(rare));
        p.setProperty("epic", Integer.toString(epic));
        p.setProperty("legendary", Integer.toString(legendary));
        p.setProperty("mythic", Integer.toString(mythic));
        p.setProperty("text", Integer.toString(text));
        p.setProperty("tracer", Integer.toString(tracer));
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) { p.store(writer, "Kairokk item ESP"); }
        } catch (IOException ignored) {
            // The in-memory controls remain usable if the profile cannot be written.
        }
    }
}
