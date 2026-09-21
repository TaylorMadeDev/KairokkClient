package com.kairokk.client;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.animation.ObjectAnimator;
import icyllis.modernui.core.Context;
import icyllis.modernui.core.Core;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.graphics.drawable.StateListDrawable;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.text.Editable;
import icyllis.modernui.text.TextWatcher;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.EditText;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.InactivityFpsLimit;
import net.minecraft.sounds.SoundSource;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.resources.language.LanguageInfo;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.packs.PackSelectionScreen;
import net.minecraft.client.gui.screens.telemetry.TelemetryInfoScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.client.NarratorStatus;
import net.minecraft.world.entity.player.ChatVisiblity;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;

/** ModernUI implementation shared by every Shulk settings page. */
public final class ShulkSettingsFragment extends Fragment {
	@Nullable private static KeyMapping pendingKeybind;
	@Nullable private static TextView pendingKeybindLabel;
	private final Screen parent;
	private final String pageTitle;
	@Nullable private FrameLayout rootView;

	public ShulkSettingsFragment(Screen parent, String pageTitle) {
		this.parent = parent;
		this.pageTitle = pageTitle;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable DataSet savedInstanceState) {
		Context context = requireContext();
		FrameLayout root = new FrameLayout(context);
		rootView = root;
		FrameLayout body = new FrameLayout(context);
		FrameLayout.LayoutParams bodyParams = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		bodyParams.bottomMargin = px(ShulkTitleFragment.FOOTER_HEIGHT_DP);
		root.addView(body, bodyParams);
		LinearLayout content = new LinearLayout(context);
		content.setOrientation(LinearLayout.VERTICAL);
		content.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.CENTER_VERTICAL);
		if ("Sounds".equals(pageTitle)) {
			// Keep the sound page compact while retaining vertical centering in its
			// available area above the persistent footer.
			TextView heading = text(context, pageTitle, 24, 0xFFEEE8EC);
			heading.setGravity(Gravity.CENTER);
			LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, px(35));
			headingParams.bottomMargin = px(10);
			content.addView(heading, headingParams);
		} else {
			addBrand(context, content);
		}
		if ("Settings".equals(pageTitle)) addHub(context, content); else if ("Video Settings".equals(pageTitle)) addDisplay(context, content); else if ("Sounds".equals(pageTitle)) addSounds(context, content); else if ("Controls".equals(pageTitle)) addControls(context, content); else if ("Language".equals(pageTitle)) addLanguage(context, content); else if ("Chat Settings".equals(pageTitle)) addChatSettings(context, content); else if ("Accessibility".equals(pageTitle)) addAccessibilitySettings(context, content); else addCategory(context, content);
		if ("Sounds".equals(pageTitle)) {
			// Measure the sound page at its natural height, then center that compact
			// group in the area above the footer. Fill-viewport sizing was stretching
			// the child and separating the sliders from the option buttons.
			ScrollView scroll = new ScrollView(context);
			scroll.setFillViewport(false);
			scroll.setEdgeEffectColor(0xFF3D8BFF);
			scroll.setBottomEdgeEffectColor(0xFF3D8BFF);
			scroll.addView(content, new ScrollView.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			body.addView(scroll, new FrameLayout.LayoutParams(
				px(640), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		} else if ("Controls".equals(pageTitle) || "Language".equals(pageTitle) || "Accessibility".equals(pageTitle)) {
			ScrollView scroll = new ScrollView(context);
			scroll.setFillViewport(true);
			scroll.setEdgeEffectColor(0xFF3D8BFF);
			scroll.setBottomEdgeEffectColor(0xFF3D8BFF);
			// Keep longer settings pages in a normal scroll child so every option
			// remains reachable when the footer reduces the available viewport height.
			scroll.addView(content, new ScrollView.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
			int available = Minecraft.getInstance().getWindow().getScreenHeight()
				- px(ShulkTitleFragment.FOOTER_HEIGHT_DP + 34);
			body.addView(scroll, new FrameLayout.LayoutParams(px(640), Math.max(px(280), available), Gravity.CENTER));
		} else {
			FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(px(640), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
			body.addView(content, params);
		}
		ShulkTitleFragment.addFooter(root, context);
		KairokkScreenTransitions.fadeIn(root);
		return root;
	}

	private void addBrand(Context context, LinearLayout content) {
		LinearLayout brand = new LinearLayout(context);
		brand.setGravity(Gravity.CENTER);
		TextView title = text(context, "K A I R O K K", 34, 0xFFF4F0F2);
		TextView version = text(context, "v0.1.0", 13, 0xFF3D8BFF);
		brand.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(54)));
		brand.addView(version, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(54)));
		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(54));
		p.bottomMargin = px(18); content.addView(brand, p);
		if (!"Settings".equals(pageTitle)) {
			TextView heading = text(context, pageTitle, 24, 0xFFEEE8EC);
			LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(35));
			hp.bottomMargin = px(10); content.addView(heading, hp);
		}
	}

	private void addHub(Context context, LinearLayout content) {
		Options options = Minecraft.getInstance().options;
		addIntSlider(context, content, "Field of View", options.fov().get(), 30, 110,
			value -> { options.fov().set(value); options.save(); }, value -> value + "°");
		String[][] pages = {{"Skin Customization", "Sounds"}, {"Video Settings", "Controls"}, {"Language", "Chat Settings"}, {"Resource Packs", "Accessibility"}, {"Telemetry", "Credits"}};
		for (String[] row : pages) addPageRow(context, content, row[0], row[1]);
		LinearLayout customRow = new LinearLayout(context); customRow.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout labels = new LinearLayout(context); labels.setOrientation(LinearLayout.VERTICAL);
		TextView label = text(context, "Custom Screens", 20, 0xFFF0EAEE); label.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		TextView detail = text(context, "Styled Shulk UI for menus", 15, 0xFF807781); detail.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		labels.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(27)));
		labels.addView(detail, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(22)));
		customRow.addView(labels, new LinearLayout.LayoutParams(0, px(49), 1));
		Button enabled = button(context, ShulkUiState.customScreensEnabled() ? "ON" : "OFF", () -> { });
		enabled.setTextColor(0xFFF4F0F2); enabled.setBackground(blueBackground());
		enabled.setOnClickListener(v -> {
			boolean isEnabled = ShulkUiState.toggleCustomScreens();
			enabled.setText(isEnabled ? "ON" : "OFF");
		});
		LinearLayout.LayoutParams enabledParams = new LinearLayout.LayoutParams(px(64), px(32)); enabledParams.leftMargin = px(12); customRow.addView(enabled, enabledParams);
		LinearLayout.LayoutParams customParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(52)); customParams.topMargin = px(12); content.addView(customRow, customParams);
		content.addView(navigationButton(context, "Done", () -> Minecraft.getInstance().setScreen(parent)), buttonParams(px(400), px(43), px(18)));
	}

	private void addCategory(Context context, LinearLayout content) {
		String[] labels = {"Maximum FPS", "VSync", "Reduce FPS", "Interface Scale", "Fullscreen", "Brightness", "Render Distance", "Simulation Distance", "Clouds", "Particles"};
		String[] values = {"120 FPS", "Off", "When Minimized", "Auto", "Off", "Default", "16 chunks", "12 chunks", "Off", "All"};
		for (int i = 0; i < labels.length; i++) {
			if (i == 0 || i == 6) addSection(context, content, i == 0 ? "DISPLAY" : "QUALITY & PERFORMANCE");
			if (i == 0 || i == 5 || i == 6 || i == 7) addSlider(context, content, labels[i], values[i], i == 0 ? .45F : .60F);
			else addSetting(context, content, labels[i], values[i]);
		}
		content.addView(navigationButton(context, "Back", () -> Minecraft.getInstance().setScreen(new ShulkSettingsScreen(parent))), buttonParams(px(400), px(43), 0));
	}

	/** Fully functional replacement for Minecraft's display/video menu. */
	private void addDisplay(Context context, LinearLayout content) {
		Options options = Minecraft.getInstance().options;
		addSection(context, content, "DISPLAY");
		addIntSlider(context, content, "Maximum FPS", options.framerateLimit().get(), 10, 260,
			value -> options.framerateLimit().set(value), value -> value >= 260 ? "Unlimited" : value + " FPS");
		addActionSetting(context, content, "VSync", () -> options.enableVsync().get() ? "On" : "Off", () -> {
			options.enableVsync().set(!options.enableVsync().get()); });
		addActionSetting(context, content, "Reduce FPS", () -> inactivityText(options.inactivityFpsLimit().get()), () -> {
			InactivityFpsLimit[] values = InactivityFpsLimit.values(); int next = (options.inactivityFpsLimit().get().ordinal() + 1) % values.length; options.inactivityFpsLimit().set(values[next]); });
		addActionSetting(context, content, "Interface Scale", () -> guiScaleText(options.guiScale().get()), () -> {
			int current = options.guiScale().get(); options.guiScale().set(current >= 4 ? 0 : current + 1); });
		addActionSetting(context, content, "Fullscreen", () -> options.fullscreen().get() ? "On" : "Off", () -> {
			options.fullscreen().set(!options.fullscreen().get()); });
		addIntSlider(context, content, "Brightness", (int) Math.round(options.gamma().get() * 100), 0, 100,
			value -> options.gamma().set(value / 100.0), value -> value == 50 ? "Default" : value + "%");
		addSection(context, content, "QUALITY & PERFORMANCE");
		addIntSlider(context, content, "Render Distance", options.renderDistance().get(), 2, 32,
			value -> options.renderDistance().set(value), value -> value + " chunks");
		addIntSlider(context, content, "Simulation Distance", options.simulationDistance().get(), 2, 32,
			value -> options.simulationDistance().set(value), value -> value + " chunks");
		addActionSetting(context, content, "Clouds", () -> cloudText(options.cloudStatus().get()), () -> {
			CloudStatus[] values = CloudStatus.values(); int next = (options.cloudStatus().get().ordinal() + 1) % values.length; options.cloudStatus().set(values[next]); });
		content.addView(navigationButton(context, "Back", () -> Minecraft.getInstance().setScreen(new ShulkSettingsScreen(parent))), buttonParams(px(400), px(43), px(12)));
	}

	/** Sound controls backed by Minecraft's real SoundSource option instances. */
	private void addSounds(Context context, LinearLayout content) {
		Minecraft minecraft = Minecraft.getInstance();
		Options options = Minecraft.getInstance().options;
		LinearLayout master = createSoundSlider(context, options, SoundSource.MASTER, "Master Volume");
		LinearLayout.LayoutParams masterParams = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		masterParams.bottomMargin = px(6);
		content.addView(master, masterParams);

		SoundSource[] sources = {
			SoundSource.MUSIC, SoundSource.RECORDS,
			SoundSource.WEATHER, SoundSource.BLOCKS,
			SoundSource.HOSTILE, SoundSource.NEUTRAL,
			SoundSource.PLAYERS, SoundSource.AMBIENT,
			SoundSource.VOICE
		};
		String[] labels = {
			"Music", "Jukebox / Note Blocks",
			"Weather", "Blocks",
			"Hostile Creatures", "Friendly Creatures",
			"Players", "Ambient / Environment",
			"Voice / Speech"
		};
		for (int index = 0; index < sources.length; index += 2) {
			LinearLayout columns = new LinearLayout(context);
			columns.setOrientation(LinearLayout.HORIZONTAL);
			columns.setGravity(Gravity.CENTER_HORIZONTAL);

			LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
				0, ViewGroup.LayoutParams.WRAP_CONTENT, 1F);
			leftParams.rightMargin = px(9);
			columns.addView(createSoundSlider(context, options, sources[index], labels[index]), leftParams);

			LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
				0, ViewGroup.LayoutParams.WRAP_CONTENT, 1F);
			rightParams.leftMargin = px(9);
			if (index + 1 < sources.length) {
				columns.addView(createSoundSlider(context, options, sources[index + 1], labels[index + 1]), rightParams);
			} else {
				columns.addView(new View(context), rightParams);
			}

			LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			rowParams.bottomMargin = px(3);
			content.addView(columns, rowParams);
		}

		Button device = liveOptionButton(context,
			() -> "Device: " + soundDeviceName(options.soundDevice().get()),
			() -> {
				List<String> devices = new ArrayList<>();
				devices.add("");
				devices.addAll(minecraft.getSoundManager().getAvailableSoundDevices());
				String current = options.soundDevice().get();
				int currentIndex = devices.indexOf(current);
				String next = devices.get((Math.max(-1, currentIndex) + 1) % devices.size());
				options.soundDevice().set(next);
				options.save();
			});
		LinearLayout.LayoutParams deviceParams = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, px(43));
		deviceParams.topMargin = 0;
		deviceParams.bottomMargin = px(4);
		content.addView(device, deviceParams);

		LinearLayout toggles = new LinearLayout(context);
		toggles.setOrientation(LinearLayout.HORIZONTAL);
		Button subtitles = liveOptionButton(context,
			() -> "Show Subtitles: " + (options.showSubtitles().get() ? "ON" : "OFF"),
			() -> {
				options.showSubtitles().set(!options.showSubtitles().get());
				options.save();
			});
		Button directional = liveOptionButton(context,
			() -> "Directional Audio: " + (options.directionalAudio().get() ? "ON" : "OFF"),
			() -> {
				options.directionalAudio().set(!options.directionalAudio().get());
				options.save();
			});
		LinearLayout.LayoutParams subtitlesParams = new LinearLayout.LayoutParams(0, px(43), 1F);
		subtitlesParams.rightMargin = px(9);
		toggles.addView(subtitles, subtitlesParams);
		LinearLayout.LayoutParams directionalParams = new LinearLayout.LayoutParams(0, px(43), 1F);
		directionalParams.leftMargin = px(9);
		toggles.addView(directional, directionalParams);
		content.addView(toggles, new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, px(43)));

		content.addView(navigationButton(context, "Done", () -> {
			options.save();
			Minecraft.getInstance().setScreen(new ShulkSettingsScreen(parent));
		}), buttonParams(px(400), px(43), px(7)));
	}

	/** Kairokk-styled two-column replacement for Minecraft's chat configuration. */
	private void addChatSettings(Context context, LinearLayout content) {
		Options options = Minecraft.getInstance().options;
		addSection(context, content, "CHAT");

		Button visibility = liveOptionButton(context,
			() -> "Chat: " + options.chatVisibility().get().caption().getString(), () -> {
				ChatVisiblity[] values = ChatVisiblity.values();
				int current = options.chatVisibility().get().ordinal();
				options.chatVisibility().set(values[(current + 1) % values.length]); options.save();
			});
		Button colors = liveOptionButton(context, () -> "Colors: " + onOff(options.chatColors().get()),
			() -> { options.chatColors().set(!options.chatColors().get()); options.save(); });
		addChatRow(context, content, visibility, colors, px(43), px(9));

		Button links = liveOptionButton(context, () -> "Web Links: " + onOff(options.chatLinks().get()),
			() -> { options.chatLinks().set(!options.chatLinks().get()); options.save(); });
		Button prompt = liveOptionButton(context, () -> "Prompt on Links: " + onOff(options.chatLinksPrompt().get()),
			() -> { options.chatLinksPrompt().set(!options.chatLinksPrompt().get()); options.save(); });
		addChatRow(context, content, links, prompt, px(43), px(10));

		addChatRow(context, content,
			createIntSlider(context, "Chat Text Opacity", percent(options.chatOpacity().get()), 0, 100,
				value -> { options.chatOpacity().set(value / 100D); options.save(); }, ShulkSettingsFragment::percentText),
			createIntSlider(context, "Chat Text Size", percent(options.chatScale().get()), 0, 100,
				value -> { options.chatScale().set(value / 100D); options.save(); }, ShulkSettingsFragment::percentText),
			px(54), px(9));

		addChatRow(context, content,
			createIntSlider(context, "Line Spacing", percent(options.chatLineSpacing().get()), 0, 100,
				value -> { options.chatLineSpacing().set(value / 100D); options.save(); }, ShulkSettingsFragment::percentText),
			createIntSlider(context, "Width", percent(options.chatWidth().get()), 0, 100,
				value -> { options.chatWidth().set(value / 100D); options.save(); }, ShulkSettingsFragment::percentText),
			px(54), px(9));

		addChatRow(context, content,
			createIntSlider(context, "Focused Height", percent(options.chatHeightFocused().get()), 0, 100,
				value -> { options.chatHeightFocused().set(value / 100D); options.save(); }, ShulkSettingsFragment::percentText),
			createIntSlider(context, "Unfocused Height", percent(options.chatHeightUnfocused().get()), 0, 100,
				value -> { options.chatHeightUnfocused().set(value / 100D); options.save(); }, ShulkSettingsFragment::percentText),
			px(54), px(9));

		addChatRow(context, content,
			createIntSlider(context, "Chat Delay", percent(options.chatDelay().get()), 0, 100,
				value -> { options.chatDelay().set(value / 100D); options.save(); }, ShulkSettingsFragment::percentText),
			liveOptionButton(context, () -> "Narrator: " + options.narrator().get().getName().getString(), () -> {
				NarratorStatus[] values = NarratorStatus.values(); int current = options.narrator().get().ordinal();
				options.narrator().set(values[(current + 1) % values.length]); options.save();
			}), px(54), px(10));

		Button secure = liveOptionButton(context, () -> "Only Secure Chat: " + onOff(options.onlyShowSecureChat().get()),
			() -> { options.onlyShowSecureChat().set(!options.onlyShowSecureChat().get()); options.save(); });
		Button drafts = liveOptionButton(context, () -> "Save Chat Drafts: " + onOff(options.saveChatDrafts().get()),
			() -> { options.saveChatDrafts().set(!options.saveChatDrafts().get()); options.save(); });
		addChatRow(context, content, secure, drafts, px(43), px(14));

		content.addView(navigationButton(context, "Back", () -> {
			options.save();
			Minecraft.getInstance().setScreen(new ShulkSettingsScreen(parent));
		}), buttonParams(px(400), px(43), 0));
	}

	/** Kairokk-styled accessibility controls, using the game's real options. */
	private void addAccessibilitySettings(Context context, LinearLayout content) {
		Options options = Minecraft.getInstance().options;
		addSection(context, content, "ACCESSIBILITY");
		Button narrator = liveOptionButton(context, () -> "Narrator: " + options.narrator().get().getName().getString(), () -> {
			NarratorStatus[] values = NarratorStatus.values(); int current = options.narrator().get().ordinal();
			options.narrator().set(values[(current + 1) % values.length]); options.save();
		});
		Button subtitles = liveOptionButton(context, () -> "Show Subtitles: " + onOff(options.showSubtitles().get()), () -> { options.showSubtitles().set(!options.showSubtitles().get()); options.save(); });
		addChatRow(context, content, narrator, subtitles, px(43), px(9));

		Button contrast = liveOptionButton(context, () -> "High Contrast: " + onOff(options.highContrast().get()), () -> { options.highContrast().set(!options.highContrast().get()); options.save(); });
		Button jump = liveOptionButton(context, () -> "Auto-Jump: " + onOff(options.autoJump().get()), () -> { options.autoJump().set(!options.autoJump().get()); options.save(); });
		addChatRow(context, content, contrast, jump, px(43), px(9));

		addChatRow(context, content,
			createIntSlider(context, "Text Background Opacity", percent(options.textBackgroundOpacity().get()), 0, 100, value -> { options.textBackgroundOpacity().set(value / 100D); options.save(); }, ShulkSettingsFragment::percentText),
			liveOptionButton(context, () -> "Text Background: " + (options.backgroundForChatOnly().get() ? "Chat" : "Everywhere"), () -> { options.backgroundForChatOnly().set(!options.backgroundForChatOnly().get()); options.save(); }), px(54), px(9));

		addChatRow(context, content,
			createIntSlider(context, "Chat Text Opacity", percent(options.chatOpacity().get()), 0, 100, value -> { options.chatOpacity().set(value / 100D); options.save(); }, ShulkSettingsFragment::percentText),
			createIntSlider(context, "Line Spacing", percent(options.chatLineSpacing().get()), 0, 100, value -> { options.chatLineSpacing().set(value / 100D); options.save(); }, ShulkSettingsFragment::percentText), px(54), px(9));

		addChatRow(context, content,
			liveOptionButton(context, () -> "Sneak: " + (options.toggleCrouch().get() ? "Toggle" : "Hold"), () -> { options.toggleCrouch().set(!options.toggleCrouch().get()); options.save(); }),
			liveOptionButton(context, () -> "Sprint: " + (options.toggleSprint().get() ? "Toggle" : "Hold"), () -> { options.toggleSprint().set(!options.toggleSprint().get()); options.save(); }), px(43), px(9));

		addChatRow(context, content,
			createIntSlider(context, "Distortion Effects", percent(options.screenEffectScale().get()), 0, 100, value -> { options.screenEffectScale().set(value / 100D); options.save(); }, ShulkSettingsFragment::percentText),
			createIntSlider(context, "FOV Effects", percent(options.fovEffectScale().get()), 0, 100, value -> { options.fovEffectScale().set(value / 100D); options.save(); }, ShulkSettingsFragment::percentText), px(54), px(9));

		addChatRow(context, content,
			createIntSlider(context, "Notification Time", (int) Math.round(options.notificationDisplayTime().get() * 10D), 5, 30, value -> { options.notificationDisplayTime().set(value / 10D); options.save(); }, value -> String.format(Locale.ROOT, "%.1fx", value / 10D)),
			liveOptionButton(context, () -> "High Contrast Outline: " + onOff(options.highContrastBlockOutline().get()), () -> { options.highContrastBlockOutline().set(!options.highContrastBlockOutline().get()); options.save(); }), px(54), px(14));

		content.addView(navigationButton(context, "Back", () -> { options.save(); Minecraft.getInstance().setScreen(new ShulkSettingsScreen(parent)); }), buttonParams(px(400), px(43), 0));
	}

	private static int percent(double value) { return Math.round((float) value * 100F); }
	private static String percentText(int value) { return value + "%"; }
	private static String onOff(boolean value) { return value ? "ON" : "OFF"; }

	private void addChatRow(Context context, LinearLayout content, View left, View right, int height, int bottomMargin) {
		LinearLayout row = new LinearLayout(context);
		row.setGravity(Gravity.CENTER_HORIZONTAL);
		LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, height, 1F);
		leftParams.rightMargin = px(9);
		row.addView(left, leftParams);
		LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, height, 1F);
		rightParams.leftMargin = px(9);
		row.addView(right, rightParams);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
		params.bottomMargin = bottomMargin;
		content.addView(row, params);
	}

	/** Custom, scrollable keybind editor. Selecting a row waits for the next key press. */
	private void addControls(Context context, LinearLayout content) {
		Options options = Minecraft.getInstance().options;
		addSection(context, content, "MOVEMENT");
		addKeyRow(context, content, "Forward", options.keyUp);
		addKeyRow(context, content, "Left", options.keyLeft);
		addKeyRow(context, content, "Back", options.keyDown);
		addKeyRow(context, content, "Right", options.keyRight);
		addKeyRow(context, content, "Jump", options.keyJump);
		addKeyRow(context, content, "Sneak", options.keyShift);
		addKeyRow(context, content, "Sprint", options.keySprint);
		addSection(context, content, "MOUSE");
		addIntSlider(context, content, "Mouse Sensitivity", (int) Math.round(options.sensitivity().get() * 100.0), 0, 100,
			value -> { options.sensitivity().set(value / 100.0); options.save(); }, value -> value + "%");
		addSection(context, content, "GAMEPLAY");
		addKeyRow(context, content, "Inventory", options.keyInventory);
		addKeyRow(context, content, "Drop Item", options.keyDrop);
		addKeyRow(context, content, "Use Item", options.keyUse);
		addKeyRow(context, content, "Attack / Destroy", options.keyAttack);
		addKeyRow(context, content, "Pick Block", options.keyPickItem);
		addKeyRow(context, content, "Chat", options.keyChat);
		addKeyRow(context, content, "Player List", options.keyPlayerList);
		addKeyRow(context, content, "Toggle Perspective", options.keyTogglePerspective);
		content.addView(button(context, "Reset All", () -> { for (KeyMapping key : options.keyMappings) key.setKey(key.getDefaultKey()); KeyMapping.resetMapping(); }), buttonParams(px(400), px(43), px(12)));
		content.addView(navigationButton(context, "Back", () -> Minecraft.getInstance().setScreen(new ShulkSettingsScreen(parent))), buttonParams(px(400), px(43), px(10)));
	}

	/** Custom language picker backed by Minecraft's language manager and resource reload flow. */
	private void addLanguage(Context context, LinearLayout content) {
		var minecraft = Minecraft.getInstance();
		var manager = minecraft.getLanguageManager();
		String selected = manager.getSelected();
		addSection(context, content, "LANGUAGE");
		EditText search = new EditText(context);
		search.setHint("Search languages");
		search.setSingleLine(true);
		search.setTextSize(textSize(context, 18));
		search.setTextColor(0xFFF0EAEE);
		search.setHintTextColor(0xFF77717C);
		search.setPadding(px(14), 0, px(14), 0);
		search.setBackground(languageSearchBackground());
		content.addView(search, buttonParams(px(640), px(44), px(4)));

		LinearLayout results = new LinearLayout(context);
		results.setOrientation(LinearLayout.VERTICAL);
		List<Map.Entry<String, LanguageInfo>> entries = new ArrayList<>(manager.getLanguages().entrySet());
		entries.sort((left, right) -> left.getValue().name().compareToIgnoreCase(right.getValue().name()));
		java.util.function.Consumer<String> renderResults = query -> {
			String normalized = query.trim().toLowerCase(Locale.ROOT);
			results.removeAllViews();
			List<Map.Entry<String, LanguageInfo>> filtered = new ArrayList<>();
			for (Map.Entry<String, LanguageInfo> entry : entries) {
				LanguageInfo info = entry.getValue();
				String searchable = (entry.getKey() + " " + info.name() + " " + info.region()).toLowerCase(Locale.ROOT);
				if (searchable.contains(normalized)) filtered.add(entry);
			}
			if (filtered.isEmpty()) {
				TextView empty = text(context, "No languages found", 17, 0xFF807781);
				empty.setGravity(Gravity.CENTER);
				results.addView(empty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(56)));
				return;
			}
			for (int index = 0; index < filtered.size(); index += 2) {
				LinearLayout row = new LinearLayout(context);
				row.setGravity(Gravity.CENTER_VERTICAL);
				row.addView(languageChoice(context, minecraft, manager, filtered.get(index), selected), languageColumnParams(true));
				if (index + 1 < filtered.size()) {
					row.addView(languageChoice(context, minecraft, manager, filtered.get(index + 1), selected), languageColumnParams(false));
				} else {
					row.addView(new View(context), languageColumnParams(false));
				}
				LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(49));
				rowParams.bottomMargin = px(5);
				results.addView(row, rowParams);
			}
		};
		renderResults.accept("");
		search.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence value, int start, int count, int after) { }
			@Override public void onTextChanged(CharSequence value, int start, int before, int count) { renderResults.accept(value.toString()); }
			@Override public void afterTextChanged(Editable value) { }
		});
		content.addView(results, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		content.addView(navigationButton(context, "Back", () -> Minecraft.getInstance().setScreen(new ShulkSettingsScreen(parent))), buttonParams(px(400), px(43), px(12)));
	}

	private View languageChoice(Context context, Minecraft minecraft, net.minecraft.client.resources.language.LanguageManager manager,
			Map.Entry<String, LanguageInfo> entry, String selected) {
		String code = entry.getKey();
		LanguageInfo info = entry.getValue();
		LinearLayout choice = new LinearLayout(context);
		choice.setGravity(Gravity.CENTER_VERTICAL);
		choice.setPadding(px(12), 0, px(12), 0);
		choice.setClickable(true);
		choice.setBackground(buttonBackground());
		KairokkUiSounds.attach(choice);
		TextView name = text(context, info.name() + " (" + info.region() + ")", 16,
			code.equals(selected) ? 0xFF3D8BFF : 0xFFF0EAEE);
		name.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		choice.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		choice.setOnClickListener(v -> Core.executeOnMainThread(() -> {
			manager.setSelected(code);
			minecraft.options.languageCode = code;
			minecraft.options.save();
			minecraft.reloadResourcePacks();
		}));
		return choice;
	}

	private static LinearLayout.LayoutParams languageColumnParams(boolean left) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
		if (left) params.rightMargin = px(4); else params.leftMargin = px(4);
		return params;
	}

	private void addKeyRow(Context context, LinearLayout content, String label, KeyMapping mapping) {
		LinearLayout row = new LinearLayout(context); row.setGravity(Gravity.CENTER_VERTICAL); row.setClickable(true); row.setFocusable(true);
		KairokkUiSounds.attach(row);
		TextView left = text(context, label, 20, 0xFFF0EAEE); left.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		TextView value = text(context, mapping.getTranslatedKeyMessage().getString(), 19, 0xFF3D8BFF); value.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		row.addView(left, new LinearLayout.LayoutParams(0, px(43), 1)); row.addView(value, new LinearLayout.LayoutParams(px(260), px(43)));
		row.setOnClickListener(view -> { pendingKeybind = mapping; pendingKeybindLabel = value; value.setText("Press a key…"); });
		content.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(43)));
	}

	public static boolean acceptPendingKey(net.minecraft.client.input.KeyEvent event) {
		KeyMapping mapping = pendingKeybind;
		TextView label = pendingKeybindLabel;
		if (mapping == null || label == null) return false;
		pendingKeybind = null; pendingKeybindLabel = null;
		if (event.key() != 256) { mapping.setKey(InputConstants.Type.KEYSYM.getOrCreate(event.key())); KeyMapping.resetMapping(); }
		Core.executeOnUiThread(() -> label.setText(mapping.getTranslatedKeyMessage().getString()));
		return true;
	}

	private LinearLayout createSoundSlider(Context context, Options options, SoundSource source, String label) {
		var option = options.getSoundSourceOptionInstance(source);
		return createIntSlider(context, label, (int) Math.round(option.get() * 100), 0, 100,
			value -> option.set(value / 100.0), value -> value + "%");
	}

	private static String soundDeviceName(String device) {
		return Options.isSoundDeviceDefault(device) ? "System Default" : device;
	}

	private Button liveOptionButton(Context context, Supplier<String> labelSupplier, Runnable action) {
		Button result = button(context, labelSupplier.get(), () -> { });
		result.setOnClickListener(view -> Core.executeOnMainThread(() -> {
			action.run();
			String nextLabel = labelSupplier.get();
			Core.executeOnUiThread(() -> {
				result.setText(nextLabel);
				result.setAlpha(0.72F);
				fade(result, 1F);
			});
		}));
		return result;
	}

	private void addActionSetting(Context context, LinearLayout content, String name, Supplier<String> valueSupplier, Runnable action) {
		LinearLayout row = new LinearLayout(context); row.setGravity(Gravity.CENTER_VERTICAL); row.setClickable(true);
		KairokkUiSounds.attach(row);
		TextView left = text(context, name, 20, 0xFFF0EAEE); left.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		LinearLayout valueBox = new LinearLayout(context); valueBox.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		TextView right = text(context, valueSupplier.get(), 19, 0xFF3D8BFF); right.setGravity(Gravity.CENTER);
		valueBox.addView(right, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(43)));
		row.addView(left, new LinearLayout.LayoutParams(0, px(43), 1)); row.addView(valueBox, new LinearLayout.LayoutParams(px(230), px(43)));
		row.setOnClickListener(view -> { action.run(); right.setText(valueSupplier.get()); right.setAlpha(0F); fade(right, 1F); });
		content.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(43)));
	}

	private void addIntSlider(Context context, LinearLayout content, String name, int initial, int min, int max,
			IntConsumer setter, IntFunction<String> formatter) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.bottomMargin = px(8);
		content.addView(createIntSlider(context, name, initial, min, max, setter, formatter), params);
	}

	private LinearLayout createIntSlider(Context context, String name, int initial, int min, int max,
			IntConsumer setter, IntFunction<String> formatter) {
		LinearLayout slider = new LinearLayout(context);
		slider.setOrientation(LinearLayout.VERTICAL);
		LinearLayout row = new LinearLayout(context); row.setGravity(Gravity.CENTER_VERTICAL);
		TextView left = text(context, name, 20, 0xFFF0EAEE); left.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		TextView right = text(context, formatter.apply(initial), 19, 0xFF3D8BFF); right.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		row.addView(left, new LinearLayout.LayoutParams(0, px(38), 1)); row.addView(right, new LinearLayout.LayoutParams(px(82), px(38)));
		slider.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(38)));
		FrameLayout track = new FrameLayout(context); track.setBackground(shape(0xFF39343A, px(3)));
		View fill = new View(context); fill.setBackground(shape(0xFF3D8BFF, px(3)));
		// Match the horizontal inset to the 5dp space above and below the 6dp fill.
		int sliderSidePadding = px(5);
		float[] currentFraction = {fraction(initial, min, max)};
		FrameLayout.LayoutParams fillParams = new FrameLayout.LayoutParams(0, px(6), Gravity.LEFT | Gravity.CENTER_VERTICAL); fillParams.leftMargin = sliderSidePadding; track.addView(fill, fillParams);
		View knob = new View(context); knob.setBackground(circle(0xFF3D8BFF));
		FrameLayout.LayoutParams knobParams = new FrameLayout.LayoutParams(px(18), px(18), Gravity.LEFT | Gravity.CENTER_VERTICAL); track.addView(knob, knobParams);
		track.addOnLayoutChangeListener((view, leftEdge, topEdge, rightEdge, bottomEdge,
				oldLeft, oldTop, oldRight, oldBottom) -> {
			int innerWidth = Math.max(1, rightEdge - leftEdge - sliderSidePadding * 2);
			fillParams.width = Math.round(innerWidth * currentFraction[0]);
			fill.setLayoutParams(fillParams);
			knobParams.leftMargin = sliderSidePadding + Math.round(innerWidth * currentFraction[0]) - px(9);
			knob.setLayoutParams(knobParams);
		});
		track.setOnTouchListener((view, event) -> {
			if (event.getActionMasked() == MotionEvent.ACTION_DOWN || event.getActionMasked() == MotionEvent.ACTION_MOVE || event.getActionMasked() == MotionEvent.ACTION_UP) {
				if (event.getActionMasked() == MotionEvent.ACTION_DOWN) KairokkUiSounds.play(KairokkUiSounds.SELECT);
				float innerWidth = Math.max(1F, view.getWidth() - sliderSidePadding * 2F);
				float f = Math.max(0F, Math.min(1F, (event.getX() - sliderSidePadding) / innerWidth));
				currentFraction[0] = f;
				int value = Math.round(min + f * (max - min));
				Core.executeOnMainThread(() -> setter.accept(value));
				right.setText(formatter.apply(value));
				fillParams.leftMargin = sliderSidePadding; fillParams.width = Math.round(innerWidth * f); fill.setLayoutParams(fillParams); knobParams.leftMargin = sliderSidePadding + Math.round(innerWidth * f) - px(9); knob.setLayoutParams(knobParams);
			}
			return true;
		});
		slider.addView(track, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(16)));
		return slider;
	}

	private void reopen() { Core.executeOnMainThread(() -> Minecraft.getInstance().setScreen(new ShulkSettingsScreen(parent, pageTitle))); }

	void navigateBack() {
		Screen destination = "Settings".equals(pageTitle) ? parent : new ShulkSettingsScreen(parent);
		KairokkScreenTransitions.fadeOut(rootView, () -> {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.options.save();
			minecraft.setScreen(destination);
		});
	}

	private static void fade(View view, float target) { ObjectAnimator.ofFloat(view, View.ALPHA, view.getAlpha(), target).setDuration(140).start(); }
	private static float fraction(int value, int min, int max) { return (value - min) / (float) Math.max(1, max - min); }
	private static String guiScaleText(int value) { return value == 0 ? "Auto" : value + "x"; }
	private static String cloudText(CloudStatus value) { return switch (value) { case OFF -> "Off"; case FAST -> "Fast"; case FANCY -> "Fancy"; }; }
	private static String inactivityText(InactivityFpsLimit value) { return switch (value) { case AFK -> "When AFK"; case MINIMIZED -> "When Minimized"; }; }

	private void addPageRow(Context context, LinearLayout content, String left, String right) {
		LinearLayout row = new LinearLayout(context); row.setGravity(Gravity.CENTER);
		Button a = button(context, left, () -> openPage(left)); Button b = button(context, right, () -> openPage(right));
		LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(0, px(41), 1); ap.rightMargin = px(9);
		LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(0, px(41), 1); bp.leftMargin = px(9);
		row.addView(a, ap); row.addView(b, bp);
		LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(41)); rp.bottomMargin = px(10); content.addView(row, rp);
	}

	private void addSection(Context context, LinearLayout content, String value) {
		TextView section = text(context, value, 14, 0xFF807781);
		section.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		content.addView(section, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(28)));
	}

	private void addSetting(Context context, LinearLayout content, String name, String value) {
		LinearLayout row = new LinearLayout(context); row.setGravity(Gravity.CENTER_VERTICAL);
		TextView left = text(context, name, 20, 0xFFF0EAEE); left.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		TextView right = text(context, value, 19, 0xFF3D8BFF); right.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		row.addView(left, new LinearLayout.LayoutParams(0, px(43), 1)); row.addView(right, new LinearLayout.LayoutParams(px(220), px(43)));
		content.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(43)));
	}

	private void addSlider(Context context, LinearLayout content, String name, String value, float amount) {
		addSetting(context, content, name, value);
		FrameLayout track = new FrameLayout(context); track.setBackground(shape(0xFF39343A, px(3)));
		View fill = new View(context); fill.setBackground(shape(0xFF3D8BFF, px(3)));
		// This compact 12dp track has 3dp above and below its 6dp fill.
		int sliderSidePadding = px(3);
		int sliderWidth = px(640) - sliderSidePadding * 2;
		FrameLayout.LayoutParams fillParams = new FrameLayout.LayoutParams(Math.round(sliderWidth * amount), px(6), Gravity.LEFT | Gravity.CENTER_VERTICAL); fillParams.leftMargin = sliderSidePadding; track.addView(fill, fillParams);
		View knob = new View(context); knob.setBackground(circle(0xFF3D8BFF));
		FrameLayout.LayoutParams kp = new FrameLayout.LayoutParams(px(18), px(18), Gravity.LEFT | Gravity.CENTER_VERTICAL); kp.leftMargin = sliderSidePadding + Math.round(sliderWidth * amount) - px(9); track.addView(knob, kp);
		LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(12)); p.bottomMargin = px(8); content.addView(track, p);
	}

	private void openPage(String title) {
		Core.executeOnMainThread(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			Screen settings = new ShulkSettingsScreen(parent);
			Options options = minecraft.options;
			if (!ShulkUiState.customScreensEnabled()) {
				navigateTo(new OptionsScreen(parent, options, false));
				return;
			}
			switch (title) {
				case "Skin Customization" -> navigateTo(new KairokkSkinCustomizationScreen(settings));
				case "Chat Settings" -> navigateTo(new ShulkSettingsScreen(parent, "Chat Settings"));
				case "Resource Packs" -> navigateTo(new KairokkResourcePackScreen(settings));
				case "Accessibility" -> navigateTo(new ShulkSettingsScreen(parent, "Accessibility"));
				case "Telemetry" -> navigateTo(new TelemetryInfoScreen(settings, options));
				case "Credits" -> navigateTo(new KairokkCreditsScreen(settings));
				default -> navigateTo(new ShulkSettingsScreen(parent, title));
			}
		});
	}
	private Button button(Context c, String label, Runnable action) { Button b = new Button(c); b.setText(label); b.setTextSize(textSize(c, 20)); b.setTextColor(0xFFF5F1F3); b.setGravity(Gravity.CENTER); b.setIncludeFontPadding(false); b.setBackground(buttonBackground()); KairokkUiSounds.attach(b); b.setOnClickListener(v -> Core.executeOnMainThread(action)); return b; }
	private Button navigationButton(Context c, String label, Runnable action) { return button(c, label, () -> KairokkScreenTransitions.fadeOut(rootView, action)); }
	private void navigateTo(Screen screen) { KairokkScreenTransitions.fadeOut(rootView, () -> Minecraft.getInstance().setScreen(screen)); }
	private static LinearLayout.LayoutParams buttonParams(int width, int height, int top) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(width, height); p.topMargin = top; return p; }
	private static TextView text(Context c, String value, float size, int color) { TextView t = new TextView(c); t.setText(value); t.setTextSize(textSize(c, size)); t.setTextColor(color); t.setGravity(Gravity.CENTER); t.setIncludeFontPadding(false); return t; }
	private static StateListDrawable buttonBackground() {
		StateListDrawable d = new StateListDrawable();
		d.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), shape(0xFF4A94FF, px(6)));
		d.addState(StateSet.WILD_CARD, shape(0xFF171419, px(6)));
		d.setEnterFadeDuration(180); d.setExitFadeDuration(180);
		return d;
	}
	private static StateListDrawable languageSearchBackground() {
		StateListDrawable d = new StateListDrawable();
		d.addState(StateSet.get(StateSet.VIEW_STATE_FOCUSED), shape(0xFF101724, px(6)));
		d.addState(StateSet.WILD_CARD, shape(0xFF121218, px(6)));
		d.setEnterFadeDuration(120); d.setExitFadeDuration(120);
		return d;
	}
	private static StateListDrawable blueBackground() {
		StateListDrawable d = new StateListDrawable();
		d.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), shape(0xFF1C67D8, px(16)));
		d.addState(StateSet.WILD_CARD, shape(0xFF4A94FF, px(16)));
		d.setEnterFadeDuration(180); d.setExitFadeDuration(180);
		return d;
	}
	private static ShapeDrawable shape(int color, int radius) { ShapeDrawable d = new ShapeDrawable(); d.setShape(ShapeDrawable.RECTANGLE); d.setColor(color); d.setCornerRadius(radius); d.setStroke(px(1), color == 0xFF171419 ? 0xFF2A2229 : color); return d; }
	private static ShapeDrawable circle(int color) { ShapeDrawable d = new ShapeDrawable(); d.setShape(ShapeDrawable.CIRCLE); d.setColor(color); return d; }
	private static int px(float value) {
		var window = Minecraft.getInstance().getWindow();
		float widthScale = window.getScreenWidth() / 855.0F;
		float heightScale = window.getScreenHeight() / 680.0F;
		return Math.round(value * Math.max(.60F, Math.min(.90F, Math.min(widthScale, heightScale))));
	}
	private static float textSize(Context c, float value) { return px(value) / Math.max(.01F, c.getResources().getDisplayMetrics().scaledDensity); }
}
