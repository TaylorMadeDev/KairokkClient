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
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

	/** Kairokk attribution screen with a slow, looping credit roll. */
public final class KairokkCreditsFragment extends Fragment {
	private static final int TEXT = 0xFFF0EAEE;
	private static final int MUTED = 0xFF98909E;
	private final Screen parent;
	@Nullable private FrameLayout rootView;
	@Nullable private ScrollView creditScroll;
	@Nullable private Runnable autoScroll;

	public KairokkCreditsFragment(Screen parent) { this.parent = parent; }

	@Nullable @Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable DataSet savedInstanceState) {
		Context context = requireContext();
		FrameLayout root = new FrameLayout(context); rootView = root;
		FrameLayout body = new FrameLayout(context);
		FrameLayout.LayoutParams bodyParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		bodyParams.bottomMargin = px(context, ShulkTitleFragment.FOOTER_HEIGHT_DP); root.addView(body, bodyParams);

		LinearLayout credits = new LinearLayout(context); credits.setOrientation(LinearLayout.VERTICAL); credits.setGravity(Gravity.CENTER_HORIZONTAL);
		credits.setPadding(0, px(context, 190), 0, px(context, 250));
		TextView brand = text(context, "K A I R O K K", 34, TEXT); brand.setGravity(Gravity.CENTER); credits.addView(brand, new LinearLayout.LayoutParams(px(context, 390), px(context, 52)));
		TextView heading = text(context, "Credits", 25, TEXT); heading.setGravity(Gravity.CENTER); credits.addView(heading, new LinearLayout.LayoutParams(px(context, 390), px(context, 58)));
		credits.addView(section(context, "Client", "TaylorMadeDev", "Kairokk client design, engineering and direction"));
		credits.addView(section(context, "Game", "Mojang Studios", "Minecraft and the original game experience"));
		credits.addView(section(context, "UI / UX", "Kairokk UI team", "Visual language, interaction design and ModernUI implementation"));
		credits.addView(section(context, "SFX", "TaylorMadeDev", "Interface sounds and interaction feedback"));
		credits.addView(section(context, "Music", "C418", "Subwoofer Lullaby and the Minecraft soundtrack"));
		credits.addView(section(context, "Libraries", "Fabric", "Fabric Loader, Fabric API and Fabric Language Kotlin"));
		credits.addView(section(context, "Libraries", "ModernUI", "ModernUI rendering, widgets and text layout"));
		credits.addView(section(context, "Libraries", "Forge Config API Port", "Configuration compatibility layer"));
		credits.addView(section(context, "Libraries", "MinecraftAuth", "Authenticated Microsoft account development launcher"));
		credits.addView(section(context, "Libraries", "JLayer / LWJGL / MixinExtras", "Audio playback, rendering and runtime support"));
		credits.addView(section(context, "Special Thanks", "Fabric and ModernUI communities", "For the tools and documentation that made Kairokk possible"));
		credits.addView(section(context, "Legal", "Minecraft is a Mojang Studios property", "Kairokk is an independent client-side project"));
		ScrollView scroll = new ScrollView(context); scroll.setFillViewport(true); scroll.setVerticalScrollBarEnabled(false);
		scroll.addView(credits, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		creditScroll = scroll;
		body.addView(scroll, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		autoScroll = () -> {
			if (creditScroll == null) return;
			int max = Math.max(0, creditScroll.getChildAt(0).getHeight() - creditScroll.getHeight());
			int next = creditScroll.getScrollY() + Math.max(1, px(context, .45f));
			if (max > 0) creditScroll.scrollTo(0, next >= max ? 0 : next);
			if (rootView != null && autoScroll != null) creditScroll.postDelayed(autoScroll, 42L);
		};
		scroll.postDelayed(autoScroll, 1200L);
		ShulkTitleFragment.addFooter(root, context);
		KairokkScreenTransitions.fadeIn(root);
		return root;
	}

	@Override public void onDestroyView() {
		if (creditScroll != null && autoScroll != null) creditScroll.removeCallbacks(autoScroll);
		creditScroll = null; autoScroll = null; rootView = null;
		super.onDestroyView();
	}

	void close() { KairokkScreenTransitions.fadeOut(rootView, () -> Minecraft.getInstance().setScreen(parent)); }

	private static View section(Context context, String label, String name, String detail) {
		LinearLayout section = new LinearLayout(context); section.setOrientation(LinearLayout.VERTICAL); section.setGravity(Gravity.CENTER);
		TextView category = text(context, label.toUpperCase(), 11, MUTED); category.setGravity(Gravity.CENTER); section.addView(category, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 24)));
		TextView credit = text(context, name, 19, TEXT); credit.setGravity(Gravity.CENTER); credit.setBackground(underline(credit)); section.addView(credit, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 31)));
		TextView description = text(context, detail, 10, MUTED); description.setGravity(Gravity.CENTER); section.addView(description, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 28)));
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(px(context, 430), px(context, 93)); params.bottomMargin = px(context, 14); section.setLayoutParams(params);
		return section;
	}

	private static ShapeDrawable underline(View view) { ShapeDrawable background = new ShapeDrawable(); background.setShape(ShapeDrawable.RECTANGLE); background.setColor(0x0010141C); background.setStroke(0, 0x00000000); return background; }
	private static TextView text(Context context, String value, float size, int color) { TextView text = new TextView(context); text.setText(value); text.setTextSize(textSize(context, size)); text.setTextColor(color); text.setIncludeFontPadding(false); return text; }
	private static int px(Context context, float value) { int width = Minecraft.getInstance().getWindow().getScreenWidth(); return Math.round(value * Math.max(.70F, Math.min(.90F, width / 1100F))); }
	private static float textSize(Context context, float value) { return px(context, value) / Math.max(.01F, context.getResources().getDisplayMetrics().scaledDensity); }
}
