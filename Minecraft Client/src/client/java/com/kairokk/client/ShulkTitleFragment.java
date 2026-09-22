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
import icyllis.modernui.util.IntProperty;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.PopupWindow;
import icyllis.modernui.widget.SeekBar;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.Minecraft;

/** Builds the title screen entirely from ModernUI views. */
public final class ShulkTitleFragment extends Fragment {
	private static final int MENU_WIDTH_DP = 400;
	private static final int BUTTON_HEIGHT_DP = 45;
	static final int FOOTER_HEIGHT_DP = 54;
	private static final int FOOTER_TEXT_COLOR = 0xFFB6B0BA;
	private static final int FOOTER_TEXT_HOVER_COLOR = 0xFF3D8BFF;
	@Nullable private FrameLayout rootView;
	private static final IntProperty<DiscordIconView> DISCORD_COLOR = new IntProperty<>("discordIconColor") {
		@Override public Integer get(DiscordIconView view) { return view.getIconColor(); }
		@Override public void setValue(DiscordIconView view, int value) { view.setIconColor(value); }
	};
	private static final IntProperty<TextView> TEXT_COLOR = new IntProperty<>("footerTextColor") {
		@Override public Integer get(TextView view) { return (int) view.getCurrentTextColor(); }
		@Override public void setValue(TextView view, int value) { view.setTextColor(value); }
	};

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable DataSet savedInstanceState) {
		Context context = requireContext();
		FrameLayout root = new FrameLayout(context);
		rootView = root;

		LinearLayout menu = new LinearLayout(context);
		menu.setOrientation(LinearLayout.VERTICAL);
		menu.setGravity(Gravity.CENTER_HORIZONTAL);

		LinearLayout brand = new LinearLayout(context);
		brand.setGravity(Gravity.CENTER);
		TextView title = makeText(context, "K A I R O K K", 50, 0xFFF4F0F2);
		title.setPadding(px(title, 5), 0, 0, 0);
		brand.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(brand, 90)));
		TextView version = makeText(context, "v0.1.0", 14, 0xFF3D8BFF);
		version.setGravity(Gravity.CENTER_VERTICAL);
		version.setPadding(px(version, 7), px(version, 4), 0, 0);
		brand.addView(version, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(brand, 90)));
		LinearLayout.LayoutParams brandParams = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, px(brand, 90));
		brandParams.bottomMargin = px(menu, 26);
		menu.addView(brand, brandParams);

		menu.addView(createMenuButton(context, "Singleplayer", () -> openSingleplayer()), fullButtonParams(menu, 9));
		menu.addView(createMenuButton(context, "Multiplayer", () -> openMultiplayer()), fullButtonParams(menu, 9));
		menu.addView(createMenuButton(context, "Add-ons", this::openAddons), fullButtonParams(menu, 12));

		LinearLayout lowerRow = new LinearLayout(context);
		lowerRow.setOrientation(LinearLayout.HORIZONTAL);
		lowerRow.setGravity(Gravity.CENTER);
		Button options = createMenuButton(context, "Options", this::openOptions);
		Button quit = createMenuButton(context, "Quit", this::quitGame);
		LinearLayout.LayoutParams optionsParams = new LinearLayout.LayoutParams(0, px(lowerRow, BUTTON_HEIGHT_DP), 1);
		optionsParams.rightMargin = px(lowerRow, 6);
		lowerRow.addView(options, optionsParams);
		LinearLayout.LayoutParams quitParams = new LinearLayout.LayoutParams(0, px(lowerRow, BUTTON_HEIGHT_DP), 1);
		quitParams.leftMargin = px(lowerRow, 6);
		lowerRow.addView(quit, quitParams);
		LinearLayout.LayoutParams lowerRowParams = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, px(lowerRow, BUTTON_HEIGHT_DP));
		menu.addView(lowerRow, lowerRowParams);

		// Keep the quick-access row directly beneath the primary menu. These are
		// intentionally lightweight text controls, matching the original client
		// layout rather than introducing another row of large buttons.
		LinearLayout shortcuts = new LinearLayout(context);
		shortcuts.setGravity(Gravity.CENTER);
		shortcuts.addView(shortcut(context, "☰ ClickGUI", this::openClickGui), shortcutParams(shortcuts));
		shortcuts.addView(shortcut(context, "⚙ Proxy", this::showProxyMenu), shortcutParams(shortcuts));
		shortcuts.addView(shortcut(context, "♟ Accounts", this::showAccountsMenu), shortcutParams(shortcuts));
		LinearLayout.LayoutParams shortcutsParams = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, px(shortcuts, 30));
		shortcutsParams.topMargin = px(shortcuts, 8);
		menu.addView(shortcuts, shortcutsParams);

		FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(px(menu, MENU_WIDTH_DP),
			ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
		root.addView(menu, menuParams);
		root.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
			positionMenu(menu, bottom - top));
		menu.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
			positionMenu(menu, root.getHeight()));
		addFooter(root, context);
		// Custom title music is explicitly opt-in and remains off across restarts.
		if (KairokkCustomizationState.titleMusicEnabled()) KairokkMusicPlayer.getInstance().start();
		KairokkScreenTransitions.fadeIn(root);
		KairokkUiSounds.play(KairokkUiSounds.OPEN);
		return root;
	}

	@Override
	public void onDestroyView() {
		rootView = null;
		super.onDestroyView();
	}

	static FrameLayout createFooter(Context context) {
		FrameLayout footer = new FrameLayout(context);
		footer.setBackground(footerBackground(footer));

		LinearLayout identity = new LinearLayout(context);
		identity.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout labels = new LinearLayout(context);
		labels.setOrientation(LinearLayout.VERTICAL);
		labels.setGravity(Gravity.CENTER_VERTICAL);
		TextView username = makeText(context, "@*****", 14, 0xFFE9E4E7);
		username.setGravity(Gravity.CENTER_VERTICAL);
		TextView privacy = makeText(context, "Privacy Mode", 13, 0xFF827A84);
		privacy.setGravity(Gravity.CENTER_VERTICAL);
		labels.addView(username, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(labels, 19)));
		labels.addView(privacy, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(labels, 18)));
		LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, px(labels, 37));
		labelParams.leftMargin = px(labels, 16);
		identity.addView(labels, labelParams);
		FrameLayout.LayoutParams identityParams = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, px(footer, 37), Gravity.LEFT | Gravity.CENTER_VERTICAL);
		identityParams.leftMargin = px(footer, 16);
		footer.addView(identity, identityParams);

		LinearLayout tools = new LinearLayout(context);
		tools.setGravity(Gravity.CENTER_VERTICAL);
		TextView footerStatus = makeText(context, "Kairokk Client", 13, FOOTER_TEXT_COLOR);
		footerStatus.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		footerStatus.setClickable(true);
		footerStatus.setTooltipText("Open Client GUI (Shift)");
		KairokkUiSounds.attachSelection(footerStatus);
		footerStatus.setOnClickListener(view -> Core.executeOnMainThread(
			() -> Minecraft.getInstance().setScreen(new KairokkClickGuiScreen(Minecraft.getInstance().screen))));
		LinearLayout.LayoutParams statusParams = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, px(tools, 32));
		statusParams.rightMargin = px(tools, 12);
		tools.addView(footerStatus, statusParams);

		DiscordIconView discord = new DiscordIconView(context);
		discord.setTooltipText("Discord");
		KairokkUiSounds.attachSelection(discord);
		discord.setOnHoverListener((view, event) -> {
			if (event.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER || event.getActionMasked() == MotionEvent.ACTION_HOVER_EXIT) {
				if (event.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER) KairokkUiSounds.play(KairokkUiSounds.HOVER);
				fadeDiscord(discord, event.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER ? FOOTER_TEXT_HOVER_COLOR : FOOTER_TEXT_COLOR);
			}
			return false;
		});
		tools.addView(discord, footerIconParams(tools, 0));

		ShulkIconView language = new ShulkIconView(context, ShulkIcon.LANGUAGE, FOOTER_TEXT_COLOR, "Language");
		language.setClickable(true);
		language.setPalette(FOOTER_TEXT_COLOR, FOOTER_TEXT_HOVER_COLOR, FOOTER_TEXT_HOVER_COLOR, 0x667A737D);
		language.setPadding(px(language, 6), px(language, 6), px(language, 6), px(language, 6));
		KairokkUiSounds.attach(language);
		language.setOnClickListener(view -> showLanguageMenu(context, footer));
		tools.addView(language, footerIconParams(tools, 8));

		ShulkIconView volume = new ShulkIconView(context, ShulkIcon.SOUND, FOOTER_TEXT_COLOR, "Mute menu music");
		volume.setClickable(true);
		volume.setPalette(FOOTER_TEXT_COLOR, FOOTER_TEXT_HOVER_COLOR, FOOTER_TEXT_HOVER_COLOR, 0x667A737D);
		volume.setPadding(px(volume, 6), px(volume, 6), px(volume, 6), px(volume, 6));
		KairokkUiSounds.attach(volume);
		volume.setOnClickListener(view -> {
			KairokkUiSounds.play(KairokkUiSounds.SELECT);
			KairokkMusicPlayer.getInstance().toggleMute();
			updateVolumeIcon(volume);
		});
		volume.setOnTouchListener((view, event) -> {
			boolean buttonPress = event.getActionMasked() == MotionEvent.ACTION_BUTTON_PRESS
				&& event.getActionButton() == MotionEvent.BUTTON_SECONDARY;
			boolean secondaryDown = event.getActionMasked() == MotionEvent.ACTION_DOWN
				&& event.isButtonPressed(MotionEvent.BUTTON_SECONDARY);
			if (!buttonPress && !secondaryDown) return false;
			showVolumeMenu(context, volume);
			return true;
		});
		updateVolumeIcon(volume);
		tools.addView(volume, footerIconParams(tools, 8));

		FrameLayout.LayoutParams toolsParams = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, px(footer, 37), Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		toolsParams.rightMargin = px(footer, 16);
		footer.addView(tools, toolsParams);
		return footer;
	}

	/** Adds the persistent footer and the active title track label immediately above it. */
	static void addFooter(FrameLayout root, Context context) {
		TextView nowPlaying = makeText(context,
			"Now playing  •  " + KairokkMusicPlayer.getInstance().getNowPlaying(), 13, 0xFFB6B0BA);
		nowPlaying.setGravity(Gravity.CENTER_VERTICAL);
		KairokkMusicPlayer.getInstance().bindNowPlayingLabel(nowPlaying);
		FrameLayout.LayoutParams nowPlayingParams = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.WRAP_CONTENT, px(nowPlaying, 28), Gravity.LEFT | Gravity.BOTTOM);
		nowPlayingParams.leftMargin = px(nowPlaying, 16);
		nowPlayingParams.bottomMargin = px(nowPlaying, FOOTER_HEIGHT_DP);
		root.addView(nowPlaying, nowPlayingParams);
		root.addView(createFooter(context), new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, px(root, FOOTER_HEIGHT_DP), Gravity.BOTTOM));
	}

	private static LinearLayout.LayoutParams footerIconParams(View view, int leftMargin) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(px(view, 30), px(view, 32));
		params.leftMargin = px(view, leftMargin);
		return params;
	}

	private static void updateVolumeIcon(ShulkIconView volume) {
		KairokkMusicPlayer player = KairokkMusicPlayer.getInstance();
		boolean muted = player.isMuted();
		volume.setIconAlpha(muted ? 0.45F : 1F);
		volume.setDescription(muted ? "Unmute menu music" : "Mute menu music");
	}

	private static void showVolumeMenu(Context context, View anchor) {
		KairokkUiSounds.play(KairokkUiSounds.OPEN);
		KairokkMusicPlayer player = KairokkMusicPlayer.getInstance();
		PopupWindow popup = new PopupWindow(context);
		popup.setWidth(px(anchor, 210));
		popup.setHeight(px(anchor, 58));
		popup.setFocusable(true);
		popup.setOutsideTouchable(true);

		FrameLayout card = new FrameLayout(context);
		card.setPadding(px(card, 12), px(card, 8), px(card, 12), px(card, 8));
		card.setBackground(rounded(card, 0xFF171419, 0xFF302A33));
		SeekBar volumeBar = new SeekBar(context);
		volumeBar.setMax(100);
		volumeBar.setProgress(Math.round(player.getVolume() * 100F));
		volumeBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
			@Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				if (fromUser) {
					player.setVolume(progress / 100F);
					updateVolumeIcon((ShulkIconView) anchor);
				}
			}
		});
		card.addView(volumeBar, new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
		popup.setContentView(card);
		popup.showAtLocation(anchor, Gravity.RIGHT | Gravity.BOTTOM,
			px(anchor, 16), px(anchor, FOOTER_HEIGHT_DP + 10));
	}

	private static void showLanguageMenu(Context context, View anchor) {
		KairokkUiSounds.play(KairokkUiSounds.OPEN);
		Minecraft minecraft = Minecraft.getInstance();
		PopupWindow popup = new PopupWindow(context);
		popup.setWidth(px(anchor, 245));
		popup.setHeight(px(anchor, 278));
		popup.setFocusable(true);
		popup.setOutsideTouchable(true);
		FrameLayout card = new FrameLayout(context);
		card.setPadding(px(card, 10), px(card, 10), px(card, 10), px(card, 10));
		card.setBackground(rounded(card, 0xFF171419, 0xFF302A33));
		LinearLayout choices = new LinearLayout(context);
		choices.setOrientation(LinearLayout.VERTICAL);
		String[] preferred = {"en_us", "es_es", "pt_br", "ru_ru", "de_de", "he_il"};
		String selected = minecraft.getLanguageManager().getSelected();
		for (String code : preferred) {
			var info = minecraft.getLanguageManager().getLanguages().get(code);
			if (info == null) continue;
			boolean isSelected = code.equals(selected);
			LinearLayout row = new LinearLayout(context);
			row.setGravity(Gravity.CENTER_VERTICAL);
			row.setClickable(true);
			KairokkUiSounds.attach(row);
			row.setBackground(languageChoiceBackground(row));
			TextView choice = makeText(context, info.name(), 21, code.equals(selected) ? 0xFF3D8BFF : 0xFFD4CFD6);
			choice.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
			choice.setPadding(px(choice, 8), 0, px(choice, 8), 0);
			TextView indicator = makeText(context, isSelected ? "•" : "", 25, 0xFF3D8BFF);
			indicator.setGravity(Gravity.CENTER);
			row.addView(choice, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
			row.addView(indicator, new LinearLayout.LayoutParams(px(row, 28), ViewGroup.LayoutParams.MATCH_PARENT));
			row.setOnClickListener(view -> {
				KairokkUiSounds.play(KairokkUiSounds.CLOSE);
				ObjectAnimator.ofFloat(card, View.ALPHA, card.getAlpha(), 0F).setDuration(120).start();
				Core.getUiHandler().postDelayed(() -> {
					popup.dismiss();
					Core.executeOnMainThread(() -> {
						minecraft.getLanguageManager().setSelected(code);
						minecraft.options.languageCode = code;
						minecraft.options.save();
						minecraft.reloadResourcePacks();
					});
				}, 120);
			});
			choices.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(choices, 42)));
		}
		card.addView(choices, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		popup.setContentView(card);
		popup.showAtLocation(anchor, Gravity.RIGHT | Gravity.BOTTOM, px(anchor, 16), px(anchor, FOOTER_HEIGHT_DP + 12));
		card.setAlpha(0F);
		ObjectAnimator.ofFloat(card, View.ALPHA, 0F, 1F).setDuration(160).start();
	}

	private static void positionMenu(View menu, int rootHeight) {
		if (rootHeight <= 0 || menu.getHeight() <= 0) {
			return;
		}
		int safeMargin = px(menu, 18);
		int footerClearance = px(menu, FOOTER_HEIGHT_DP + 12);
		float centeredTop = (rootHeight - menu.getHeight()) * 0.5F;
		// Keep the whole composition visually centered at every window size.
		float preferredTop = centeredTop;
		float maximumTop = Math.max(safeMargin, rootHeight - menu.getHeight() - footerClearance);
		float top = Math.max(safeMargin, Math.min(preferredTop, maximumTop));
		menu.setTranslationY(top - centeredTop);
	}

	/**
	 * The Options screen can replace ModernUI's display density while it is open.
	 * Title-screen measurements must therefore be derived from the Minecraft window,
	 * rather than {@link View#dp(float)}, or a fullscreen return makes the UI larger.
	 */
	private static int px(View view, float value) {
		return px(view.getContext(), value);
	}

	private static int px(Context context, float value) {
		int screenWidth = Minecraft.getInstance().getWindow().getScreenWidth();
		float scale = Math.max(0.80F, Math.min(0.90F, screenWidth / 855.0F));
		return Math.round(value * scale);
	}

	private static float stableTextSize(Context context, float value) {
		float scaledDensity = context.getResources().getDisplayMetrics().scaledDensity;
		return px(context, value) / Math.max(0.01F, scaledDensity);
	}

	private static TextView makeText(Context context, String text, float size, int color) {
		TextView view = new TextView(context);
		view.setText(text);
		view.setTextSize(stableTextSize(context, size));
		view.setTextColor(color);
		view.setGravity(Gravity.CENTER);
		view.setIncludeFontPadding(false);
		return view;
	}

	private static TextView makeFooterLinkText(Context context, String text, float size, int color) {
		TextView view = new FooterLinkTextView(context);
		view.setText(text);
		view.setTextSize(stableTextSize(context, size));
		view.setTextColor(color);
		view.setGravity(Gravity.CENTER);
		view.setIncludeFontPadding(false);
		return view;
	}

	private static void fadeTextColor(TextView view, int targetColor) {
		ObjectAnimator.ofArgb(view, TEXT_COLOR, (int) view.getCurrentTextColor(), targetColor).setDuration(140).start();
	}

	private static void fadeDiscord(DiscordIconView view, int targetColor) {
		ObjectAnimator.ofArgb(view, DISCORD_COLOR, view.getIconColor(), targetColor).setDuration(140).start();
	}

	private static Button createMenuButton(Context context, String text, Runnable action) {
		Button button = new Button(context);
		button.setText(text);
		button.setTextSize(stableTextSize(context, 21));
		button.setTextColor(0xFFF5F1F3);
		button.setGravity(Gravity.CENTER);
		button.setIncludeFontPadding(false);
		button.setPadding(px(button, 16), 0, px(button, 16), 0);
		button.setBackground(makeButtonBackground(button));
		KairokkUiSounds.attach(button);
		button.setOnClickListener(ignored -> Core.executeOnMainThread(action));
		return button;
	}

	private static Button createPrimaryButton(Context context, String text, Runnable action) {
		Button button = createMenuButton(context, text, action);
		button.setBackground(primaryButtonBackground(button));
		return button;
	}

	private static LinearLayout.LayoutParams fullButtonParams(View view, int bottomMarginDp) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, px(view, BUTTON_HEIGHT_DP));
		params.bottomMargin = px(view, bottomMarginDp);
		return params;
	}

	private static LinearLayout.LayoutParams shortcutParams(View view) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(view, 24));
		params.leftMargin = px(view, 5); params.rightMargin = px(view, 5); return params;
	}

	private static LinearLayout.LayoutParams separatorParams(View view) {
		return new LinearLayout.LayoutParams(px(view, 10), px(view, 24));
	}

	private static TextView shortcut(Context context, String label, Runnable action) {
		TextView view = makeFooterLinkText(context, label, 15, 0xFF807781);
		view.setClickable(true);
		view.setOnHoverListener((target, event) -> {
			if (event.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER || event.getActionMasked() == MotionEvent.ACTION_HOVER_EXIT) {
				if (event.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER) KairokkUiSounds.play(KairokkUiSounds.HOVER);
				fadeTextColor(view, event.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER ? FOOTER_TEXT_HOVER_COLOR : 0xFF807781);
			}
			return false;
		});
		KairokkUiSounds.attachSelection(view);
		view.setOnClickListener(target -> Core.executeOnMainThread(action));
		return view;
	}

	private static View shortcutSeparator(Context context) {
		View separator = new View(context);
		separator.setBackground(rounded(separator, 0xFF514A54, 0xFF514A54));
		separator.setPadding(px(separator, 3), px(separator, 9), px(separator, 3), px(separator, 9));
		return separator;
	}

	private static StateListDrawable makeButtonBackground(View view) {
		StateListDrawable background = new StateListDrawable();
		background.addState(StateSet.get(StateSet.VIEW_STATE_PRESSED), rounded(view, 0xFF1C67D8, 0xFF1C67D8));
		background.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), rounded(view, 0xFF4A94FF, 0xFF4A94FF));
		background.addState(StateSet.WILD_CARD, rounded(view, 0xFF171419, 0xFF2A2229));
		background.setEnterFadeDuration(110);
		background.setExitFadeDuration(140);
		return background;
	}

	private static StateListDrawable primaryButtonBackground(View view) {
		StateListDrawable background = new StateListDrawable();
		background.addState(StateSet.get(StateSet.VIEW_STATE_PRESSED), rounded(view, 0xFF1C67D8, 0xFF1C67D8));
		background.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), rounded(view, 0xFF66A8FF, 0xFF66A8FF));
		background.addState(StateSet.WILD_CARD, rounded(view, 0xFF4A94FF, 0xFF4A94FF));
		background.setEnterFadeDuration(110);
		background.setExitFadeDuration(140);
		return background;
	}

	private static StateListDrawable languageChoiceBackground(View view) {
		StateListDrawable background = new StateListDrawable();
		background.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), rounded(view, 0xFF242028, 0xFF242028));
		background.addState(StateSet.WILD_CARD, rounded(view, 0x00171419, 0x00171419));
		background.setEnterFadeDuration(120);
		background.setExitFadeDuration(120);
		return background;
	}

	private static ShapeDrawable rounded(View view, int fill, int outline) {
		ShapeDrawable drawable = new ShapeDrawable();
		drawable.setShape(ShapeDrawable.RECTANGLE);
		drawable.setColor(fill);
		drawable.setCornerRadius(px(view, 6));
		drawable.setStroke(px(view, 1), outline);
		return drawable;
	}

	private static ShapeDrawable footerBackground(View view) {
		ShapeDrawable drawable = new ShapeDrawable();
		drawable.setShape(ShapeDrawable.RECTANGLE);
		drawable.setColor(0xF20B0A0E);
		drawable.setStroke(px(view, 1), 0xFF211D24);
		return drawable;
	}

	private static ShapeDrawable circle(View view, int fill, int outline) {
		ShapeDrawable drawable = new ShapeDrawable();
		drawable.setShape(ShapeDrawable.CIRCLE);
		drawable.setColor(fill);
		drawable.setStroke(px(view, 1), outline);
		return drawable;
	}

	private void openSingleplayer() {
		Minecraft minecraft = Minecraft.getInstance();
		KairokkScreenTransitions.fadeOut(rootView, () -> {
			minecraft.setScreen(new ShulkWorldSelectScreen(new ShulkTitleScreen()));
		});
	}

	private void openMultiplayer() {
		Minecraft minecraft = Minecraft.getInstance();
		KairokkScreenTransitions.fadeOut(rootView, () -> {
			minecraft.setScreen(new ShulkMultiplayerScreen(new ShulkTitleScreen()));
		});
	}

	private void openAddons() {
		KairokkScreenTransitions.fadeOut(rootView,
			() -> Minecraft.getInstance().setScreen(new KairokkAddonsScreen(new ShulkTitleScreen())));
	}

	private void openOptions() {
		Minecraft minecraft = Minecraft.getInstance();
		KairokkScreenTransitions.fadeOut(rootView, () -> minecraft.setScreen(new ShulkSettingsScreen(new ShulkTitleScreen())));
	}

	private void openClickGui() {
		Minecraft minecraft = Minecraft.getInstance();
		KairokkScreenTransitions.fadeOut(rootView,
			() -> minecraft.setScreen(new KairokkClickGuiScreen(new ShulkTitleScreen())));
	}

	private void showProxyMenu() {
		Core.executeOnUiThread(() -> showQuickAccessNotice("Proxy", "Proxy profiles are coming soon."));
	}

	private void showAccountsMenu() {
		Core.executeOnUiThread(() -> showQuickAccessNotice("Accounts", "Account management is coming soon."));
	}

	private void showQuickAccessNotice(String title, String message) {
		if (rootView == null) return;
		Context context = requireContext();
		LinearLayout card = new LinearLayout(context);
		card.setOrientation(LinearLayout.VERTICAL);
		card.setGravity(Gravity.CENTER);
		card.setPadding(px(card, 18), px(card, 10), px(card, 18), px(card, 10));
		card.setBackground(rounded(card, 0xFF171419, 0xFF2A2229));
		TextView heading = makeText(context, title, 18, 0xFFF5F1F3);
		TextView detail = makeText(context, message, 14, 0xFFAAA2AC);
		card.addView(heading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(card, 24)));
		card.addView(detail, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(card, 22)));
		PopupWindow popup = new PopupWindow(card, px(card, 260), px(card, 72), true);
		popup.setOutsideTouchable(true);
		popup.showAtLocation(rootView, Gravity.CENTER, 0, px(card, 84));
		card.setAlpha(0F);
		ObjectAnimator.ofFloat(card, View.ALPHA, 0F, 1F).setDuration(140).start();
	}

	void closeMenu() {
		KairokkScreenTransitions.fadeOut(rootView, () -> {
			KairokkMusicPlayer.getInstance().stop();
			Minecraft.getInstance().setScreen(null);
		});
	}

	private void quitGame() {
		KairokkMusicPlayer.getInstance().stop();
		Minecraft.getInstance().stop();
	}
}
