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
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Fully functional blue resource-pack manager backed by Minecraft's pack repository. */
public final class KairokkResourcePackFragment extends Fragment {
	private static final int ACCENT = 0xFF3D8BFF;
	private static final int TEXT = 0xFFF0EAEE;
	private static final int MUTED = 0xFF8D8591;
	private final Screen parent;
	private final List<String> selectedIds = new ArrayList<>();
	@Nullable private FrameLayout rootView;
	@Nullable private LinearLayout availableList;
	@Nullable private LinearLayout selectedList;
	@Nullable private TextView status;
	@Nullable private String highlightedId;

	public KairokkResourcePackFragment(Screen parent) {
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
		FrameLayout.LayoutParams bodyParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		bodyParams.bottomMargin = px(context, ShulkTitleFragment.FOOTER_HEIGHT_DP);
		root.addView(body, bodyParams);

		LinearLayout content = new LinearLayout(context);
		content.setOrientation(LinearLayout.VERTICAL);
		content.setGravity(Gravity.CENTER_HORIZONTAL);
		content.addView(brand(context), full(context, 58, 0));
		TextView heading = text(context, "Resource Packs", 29, TEXT);
		heading.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		content.addView(heading, full(context, 38, 0));
		status = text(context, "Choose packs and set their priority", 14, MUTED);
		status.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		content.addView(status, full(context, 27, 10));

		LinearLayout columns = new LinearLayout(context);
		columns.setGravity(Gravity.CENTER);
		columns.addView(packColumn(context, "Available Resource Packs", false), columnParams(context, true));
		columns.addView(packColumn(context, "Selected Resource Packs", true), columnParams(context, false));
		content.addView(columns, full(context, 330, 10));

		LinearLayout controls = new LinearLayout(context);
		controls.setGravity(Gravity.CENTER);
		controls.addView(button(context, "Enable", this::enableHighlighted), controlParams(context));
		controls.addView(button(context, "Disable", this::disableHighlighted), controlParams(context));
		controls.addView(button(context, "Move Up", () -> moveHighlighted(-1)), controlParams(context));
		controls.addView(button(context, "Move Down", () -> moveHighlighted(1)), controlParams(context));
		content.addView(controls, full(context, 42, 9));

		LinearLayout actions = new LinearLayout(context);
		actions.setGravity(Gravity.CENTER);
		actions.addView(button(context, "Open Pack Folder", this::openFolder), actionParams(context, true));
		actions.addView(primaryButton(context, "Done", this::apply), actionParams(context, false));
		content.addView(actions, full(context, 43, 0));

		body.addView(content, new FrameLayout.LayoutParams(px(context, 820), ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
		ShulkTitleFragment.addFooter(root, context);
		loadPacks();
		KairokkScreenTransitions.fadeIn(root);
		return root;
	}

	private View brand(Context context) {
		LinearLayout brand = new LinearLayout(context); brand.setGravity(Gravity.CENTER);
		brand.addView(text(context, "K A I R O K K", 34, TEXT), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
		TextView version = text(context, "v0.1.0", 13, ACCENT); version.setPadding(px(context, 7), 0, 0, 0);
		brand.addView(version, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
		return brand;
	}

	private View packColumn(Context context, String title, boolean selected) {
		LinearLayout column = new LinearLayout(context); column.setOrientation(LinearLayout.VERTICAL);
		TextView label = text(context, title, 15, selected ? 0xFFA8C9FF : MUTED); label.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		column.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 27)));
		ScrollView scroll = new ScrollView(context); scroll.setEdgeEffectColor(ACCENT); scroll.setBottomEdgeEffectColor(ACCENT);
		LinearLayout list = new LinearLayout(context); list.setOrientation(LinearLayout.VERTICAL);
		scroll.addView(list, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		scroll.setBackground(box(scroll, 0xB90E121A, 0xFF29364A, 8));
		column.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
		if (selected) selectedList = list; else availableList = list;
		return column;
	}

	private void loadPacks() {
		PackRepository repository = Minecraft.getInstance().getResourcePackRepository();
		repository.reload();
		selectedIds.clear();
		selectedIds.addAll(repository.getSelectedIds());
		highlightedId = null;
		renderLists();
	}

	private void renderLists() {
		if (availableList == null || selectedList == null || status == null) return;
		PackRepository repository = Minecraft.getInstance().getResourcePackRepository();
		availableList.removeAllViews(); selectedList.removeAllViews();
		for (Pack pack : repository.getAvailablePacks()) {
			if (!selectedIds.contains(pack.getId())) availableList.addView(packRow(availableList.getContext(), pack, false), rowParams(availableList.getContext()));
		}
		for (String id : selectedIds) {
			Pack pack = repository.getPack(id);
			if (pack != null) selectedList.addView(packRow(selectedList.getContext(), pack, true), rowParams(selectedList.getContext()));
		}
		if (availableList.getChildCount() == 0) availableList.addView(emptyRow(availableList.getContext(), "No more packs available"), rowParams(availableList.getContext()));
		if (selectedList.getChildCount() == 0) selectedList.addView(emptyRow(selectedList.getContext(), "Default Minecraft resources"), rowParams(selectedList.getContext()));
		status.setText(selectedIds.size() + (selectedIds.size() == 1 ? " pack selected" : " packs selected") + " · Changes apply when you press Done");
	}

	private View packRow(Context context, Pack pack, boolean selectedColumn) {
		LinearLayout row = new LinearLayout(context); row.setOrientation(LinearLayout.VERTICAL); row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding(px(context, 12), px(context, 6), px(context, 12), px(context, 6)); row.setClickable(true);
		row.setBackground(rowBackground(row, pack.getId().equals(highlightedId)));
		KairokkUiSounds.attach(row);
		TextView title = text(context, pack.getTitle().getString(), 17, TEXT); title.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		TextView detail = text(context, trim(pack.getDescription().getString()), 12, MUTED); detail.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		row.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 25)));
		row.addView(detail, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 20)));
		row.setOnClickListener(view -> { highlightedId = pack.getId(); renderLists(); });
		return row;
	}

	private View emptyRow(Context context, String value) {
		TextView empty = text(context, value, 14, MUTED); empty.setGravity(Gravity.CENTER); return empty;
	}

	private void enableHighlighted() {
		if (highlightedId == null || selectedIds.contains(highlightedId)) return;
		selectedIds.add(0, highlightedId); renderLists();
	}

	private void disableHighlighted() {
		if (highlightedId == null) return;
		Pack pack = Minecraft.getInstance().getResourcePackRepository().getPack(highlightedId);
		if (pack == null || pack.isRequired() || pack.isFixedPosition()) return;
		selectedIds.remove(highlightedId); highlightedId = null; renderLists();
	}

	private void moveHighlighted(int direction) {
		if (highlightedId == null) return;
		int index = selectedIds.indexOf(highlightedId); int next = index + direction;
		if (index < 0 || next < 0 || next >= selectedIds.size()) return;
		String displaced = selectedIds.get(next); selectedIds.set(next, highlightedId); selectedIds.set(index, displaced); renderLists();
	}

	private void openFolder() { Util.getPlatform().openPath(Minecraft.getInstance().getResourcePackDirectory()); }

	private void apply() {
		Core.executeOnMainThread(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			PackRepository repository = minecraft.getResourcePackRepository();
			repository.setSelected(selectedIds);
			Options options = minecraft.options;
			options.updateResourcePacks(repository);
			minecraft.reloadResourcePacks();
			minecraft.setScreen(parent);
		});
	}

	void cancel() { KairokkScreenTransitions.fadeOut(rootView, () -> Minecraft.getInstance().setScreen(parent)); }

	private static Button button(Context context, String label, Runnable action) {
		Button button = new Button(context); button.setText(label); button.setTextSize(textSize(context, 15)); button.setTextColor(TEXT);
		button.setGravity(Gravity.CENTER); button.setIncludeFontPadding(false); button.setBackground(buttonBackground(button));
		KairokkUiSounds.attach(button); button.setOnClickListener(view -> Core.executeOnUiThread(action)); return button;
	}
	private static Button primaryButton(Context context, String label, Runnable action) { Button button = button(context, label, action); button.setBackground(primaryBackground(button)); return button; }
	private static TextView text(Context context, String value, float size, int color) { TextView text = new TextView(context); text.setText(value); text.setTextSize(textSize(context, size)); text.setTextColor(color); text.setIncludeFontPadding(false); return text; }
	private static LinearLayout.LayoutParams full(Context context, int height, int bottom) { LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, height)); params.bottomMargin = px(context, bottom); return params; }
	private static LinearLayout.LayoutParams columnParams(Context context, boolean left) { LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1); if (left) params.rightMargin = px(context, 8); else params.leftMargin = px(context, 8); return params; }
	private static LinearLayout.LayoutParams controlParams(Context context) { LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1); params.leftMargin = params.rightMargin = px(context, 4); return params; }
	private static LinearLayout.LayoutParams actionParams(Context context, boolean left) { LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1); if (left) params.rightMargin = px(context, 6); else params.leftMargin = px(context, 6); return params; }
	private static LinearLayout.LayoutParams rowParams(Context context) { LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 57)); params.leftMargin = params.rightMargin = px(context, 6); params.topMargin = px(context, 5); return params; }
	private static String trim(String value) { return value.length() > 58 ? value.substring(0, 57) + "…" : value; }
	private static StateListDrawable buttonBackground(View view) { StateListDrawable background = new StateListDrawable(); background.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), box(view, 0xFF4A94FF, 0xFF4A94FF, 6)); background.addState(StateSet.WILD_CARD, box(view, 0xFF171419, 0xFF303A4C, 6)); return background; }
	private static StateListDrawable primaryBackground(View view) { StateListDrawable background = new StateListDrawable(); background.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), box(view, 0xFF5B9DFF, 0xFF5B9DFF, 6)); background.addState(StateSet.WILD_CARD, box(view, ACCENT, ACCENT, 6)); return background; }
	private static StateListDrawable rowBackground(View view, boolean selected) { StateListDrawable background = new StateListDrawable(); background.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), box(view, 0xFF161E2B, ACCENT, 7)); background.addState(StateSet.WILD_CARD, box(view, selected ? 0xFF18243A : 0xFF11151E, selected ? ACCENT : 0xFF29364A, 7)); return background; }
	private static ShapeDrawable box(View view, int fill, int outline, float radius) { ShapeDrawable drawable = new ShapeDrawable(); drawable.setShape(ShapeDrawable.RECTANGLE); drawable.setColor(fill); drawable.setCornerRadius(px(view.getContext(), radius)); drawable.setStroke(px(view.getContext(), 1), outline); return drawable; }
	private static int px(Context context, float value) { int width = Minecraft.getInstance().getWindow().getScreenWidth(); return Math.round(value * Math.max(.70F, Math.min(.90F, width / 1100F))); }
	private static float textSize(Context context, float value) { return px(context, value) / Math.max(.01F, context.getResources().getDisplayMetrics().scaledDensity); }
}
