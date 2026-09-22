package com.kairokk.client;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

/**
 * The persisted appearance contract for Kairokk's client UI.
 *
 * The screen deliberately talks to this class instead of keeping decorative
 * values in individual views. That means a change made in Misc is still the
 * active value when another Kairokk screen is opened later.
 */
public final class KairokkCustomizationState {
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("kairokk-customization.properties");

    private static final int DEFAULT_PRIMARY = 0xFF3B82F6;
    private static final int DEFAULT_SECONDARY = 0xFF8B5CF6;
    private static final int DEFAULT_ACCENT = 0xFF06B6D4;
    private static final int DEFAULT_DANGER = 0xFFEF4444;
    private static final int DEFAULT_PANEL = 0xFF152231;
    private static final int DEFAULT_PANEL_BORDER = 0xFF3B587C;
    private static final int DEFAULT_TEXT = 0xFFF0F4FF;
    private static final int DEFAULT_MUTED = 0xFFA8B4C8;

    private static String theme = "Default";
    private static int primary = DEFAULT_PRIMARY;
    private static int secondary = DEFAULT_SECONDARY;
    private static int accent = DEFAULT_ACCENT;
    private static int danger = DEFAULT_DANGER;
    private static int panel = DEFAULT_PANEL;
    private static int panelBorder = DEFAULT_PANEL_BORDER;
    private static int text = DEFAULT_TEXT;
    private static int muted = DEFAULT_MUTED;
    private static String fontFamily = "Inter";
    private static String fontWeight = "Normal";
    private static int fontSize = 14;
    private static int roundness = 8;
    private static int borderThickness = 1;
    private static int moduleSpacing = 6;
    private static int uiScale = 100;
    private static String backgroundStyle = "Blurred";
    private static int backgroundOpacity = 60;
    private static int animationSpeed = 100;
    private static boolean menuAnimations = true;
    private static boolean moduleAnimations = true;
    private static boolean smoothScrolling = true;
    private static boolean backgroundBlur = true;
    private static boolean animatedBackground = true;
    private static boolean particleEffects = true;
    private static boolean showClientLogo = true;
    private static boolean modernUiElements = true;
    private static boolean showModuleDescriptions = true;
    private static boolean compactMode;
    private static boolean nativeTitleBar;
    private static boolean interfaceSounds = true;
    // Title music is opt-in. A launcher restart must never silently start it.
    private static boolean titleMusicEnabled;
    private static boolean colorizeIcons = true;
    private static boolean transparentPanels;
    private static String customCss = "";

    static {
        load();
    }

    private KairokkCustomizationState() {
    }

    public static synchronized String theme() { return theme; }
    public static synchronized int primary() { return primary; }
    public static synchronized int secondary() { return secondary; }
    public static synchronized int accent() { return accent; }
    public static synchronized int danger() { return danger; }
    public static synchronized int panel() { return panel; }
    public static synchronized int panelBorder() { return panelBorder; }
    public static synchronized int textColor() { return text; }
    public static synchronized int mutedColor() { return muted; }
    public static synchronized int primaryLight() { return blend(primary, 0xFFFFFFFF, .28F); }
    public static synchronized String fontFamily() { return fontFamily; }
    public static synchronized String fontWeight() { return fontWeight; }
    public static synchronized int fontSize() { return fontSize; }
    public static synchronized int roundness() { return roundness; }
    public static synchronized int borderThickness() { return borderThickness; }
    public static synchronized int moduleSpacing() { return moduleSpacing; }
    public static synchronized int uiScale() { return uiScale; }
    public static synchronized String backgroundStyle() { return backgroundStyle; }
    public static synchronized int backgroundOpacity() { return backgroundOpacity; }
    public static synchronized int animationSpeed() { return animationSpeed; }
    public static synchronized boolean menuAnimations() { return menuAnimations; }
    public static synchronized boolean moduleAnimations() { return moduleAnimations; }
    public static synchronized boolean smoothScrolling() { return smoothScrolling; }
    public static synchronized boolean backgroundBlur() { return backgroundBlur; }
    public static synchronized boolean animatedBackground() { return animatedBackground; }
    public static synchronized boolean particleEffects() { return particleEffects; }
    public static synchronized boolean showClientLogo() { return showClientLogo; }
    public static synchronized boolean modernUiElements() { return modernUiElements; }
    public static synchronized boolean showModuleDescriptions() { return showModuleDescriptions; }
    public static synchronized boolean compactMode() { return compactMode; }
    public static synchronized boolean nativeTitleBar() { return nativeTitleBar; }
    public static synchronized boolean interfaceSounds() { return interfaceSounds; }
    public static synchronized boolean titleMusicEnabled() { return titleMusicEnabled; }
    public static synchronized boolean colorizeIcons() { return colorizeIcons; }
    public static synchronized boolean transparentPanels() { return transparentPanels; }
    public static synchronized String customCss() { return customCss; }

