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
import icyllis.modernui.widget.EditText;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.ScrollView;
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
		content.addView(brand(context), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 50)));

		LinearLayout headingRow = new LinearLayout(context);
		headingRow.setGravity(Gravity.CENTER_VERTICAL);
		Button back = button(context, "‹  Back", this::navigateBack);
		back.setTextSize(textSize(context, 14));
		headingRow.addView(back, new LinearLayout.LayoutParams(px(context, 86), px(context, 38)));
		LinearLayout headingCopy = new LinearLayout(context);
		headingCopy.setOrientation(LinearLayout.VERTICAL);
		headingCopy.setPadding(px(context, 16), 0, 0, 0);
		TextView heading = text(context, "Add-ons", 27, TEXT);
		headingCopy.addView(heading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 30)));
		TextView copy = text(context, "Extend your client with optional modules", 13, 0xFFA7B8CF);
		headingCopy.addView(copy, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 22)));
		headingRow.addView(headingCopy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
		content.addView(headingRow, full(context, 49, 11));

		content.addView(packDisablerCard(context), full(context, 143, 11));

		LinearLayout searchRow = new LinearLayout(context);
		searchRow.setGravity(Gravity.CENTER_VERTICAL);
		EditText search = new EditText(context);
		search.setHint("⌕  Search add-ons...");
		search.setSingleLine(true);
		search.setTextSize(textSize(context, 13));
		search.setTextColor(TEXT);
		search.setHintTextColor(0xFF7C91AD);
		search.setPadding(px(context, 12), 0, px(context, 10), 0);
		search.setBackground(box(search, 0xAA0C1827, 0xFF1D3A5B, 6));
		searchRow.addView(search, new LinearLayout.LayoutParams(0, px(context, 38), 1));
		Button category = button(context, "All Categories   ⌄", () -> { });
		category.setTextSize(textSize(context, 12));
		LinearLayout.LayoutParams categoryParams = new LinearLayout.LayoutParams(px(context, 145), px(context, 38));
		categoryParams.leftMargin = px(context, 10);
		searchRow.addView(category, categoryParams);
		LinearLayout.LayoutParams searchParams = full(context, 38, 10);
		searchRow.setClipChildren(false);
		content.addView(searchRow, searchParams);

		String[][] addons = {
			{"Baritone", "Automated navigation, mining, and build goals.", "AUTOMATION", "baritone"},
			{"Sodium + Shaders", "Performance tuning with an Iris shader bridge.", "PERFORMANCE", "sodium"},
			{"Pathfinding", "Smart waypoint routing and route previews.", "NAVIGATION", "pathfinding"},
			{"Replay Capture", "Record sessions and create camera paths.", "CREATIVE", "replay"},
			{"BetterTooltips", "Enhanced item, block and entity information.", "UTILITY", "tooltips"},
			{"Freecam", "Explore your world with complete freedom.", "VISUAL", "freecam"},
			{"TargetHUD", "Clean and informative target information.", "COMBAT", "targethud"},
			{"Litematica", "Schematic building and world editing tools.", "WORLD", "litematica"}
		};
		for (int i = 0; i < addons.length; i += 2) {
			LinearLayout row = new LinearLayout(context);
			row.setGravity(Gravity.CENTER);
			row.addView(card(context, addons[i][0], addons[i][1], addons[i][2], addons[i][3]), half(context, true));
			row.addView(card(context, addons[i + 1][0], addons[i + 1][1], addons[i + 1][2], addons[i + 1][3]), half(context, false));
			content.addView(row, full(context, 142, 10));
		}

		content.addView(pagination(context), full(context, 34, 16));
		ScrollView scroll = new ScrollView(context);
		scroll.setFillViewport(true);
		scroll.addView(content, new ScrollView.LayoutParams(px(context, 760), ViewGroup.LayoutParams.WRAP_CONTENT));
		body.addView(scroll, new FrameLayout.LayoutParams(px(context, 760), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));
		ShulkTitleFragment.addFooter(root, context);
		KairokkScreenTransitions.fadeIn(root);
		return root;
	}

	private View packDisablerCard(Context context) {
		LinearLayout card = new LinearLayout(context);
		card.setOrientation(LinearLayout.HORIZONTAL);
		card.setGravity(Gravity.CENTER_VERTICAL);
		card.setPadding(px(context, 15), px(context, 12), px(context, 15), px(context, 11));
		card.setBackground(cardBackground(card));
		KairokkModrinthIconView icon = new KairokkModrinthIconView(context);
		icon.setBackground(box(icon, 0xFF172944, 0xFF2A75B8, 6));
		card.addView(icon, new LinearLayout.LayoutParams(px(context, 65), px(context, 65)));

		LinearLayout copy = new LinearLayout(context);
		copy.setOrientation(LinearLayout.VERTICAL);
		copy.setPadding(px(context, 12), 0, px(context, 10), 0);
		TextView categoryLabel = text(context, "FEATURED  ·  MODRINTH  ·  FABRIC 26.1.2", 10, ACCENT);
		copy.addView(categoryLabel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 17)));
		TextView title = text(context, "PackDisabler", 18, TEXT);
		copy.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 24)));
		TextView body = text(context, "Keep your own visuals on Hypixel SkyBlock by disabling the forced server resource pack.", 11, MUTED);
		body.setGravity(Gravity.LEFT | Gravity.TOP);
		copy.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 27)));
		LinearLayout tags = new LinearLayout(context);
		tags.setGravity(Gravity.CENTER_VERTICAL);
		tags.addView(chip(context, "Visual"), new LinearLayout.LayoutParams(px(context, 52), px(context, 22)));
		tags.addView(chip(context, "Hypixel"), chipParams(context, 60, 5));
		tags.addView(chip(context, "Utility"), chipParams(context, 55, 5));
		copy.addView(tags, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 23)));
		card.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

		LinearLayout action = new LinearLayout(context);
		action.setOrientation(LinearLayout.VERTICAL);
		action.setGravity(Gravity.CENTER_VERTICAL);
		PathState state = new PathState(Minecraft.getInstance().gameDirectory.toPath());
		boolean installed = state.installed();
		TextView status = text(context, installed ? "Restart required" : "⇩  1.3M    ★  4.8", 11, installed ? 0xFF8FC0FF : 0xFFA7B8CF);
		status.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		Button download = button(context, installed ? "Installed" : "Download", () -> { });
		download.setTextSize(textSize(context, 12));
		download.setEnabled(!installed);
		action.addView(status, new LinearLayout.LayoutParams(px(context, 108), px(context, 24)));
		action.addView(download, new LinearLayout.LayoutParams(px(context, 108), px(context, 32)));
		card.addView(action, new LinearLayout.LayoutParams(px(context, 108), ViewGroup.LayoutParams.MATCH_PARENT));

		FrameLayout progressTrack = new FrameLayout(context);
		progressTrack.setVisibility(View.GONE);
		progressTrack.setBackground(box(progressTrack, 0xFF1C2C43, 0xFF2D4668, 4));
		View progressFill = new View(context);
		progressFill.setBackground(box(progressFill, ACCENT, ACCENT, 4));
		FrameLayout.LayoutParams progressFillParams = new FrameLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.LEFT);
		progressTrack.addView(progressFill, progressFillParams);
		final float[] progressFraction = {0F};
		Runnable updateProgress = () -> {
			int width = Math.max(1, progressTrack.getWidth());
			progressFillParams.width = Math.max(1, Math.round(width * progressFraction[0]));
			progressFill.setLayoutParams(progressFillParams);
		};
		progressTrack.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> updateProgress.run());
		LinearLayout.LayoutParams progressParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 8));
		progressParams.topMargin = px(context, 5);
		action.addView(progressTrack, progressParams);
		download.setOnClickListener(view -> {
			if (!download.isEnabled()) return;
			download.setEnabled(false);
			download.setText("Downloading…");
			status.setText("Contacting Modrinth…");
			progressTrack.setVisibility(View.VISIBLE);
			KairokkModrinthDownloader.downloadPackDisabler(state.gameDirectory(), progress -> Core.executeOnUiThread(() -> {
				progressFraction[0] = (float) progress.fraction();
				updateProgress.run();
				int percent = Math.round(progressFraction[0] * 100F);
				status.setText(percent > 0 ? "Downloading " + progress.filename() + " · " + percent + "%" : "Downloading " + progress.filename() + "…");
			})).thenAccept(result -> Core.executeOnUiThread(() -> {
				status.setText(result.message());
				status.setTextColor(result.success() ? 0xFF8FC0FF : 0xFFFF9A9A);
				if (result.success()) {
					progressFraction[0] = 1F;
					updateProgress.run();
				}
				download.setText(result.success() ? "Installed" : "Retry");
					download.setEnabled(!result.success());
			}));
		});
		return card;
	}

	private static final class PathState {
		private final java.nio.file.Path gameDirectory;
		private PathState(java.nio.file.Path gameDirectory) { this.gameDirectory = gameDirectory; }
		private java.nio.file.Path gameDirectory() { return gameDirectory; }
		private boolean installed() { return KairokkModrinthDownloader.isPackDisablerInstalled(gameDirectory); }
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
		card.setOrientation(LinearLayout.HORIZONTAL);
		card.setGravity(Gravity.CENTER_VERTICAL);
		card.setPadding(px(context, 11), px(context, 10), px(context, 11), px(context, 10));
		card.setBackground(cardBackground(card));
		ShulkIconView iconView = new ShulkIconView(context, addonIcon(id), ACCENT, name);
		iconView.setBackground(box(iconView, 0xFF101E32, 0xFF287CC6, 6));
		card.addView(iconView, new LinearLayout.LayoutParams(px(context, 48), px(context, 48)));

		LinearLayout copy = new LinearLayout(context);
		copy.setOrientation(LinearLayout.VERTICAL);
		copy.setPadding(px(context, 10), 0, px(context, 7), 0);
		TextView categoryLabel = text(context, category, 11, ACCENT);
		copy.addView(categoryLabel, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 15)));
		TextView title = text(context, name, 15, TEXT);
		copy.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 21)));
		TextView body = text(context, description, 11, MUTED);
		body.setGravity(Gravity.LEFT | Gravity.TOP);
		copy.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 25)));
		LinearLayout tags = new LinearLayout(context);
		tags.setGravity(Gravity.CENTER_VERTICAL);
		tags.addView(chip(context, category.substring(0, 1) + category.substring(1).toLowerCase()), chipParams(context, 68, 4));
		tags.addView(chip(context, id.equals("pathfinding") ? "Utility" : "Visual"), chipParams(context, 58, 0));
		copy.addView(tags, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 21)));
		card.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));

		LinearLayout actionRow = new LinearLayout(context);
		actionRow.setOrientation(LinearLayout.VERTICAL);
		actionRow.setGravity(Gravity.CENTER_VERTICAL);
		TextView status = text(context, installed(id) ? "⇩  Ready" : "⇩  892K   ★ 4.7", 9, installed(id) ? 0xFF8FC0FF : 0xFFA7B8CF);
		status.setGravity(Gravity.CENTER);
		Button install = button(context, installed(id) ? "Installed" : "Install", () -> { });
		install.setTextSize(textSize(context, 11));
		install.setOnClickListener(view -> Core.executeOnUiThread(() -> {
			if (!installed(id)) INSTALLED.add(id);
			status.setText(installedText(id));
			status.setTextColor(0xFF8FC0FF);
			install.setText("Installed");
			install.setEnabled(false);
		}));
		install.setEnabled(!installed(id));
		actionRow.addView(status, new LinearLayout.LayoutParams(px(context, 92), px(context, 22)));
		actionRow.addView(install, new LinearLayout.LayoutParams(px(context, 84), px(context, 30)));
		card.addView(actionRow, new LinearLayout.LayoutParams(px(context, 92), ViewGroup.LayoutParams.MATCH_PARENT));
		return card;
	}

	private static ShulkIcon addonIcon(String id) {
		return switch (id) {
			case "baritone" -> ShulkIcon.COMPASS;
			case "sodium" -> ShulkIcon.PERFORMANCE;
			case "pathfinding" -> ShulkIcon.TARGET;
			case "replay" -> ShulkIcon.PLAY;
			case "tooltips" -> ShulkIcon.INFO;
			case "freecam" -> ShulkIcon.VISIBLE;
			case "targethud" -> ShulkIcon.TARGET;
			case "litematica" -> ShulkIcon.LAYOUT;
			default -> ShulkIcon.MODULES;
		};
	}

	private static TextView chip(Context context, String label) {
		TextView chip = text(context, label, 9, 0xFF9BB5D2);
		chip.setGravity(Gravity.CENTER);
		chip.setBackground(box(chip, 0x66182A40, 0x66304D70, 4));
		return chip;
	}

	private static LinearLayout.LayoutParams chipParams(Context context, int width, int leftMargin) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(px(context, width), px(context, 22));
		params.leftMargin = px(context, leftMargin);
		return params;
	}

	private View pagination(Context context) {
		LinearLayout pagination = new LinearLayout(context);
		pagination.setGravity(Gravity.CENTER);
		Button previous = button(context, "‹", () -> { });
		previous.setTextSize(textSize(context, 18));
		pagination.addView(previous, new LinearLayout.LayoutParams(px(context, 34), px(context, 32)));
		for (String page : new String[]{"1", "2", "3", "4"}) {
			Button number = button(context, page, () -> { });
			number.setTextSize(textSize(context, 12));
			if ("1".equals(page)) number.setBackground(box(number, 0xFF1B5DB5, ACCENT, 5));
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(px(context, 34), px(context, 32));
			params.leftMargin = px(context, 4);
			params.rightMargin = px(context, 4);
			pagination.addView(number, params);
		}
		Button next = button(context, "›", () -> { });
		next.setTextSize(textSize(context, 18));
		LinearLayout.LayoutParams nextParams = new LinearLayout.LayoutParams(px(context, 34), px(context, 32));
		nextParams.leftMargin = px(context, 2);
		pagination.addView(next, nextParams);
		return pagination;
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
