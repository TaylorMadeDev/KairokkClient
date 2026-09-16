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
import icyllis.modernui.widget.TextView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/** Static visual shell for Kairokk's future module manager. */
public final class KairokkClickGuiFragment extends Fragment {
	private static final int ACCENT = 0xFF4A94FF;
	private static final int TEXT = 0xFFF0F4FF;
	private static final int MUTED = 0xFF9AA7BC;
	private final Screen parent;

	public KairokkClickGuiFragment(Screen parent) {
		this.parent = parent;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable DataSet savedInstanceState) {
		Context context = requireContext();
		FrameLayout root = new FrameLayout(context);
		LinearLayout shell = new LinearLayout(context);
		shell.setOrientation(LinearLayout.VERTICAL);
		shell.setPadding(px(context, 22), px(context, 20), px(context, 22), px(context, 20));
		shell.setBackground(box(shell, 0xE6101825, 0x883A516E, 11));

		shell.addView(topBar(context), full(context, 51, 15));
		LinearLayout body = new LinearLayout(context);
		body.setGravity(Gravity.CENTER_VERTICAL);
		body.addView(categories(context), fixed(context, 178, ViewGroup.LayoutParams.MATCH_PARENT, 12));
		body.addView(modulePanel(context), weighted(context, 1, true));
		body.addView(detailsPanel(context), fixed(context, 260, ViewGroup.LayoutParams.MATCH_PARENT, 0));
		shell.addView(body, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(px(context, 1180), px(context, 570), Gravity.CENTER);
		root.addView(shell, params);
		KairokkScreenTransitions.fadeIn(root);
		return root;
	}

	private View topBar(Context context) {
		LinearLayout bar = new LinearLayout(context);
		bar.setGravity(Gravity.CENTER_VERTICAL);
		bar.setPadding(px(context, 15), 0, px(context, 8), 0);
		bar.setBackground(box(bar, 0xCC172231, 0x66405A7D, 8));
		ShulkIconView brand = new ShulkIconView(context, ShulkIcon.BRAND, ACCENT, "Kairokk Client GUI");
		bar.addView(brand, new LinearLayout.LayoutParams(px(context, 30), px(context, 30)));
		TextView title = text(context, "C L I E N T", 16, TEXT);
		title.setPadding(px(context, 10), 0, px(context, 30), 0);
		bar.addView(title, new LinearLayout.LayoutParams(px(context, 205), ViewGroup.LayoutParams.MATCH_PARENT));
		for (String tab : new String[]{"VISUALS", "COMBAT", "MOVEMENT", "PLAYER", "WORLD", "MISC"}) {
			TextView view = text(context, tab, 14, "VISUALS".equals(tab) ? TEXT : MUTED);
			view.setGravity(Gravity.CENTER);
			view.setBackground("VISUALS".equals(tab) ? tabBackground(view) : null);
			bar.addView(view, new LinearLayout.LayoutParams(px(context, 114), ViewGroup.LayoutParams.MATCH_PARENT));
		}
		View spacer = new View(context);
		bar.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
		ShulkIconView search = new ShulkIconView(context, ShulkIcon.SEARCH, MUTED, "Search modules");
		bar.addView(search, new LinearLayout.LayoutParams(px(context, 36), ViewGroup.LayoutParams.MATCH_PARENT));
		ShulkIconView settings = new ShulkIconView(context, ShulkIcon.SETTINGS, MUTED, "Client GUI settings");
		bar.addView(settings, new LinearLayout.LayoutParams(px(context, 36), ViewGroup.LayoutParams.MATCH_PARENT));
		ShulkIconView close = new ShulkIconView(context, ShulkIcon.CLOSE, MUTED, "Close client GUI");
		close.setClickable(true);
		close.setOnClickListener(view -> Core.executeOnMainThread(() -> Minecraft.getInstance().setScreen(parent)));
		bar.addView(close, new LinearLayout.LayoutParams(px(context, 36), ViewGroup.LayoutParams.MATCH_PARENT));
		return bar;
	}

	private View categories(Context context) {
		LinearLayout column = new LinearLayout(context);
		column.setOrientation(LinearLayout.VERTICAL);
		column.setPadding(px(context, 9), px(context, 10), px(context, 9), px(context, 10));
		column.setBackground(box(column, 0xCC141E2B, 0x66344E70, 8));
		category(column, context, ShulkIcon.VISIBLE, "Visuals", true);
		category(column, context, ShulkIcon.TARGET, "Combat", false);
		category(column, context, ShulkIcon.FORWARD, "Movement", false);
		category(column, context, ShulkIcon.PLAYER, "Player", false);
		category(column, context, ShulkIcon.GLOBE, "World", false);
		category(column, context, ShulkIcon.MORE, "Misc", false);
		return column;
	}

	private void category(LinearLayout target, Context context, ShulkIcon icon, String label, boolean active) {
		LinearLayout row = new LinearLayout(context);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding(px(context, 11), 0, px(context, 9), 0);
		row.setBackground(active ? box(row, 0xFF263B58, ACCENT, 7) : null);
		ShulkIconView glyph = new ShulkIconView(context, icon, active ? TEXT : MUTED, label);
		row.addView(glyph, new LinearLayout.LayoutParams(px(context, 29), ViewGroup.LayoutParams.MATCH_PARENT));
		TextView name = text(context, label, 16, active ? TEXT : MUTED);
		name.setGravity(Gravity.CENTER_VERTICAL);
		name.setPadding(px(context, 9), 0, 0, 0);
		row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 48));
		params.bottomMargin = px(context, 6);
		target.addView(row, params);
	}

	private View modulePanel(Context context) {
		LinearLayout panel = new LinearLayout(context);
		panel.setOrientation(LinearLayout.VERTICAL);
		panel.setPadding(px(context, 17), px(context, 15), px(context, 17), px(context, 14));
		panel.setBackground(box(panel, 0xCC111A26, 0x66344E70, 8));
		LinearLayout heading = new LinearLayout(context);
		heading.setGravity(Gravity.CENTER_VERTICAL);
		TextView title = text(context, "V I S U A L S", 16, TEXT);
		heading.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
		TextView detail = text(context, "See more. Play better.", 13, MUTED);
		detail.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		heading.addView(detail, new LinearLayout.LayoutParams(px(context, 185), ViewGroup.LayoutParams.MATCH_PARENT));
		panel.addView(heading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 43)));
		String[][] rows = {
			{"ESP", "on", "Chest ESP", "off"}, {"Name Tags", "on", "Player ESP", "on"},
			{"Tracers", "off", "Mob ESP", "off"}, {"Chams", "off", "Entity ESP", "off"},
			{"Fullbright", "on", "Waypoints", "on"}, {"Xray", "off", "No Render", "off"},
			{"Block Outline", "on", "HUD", "on"}, {"Item ESP", "off", "Animations", "off"}
		};
		for (String[] row : rows) {
			LinearLayout line = new LinearLayout(context);
			line.addView(moduleCard(context, row[0], "on".equals(row[1])), weighted(context, 1, true));
			line.addView(moduleCard(context, row[2], "on".equals(row[3])), weighted(context, 1, false));
			LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 43));
			lineParams.bottomMargin = px(context, 6);
			panel.addView(line, lineParams);
		}
		return panel;
	}

	private View moduleCard(Context context, String label, boolean enabled) {
		LinearLayout card = new LinearLayout(context);
		card.setGravity(Gravity.CENTER_VERTICAL);
		card.setPadding(px(context, 12), 0, px(context, 10), 0);
		card.setBackground(box(card, 0xE61C2836, 0x55445B79, 7));
		TextView name = text(context, label, 15, TEXT);
		name.setGravity(Gravity.CENTER_VERTICAL);
		card.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
		TextView toggle = text(context, enabled ? "ON" : "OFF", 11, enabled ? 0xFFFFFFFF : MUTED);
		toggle.setGravity(Gravity.CENTER);
		toggle.setBackground(pill(toggle, enabled ? ACCENT : 0xFF3D4A5B));
		card.addView(toggle, new LinearLayout.LayoutParams(px(context, 42), px(context, 23)));
		return card;
	}

	private View detailsPanel(Context context) {
		LinearLayout panel = new LinearLayout(context);
		panel.setOrientation(LinearLayout.VERTICAL);
		panel.setPadding(px(context, 14), px(context, 14), px(context, 14), px(context, 14));
		panel.setBackground(box(panel, 0xCC111A26, 0x66344E70, 8));
		LinearLayout searchShell = new LinearLayout(context);
		searchShell.setGravity(Gravity.CENTER_VERTICAL);
		searchShell.setPadding(px(context, 9), 0, px(context, 9), 0);
		searchShell.setBackground(box(searchShell, 0xFF182331, 0xFF374E6E, 7));
		searchShell.addView(new ShulkIconView(context, ShulkIcon.SEARCH, MUTED, "Search"), new LinearLayout.LayoutParams(px(context, 27), ViewGroup.LayoutParams.MATCH_PARENT));
		EditText search = new EditText(context);
		search.setHint("Search..."); search.setSingleLine(true); search.setTextSize(textSize(context, 14));
		search.setHintTextColor(MUTED); search.setTextColor(TEXT); search.setBackground(null);
		searchShell.addView(search, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
		panel.addView(searchShell, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 43)));
		TextView title = text(context, "ESP", 22, TEXT); title.setPadding(0, px(context, 18), 0, 0);
		panel.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 47)));
		TextView copy = text(context, "Highlights players, mobs and entities through walls.", 13, MUTED);
		copy.setGravity(Gravity.LEFT | Gravity.TOP);
		panel.addView(copy, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 58)));
		View line = new View(context); line.setBackground(box(line, 0x66506987, 0x66506987, 0));
		panel.addView(line, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 1)));
		for (String[] item : new String[][]{{"Players", "on"}, {"Mobs", "on"}, {"Items", "off"}, {"Chests", "off"}, {"Tracers", "off"}, {"Box", "on"}, {"Outline", "off"}}) {
			LinearLayout row = new LinearLayout(context); row.setGravity(Gravity.CENTER_VERTICAL);
			TextView label = text(context, item[0], 14, MUTED); row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
			TextView toggle = text(context, "on".equals(item[1]) ? "ON" : "OFF", 10, "on".equals(item[1]) ? 0xFFFFFFFF : MUTED);
			toggle.setGravity(Gravity.CENTER); toggle.setBackground(pill(toggle, "on".equals(item[1]) ? ACCENT : 0xFF3D4A5B));
			row.addView(toggle, new LinearLayout.LayoutParams(px(context, 42), px(context, 22)));
			panel.addView(row, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 36)));
		}
		return panel;
	}

	private static LinearLayout.LayoutParams full(Context context, int height, int bottomMargin) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, height)); p.bottomMargin = px(context, bottomMargin); return p; }
	private static LinearLayout.LayoutParams fixed(Context context, int width, int height, int rightMargin) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(px(context, width), height); p.rightMargin = px(context, rightMargin); return p; }
	private static LinearLayout.LayoutParams weighted(Context context, int weight, boolean rightMargin) { LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight); if (rightMargin) p.rightMargin = px(context, 12); return p; }
	private static TextView text(Context context, String value, float size, int color) { TextView view = new TextView(context); view.setText(value); view.setTextSize(textSize(context, size)); view.setTextColor(color); view.setIncludeFontPadding(false); return view; }
	private static ShapeDrawable box(View view, int fill, int outline, float radius) { ShapeDrawable drawable = new ShapeDrawable(); drawable.setShape(ShapeDrawable.RECTANGLE); drawable.setColor(fill); drawable.setCornerRadius(px(view.getContext(), radius)); drawable.setStroke(px(view.getContext(), 1), outline); return drawable; }
	private static ShapeDrawable pill(View view, int color) { ShapeDrawable drawable = new ShapeDrawable(); drawable.setShape(ShapeDrawable.RECTANGLE); drawable.setColor(color); drawable.setCornerRadius(px(view.getContext(), 12)); return drawable; }
	private static StateListDrawable tabBackground(View view) { StateListDrawable drawable = new StateListDrawable(); drawable.addState(StateSet.WILD_CARD, box(view, 0x00202020, ACCENT, 0)); return drawable; }
	private static int px(Context context, float value) {
		int width = Minecraft.getInstance().getWindow().getScreenWidth();
		// Preserve the compact minimum-window layout, but let the panel use the room
		// available on a proper fullscreen display instead of remaining thumbnail-sized.
		float scale = width < 1000 ? .62F : width / 1250F;
		return Math.round(value * scale);
	}
	private static float textSize(Context context, float value) { return px(context, value) / Math.max(.01F, context.getResources().getDisplayMetrics().scaledDensity); }
}