    /**
     * Reads a small, deliberately safe CSS-variable subset used by the live UI.
     * Values are optional; invalid declarations simply fall back to the normal
     * palette so an accidental typo can never break the screen.
     */
    public static synchronized int cssColor(String property, int fallback) {
        String value = cssValue(property);
        if (value == null) return fallback;
        String clean = value.trim().toLowerCase(Locale.ROOT);
        if (clean.startsWith("#")) clean = clean.substring(1);
        try {
            if (clean.length() == 6) return 0xFF000000 | Integer.parseInt(clean, 16);
            if (clean.length() == 8) return (int) Long.parseLong(clean, 16);
        } catch (NumberFormatException ignored) {
            // Keep the live UI usable when a custom declaration is malformed.
        }
        return fallback;
    }

    /** Applies the selected panel tint while preserving each control's intended alpha. */
    public static synchronized int panelColor(int fallback) {
        return withAlpha(cssColor("panel", panel), fallback);
    }

    /** Applies the selected panel border tint while preserving each outline's alpha. */
    public static synchronized int panelBorderColor(int fallback) {
        return withAlpha(cssColor("border", panelBorder), fallback);
    }

    public static synchronized float cssPixels(String property, float fallback) {
        String value = cssValue(property);
        if (value == null) return fallback;
        String clean = value.trim().toLowerCase(Locale.ROOT).replace("px", "").trim();
        try { return Float.parseFloat(clean); }
        catch (NumberFormatException ignored) { return fallback; }
    }

    private static String cssValue(String property) {
        if (customCss == null || customCss.isBlank()) return null;
        String key = property.startsWith("--") ? property : "--" + property;
        for (String declaration : customCss.split(";")) {
            int colon = declaration.indexOf(':');
            if (colon < 0) continue;
            String name = declaration.substring(0, colon).trim();
            if (key.equalsIgnoreCase(name)) return declaration.substring(colon + 1).trim();
        }
        return null;
    }

    public static synchronized void applyTheme(String value) {
        String next = value == null ? "Default" : value;
        theme = next;
        switch (next) {
            case "Dark" -> setColors(0xFF60A5FA, 0xFF818CF8, 0xFF22D3EE, 0xFFF87171, 0xFF101827, 0xFF334155, 0xFFF8FAFC, 0xFF94A3B8);
            case "Light" -> setColors(0xFF2563EB, 0xFF7C3AED, 0xFF0891B2, 0xFFDC2626, 0xFFF1F5F9, 0xFFCBD5E1, 0xFF172033, 0xFF64748B);
            case "Abyss" -> setColors(0xFF3B82F6, 0xFF6366F1, 0xFF0EA5E9, 0xFF60A5FA, 0xFF0B1220, 0xFF263A5C, 0xFFE8F0FF, 0xFF93A9C8);
            case "Forest" -> setColors(0xFF22C55E, 0xFF14B8A6, 0xFF84CC16, 0xFFEF4444, 0xFF10251F, 0xFF2E5A4A, 0xFFE9FFF3, 0xFF9BC7B2);
            case "Solar" -> setColors(0xFFF59E0B, 0xFFEF4444, 0xFFF97316, 0xFFDC2626, 0xFF2A1D12, 0xFF76522A, 0xFFFFF7E8, 0xFFD6B77D);
            case "Default" -> setColors(DEFAULT_PRIMARY, DEFAULT_SECONDARY, DEFAULT_ACCENT, DEFAULT_DANGER, DEFAULT_PANEL, DEFAULT_PANEL_BORDER, DEFAULT_TEXT, DEFAULT_MUTED);
            case "Custom" -> { /* Keep the hand-picked palette. */ }
            default -> {
                theme = "Default";
                setColors(DEFAULT_PRIMARY, DEFAULT_SECONDARY, DEFAULT_ACCENT, DEFAULT_DANGER);
            }
        }
        save();
    }

    public static synchronized void setColor(String slot, int value) {
        int opaque = value | 0xFF000000;
        switch (slot.toLowerCase(Locale.ROOT)) {
            case "primary" -> primary = opaque;
            case "secondary" -> secondary = opaque;
            case "accent" -> accent = opaque;
            case "danger" -> danger = opaque;
            case "panel" -> panel = opaque;
            case "border" -> panelBorder = opaque;
            case "text" -> text = opaque;
            case "muted" -> muted = opaque;
            default -> { return; }
        }
        theme = "Custom";
        save();
    }

    /** Restores only the displayed theme name after cancelling a live colour edit. */
    public static synchronized void setThemeName(String value) { theme = safe(value, "Default"); save(); }

