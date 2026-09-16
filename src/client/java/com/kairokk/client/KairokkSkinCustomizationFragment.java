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
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelPart;

/** Builds the blue skin customization page against Minecraft's real options. */
public final class KairokkSkinCustomizationFragment extends Fragment {
	private static final int ACCENT = 0xFF3D8BFF;
	private static final int TEXT = 0xFFF0EAEE;
	private static final int MUTED = 0xFF807781;
	private final Screen parent;
	@Nullable private FrameLayout rootView;

	public KairokkSkinCustomizationFragment(Screen parent) {
		this.parent = parent;
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
		content.setGravity(Gravity.CENTER_HORIZONTAL);
		addBrand(context, content);

		TextView heading = text(context, "Skin Customization", 24, TEXT);
		heading.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		content.addView(heading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(35)));

		Options options = Minecraft.getInstance().options;
		addToggleRow(context, content,
			new Toggle("Cape", PlayerModelPart.CAPE), new Toggle("Jacket", PlayerModelPart.JACKET));
		addToggleRow(context, content,
			new Toggle("Left Sleeve", PlayerModelPart.LEFT_SLEEVE), new Toggle("Right Sleeve", PlayerModelPart.RIGHT_SLEEVE));
		addToggleRow(context, content,
			new Toggle("Left Pants Leg", PlayerModelPart.LEFT_PANTS_LEG), new Toggle("Right Pants Leg", PlayerModelPart.RIGHT_PANTS_LEG));
		addToggleRow(context, content,
			new Toggle("Hat", PlayerModelPart.HAT), null);

		LinearLayout handRow = new LinearLayout(context);
		handRow.setGravity(Gravity.CENTER_VERTICAL);
		TextView handLabel = text(context, "Main Hand", 20, TEXT);
		handLabel.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		Button hand = button(context, mainHandText(options), () -> { });
		hand.setOnClickListener(view -> Core.executeOnMainThread(() -> {
			HumanoidArm next = options.mainHand().get() == HumanoidArm.RIGHT ? HumanoidArm.LEFT : HumanoidArm.RIGHT;
			options.mainHand().set(next);
			options.save();
			String nextLabel = mainHandText(options);
			Core.executeOnUiThread(() -> hand.setText(nextLabel));
		}));
		handRow.addView(handLabel, new LinearLayout.LayoutParams(0, px(43), 1));
		handRow.addView(hand, new LinearLayout.LayoutParams(px(260), px(43)));
		content.addView(handRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(49)));

		Button done = button(context, "Done", () -> KairokkScreenTransitions.returnTo(rootView, parent));
		content.addView(done, buttonParams(px(400), px(43), px(18)));

		body.addView(content, new FrameLayout.LayoutParams(px(640), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		ShulkTitleFragment.addFooter(root, context);
		KairokkScreenTransitions.fadeIn(root);
		return root;
	}

	private void addBrand(Context context, LinearLayout content) {
		LinearLayout brand = new LinearLayout(context);
		brand.setGravity(Gravity.CENTER);
		brand.addView(text(context, "K A I R O K K", 34, TEXT),
			new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(54)));
		brand.addView(text(context, "v0.1.0", 13, ACCENT),
			new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(54)));
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(54));
		params.bottomMargin = px(18);
		content.addView(brand, params);
	}

	private void addToggleRow(Context context, LinearLayout content, Toggle left, @Nullable Toggle right) {
		LinearLayout row = new LinearLayout(context);
		row.setGravity(Gravity.CENTER);
		row.addView(toggleButton(context, left), columnParams(px(260), true));
		if (right != null) {
			row.addView(toggleButton(context, right), columnParams(px(260), false));
		}
		LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(49));
		rowParams.bottomMargin = px(10);
		content.addView(row, rowParams);
	}

	private Button toggleButton(Context context, Toggle toggle) {
		Options options = Minecraft.getInstance().options;
		Button button = button(context, toggle.label + ": " + onOff(options.isModelPartEnabled(toggle.part)), () -> { });
		button.setOnClickListener(view -> Core.executeOnMainThread(() -> {
			boolean enabled = !options.isModelPartEnabled(toggle.part);
			options.setModelPart(toggle.part, enabled);
			options.save();
			String nextLabel = toggle.label + ": " + onOff(enabled);
			Core.executeOnUiThread(() -> button.setText(nextLabel));
		}));
		return button;
	}

	private static String mainHandText(Options options) {
		return options.mainHand().get() == HumanoidArm.RIGHT ? "Right" : "Left";
	}

	private static String onOff(boolean value) {
		return value ? "ON" : "OFF";
	}

	void navigateBack() {
		KairokkScreenTransitions.returnTo(rootView, parent);
	}

	private static LinearLayout.LayoutParams columnParams(int width, boolean left) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT);
		if (left) params.rightMargin = px(9); else params.leftMargin = px(9);
		return params;
	}

	private static Button button(Context context, String label, Runnable action) {
		Button button = new Button(context);
		button.setText(label);
		button.setTextSize(textSize(context, 20));
		button.setTextColor(0xFFF5F1F3);
		button.setGravity(Gravity.CENTER);
		button.setIncludeFontPadding(false);
		button.setBackground(buttonBackground(button));
		KairokkUiSounds.attach(button);
		button.setOnClickListener(view -> Core.executeOnMainThread(action));
		return button;
	}

	private static TextView text(Context context, String value, float size, int color) {
		TextView view = new TextView(context);
		view.setText(value);
		view.setTextSize(textSize(context, size));
		view.setTextColor(color);
		view.setGravity(Gravity.CENTER);
		view.setIncludeFontPadding(false);
		return view;
	}

	private static StateListDrawable buttonBackground(View view) {
		StateListDrawable background = new StateListDrawable();
		background.addState(StateSet.get(StateSet.VIEW_STATE_PRESSED), shape(view, 0xFF1C67D8));
		background.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), shape(view, 0xFF4A94FF));
		background.addState(StateSet.WILD_CARD, shape(view, 0xFF171419));
		background.setEnterFadeDuration(160);
		background.setExitFadeDuration(180);
		return background;
	}

	private static ShapeDrawable shape(View view, int color) {
		ShapeDrawable drawable = new ShapeDrawable();
		drawable.setShape(ShapeDrawable.RECTANGLE);
		drawable.setColor(color);
		drawable.setCornerRadius(px(view, 6));
		drawable.setStroke(px(view, 1), color == 0xFF171419 ? 0xFF2A2229 : color);
		return drawable;
	}

	private static LinearLayout.LayoutParams buttonParams(int width, int height, int top) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
		params.topMargin = top;
		return params;
	}

	private static int px(float value) {
		var window = Minecraft.getInstance().getWindow();
		float widthScale = window.getScreenWidth() / 855.0F;
		float heightScale = window.getScreenHeight() / 680.0F;
		return Math.round(value * Math.max(.60F, Math.min(.90F, Math.min(widthScale, heightScale))));
	}

	private static int px(View view, float value) {
		return px(value);
	}

	private static float textSize(Context context, float value) {
		return px(value) / Math.max(.01F, context.getResources().getDisplayMetrics().scaledDensity);
	}

	private record Toggle(String label, PlayerModelPart part) {
	}
}
