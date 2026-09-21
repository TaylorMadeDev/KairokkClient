package com.kairokk.client;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.core.Core;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.graphics.drawable.StateListDrawable;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.ShareToLanScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;

import java.net.URI;

/** Builds a compact, functional custom pause menu over the active world. */
public final class KairokkPauseFragment extends Fragment {
	private static final int MENU_WIDTH_DP = 400;
	private static final int BUTTON_HEIGHT_DP = 45;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable DataSet savedInstanceState) {
		Context context = requireContext();
		FrameLayout root = new FrameLayout(context);

		LinearLayout menu = new LinearLayout(context);
		menu.setOrientation(LinearLayout.VERTICAL);
		menu.setGravity(Gravity.CENTER_HORIZONTAL);
		// The controls float directly over the dimmed game; do not put them in a
		// second dark rectangle, which made the menu feel boxed in.
		menu.setPadding(px(menu, 5), px(menu, 5), px(menu, 5), px(menu, 5));

		TextView title = text(context, "GAME MENU", 17, 0xFFB6B0BA);
		title.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, px(menu, 30));
		titleParams.bottomMargin = px(menu, 10);
		menu.addView(title, titleParams);

		menu.addView(button(context, "Back to Game", KairokkPauseFragment::resumeGame), buttonParams(menu, 0));
		addPair(context, menu, "Advancements", this::openAdvancements, "Statistics", this::openStatistics);
		addPair(context, menu, "Give Feedback", () -> openLink("https://feedback.minecraft.net/"),
			"Report Bugs", () -> openLink("https://bugs.mojang.com/"));
		addPair(context, menu, "Options", this::openOptions,
			Minecraft.getInstance().hasSingleplayerServer() ? "Open to LAN" : "Social Interactions",
			Minecraft.getInstance().hasSingleplayerServer() ? this::openLan : this::openSocial);
		menu.addView(button(context, Minecraft.getInstance().isLocalServer() ? "Save and Quit to Title" : "Disconnect",
			this::quitToTitle), buttonParams(menu, 8));

		FrameLayout.LayoutParams menuParams = new FrameLayout.LayoutParams(px(menu, MENU_WIDTH_DP),
			ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
		root.addView(menu, menuParams);
		KairokkScreenTransitions.fadeIn(root);
		KairokkUiSounds.play(KairokkUiSounds.OPEN);
		return root;
	}

	private void addPair(Context context, LinearLayout menu, String left, Runnable leftAction, String right, Runnable rightAction) {
		LinearLayout row = new LinearLayout(context);
		row.setGravity(Gravity.CENTER);
		LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(0, px(row, BUTTON_HEIGHT_DP), 1);
		leftParams.rightMargin = px(row, 5);
		row.addView(button(context, left, leftAction), leftParams);
		LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(0, px(row, BUTTON_HEIGHT_DP), 1);
		rightParams.leftMargin = px(row, 5);
		row.addView(button(context, right, rightAction), rightParams);
		LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, px(row, BUTTON_HEIGHT_DP));
		rowParams.topMargin = px(row, 8);
		menu.addView(row, rowParams);
	}

	private void openAdvancements() {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.setScreen(new AdvancementsScreen(minecraft.player.connection.getAdvancements(), new KairokkPauseScreen()));
	}

	private void openStatistics() {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.setScreen(new StatsScreen(new KairokkPauseScreen(), minecraft.player.getStats()));
	}

	private void openOptions() {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.setScreen(new ShulkSettingsScreen(new KairokkPauseScreen()));
	}

	private void openLan() {
		Minecraft.getInstance().setScreen(new ShareToLanScreen(new KairokkPauseScreen()));
	}

	private void openSocial() {
		Minecraft minecraft = Minecraft.getInstance();
		minecraft.setScreen(new net.minecraft.client.gui.screens.social.SocialInteractionsScreen(new KairokkPauseScreen()));
	}

	private void openLink(String target) {
		ConfirmLinkScreen.confirmLinkNow(new KairokkPauseScreen(), URI.create(target));
	}

	private void quitToTitle() {
		Minecraft.getInstance().disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE);
	}

	static void resumeGame() {
		Core.executeOnMainThread(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			minecraft.setScreen(null);
			minecraft.mouseHandler.grabMouse();
		});
	}

	private static Button button(Context context, String label, Runnable action) {
		Button button = new Button(context);
		button.setText(label);
		button.setTextSize(textSize(context, 18));
		button.setTextColor(0xFFF5F1F3);
		button.setGravity(Gravity.CENTER);
		button.setIncludeFontPadding(false);
		button.setBackground(buttonBackground(button));
		KairokkUiSounds.attach(button);
		button.setOnClickListener(view -> Core.executeOnMainThread(action));
		return button;
	}

	private static LinearLayout.LayoutParams buttonParams(View view, int topMargin) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, px(view, BUTTON_HEIGHT_DP));
		params.topMargin = px(view, topMargin);
		return params;
	}

	private static TextView text(Context context, String value, float size, int color) {
		TextView text = new TextView(context);
		text.setText(value);
		text.setTextSize(textSize(context, size));
		text.setTextColor(color);
		text.setIncludeFontPadding(false);
		return text;
	}

	private static StateListDrawable buttonBackground(View view) {
		StateListDrawable drawable = new StateListDrawable();
		drawable.addState(StateSet.get(StateSet.VIEW_STATE_PRESSED), shape(0xFF1C67D8, px(view, 7), 0xFF2679EC));
		drawable.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), shape(0xFF24344B, px(view, 7), 0xFF4A94FF));
		drawable.addState(StateSet.WILD_CARD, shape(0xFF171419, px(view, 7), 0xFF302A33));
		drawable.setEnterFadeDuration(140);
		drawable.setExitFadeDuration(140);
		return drawable;
	}

	private static ShapeDrawable shape(int color, int radius, int stroke) {
		ShapeDrawable drawable = new ShapeDrawable();
		drawable.setShape(ShapeDrawable.RECTANGLE);
		drawable.setColor(color);
		drawable.setCornerRadius(radius);
		drawable.setStroke(1, stroke);
		return drawable;
	}

	private static int px(View view, float value) { return px(view.getContext(), value); }
	private static int px(Context context, float value) {
		float scale = Math.max(0.80F, Math.min(0.90F, Minecraft.getInstance().getWindow().getScreenWidth() / 855.0F));
		return Math.round(value * scale);
	}
	private static float textSize(Context context, float value) {
		return px(context, value) / Math.max(0.01F, context.getResources().getDisplayMetrics().scaledDensity);
	}
}
