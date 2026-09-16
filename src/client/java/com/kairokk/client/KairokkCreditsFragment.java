package com.kairokk.client;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.core.Context;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Minimal Kairokk attribution screen. */
public final class KairokkCreditsFragment extends Fragment {
	private static final int TEXT = 0xFFF0EAEE;
	private static final int MUTED = 0xFF98909E;
	private final Screen parent;
	@Nullable private FrameLayout rootView;

	public KairokkCreditsFragment(Screen parent) { this.parent = parent; }

	@Nullable @Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable DataSet savedInstanceState) {
		Context context = requireContext();
		FrameLayout root = new FrameLayout(context); rootView = root;
		FrameLayout body = new FrameLayout(context);
		FrameLayout.LayoutParams bodyParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		bodyParams.bottomMargin = px(context, ShulkTitleFragment.FOOTER_HEIGHT_DP); root.addView(body, bodyParams);

		LinearLayout credits = new LinearLayout(context); credits.setOrientation(LinearLayout.VERTICAL); credits.setGravity(Gravity.CENTER_HORIZONTAL);
		TextView brand = text(context, "K A I R O K K", 34, TEXT); brand.setGravity(Gravity.CENTER); credits.addView(brand, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 46)));
		TextView heading = text(context, "Credits", 25, TEXT); heading.setGravity(Gravity.CENTER); credits.addView(heading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 52)));
		credits.addView(section(context, "Game", "Mojang"), new LinearLayout.LayoutParams(px(context, 310), px(context, 78)));
		credits.addView(section(context, "SFX", "TaylormadeDev"), new LinearLayout.LayoutParams(px(context, 310), px(context, 78)));
		body.addView(credits, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		ShulkTitleFragment.addFooter(root, context);
		KairokkScreenTransitions.fadeIn(root);
		return root;
	}

	void close() { KairokkScreenTransitions.fadeOut(rootView, () -> Minecraft.getInstance().setScreen(parent)); }

	private static View section(Context context, String label, String name) {
		LinearLayout section = new LinearLayout(context); section.setOrientation(LinearLayout.VERTICAL); section.setGravity(Gravity.CENTER);
		TextView category = text(context, label.toUpperCase(), 13, MUTED); category.setGravity(Gravity.CENTER); section.addView(category, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 27)));
		TextView credit = text(context, name, 20, TEXT); credit.setGravity(Gravity.CENTER); credit.setBackground(underline(credit)); section.addView(credit, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 39)));
		return section;
	}

	private static ShapeDrawable underline(View view) { ShapeDrawable background = new ShapeDrawable(); background.setShape(ShapeDrawable.RECTANGLE); background.setColor(0x0010141C); background.setStroke(0, 0x00000000); return background; }
	private static TextView text(Context context, String value, float size, int color) { TextView text = new TextView(context); text.setText(value); text.setTextSize(textSize(context, size)); text.setTextColor(color); text.setIncludeFontPadding(false); return text; }
	private static int px(Context context, float value) { int width = Minecraft.getInstance().getWindow().getScreenWidth(); return Math.round(value * Math.max(.70F, Math.min(.90F, width / 1100F))); }
	private static float textSize(Context context, float value) { return px(context, value) / Math.max(.01F, context.getResources().getDisplayMetrics().scaledDensity); }
}