    public static synchronized void setFontFamily(String value) { fontFamily = safe(value, "Inter"); save(); }
    public static synchronized void setFontWeight(String value) { fontWeight = safe(value, "Normal"); save(); }
    public static synchronized void setFontSize(int value) { fontSize = clamp(value, 10, 22); save(); }
    public static synchronized void setRoundness(int value) { roundness = clamp(value, 0, 16); save(); }
    public static synchronized void setBorderThickness(int value) { borderThickness = clamp(value, 0, 3); save(); }
    public static synchronized void setModuleSpacing(int value) { moduleSpacing = clamp(value, 0, 16); save(); }
    public static synchronized void setUiScale(int value) { uiScale = clamp(value, 75, 125); save(); }
    public static synchronized void setBackgroundStyle(String value) { backgroundStyle = safe(value, "Blurred"); save(); }
    public static synchronized void setBackgroundOpacity(int value) { backgroundOpacity = clamp(value, 0, 100); save(); }
    public static synchronized void setAnimationSpeed(int value) { animationSpeed = clamp(value, 50, 150); save(); }
    public static synchronized void setCustomCss(String value) { customCss = value == null ? "" : value; save(); }

    public static synchronized void setBoolean(String key, boolean value) {
        switch (key) {
            case "menuAnimations" -> menuAnimations = value;
            case "moduleAnimations" -> moduleAnimations = value;
            case "smoothScrolling" -> smoothScrolling = value;
            case "backgroundBlur" -> backgroundBlur = value;
            case "animatedBackground" -> animatedBackground = value;
            case "particleEffects" -> particleEffects = value;
            case "showClientLogo" -> showClientLogo = value;
            case "modernUiElements" -> modernUiElements = value;
            case "showModuleDescriptions" -> showModuleDescriptions = value;
            case "compactMode" -> compactMode = value;
            case "nativeTitleBar" -> nativeTitleBar = value;
            case "interfaceSounds" -> interfaceSounds = value;
            case "titleMusicEnabled" -> titleMusicEnabled = value;
            case "colorizeIcons" -> colorizeIcons = value;
            case "transparentPanels" -> transparentPanels = value;
            default -> { return; }
        }
        save();
    }

    public static synchronized void reset() {
        theme = "Default";
        primary = DEFAULT_PRIMARY;
        secondary = DEFAULT_SECONDARY;
        accent = DEFAULT_ACCENT;
        danger = DEFAULT_DANGER;
        panel = DEFAULT_PANEL;
        panelBorder = DEFAULT_PANEL_BORDER;
        text = DEFAULT_TEXT;
        muted = DEFAULT_MUTED;
        fontFamily = "Inter";
        fontWeight = "Normal";
        fontSize = 14;
        roundness = 8;
        borderThickness = 1;
        moduleSpacing = 6;
        uiScale = 100;
        backgroundStyle = "Blurred";
        backgroundOpacity = 60;
        animationSpeed = 100;
        menuAnimations = true;
        moduleAnimations = true;
        smoothScrolling = true;
        backgroundBlur = true;
        animatedBackground = true;
        particleEffects = true;
        showClientLogo = true;
        modernUiElements = true;
        showModuleDescriptions = true;
        compactMode = false;
        nativeTitleBar = false;
        interfaceSounds = true;
        titleMusicEnabled = false;
        colorizeIcons = true;
        transparentPanels = false;
        customCss = "";
        save();
    }

    private static void setColors(int primaryValue, int secondaryValue, int accentValue, int dangerValue) {
        primary = primaryValue;
        secondary = secondaryValue;
        accent = accentValue;
        danger = dangerValue;
    }

    private static void setColors(int primaryValue, int secondaryValue, int accentValue, int dangerValue,
                                  int panelValue, int borderValue, int textValue, int mutedValue) {
        setColors(primaryValue, secondaryValue, accentValue, dangerValue);
        panel = panelValue;
        panelBorder = borderValue;
        text = textValue;
        muted = mutedValue;
    }

    private static String safe(String value, String fallback) { return value == null || value.isBlank() ? fallback : value; }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static int blend(int from, int to, float amount) {
        int r = Math.round(((from >> 16) & 255) * (1 - amount) + ((to >> 16) & 255) * amount);
        int g = Math.round(((from >> 8) & 255) * (1 - amount) + ((to >> 8) & 255) * amount);
        int b = Math.round((from & 255) * (1 - amount) + (to & 255) * amount);
        return 0xFF000000 | r << 16 | g << 8 | b;
    }

    private static int withAlpha(int color, int alphaSource) {
        return (alphaSource & 0xFF000000) | (color & 0x00FFFFFF);
    }

