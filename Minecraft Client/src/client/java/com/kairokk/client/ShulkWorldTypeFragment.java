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
import net.minecraft.client.gui.screens.Screen;

/** ModernUI selector for the built-in Minecraft world-generation presets. */
public final class ShulkWorldTypeFragment extends Fragment {
	private static final int WIDTH_DP = 620;
	private final Screen returnScreen;
	private final ShulkCreateWorldFragment owner;
	private final Button[] optionButtons = new Button[ShulkCreateWorldRequest.WorldType.values().length];
	private TextView description;
	private ShulkCreateWorldRequest.WorldType selected;

	public ShulkWorldTypeFragment(Screen returnScreen, ShulkCreateWorldFragment owner) {
		this.returnScreen = returnScreen;
		this.owner = owner;
		this.selected = owner.getWorldType();
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable DataSet savedInstanceState) {
		Context context = requireContext();
		FrameLayout root = new FrameLayout(context);
		LinearLayout panel = new LinearLayout(context);
		panel.setOrientation(LinearLayout.VERTICAL);
		panel.setPadding(px(context, 26), px(context, 20), px(context, 26), px(context, 22));
		panel.setBackground(shape(panel, 0xE80B111C, 0xFF2C2630, 10));

		TextView eyebrow = text(context, "K A I R O K K   ·   WORLD GENERATION", 13, 0xFF3D8BFF);
		eyebrow.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		panel.addView(eyebrow, full(context, 24, 4));
		TextView heading = text(context, "World Type", 30, 0xFFF4F0F2);
		heading.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		panel.addView(heading, full(context, 46, 4));
		TextView intro = text(context, "Choose how the terrain and biomes should generate.", 14, 0xFF817984);
		intro.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		panel.addView(intro, full(context, 26, 14));

		LinearLayout firstRow = new LinearLayout(context);
		firstRow.setOrientation(LinearLayout.HORIZONTAL);
		addOption(context, firstRow, 0, ShulkCreateWorldRequest.WorldType.NORMAL, true);
		addOption(context, firstRow, 1, ShulkCreateWorldRequest.WorldType.FLAT, false);
		panel.addView(firstRow, full(context, 48, 10));

		LinearLayout secondRow = new LinearLayout(context);
		secondRow.setOrientation(LinearLayout.HORIZONTAL);
		addOption(context, secondRow, 2, ShulkCreateWorldRequest.WorldType.LARGE_BIOMES, true);
		addOption(context, secondRow, 3, ShulkCreateWorldRequest.WorldType.AMPLIFIED, false);
		panel.addView(secondRow, full(context, 48, 10));

		LinearLayout thirdRow = new LinearLayout(context);
		thirdRow.setOrientation(LinearLayout.HORIZONTAL);
		addOption(context, thirdRow, 4, ShulkCreateWorldRequest.WorldType.SINGLE_BIOME_SURFACE, true);
		panel.addView(thirdRow, full(context, 48, 12));

		description = text(context, selected.description(), 14, 0xFFB6ADB7);
		description.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		panel.addView(description, full(context, 28, 14));

		LinearLayout actions = new LinearLayout(context);
		actions.setOrientation(LinearLayout.HORIZONTAL);
		Button back = button(context, "Back", this::goBack);
		Button apply = button(context, "Apply", this::apply);
		apply.setBackground(primaryBackground(apply));
		actions.addView(back, half(context, true));
		actions.addView(apply, half(context, false));
		panel.addView(actions, full(context, 48, 0));

		FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(px(context, WIDTH_DP),
				ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
		panelParams.bottomMargin = px(context, 20);
		root.addView(panel, panelParams);
		ShulkTitleFragment.addFooter(root, context);
		KairokkScreenTransitions.fadeIn(root);
		refreshButtons();
		return root;
	}

	private void addOption(Context context, LinearLayout row, int index,
			ShulkCreateWorldRequest.WorldType type, boolean left) {
		Button option = button(context, type.displayName(), () -> select(type));
		optionButtons[index] = option;
		row.addView(option, half(context, left));
	}

	private void select(ShulkCreateWorldRequest.WorldType type) {
		selected = type;
		description.setText(type.description());
		refreshButtons();
	}

	private void refreshButtons() {
		if (description != null) description.setText(selected.description());
		for (int i = 0; i < optionButtons.length; i++) {
			Button option = optionButtons[i];
			if (option == null) continue;
			ShulkCreateWorldRequest.WorldType type = ShulkCreateWorldRequest.WorldType.values()[i];
			option.setBackground(type == selected ? selectedBackground(option) : buttonBackground(option));
		}
	}

	private void apply() {
		owner.setWorldType(selected);
		Core.executeOnMainThread(() -> Minecraft.getInstance().setScreen(returnScreen));
	}

	private void goBack() {
		Core.executeOnMainThread(() -> Minecraft.getInstance().setScreen(returnScreen));
	}

	private static Button button(Context context, String value, Runnable action) {
		Button button = new Button(context);
		button.setText(value);
		button.setTextSize(textSize(context, 17));
		button.setTextColor(0xFFF3EEF1);
		button.setGravity(Gravity.CENTER);
		button.setIncludeFontPadding(false);
		button.setBackground(buttonBackground(button));
		ShulkIcon icon = ShulkIcon.forAction(value);
		if (icon != null) ShulkIconView.decorate(button, icon, px(context, 16), 0xFFF3EEF1, 0xFFFFFFFF, 0xFFFFFFFF, 0x667A737D, value);
		KairokkUiSounds.attach(button);
		button.setOnClickListener(ignored -> action.run());
		return button;
	}

	private static TextView text(Context context, String value, float size, int color) {
		TextView text = new TextView(context);
		text.setText(value);
		text.setTextSize(textSize(context, size));
		text.setTextColor(color);
		text.setIncludeFontPadding(false);
		text.setGravity(Gravity.CENTER);
		return text;
	}

	private static LinearLayout.LayoutParams full(Context context, int height, int bottom) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, height));
		params.bottomMargin = px(context, bottom);
		return params;
	}

	private static LinearLayout.LayoutParams half(Context context, boolean left) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
		if (left) params.rightMargin = px(context, 6); else params.leftMargin = px(context, 6);
		return params;
	}

	private static StateListDrawable buttonBackground(View view) {
		StateListDrawable background = new StateListDrawable();
		background.addState(StateSet.get(StateSet.VIEW_STATE_PRESSED), shape(view, 0xFF286BD8, 0xFF286BD8, 6));
		background.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), shape(view, 0xFF4B94FF, 0xFF4B94FF, 6));
		background.addState(StateSet.WILD_CARD, shape(view, 0xFF1A171D, 0xFF332C35, 6));
		background.setEnterFadeDuration(110);
		background.setExitFadeDuration(150);
		return background;
	}

	private static StateListDrawable selectedBackground(View view) {
		StateListDrawable background = new StateListDrawable();
		background.addState(StateSet.get(StateSet.VIEW_STATE_PRESSED), shape(view, 0xFF286BD8, 0xFF286BD8, 6));
		background.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), shape(view, 0xFF5B9DFF, 0xFF5B9DFF, 6));
		background.addState(StateSet.WILD_CARD, shape(view, 0xFF245AAE, 0xFF4B94FF, 6));
		background.setEnterFadeDuration(110);
		background.setExitFadeDuration(150);
		return background;
	}

	private static StateListDrawable primaryBackground(View view) {
		StateListDrawable background = new StateListDrawable();
		background.addState(StateSet.get(StateSet.VIEW_STATE_PRESSED), shape(view, 0xFF286BD8, 0xFF286BD8, 6));
		background.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), shape(view, 0xFF5B9DFF, 0xFF5B9DFF, 6));
		background.addState(StateSet.WILD_CARD, shape(view, 0xFF4B94FF, 0xFF4B94FF, 6));
		background.setEnterFadeDuration(110);
		background.setExitFadeDuration(150);
		return background;
	}

	private static ShapeDrawable shape(View view, int fill, int outline, float radius) {
		ShapeDrawable shape = new ShapeDrawable();
		shape.setShape(ShapeDrawable.RECTANGLE);
		shape.setColor(fill);
		shape.setCornerRadius(px(view.getContext(), radius));
		shape.setStroke(px(view.getContext(), 1), outline);
		return shape;
	}

	private static int px(Context context, float value) {
		int screenWidth = Minecraft.getInstance().getWindow().getScreenWidth();
		int screenHeight = Minecraft.getInstance().getWindow().getScreenHeight();
		float widthScale = screenWidth / 855.0F;
		float heightScale = screenHeight / 620.0F;
		float scale = Math.max(0.62F, Math.min(0.90F, Math.min(widthScale, heightScale)));
		return Math.round(value * scale);
	}

	private static float textSize(Context context, float value) {
		return px(context, value) / Math.max(0.01F, context.getResources().getDisplayMetrics().scaledDensity);
	}
}
