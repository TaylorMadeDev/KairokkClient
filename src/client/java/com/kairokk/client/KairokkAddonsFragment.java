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

import java.util.HashSet;
import java.util.Set;

/** A visual library for optional Kairokk modules, staged for the next launch. */
public final class KairokkAddonsFragment extends Fragment {
	private static final int ACCENT = 0xFF3D8BFF;
	private static final int TEXT = 0xFFF0EAEE;
	private static final int MUTED = 0xFF948C98;
	private static final Set<String> INSTALLED = new HashSet<>();
	private final Screen parent;
	@Nullable private FrameLayout rootView;

	public KairokkAddonsFragment(Screen parent) {
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
		bodyParams.bottomMargin = px(context, ShulkTitleFragment.FOOTER_HEIGHT_DP);
		root.addView(body, bodyParams);

		LinearLayout content = new LinearLayout(context);
		content.setOrientation(LinearLayout.VERTICAL);
		content.setGravity(Gravity.CENTER_HORIZONTAL);
		content.addView(brand(context), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 58)));

		TextView heading = text(context, "Add-ons", 29, TEXT);
		heading.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		content.addView(heading, full(context, 39, 0));
		TextView copy = text(context, "Extend your client with optional modules", 14, MUTED);
		copy.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		content.addView(copy, full(context, 27, 15));

		LinearLayout firstRow = new LinearLayout(context);
		firstRow.setGravity(Gravity.CENTER);
		firstRow.addView(card(context, "Baritone", "Automated navigation, mining, and build goals.", "AUTOMATION", "baritone"), half(context, true));
		firstRow.addView(card(context, "Sodium + Shaders", "Performance tuning with an Iris shader bridge.", "PERFORMANCE", "sodium"), half(context, false));
		content.addView(firstRow, full(context, 150, 12));

		LinearLayout secondRow = new LinearLayout(context);
		secondRow.setGravity(Gravity.CENTER);
		secondRow.addView(card(context, "Pathfinding", "Smart waypoint routing and route previews.", "NAVIGATION", "pathfinding"), half(context, true));
		secondRow.addView(card(context, "Replay Capture", "Record sessions and create camera paths.", "CREATIVE", "replay"), half(context, false));
		content.addView(secondRow, full(context, 150, 13));

		LinearLayout notice = new LinearLayout(context);
		notice.setGravity(Gravity.CENTER_VERTICAL);
		notice.setPadding(px(context, 15), 0, px(context, 15), 0);
		notice.setBackground(box(notice, 0xFF111927, 0xFF263B59, 7));
		TextView info = text(context, "Selected add-ons are staged for setup. Restart Kairokk after installation.", 13, 0xFF9DBBE6);
		info.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		notice.addView(info, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		content.addView(notice, full(context, 43, 16));

		Button back = button(context, "Back", this::navigateBack);
		content.addView(back, buttonParams(context, 400, 43, 0));
		body.addView(content, new FrameLayout.LayoutParams(px(context, 700), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		ShulkTitleFragment.addFooter(root, context);
		KairokkScreenTransitions.fadeIn(root);
		return root;
	}

	private View brand(Context context) {
		LinearLayout brand = new LinearLayout(context);
		brand.setGravity(Gravity.CENTER);
		brand.addView(text(context, "K A I R O K K", 34, TEXT), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
		TextView version = text(context, "v0.1.0", 13, ACCENT);
		version.setPadding(px(context, 7), 0, 0, 0);
		brand.addView(version, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
		return brand;
	}

	private View card(Context context, String name, String description, String category, String id) {
		LinearLayout card = new LinearLayout(context);
		card.setOrientation(LinearLayout.VERTICAL);
		card.setPadding(px(context, 17), px(context, 14), px(context, 17), px(context, 13));
		card.setBackground(cardBackground(card));
		TextView categoryLabel = text(context, category, 11, ACCENT);
		categoryLabel.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		card.addView(categoryLabel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 19)));
		TextView title = text(context, name, 21, TEXT);
		title.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		card.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 31)));
		TextView body = text(context, description, 13, MUTED);
		body.setGravity(Gravity.LEFT | Gravity.TOP);
		card.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 39)));

		LinearLayout actionRow = new LinearLayout(context);
		actionRow.setGravity(Gravity.CENTER_VERTICAL);
		TextView status = text(context, installedText(id), 12, installed(id) ? 0xFF8FC0FF : 0xFF827A84);
		status.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		Button install = button(context, installed(id) ? "Installed" : "Install", () -> { });
		install.setOnClickListener(view -> Core.executeOnUiThread(() -> {
			if (!installed(id)) INSTALLED.add(id);
			status.setText(installedText(id));
			status.setTextColor(0xFF8FC0FF);
			install.setText("Installed");
			install.setEnabled(false);
		}));
		install.setEnabled(!installed(id));
		actionRow.addView(status, new LinearLayout.LayoutParams(0, px(context, 33), 1));
		actionRow.addView(install, new LinearLayout.LayoutParams(px(context, 110), px(context, 33)));
		card.addView(actionRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 36)));
		return card;
	}

	private static boolean installed(String id) { return INSTALLED.contains(id); }
	private static String installedText(String id) { return installed(id) ? "Restart required" : "Ready to install"; }

	void navigateBack() {
		KairokkScreenTransitions.fadeOut(rootView, () -> Minecraft.getInstance().setScreen(parent));
	}

	private static Button button(Context context, String label, Runnable action) {
		Button button = new Button(context);
		button.setText(label);
		button.setTextSize(textSize(context, 16));
		button.setTextColor(TEXT);
		button.setGravity(Gravity.CENTER);
		button.setIncludeFontPadding(false);
		button.setBackground(buttonBackground(button));
		KairokkUiSounds.attach(button);
		button.setOnClickListener(view -> Core.executeOnUiThread(action));
		return button;
	}

	private static TextView text(Context context, String value, float size, int color) {
		TextView text = new TextView(context);
		text.setText(value);
		text.setTextSize(textSize(context, size));
		text.setTextColor(color);
		text.setIncludeFontPadding(false);
		return text;
	}

	private static LinearLayout.LayoutParams full(Context context, int height, int bottomMargin) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, height));
		params.bottomMargin = px(context, bottomMargin);
		return params;
	}

	private static LinearLayout.LayoutParams half(Context context, boolean left) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
		if (left) params.rightMargin = px(context, 7); else params.leftMargin = px(context, 7);
		return params;
	}

	private static LinearLayout.LayoutParams buttonParams(Context context, int width, int height, int topMargin) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(px(context, width), px(context, height));
		params.topMargin = px(context, topMargin);
		return params;
	}

	private static StateListDrawable buttonBackground(View view) {
		StateListDrawable background = new StateListDrawable();
		background.addState(StateSet.get(StateSet.VIEW_STATE_PRESSED), box(view, 0xFF1D67D8, 0xFF1D67D8, 6));
		background.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), box(view, 0xFF4A94FF, 0xFF4A94FF, 6));
		background.addState(StateSet.WILD_CARD, box(view, 0xFF171419, 0xFF30334A, 6));
		return background;
	}

	private static StateListDrawable cardBackground(View view) {
		StateListDrawable background = new StateListDrawable();
		background.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), box(view, 0xFF151D2A, 0xFF3D8BFF, 8));
		background.addState(StateSet.WILD_CARD, box(view, 0xFF11141C, 0xFF283448, 8));
		background.setEnterFadeDuration(130);
		background.setExitFadeDuration(150);
		return background;
	}

	private static ShapeDrawable box(View view, int fill, int outline, float radius) {
		ShapeDrawable drawable = new ShapeDrawable();
		drawable.setShape(ShapeDrawable.RECTANGLE);
		drawable.setColor(fill);
		drawable.setCornerRadius(px(view.getContext(), radius));
		drawable.setStroke(px(view.getContext(), 1), outline);
		return drawable;
	}

	private static int px(Context context, float value) {
		int screenWidth = Minecraft.getInstance().getWindow().getScreenWidth();
		float scale = Math.max(0.80F, Math.min(0.90F, screenWidth / 855.0F));
		return Math.round(value * scale);
	}

	private static float textSize(Context context, float value) {
		return px(context, value) / Math.max(0.01F, context.getResources().getDisplayMetrics().scaledDensity);
	}
}