    private static void load() {
        Properties p = new Properties();
        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            p.load(reader);
        } catch (IOException ignored) {
            return;
        }
        theme = p.getProperty("theme", theme);
        primary = color(p, "primary", primary);
        secondary = color(p, "secondary", secondary);
        accent = color(p, "accent", accent);
        danger = color(p, "danger", danger);
        panel = color(p, "panel", panel);
        panelBorder = color(p, "panelBorder", panelBorder);
        text = color(p, "text", text);
        muted = color(p, "muted", muted);
        fontFamily = p.getProperty("fontFamily", fontFamily);
        fontWeight = p.getProperty("fontWeight", fontWeight);
        fontSize = integer(p, "fontSize", fontSize, 10, 22);
        roundness = integer(p, "roundness", roundness, 0, 16);
        borderThickness = integer(p, "borderThickness", borderThickness, 0, 3);
        moduleSpacing = integer(p, "moduleSpacing", moduleSpacing, 0, 16);
        uiScale = integer(p, "uiScale", uiScale, 75, 125);
        backgroundStyle = p.getProperty("backgroundStyle", backgroundStyle);
        backgroundOpacity = integer(p, "backgroundOpacity", backgroundOpacity, 0, 100);
        animationSpeed = integer(p, "animationSpeed", animationSpeed, 50, 150);
        menuAnimations = bool(p, "menuAnimations", menuAnimations);
        moduleAnimations = bool(p, "moduleAnimations", moduleAnimations);
        smoothScrolling = bool(p, "smoothScrolling", smoothScrolling);
        backgroundBlur = bool(p, "backgroundBlur", backgroundBlur);
        animatedBackground = bool(p, "animatedBackground", animatedBackground);
        particleEffects = bool(p, "particleEffects", particleEffects);
        showClientLogo = bool(p, "showClientLogo", showClientLogo);
        modernUiElements = bool(p, "modernUiElements", modernUiElements);
        showModuleDescriptions = bool(p, "showModuleDescriptions", showModuleDescriptions);
        compactMode = bool(p, "compactMode", compactMode);
        nativeTitleBar = bool(p, "nativeTitleBar", nativeTitleBar);
        interfaceSounds = bool(p, "interfaceSounds", interfaceSounds);
        titleMusicEnabled = bool(p, "titleMusicEnabled", titleMusicEnabled);
        colorizeIcons = bool(p, "colorizeIcons", colorizeIcons);
        transparentPanels = bool(p, "transparentPanels", transparentPanels);
        customCss = p.getProperty("customCss", customCss);
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
        p.setProperty("theme", theme);
        p.setProperty("primary", Integer.toString(primary));
        p.setProperty("secondary", Integer.toString(secondary));
        p.setProperty("accent", Integer.toString(accent));
        p.setProperty("danger", Integer.toString(danger));
        p.setProperty("panel", Integer.toString(panel));
        p.setProperty("panelBorder", Integer.toString(panelBorder));
        p.setProperty("text", Integer.toString(text));
        p.setProperty("muted", Integer.toString(muted));
        p.setProperty("fontFamily", fontFamily);
        p.setProperty("fontWeight", fontWeight);
        p.setProperty("fontSize", Integer.toString(fontSize));
        p.setProperty("roundness", Integer.toString(roundness));
        p.setProperty("borderThickness", Integer.toString(borderThickness));
        p.setProperty("moduleSpacing", Integer.toString(moduleSpacing));
        p.setProperty("uiScale", Integer.toString(uiScale));
        p.setProperty("backgroundStyle", backgroundStyle);
        p.setProperty("backgroundOpacity", Integer.toString(backgroundOpacity));
        p.setProperty("animationSpeed", Integer.toString(animationSpeed));
        p.setProperty("menuAnimations", Boolean.toString(menuAnimations));
        p.setProperty("moduleAnimations", Boolean.toString(moduleAnimations));
        p.setProperty("smoothScrolling", Boolean.toString(smoothScrolling));
        p.setProperty("backgroundBlur", Boolean.toString(backgroundBlur));
        p.setProperty("animatedBackground", Boolean.toString(animatedBackground));
        p.setProperty("particleEffects", Boolean.toString(particleEffects));
        p.setProperty("showClientLogo", Boolean.toString(showClientLogo));
        p.setProperty("modernUiElements", Boolean.toString(modernUiElements));
        p.setProperty("showModuleDescriptions", Boolean.toString(showModuleDescriptions));
        p.setProperty("compactMode", Boolean.toString(compactMode));
        p.setProperty("nativeTitleBar", Boolean.toString(nativeTitleBar));
        p.setProperty("interfaceSounds", Boolean.toString(interfaceSounds));
        p.setProperty("titleMusicEnabled", Boolean.toString(titleMusicEnabled));
        p.setProperty("colorizeIcons", Boolean.toString(colorizeIcons));
        p.setProperty("transparentPanels", Boolean.toString(transparentPanels));
        p.setProperty("customCss", customCss);
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) { p.store(writer, "Kairokk client customization"); }
        } catch (IOException ignored) {
            // The running UI still works if a read-only profile prevents saving.
        }
    }
}
