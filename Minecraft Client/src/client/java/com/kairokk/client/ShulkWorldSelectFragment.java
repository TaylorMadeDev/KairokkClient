package com.kairokk.client;

import com.kairokk.KairokkClient;
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
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.worldselection.EditWorldScreen;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.LevelSummary;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Shulk's functional saved-world selector. */
public final class ShulkWorldSelectFragment extends Fragment {
	private static final int FOOTER_HEIGHT_DP = 54;
	private static final int CONTENT_WIDTH_DP = 720;
	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy");

	private final Screen parent;
	private final List<WorldRow> rows = new ArrayList<>();
	private LinearLayout worldList;
	private TextView status;
	private Button manageButton;
	private LevelSummary selected;
	private boolean viewAlive;

	public ShulkWorldSelectFragment(Screen parent) {
		this.parent = parent;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable DataSet savedInstanceState) {
		viewAlive = true;
		Context context = requireContext();
		FrameLayout root = new FrameLayout(context);

		LinearLayout content = new LinearLayout(context);
		content.setOrientation(LinearLayout.VERTICAL);
		content.setGravity(Gravity.CENTER_HORIZONTAL);
		content.addView(createBrand(context), new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, px(context, 92)));

		TextView subtitle = text(context, "Select World", 29, 0xFFAAA3AD);
		LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, px(context, 42));
		subtitleParams.bottomMargin = px(context, 12);
		content.addView(subtitle, subtitleParams);

		ScrollView scroll = new ScrollView(context);
		worldList = new LinearLayout(context);
		worldList.setOrientation(LinearLayout.VERTICAL);
		status = text(context, "Loading worlds…", 18, 0xFF807983);
		worldList.addView(status, new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, px(context, 58)));
		scroll.addView(worldList, new ScrollView.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		content.addView(scroll, new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

		content.addView(divider(context), new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, px(context, 1)));
		content.addView(createActions(context), new LinearLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, px(context, 72)));

		FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
			px(context, CONTENT_WIDTH_DP), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER_HORIZONTAL);
		contentParams.topMargin = px(context, 18);
		contentParams.bottomMargin = px(context, FOOTER_HEIGHT_DP + 12);
		root.addView(content, contentParams);
		ShulkTitleFragment.addFooter(root, context);

		loadWorlds();
		KairokkScreenTransitions.fadeIn(root);
		return root;
	}

	@Override
	public void onDestroyView() {
		viewAlive = false;
		super.onDestroyView();
	}

	private View createBrand(Context context) {
		LinearLayout brand = new LinearLayout(context);
		brand.setGravity(Gravity.CENTER);
		ShulkIconView mark = new ShulkIconView(context, ShulkIcon.BRAND, 0xFF3D8BFF, "Kairokk");
		brand.addView(mark, new LinearLayout.LayoutParams(px(context, 68), px(context, 74)));
		TextView title = text(context, "K A I R O K K", 38, 0xFFF4F0F2);
		title.setPadding(px(context, 4), 0, 0, 0);
		brand.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(context, 74)));
		TextView version = text(context, "v0.1.0", 13, 0xFF3D8BFF);
		version.setGravity(Gravity.CENTER_VERTICAL);
		version.setPadding(px(context, 7), px(context, 3), 0, 0);
		brand.addView(version, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(context, 74)));
		return brand;
	}

	private View createActions(Context context) {
		LinearLayout row = new LinearLayout(context);
		row.setGravity(Gravity.CENTER);
		Button create = button(context, "Create New", this::createWorld);
		manageButton = button(context, "Manage", this::manageWorld);
		manageButton.setEnabled(false);
		Button back = button(context, "Back", this::goBack);
		for (Button button : new Button[]{create, manageButton, back}) {
			LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, px(context, 44), 1);
			params.leftMargin = px(context, 6);
			params.rightMargin = px(context, 6);
			row.addView(button, params);
		}
		return row;
	}

	private void loadWorlds() {
		Minecraft minecraft = Minecraft.getInstance();
		try {
			var candidates = minecraft.getLevelSource().findLevelCandidates();
			minecraft.getLevelSource().loadLevelSummaries(candidates).whenComplete((summaries, error) ->
				Core.executeOnUiThread(() -> {
					if (!viewAlive) return;
					if (error != null) {
						KairokkClient.LOGGER.error("Failed to load saved worlds", error);
						status.setText("Could not load saved worlds");
						return;
					}
					populateWorlds(summaries);
				}));
		} catch (Exception error) {
			KairokkClient.LOGGER.error("Failed to find saved worlds", error);
			status.setText("Could not load saved worlds");
		}
	}

	private void populateWorlds(List<LevelSummary> summaries) {
		worldList.removeAllViews();
		rows.clear();
		if (summaries.isEmpty()) {
			status.setText("No worlds yet — create your first one below");
			worldList.addView(status, new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, px(status.getContext(), 58)));
			return;
		}
		for (LevelSummary summary : summaries) {
			WorldRow row = new WorldRow(worldList.getContext(), summary);
			rows.add(row);
			worldList.addView(row.view, new LinearLayout.LayoutParams(
				ViewGroup.LayoutParams.MATCH_PARENT, px(row.view.getContext(), 63)));
		}
	}

	private void select(WorldRow row) {
		if (selected == row.summary) {
			playWorld();
			return;
		}
		selected = row.summary;
		manageButton.setEnabled(selected.canEdit());
		for (WorldRow candidate : rows) candidate.updateSelection(candidate == row);
	}

	private void playWorld() {
		if (selected == null || !selected.primaryActionActive()) return;
		String levelId = selected.getLevelId();
		Core.executeOnMainThread(() -> Minecraft.getInstance().createWorldOpenFlows().openWorld(levelId,
			() -> Minecraft.getInstance().setScreen(new ShulkWorldSelectScreen(parent))));
	}

	private void createWorld() {
		Core.executeOnMainThread(() -> Minecraft.getInstance().setScreen(
			new ShulkCreateWorldScreen(new ShulkWorldSelectScreen(parent))));
	}

	private void manageWorld() {
		if (selected == null || !selected.canEdit()) return;
		String levelId = selected.getLevelId();
		Core.executeOnMainThread(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			LevelStorageSource.LevelStorageAccess access = null;
			try {
				access = minecraft.getLevelSource().validateAndCreateAccess(levelId);
				LevelStorageSource.LevelStorageAccess finalAccess = access;
				minecraft.setScreen(EditWorldScreen.create(minecraft, access, changed -> {
					finalAccess.safeClose();
					minecraft.setScreen(new ShulkWorldSelectScreen(parent));
				}));
			} catch (Exception error) {
				if (access != null) access.safeClose();
				KairokkClient.LOGGER.error("Failed to open world editor for {}", levelId, error);
				SystemToast.onWorldAccessFailure(minecraft, levelId);
				minecraft.setScreen(new ShulkWorldSelectScreen(parent));
			}
		});
	}

	private void goBack() {
		Core.executeOnMainThread(() -> Minecraft.getInstance().setScreen(parent));
	}

	private final class WorldRow {
		private final LevelSummary summary;
		private final LinearLayout view;

		private WorldRow(Context context, LevelSummary summary) {
			this.summary = summary;
			view = new LinearLayout(context);
			view.setOrientation(LinearLayout.VERTICAL);
			view.setGravity(Gravity.CENTER_VERTICAL);
			view.setPadding(px(context, 10), px(context, 5), px(context, 10), px(context, 5));
			view.setClickable(true);
			KairokkUiSounds.attach(view);
			updateSelection(false);
			TextView name = text(context, summary.getLevelName(), 22, 0xFFF0EBEF);
			name.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
			TextView details = text(context, details(summary), 15, 0xFF77717C);
			details.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
			view.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 30)));
			view.addView(details, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 23)));
			view.setOnClickListener(ignored -> select(this));
		}

		private void updateSelection(boolean selected) {
			view.setBackground(worldBackground(view, selected));
		}
	}

	private static String details(LevelSummary summary) {
		String date = DATE.format(Instant.ofEpochMilli(summary.getLastPlayed()).atZone(ZoneId.systemDefault()));
		return date + "  ·  " + summary.getLevelId();
	}

	private static Button button(Context context, String label, Runnable action) {
		Button button = new Button(context);
		button.setText(label);
		button.setTextSize(textSize(context, 19));
		button.setTextColor(0xFFF3EEF1);
		button.setIncludeFontPadding(false);
		button.setGravity(Gravity.CENTER);
		button.setBackground(buttonBackground(button));
		KairokkUiSounds.attach(button);
		ShulkIcon icon = ShulkIcon.forAction(label);
		if (icon != null) ShulkIconView.decorate(button, icon, px(context, 17), 0xFFF3EEF1, 0xFFFFFFFF, 0xFFFFFFFF, 0x667A737D, label);
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

	private static View divider(Context context) {
		View view = new View(context);
		view.setBackground(shape(view, 0xFF211D24, 0xFF211D24, 0));
		return view;
	}

	private static StateListDrawable buttonBackground(View view) {
		StateListDrawable drawable = new StateListDrawable();
		drawable.addState(StateSet.get(StateSet.VIEW_STATE_PRESSED), shape(view, 0xFF286BD8, 0xFF286BD8, 6));
		drawable.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), shape(view, 0xFF4B94FF, 0xFF4B94FF, 6));
		drawable.addState(StateSet.WILD_CARD, shape(view, 0xFF171419, 0xFF232C3D, 6));
		drawable.setEnterFadeDuration(110);
		drawable.setExitFadeDuration(140);
		return drawable;
	}

	private static StateListDrawable worldBackground(View view, boolean selected) {
		StateListDrawable drawable = new StateListDrawable();
		drawable.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), shape(view, 0xFF201B22, 0xFF2A3446, 6));
		drawable.addState(StateSet.WILD_CARD, shape(view, selected ? 0xFF19161D : 0x00171619,
			selected ? 0xFF2A242D : 0xFF211D24, selected ? 6 : 0));
		drawable.setEnterFadeDuration(120);
		drawable.setExitFadeDuration(150);
		return drawable;
	}

	private static ShapeDrawable shape(View view, int fill, int outline, float radius) {
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
