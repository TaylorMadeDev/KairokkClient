package com.kairokk.client;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;
import icyllis.modernui.animation.Animator;
import icyllis.modernui.animation.AnimatorListener;
import icyllis.modernui.animation.BezierInterpolator;
import icyllis.modernui.animation.ObjectAnimator;
import icyllis.modernui.animation.ValueAnimator;
import icyllis.modernui.core.Context;
import icyllis.modernui.core.Core;
import icyllis.modernui.text.Editable;
import icyllis.modernui.text.TextWatcher;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.graphics.drawable.StateListDrawable;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Color;
import icyllis.modernui.graphics.LinearGradient;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.graphics.Shader;
import icyllis.modernui.graphics.text.FontPaint;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.FrameLayout;
import icyllis.modernui.widget.EditText;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.PopupWindow;
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import com.kairokk.KairokkClient;

import com.kairokk.client.pathfinder.PathFinderSettings;
import com.kairokk.client.pathfinder.PathfinderOptions;
import com.kairokk.client.pathfinder.RotationController;

/** The clean first layer of the rebuilt Kairokk client GUI. */
public final class KairokkClickGuiFragment extends Fragment {
	private static int ACCENT = 0xFF4A94FF;
	private static int TEXT = 0xFFF0F4FF;
	private static int MUTED = 0xFFA8B4C8;
	private static int BLUE = 0xFF4A94FF;
	private static int BLUE_LIGHT = 0xFF70AEFF;
	private static final int[] BOX_COLORS = {0xFF4A94FF, 0xFF6BD5FF, 0xFF9D8BFF, 0xFFFFFFFF};
	private final java.util.Map<String, Boolean> switches = new java.util.HashMap<>();
	private final java.util.Map<String, Boolean> mobSwitches = new java.util.HashMap<>();
	private final java.util.Map<String, Boolean> mobSelections = new java.util.LinkedHashMap<>();
	private boolean mobIncludeMode = true;
	private String mobSearch = "";
	private int boxColor = BOX_COLORS[0];
	private String boxStyle = "Corner";
	private String selectedCategory = "Players";
	private String selectedTab = "VISUALS";
	private String selectedMiscPage = "Client Customization";
	private String selectedPathfinderPage = "Pathfinder";
	private String selectedPathfinderSection = "General";
	private boolean contentTransitionRunning;
	private FrameLayout rootView;
	private View workspaceView;
	private int lastViewportWidth;
	private final java.util.List<TextView> tabs = new java.util.ArrayList<>();
	// Palette-dependent controls are kept so a colour picker can repaint them live.
	private final java.util.List<View> paletteBlueViews = new java.util.ArrayList<>();
	private final java.util.List<View> paletteLightViews = new java.util.ArrayList<>();
	private LinearLayout visualColumns;
	private TextView visualPageTitle;
	private TextView visualPageSubtitle;
	private ItemPreview itemPreview;
	private volatile PreviewBounds previewBounds;
	private volatile PreviewStyle previewStyle;
	private final Screen parent;

	private void syncCustomizationPalette() {
		ACCENT = KairokkCustomizationState.accent();
		BLUE = KairokkCustomizationState.primary();
		BLUE_LIGHT = KairokkCustomizationState.primaryLight();
		for (View view : paletteBlueViews) view.setBackground(pill(view, BLUE));
		for (View view : paletteLightViews) view.setBackground(pill(view, BLUE_LIGHT));
	}

	public KairokkClickGuiFragment(Screen parent) {
		this.parent = parent;
		switches.put("Player ESP", true);
		switches.put("Name Tags", true);
		switches.put("Distance", true);
		switches.put("Health", false);
		switches.put("Tracers", false);
		 switches.put("Chams", false);
		mobSwitches.put("Mob ESP", true);
		mobSwitches.put("Outline", true);
		mobSwitches.put("Name Tags", true);
		mobSwitches.put("Distance", true);
		mobSwitches.put("Health", true);
		mobSwitches.put("Tracers", false);
		mobSwitches.put("Chams", false);
		mobSwitches.put("Ignore Invisible", true);
		for (String mob : MOB_NAMES) mobSelections.put(mob, true);
	}

	private static final String[] MOB_NAMES = {"Zombie", "Skeleton", "Creeper", "Spider", "Enderman", "Slime", "Ghast", "Wither Skeleton", "Blaze", "Cow", "Pig", "Sheep", "Chicken", "Wolf", "Cat", "Horse", "Villager", "Iron Golem"};
	private static final int[] MOB_COLORS = {0xFF6FA954, 0xFFE5E5E5, 0xFF65BA58, 0xFF493B39, 0xFF20212A, 0xFF80D86A, 0xFFF2F2F2, 0xFF575757, 0xFFFFB52E, 0xFFB8845C, 0xFFFF99A4, 0xFFF4F3E8, 0xFFFFE6CA, 0xFFE9ECEE, 0xFFF0A86C, 0xFFA76B43, 0xFF9A6B45, 0xFFD8D2C7};

	@Nullable @Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
			@Nullable DataSet savedInstanceState) {
		Context context = requireContext();
		// Deliberately no background or body: the game remains visible behind this bar.
		FrameLayout root = new FrameLayout(context);
		rootView = root;
		populateRoot(context);
		root.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
			int width = right - left;
			if (oldRight - oldLeft <= 0 || width <= 0 || width == lastViewportWidth) return;
			lastViewportWidth = width;
			view.post(() -> populateRoot(context));
		});
		KairokkScreenTransitions.fadeIn(root);
		return root;
	}

	/** Recreate fixed-pixel controls when Minecraft changes its framebuffer size. */
	private void populateRoot(Context context) {
		if (rootView == null) return;
		paletteBlueViews.clear();
		paletteLightViews.clear();
		syncCustomizationPalette();
		rootView.removeAllViews();
		tabs.clear();
		FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, px(context, 56), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
		barParams.leftMargin = px(context, 16);
		barParams.rightMargin = px(context, 16);
		barParams.topMargin = px(context, 14);
		rootView.addView(topBar(context), barParams);
		FrameLayout.LayoutParams visualsParams = new FrameLayout.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT, workspaceHeight(context), Gravity.TOP | Gravity.CENTER_HORIZONTAL);
		visualsParams.leftMargin = px(context, 28);
		visualsParams.rightMargin = px(context, 28);
		visualsParams.topMargin = px(context, 88);
		workspaceView = "VISUALS".equals(selectedTab) ? visualsWorkspace(context)
			: "PATHFINDER".equals(selectedTab) ? pathfinderWorkspace(context)
			: "MISC".equals(selectedTab) ? miscWorkspace(context) : topTabPlaceholder(context);
		rootView.addView(workspaceView, visualsParams);
	}

	private View topTabPlaceholder(Context context) {
		TextView comingSoon = text(context, selectedTab + " settings are coming soon.", 18, MUTED);
		comingSoon.setGravity(Gravity.CENTER);
		comingSoon.setBackground(box(comingSoon, 0xBD101A26, 0x77506A90, 9));
		return comingSoon;
	}

	private View miscWorkspace(Context context) {
		LinearLayout shell = new LinearLayout(context);
		shell.setPadding(px(context, 10), px(context, 10), px(context, 10), px(context, 10));
		shell.setBackground(box(shell, 0xBD101A26, 0x77506A90, 9));

		LinearLayout sidebar = new LinearLayout(context);
		sidebar.setOrientation(LinearLayout.VERTICAL);
		sidebar.setPadding(px(context, 10), px(context, 10), px(context, 10), px(context, 10));
		sidebar.setBackground(box(sidebar, 0x6D152231, 0x553B587C, 7));
		TextView sidebarTitle = text(context, "M I S C", 17, TEXT);
		sidebarTitle.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		sidebar.addView(sidebarTitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 29)));
		TextView sidebarSubtitle = text(context, "Client utilities and settings.", 10, BLUE_LIGHT);
		sidebarSubtitle.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams sidebarSubtitleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 24));
		sidebarSubtitleParams.bottomMargin = px(context, 10);
		sidebar.addView(sidebarSubtitle, sidebarSubtitleParams);
		addMiscNavigation(sidebar, context, ShulkIcon.CUSTOMIZE, "Client Customization", "Themes, colours & interface.");
		addMiscNavigation(sidebar, context, ShulkIcon.FOLDER, "Config Management", "Profiles and local sync.");
		addMiscNavigation(sidebar, context, ShulkIcon.CONTROLS, "Keybinds", "Manage your keybinds.");
		addMiscNavigation(sidebar, context, ShulkIcon.INFO, "Notifications", "In-game notifications.");
		addMiscNavigation(sidebar, context, ShulkIcon.LANGUAGE, "Language", "Change client language.");
		addMiscNavigation(sidebar, context, ShulkIcon.RESET, "Updates", "Client version and updates.");
		addMiscNavigation(sidebar, context, ShulkIcon.SETTINGS, "Advanced", "Experimental features.");
		LinearLayout.LayoutParams sidebarParams = new LinearLayout.LayoutParams(px(context, 195), ViewGroup.LayoutParams.MATCH_PARENT);
		sidebarParams.rightMargin = px(context, 12);
		shell.addView(sidebar, sidebarParams);

		LinearLayout main = new LinearLayout(context);
		main.setOrientation(LinearLayout.VERTICAL);
		main.setPadding(px(context, 2), px(context, 2), px(context, 2), px(context, 2));
		LinearLayout header = new LinearLayout(context);
		header.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout headerCopy = new LinearLayout(context);
		headerCopy.setOrientation(LinearLayout.VERTICAL);
		TextView title = text(context, selectedMiscPage.toUpperCase(java.util.Locale.ROOT), 18, TEXT);
		title.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		TextView subtitle = text(context, miscDescription(selectedMiscPage), 11, BLUE_LIGHT);
		subtitle.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		headerCopy.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 29)));
		headerCopy.addView(subtitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 22)));
		header.addView(headerCopy, new LinearLayout.LayoutParams(0, px(context, 55), 1));
		if ("Client Customization".equals(selectedMiscPage)) {
			View theme = themeDropdown(context);
			LinearLayout.LayoutParams themeParams = new LinearLayout.LayoutParams(px(context, 132), px(context, 36));
			themeParams.rightMargin = px(context, 7);
			header.addView(theme, themeParams);
			TextView reset = miscAction(context, "↻  Reset to Default", false);
			reset.setOnClickListener(view -> {
				KairokkCustomizationState.reset();
				syncCustomizationPalette();
				refreshCustomization(context);
			});
			header.addView(reset, new LinearLayout.LayoutParams(px(context, 139), px(context, 36)));
		}
		main.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 58)));

		ScrollView scroll = new ScrollView(context);
		scroll.setFillViewport(true);
		scroll.setSmoothScrollingEnabled(KairokkCustomizationState.smoothScrolling());
		scroll.setEdgeEffectColor(BLUE);
		scroll.setBottomEdgeEffectColor(BLUE);
		LinearLayout body = new LinearLayout(context);
		body.setOrientation(LinearLayout.VERTICAL);
		body.setPadding(0, px(context, 2), px(context, 5), px(context, 8));
		if ("Client Customization".equals(selectedMiscPage)) addCustomizationBody(context, body);
		else addMiscPlaceholder(context, body, selectedMiscPage);
		scroll.addView(body, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		main.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
		shell.addView(main, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
		return shell;
	}

	private void addMiscNavigation(LinearLayout sidebar, Context context, ShulkIcon icon, String label, String detail) {
		boolean selected = label.equals(selectedMiscPage);
		LinearLayout row = new LinearLayout(context);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding(px(context, 8), 0, px(context, 7), 0);
		row.setBackground(selected ? categoryBackground(row) : box(row, 0x00152231, 0x00000000, 6));
		ShulkIconView glyph = new ShulkIconView(context, icon, selected ? TEXT : BLUE_LIGHT, label);
		row.addView(glyph, new LinearLayout.LayoutParams(px(context, 27), px(context, 38)));
		LinearLayout labels = new LinearLayout(context);
		labels.setOrientation(LinearLayout.VERTICAL);
		labels.setGravity(Gravity.CENTER_VERTICAL);
		TextView name = text(context, label, 11, selected ? TEXT : MUTED);
		TextView explanation = text(context, detail, 8, selected ? BLUE_LIGHT : MUTED);
		labels.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 17)));
		labels.addView(explanation, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 14)));
		LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
		labelParams.leftMargin = px(context, 7);
		row.addView(labels, labelParams);
		row.setClickable(true);
		attachSidebarHover(row, () -> label.equals(selectedMiscPage));
		row.setOnClickListener(view -> {
			if (label.equals(selectedMiscPage) || contentTransitionRunning) return;
			selectedMiscPage = label;
			transitionMisc(context);
		});
		KairokkUiSounds.attachSelection(row);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 46));
		params.bottomMargin = px(context, 4);
		sidebar.addView(row, params);
	}

	private String miscDescription(String page) {
		return switch (page) {
			case "Config Management" -> "Profiles and local sync.";
			case "Keybinds" -> "Manage your keybinds.";
			case "Notifications" -> "In-game notifications.";
			case "Language" -> "Change client language.";
			case "Updates" -> "Client version and updates.";
			case "Advanced" -> "Experimental features.";
			default -> "Make the client look and feel the way you want.";
		};
	}

	private void transitionMisc(Context context) {
		if (rootView == null || workspaceView == null || contentTransitionRunning) return;
		contentTransitionRunning = true;
		View previous = workspaceView;
		FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) previous.getLayoutParams();
		KairokkScreenTransitions.crossFade(previous, () -> {
			syncCustomizationPalette();
			rootView.removeView(previous);
			workspaceView = miscWorkspace(context);
			rootView.addView(workspaceView, params);
			KairokkScreenTransitions.fadeContentIn(workspaceView);
			contentTransitionRunning = false;
		});
	}

	/** Rebuilds customization controls without the page fade used for navigation. */
	private void refreshCustomization(Context context) {
		if (rootView == null || workspaceView == null || !"MISC".equals(selectedTab)
				|| !"Client Customization".equals(selectedMiscPage)) return;
		int scrollY = 0;
		if (workspaceView instanceof ViewGroup shell && shell.getChildCount() > 1
				&& shell.getChildAt(1) instanceof ViewGroup main && main.getChildCount() > 1
				&& main.getChildAt(1) instanceof ScrollView scroll) {
			scrollY = scroll.getScrollY();
		}
		FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) workspaceView.getLayoutParams();
		rootView.removeView(workspaceView);
		syncCustomizationPalette();
		workspaceView = miscWorkspace(context);
		rootView.addView(workspaceView, params);
		final int retainedScrollY = scrollY;
		workspaceView.post(() -> {
			if (workspaceView instanceof ViewGroup shell && shell.getChildCount() > 1
					&& shell.getChildAt(1) instanceof ViewGroup main && main.getChildCount() > 1
					&& main.getChildAt(1) instanceof ScrollView scroll) scroll.scrollTo(0, retainedScrollY);
		});
	}

	private void addMiscPlaceholder(Context context, LinearLayout body, String page) {
		LinearLayout card = miscCard(context, ShulkIcon.forAction(page), page, miscDescription(page));
		TextView message = text(context, "This Kairokk settings area is ready for your local profile.", 13, MUTED);
		message.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		card.addView(message, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 54)));
		TextView back = miscAction(context, "Back to Client Customization", true);
		back.setOnClickListener(view -> { selectedMiscPage = "Client Customization"; transitionMisc(context); });
		card.addView(back, new LinearLayout.LayoutParams(px(context, 210), px(context, 36)));
		body.addView(card, miscCardParams(context, 0));
	}

	private void addCustomizationBody(Context context, LinearLayout body) {
		LinearLayout top = new LinearLayout(context);
		top.setOrientation(LinearLayout.HORIZONTAL);
		top.setGravity(Gravity.TOP);
		top.addView(paletteCard(context), miscWeightParams(context, 1.25F, true));
		top.addView(uiElementsCard(context), miscWeightParams(context, 1F, false));
		body.addView(top, miscCardParams(context, 0));

		LinearLayout middle = new LinearLayout(context);
		middle.setOrientation(LinearLayout.HORIZONTAL);
		middle.setGravity(Gravity.TOP);
		middle.addView(animationsCard(context), miscWeightParams(context, 1F, true));
		middle.addView(backgroundCard(context), miscWeightParams(context, 1F, true));
		middle.addView(miscOptionsCard(context), miscWeightParams(context, 1F, false));
		body.addView(middle, miscCardParams(context, 0));

		body.addView(extraCard(context), miscCardParams(context, 0));
	}

	private View themeDropdown(Context context) {
		LinearLayout choice = new LinearLayout(context);
		choice.setGravity(Gravity.CENTER_VERTICAL);
		choice.setPadding(px(context, 9), 0, px(context, 5), 0);
		choice.setBackground(box(choice, 0x5D223147, 0x885377A4, 5));
		TextView value = text(context, KairokkCustomizationState.theme(), 9, TEXT);
		value.setGravity(Gravity.CENTER_VERTICAL);
		choice.addView(value, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
		ShulkIconView arrow = new ShulkIconView(context, ShulkIcon.EXPAND, BLUE_LIGHT, "Choose theme");
		arrow.setPadding(px(context, 2), px(context, 8), px(context, 2), px(context, 8));
		choice.addView(arrow, new LinearLayout.LayoutParams(px(context, 17), ViewGroup.LayoutParams.MATCH_PARENT));
		choice.setClickable(true);
		KairokkUiSounds.attach(choice);
		choice.setOnClickListener(view -> showCustomizationMenu(context, choice, value, arrow,
				new String[]{"Default", "Dark", "Light", "Abyss", "Forest", "Solar", "Custom"}, option -> {
					KairokkCustomizationState.applyTheme(option);
					syncCustomizationPalette();
					value.setText(option);
					Core.getUiHandler().post(() -> refreshCustomization(context));
				}));
		return choice;
	}

	private LinearLayout paletteCard(Context context) {
		LinearLayout card = miscCard(context, ShulkIcon.PALETTE, "COLOUR PALETTE", "Customize the interface colours.");
		card.addView(paletteRow(context, "Primary", "primary", "Secondary", "secondary"), miscRowParams(context));
		card.addView(paletteRow(context, "Accent", "accent", "Danger", "danger"), miscRowParams(context));
		card.addView(paletteRow(context, "Panel", "panel", "Panel Border", "border"), miscRowParams(context));
		card.addView(paletteRow(context, "Text", "text", "Muted Text", "muted"), miscRowParams(context));
		return card;
	}

	private LinearLayout paletteRow(Context context, String leftLabel, String leftSlot, String rightLabel, String rightSlot) {
		LinearLayout row = new LinearLayout(context);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.addView(customColorRow(context, leftLabel, leftSlot), miscWeightParams(context, 1F, true));
		row.addView(customColorRow(context, rightLabel, rightSlot), miscWeightParams(context, 1F, false));
		return row;
	}

	private View customColorRow(Context context, String label, String slot) {
		LinearLayout row = new LinearLayout(context);
		row.setGravity(Gravity.CENTER_VERTICAL);
		TextView name = text(context, label, 10, TEXT);
		name.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		row.addView(name, new LinearLayout.LayoutParams(px(context, 66), px(context, 31)));
		View swatch = new View(context);
		swatch.setBackground(colorSwatch(swatch, customizationColor(slot)));
		swatch.setClickable(true);
		KairokkUiSounds.attach(swatch);
		swatch.setOnClickListener(view -> showCustomizationColorPicker(context, swatch, slot));
		row.addView(swatch, new LinearLayout.LayoutParams(px(context, 25), px(context, 25)));
		EditText hexField = new EditText(context);
		hexField.setSingleLine(true);
		hexField.setText(hex(customizationColor(slot)));
		hexField.setTextSize(textSize(context, 9));
		hexField.setTextColor(TEXT);
		hexField.setGravity(Gravity.CENTER);
		hexField.setPadding(px(context, 4), 0, px(context, 4), 0);
		hexField.setBackground(box(hexField, 0x5D17273A, 0x553B587C, 5));
		hexField.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence value, int start, int count, int after) { }
			@Override public void onTextChanged(CharSequence value, int start, int before, int count) {
				int parsed = parseHexColor(value.toString());
				if (parsed != -1) {
					KairokkCustomizationState.setColor(slot, parsed);
					syncCustomizationPalette();
					swatch.setBackground(colorSwatch(swatch, parsed));
				}
			}
			@Override public void afterTextChanged(Editable value) { }
		});
		// Keep the editor in place while the user types. Rebuilding here used to
		// steal focus and make multi-character colour edits nearly impossible.
		LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(0, px(context, 27), 1);
		fieldParams.leftMargin = px(context, 6);
		row.addView(hexField, fieldParams);
		ShulkIconView picker = new ShulkIconView(context, ShulkIcon.PALETTE, BLUE_LIGHT, "Pick " + label + " colour");
		picker.setClickable(true);
		picker.setPadding(px(context, 7), px(context, 7), px(context, 7), px(context, 7));
		KairokkUiSounds.attach(picker);
		picker.setOnClickListener(view -> showCustomizationColorPicker(context, swatch, slot));
		row.addView(picker, new LinearLayout.LayoutParams(px(context, 29), px(context, 29)));
		return row;
	}

	private int customizationColor(String slot) {
		return switch (slot) {
			case "secondary" -> KairokkCustomizationState.secondary();
			case "accent" -> KairokkCustomizationState.accent();
			case "danger" -> KairokkCustomizationState.danger();
			default -> KairokkCustomizationState.primary();
		};
	}

	private LinearLayout uiElementsCard(Context context) {
		LinearLayout card = miscCard(context, ShulkIcon.LAYOUT, "UI ELEMENTS", "Adjust the look and feel of interface elements.");
		card.addView(customizationSlider(context, "UI Roundness", KairokkCustomizationState.roundness(), 0, 16, "px", KairokkCustomizationState::setRoundness), miscRowParams(context));
		card.addView(customizationSlider(context, "Border Thickness", KairokkCustomizationState.borderThickness(), 0, 3, "px", KairokkCustomizationState::setBorderThickness), miscRowParams(context));
		card.addView(customizationSlider(context, "Module Spacing", KairokkCustomizationState.moduleSpacing(), 0, 16, "px", KairokkCustomizationState::setModuleSpacing), miscRowParams(context));
		card.addView(customizationSlider(context, "UI Scale", KairokkCustomizationState.uiScale(), 75, 125, "%", KairokkCustomizationState::setUiScale), miscRowParams(context));
		return card;
	}

	private LinearLayout animationsCard(Context context) {
		LinearLayout card = miscCard(context, ShulkIcon.RESET, "ANIMATIONS", "Configure client animations and transitions.");
		card.addView(customizationToggleRow(context, "Menu Animations", "Fade pages and panels.", "menuAnimations"), miscRowParams(context));
		card.addView(customizationToggleRow(context, "Module Animations", "Animate module changes.", "moduleAnimations"), miscRowParams(context));
		card.addView(customizationSlider(context, "Animation Speed", KairokkCustomizationState.animationSpeed(), 50, 150, "%", KairokkCustomizationState::setAnimationSpeed), miscRowParams(context));
		card.addView(customizationToggleRow(context, "Smooth Scrolling", "Ease list movement.", "smoothScrolling"), miscRowParams(context));
		return card;
	}

	private LinearLayout backgroundCard(Context context) {
		LinearLayout card = miscCard(context, ShulkIcon.GLOBE, "BACKGROUND", "Customize backgrounds and visual effects.");
		card.addView(customizationChoice(context, "Background Style", KairokkCustomizationState.backgroundStyle(), new String[]{"Blurred", "Solid", "Ambient"}, KairokkCustomizationState::setBackgroundStyle), miscRowParams(context));
		card.addView(customizationSlider(context, "Background Opacity", KairokkCustomizationState.backgroundOpacity(), 0, 100, "%", KairokkCustomizationState::setBackgroundOpacity), miscRowParams(context));
		card.addView(customizationToggleRow(context, "Enable Background Blur", "Soften the game behind menus.", "backgroundBlur"), miscRowParams(context));
		card.addView(customizationToggleRow(context, "Animated Background", "Keep the ambient field alive.", "animatedBackground"), miscRowParams(context));
		card.addView(customizationToggleRow(context, "Particle Effects", "Show ambient blue particles.", "particleEffects"), miscRowParams(context));
		return card;
	}

	private LinearLayout miscOptionsCard(Context context) {
		LinearLayout card = miscCard(context, ShulkIcon.SETTINGS, "MISC OPTIONS", "Small preferences that shape the client.");
		card.addView(customizationToggleRow(context, "Show Client Logo", "Display Kairokk branding.", "showClientLogo"), miscRowParams(context));
		card.addView(customizationToggleRow(context, "Modern UI Elements", "Use the styled controls.", "modernUiElements"), miscRowParams(context));
		card.addView(customizationToggleRow(context, "Show Module Descriptions", "Keep helper text visible.", "showModuleDescriptions"), miscRowParams(context));
		card.addView(customizationToggleRow(context, "Compact Mode", "Reduce spacing in dense pages.", "compactMode"), miscRowParams(context));
		card.addView(customizationToggleRow(context, "Use Native Title Bar", "Keep the operating-system title bar.", "nativeTitleBar"), miscRowParams(context));
		return card;
	}

	private LinearLayout extraCard(Context context) {
		LinearLayout card = miscCard(context, ShulkIcon.CUSTOMIZE, "EXTRA", "Additional visual customizations.");
		LinearLayout cssRow = new LinearLayout(context);
		cssRow.setGravity(Gravity.CENTER_VERTICAL);
		TextView cssLabel = text(context, "Custom CSS", 10, TEXT);
		cssRow.addView(cssLabel, new LinearLayout.LayoutParams(0, px(context, 32), 1));
		TextView edit = miscAction(context, "</>  Edit", false);
		edit.setOnClickListener(view -> showCustomCssEditor(context));
		cssRow.addView(edit, new LinearLayout.LayoutParams(px(context, 93), px(context, 30)));
		card.addView(cssRow, miscRowParams(context));
		card.addView(customizationToggleRow(context, "Interface Sounds", "Use Kairokk UI sound cues.", "interfaceSounds"), miscRowParams(context));
		card.addView(customizationToggleRow(context, "Colorize Icons", "Tint icons with the active palette.", "colorizeIcons"), miscRowParams(context));
		card.addView(customizationToggleRow(context, "Transparent Panels", "Let the game show through panels.", "transparentPanels"), miscRowParams(context));
		return card;
	}

	private LinearLayout miscCard(Context context, ShulkIcon cardIcon, String title, String subtitle) {
		LinearLayout card = new LinearLayout(context);
		card.setOrientation(LinearLayout.VERTICAL);
		card.setPadding(px(context, 11), px(context, 9), px(context, 11), px(context, 9));
		card.setBackground(box(card, KairokkCustomizationState.transparentPanels() ? 0x42152231 : 0xA3152231, 0x663B587C, KairokkCustomizationState.roundness()));
		LinearLayout header = new LinearLayout(context);
		header.setGravity(Gravity.CENTER_VERTICAL);
		ShulkIconView icon = new ShulkIconView(context, cardIcon == null ? ShulkIcon.INFO : cardIcon, BLUE_LIGHT, title);
		header.addView(icon, new LinearLayout.LayoutParams(px(context, 21), px(context, 21)));
		LinearLayout labels = new LinearLayout(context);
		labels.setOrientation(LinearLayout.VERTICAL);
		labels.setGravity(Gravity.CENTER_VERTICAL);
		TextView heading = text(context, title, 11, TEXT);
		heading.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		TextView detail = text(context, subtitle, 8, BLUE_LIGHT);
		detail.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		labels.addView(heading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 17)));
		labels.addView(detail, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 14)));
		LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, px(context, 34), 1);
		labelParams.leftMargin = px(context, 6);
		header.addView(labels, labelParams);
		card.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 36)));
		return card;
	}

	private TextView miscAction(Context context, String label, boolean primary) {
		TextView action = text(context, label, 10, primary ? TEXT : MUTED);
		action.setGravity(Gravity.CENTER);
		action.setClickable(true);
		action.setBackground(box(action, primary ? 0x86316AC0 : 0x55152231, primary ? BLUE : 0x55415B7C, 6));
		KairokkUiSounds.attach(action);
		return action;
	}

	private View customizationChoice(Context context, String label, String current, String[] choices, java.util.function.Consumer<String> setter) {
		LinearLayout row = new LinearLayout(context);
		row.setGravity(Gravity.CENTER_VERTICAL);
		TextView name = text(context, label, 10, TEXT);
		name.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		row.addView(name, new LinearLayout.LayoutParams(0, px(context, 32), 1));
		LinearLayout choice = new LinearLayout(context);
		choice.setGravity(Gravity.CENTER_VERTICAL);
		choice.setPadding(px(context, 8), 0, px(context, 5), 0);
		choice.setBackground(box(choice, 0x5D223147, 0x885377A4, 5));
		TextView value = text(context, current, 9, TEXT);
		value.setGravity(Gravity.CENTER_VERTICAL);
		choice.addView(value, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
		ShulkIconView arrow = new ShulkIconView(context, ShulkIcon.EXPAND, BLUE_LIGHT, "Choose " + label);
		arrow.setPadding(px(context, 2), px(context, 8), px(context, 2), px(context, 8));
		choice.addView(arrow, new LinearLayout.LayoutParams(px(context, 17), ViewGroup.LayoutParams.MATCH_PARENT));
		choice.setClickable(true);
		KairokkUiSounds.attach(choice);
		choice.setOnClickListener(view -> showCustomizationMenu(context, choice, value, arrow, choices, setter));
		row.addView(choice, new LinearLayout.LayoutParams(px(context, 116), px(context, 31)));
		KairokkUiSounds.attachSelection(row);
		return row;
	}

	private void showCustomizationMenu(Context context, LinearLayout anchor, TextView value, ShulkIconView arrow, String[] choices, java.util.function.Consumer<String> setter) {
		KairokkUiSounds.play(KairokkUiSounds.OPEN);
		LinearLayout menu = new LinearLayout(context);
		menu.setOrientation(LinearLayout.VERTICAL);
		menu.setPadding(px(context, 5), px(context, 5), px(context, 5), px(context, 5));
		menu.setBackground(box(menu, 0xF7223044, 0xFF5882BC, 7));
		PopupWindow popup = new PopupWindow(menu, px(context, 150), px(context, choices.length * 31 + 10), true);
		popup.setOutsideTouchable(true);
		popup.setElevation(px(context, 8));
		anchor.setBackground(box(anchor, 0xFF2A4568, BLUE, 5));
		ValueAnimator openArrow = ValueAnimator.ofFloat(arrow.getRotation(), 180F);
		openArrow.setDuration(180);
		openArrow.addUpdateListener(frame -> arrow.setRotation(((Number) frame.getAnimatedValue()).floatValue()));
		openArrow.start();
		popup.setOnDismissListener(() -> {
			anchor.setBackground(box(anchor, 0x5D223147, 0x885377A4, 5));
			ValueAnimator closeArrow = ValueAnimator.ofFloat(arrow.getRotation(), 0F);
			closeArrow.setDuration(160);
			closeArrow.addUpdateListener(frame -> arrow.setRotation(((Number) frame.getAnimatedValue()).floatValue()));
			closeArrow.start();
		});
		for (String option : choices) {
			TextView item = text(context, option, 10, option.equals(value.getText().toString()) ? BLUE_LIGHT : TEXT);
			item.setGravity(Gravity.CENTER_VERTICAL);
			item.setPadding(px(context, 9), 0, 0, 0);
			item.setBackground(box(item, option.equals(value.getText().toString()) ? 0x704A94FF : 0x00223044, option.equals(value.getText().toString()) ? 0x774A94FF : 0x00000000, 5));
			item.setClickable(true);
			KairokkUiSounds.attach(item);
			item.setOnClickListener(view -> { setter.accept(option); value.setText(option); popup.dismiss(); });
			menu.addView(item, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 31)));
		}
		menu.setAlpha(0F);
		menu.setScaleY(.88F);
		popup.showAsDropDown(anchor, 0, px(context, 4));
		ValueAnimator open = ValueAnimator.ofFloat(0F, 1F);
		open.setDuration(180);
		open.addUpdateListener(frame -> { float progress = ((Number) frame.getAnimatedValue()).floatValue(); menu.setAlpha(progress); menu.setScaleY(.88F + .12F * progress); });
		open.start();
	}

	private View customizationToggleRow(Context context, String label, String detail, String key) {
		LinearLayout row = new LinearLayout(context);
		row.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout labels = new LinearLayout(context);
		labels.setOrientation(LinearLayout.VERTICAL);
		labels.setGravity(Gravity.CENTER_VERTICAL);
		TextView name = text(context, label, 10, TEXT);
		TextView explanation = text(context, detail, 8, MUTED);
		explanation.setVisibility(KairokkCustomizationState.showModuleDescriptions() ? View.VISIBLE : View.GONE);
		labels.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 17)));
		labels.addView(explanation, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 14)));
		row.addView(labels, new LinearLayout.LayoutParams(0, px(context, 34), 1));
		FrameLayout toggle = animatedToggle(context, customizationBoolean(key), enabled -> {
			KairokkCustomizationState.setBoolean(key, enabled);
			if ("showModuleDescriptions".equals(key) || "compactMode".equals(key)
					|| "transparentPanels".equals(key)) refreshCustomization(context);
		});
		row.addView(toggle, new LinearLayout.LayoutParams(px(context, 48), px(context, 24)));
		return row;
	}

	private boolean customizationBoolean(String key) {
		return switch (key) {
			case "menuAnimations" -> KairokkCustomizationState.menuAnimations();
			case "moduleAnimations" -> KairokkCustomizationState.moduleAnimations();
			case "smoothScrolling" -> KairokkCustomizationState.smoothScrolling();
			case "backgroundBlur" -> KairokkCustomizationState.backgroundBlur();
			case "animatedBackground" -> KairokkCustomizationState.animatedBackground();
			case "particleEffects" -> KairokkCustomizationState.particleEffects();
			case "showClientLogo" -> KairokkCustomizationState.showClientLogo();
			case "modernUiElements" -> KairokkCustomizationState.modernUiElements();
			case "showModuleDescriptions" -> KairokkCustomizationState.showModuleDescriptions();
			case "compactMode" -> KairokkCustomizationState.compactMode();
			case "nativeTitleBar" -> KairokkCustomizationState.nativeTitleBar();
			case "interfaceSounds" -> KairokkCustomizationState.interfaceSounds();
			case "colorizeIcons" -> KairokkCustomizationState.colorizeIcons();
			case "transparentPanels" -> KairokkCustomizationState.transparentPanels();
			default -> false;
		};
	}

	private View customizationSlider(Context context, String label, int initial, int min, int max, String suffix,
			java.util.function.IntConsumer setter) {
		LinearLayout slider = new LinearLayout(context);
		slider.setOrientation(LinearLayout.VERTICAL);
		LinearLayout titleRow = new LinearLayout(context);
		titleRow.setGravity(Gravity.CENTER_VERTICAL);
		TextView name = text(context, label, 10, TEXT);
		name.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		TextView amount = text(context, initial + suffix, 9, BLUE_LIGHT);
		amount.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		titleRow.addView(name, new LinearLayout.LayoutParams(0, px(context, 18), 1));
		titleRow.addView(amount, new LinearLayout.LayoutParams(px(context, 48), px(context, 18)));
		slider.addView(titleRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 19)));
		FrameLayout track = new FrameLayout(context);
		track.setBackground(pill(track, 0xFF20344C));
		// Keep the thumb fully inside the track at both extrema.
		int pad = px(context, 6);
		float[] fraction = { (initial - min) / (float) Math.max(1, max - min) };
		View fill = new View(context);
		fill.setBackground(pill(fill, BLUE));
		paletteBlueViews.add(fill);
		FrameLayout.LayoutParams fillParams = new FrameLayout.LayoutParams(0, px(context, 5), Gravity.LEFT | Gravity.CENTER_VERTICAL);
		fillParams.leftMargin = pad;
		track.addView(fill, fillParams);
		View knob = new View(context);
		knob.setBackground(pill(knob, BLUE_LIGHT));
		paletteLightViews.add(knob);
		FrameLayout.LayoutParams knobParams = new FrameLayout.LayoutParams(px(context, 12), px(context, 12), Gravity.LEFT | Gravity.CENTER_VERTICAL);
		track.addView(knob, knobParams);
		Runnable update = () -> {
			int inner = Math.max(1, track.getWidth() - pad * 2);
			fillParams.leftMargin = pad;
			fillParams.width = Math.round(inner * fraction[0]);
			fill.setLayoutParams(fillParams);
			knobParams.leftMargin = pad + Math.round(inner * fraction[0]) - px(context, 6);
			knob.setLayoutParams(knobParams);
		};
		track.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> update.run());
		track.setOnTouchListener((view, event) -> {
			int action = event.getActionMasked();
			if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
				if (action == MotionEvent.ACTION_DOWN) KairokkUiSounds.play(KairokkUiSounds.SELECT);
				float inner = Math.max(1F, view.getWidth() - pad * 2F);
				fraction[0] = clamp((event.getX() - pad) / inner);
				int value = Math.round(min + fraction[0] * (max - min));
				setter.accept(value);
				amount.setText(value + suffix);
				update.run();
			}
			return true;
		});
		slider.addView(track, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 13)));
		return slider;
	}

	private void showCustomizationColorPicker(Context context, View anchor, String slot) {
		KairokkUiSounds.play(KairokkUiSounds.OPEN);
		int originalColor = customizationColor(slot);
		String originalTheme = KairokkCustomizationState.theme();
		float[] initial = hsv(originalColor);
		FrameLayout card = new FrameLayout(context);
		card.setPadding(px(context, 13), px(context, 11), px(context, 13), px(context, 11));
		card.setBackground(box(card, 0xF51A2738, 0xFF5277A6, 9));
		LinearLayout content = new LinearLayout(context);
		content.setOrientation(LinearLayout.VERTICAL);
		card.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		TextView title = text(context, "C O L O U R   P I C K E R", 12, TEXT);
		title.setGravity(Gravity.CENTER_VERTICAL);
		content.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 25)));
		HueStrip hueStrip = new HueStrip(context, initial[0]);
		LinearLayout.LayoutParams hueParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 16));
		hueParams.topMargin = px(context, 8);
		content.addView(hueStrip, hueParams);
		ColorField field = new ColorField(context, initial[0], initial[1], initial[2]);
		LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 118));
		fieldParams.topMargin = px(context, 8);
		content.addView(field, fieldParams);
		final PopupWindow[] popupRef = {null};
		final boolean[] committed = {false};
		java.util.function.IntConsumer apply = color -> {
			KairokkCustomizationState.setColor(slot, color);
			syncCustomizationPalette();
			anchor.setBackground(colorSwatch(anchor, color));
		};
		Runnable restore = () -> {
			KairokkCustomizationState.setColor(slot, originalColor);
			KairokkCustomizationState.setThemeName(originalTheme);
			syncCustomizationPalette();
			field.setColor(originalColor);
			hueStrip.setHue(field.getHue());
			anchor.setBackground(colorSwatch(anchor, originalColor));
		};
		PickerActions actions = new PickerActions(context,
			restore,
			() -> {
				committed[0] = true;
				apply.accept(field.getColor());
				if (popupRef[0] != null) popupRef[0].dismiss();
				Core.getUiHandler().post(() -> refreshCustomization(context));
			});
		LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 31));
		actionParams.topMargin = px(context, 10);
		content.addView(actions, actionParams);
		PopupWindow popup = new PopupWindow(card, px(context, 300), px(context, 238), true);
		popupRef[0] = popup;
		popup.setOutsideTouchable(true);
		field.setOnColorChanged(apply);
		hueStrip.setOnHueChanged(value -> { field.setHue(value); apply.accept(field.getColor()); });
		popup.setOnDismissListener(() -> { if (!committed[0]) restore.run(); });
		card.setAlpha(0F);
		popup.showAtLocation(anchor, Gravity.CENTER, 0, 0);
		ValueAnimator open = ValueAnimator.ofFloat(0F, 1F);
		open.setDuration(190);
		open.addUpdateListener(frame -> card.setAlpha(((Number) frame.getAnimatedValue()).floatValue()));
		open.start();
	}

	private void showCustomCssEditor(Context context) {
		KairokkUiSounds.play(KairokkUiSounds.OPEN);
		FrameLayout card = new FrameLayout(context);
		card.setPadding(px(context, 14), px(context, 12), px(context, 14), px(context, 12));
		card.setBackground(box(card, 0xF51A2738, 0xFF5277A6, 9));
		LinearLayout content = new LinearLayout(context);
		content.setOrientation(LinearLayout.VERTICAL);
		card.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		TextView heading = text(context, "C U S T O M   C S S", 13, TEXT);
		heading.setGravity(Gravity.CENTER_VERTICAL);
		content.addView(heading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 27)));
		EditText editor = new EditText(context);
		editor.setText(KairokkCustomizationState.customCss());
		editor.setHint("--panel: #152231; --text: #F0F4FF; --accent: #4A94FF; --radius: 8px;");
		editor.setSingleLine(false);
		editor.setTextSize(textSize(context, 11));
		editor.setTextColor(TEXT);
		editor.setHintTextColor(MUTED);
		editor.setGravity(Gravity.TOP | Gravity.LEFT);
		editor.setPadding(px(context, 9), px(context, 7), px(context, 9), px(context, 7));
		editor.setBackground(box(editor, 0xFF101A28, 0xFF405B80, 6));
		content.addView(editor, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 110)));
		LinearLayout actions = new LinearLayout(context);
		actions.setGravity(Gravity.CENTER_VERTICAL);
		TextView cancel = miscAction(context, "Cancel", false);
		TextView save = miscAction(context, "Save", true);
		actions.addView(cancel, new LinearLayout.LayoutParams(0, px(context, 31), 1));
		LinearLayout.LayoutParams saveParams = new LinearLayout.LayoutParams(0, px(context, 31), 1);
		saveParams.leftMargin = px(context, 7);
		actions.addView(save, saveParams);
		content.addView(actions, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 31)));
		PopupWindow popup = new PopupWindow(card, px(context, 340), px(context, 205), true);
		popup.setOutsideTouchable(true);
		cancel.setOnClickListener(view -> popup.dismiss());
		save.setOnClickListener(view -> { KairokkCustomizationState.setCustomCss(editor.getText().toString()); popup.dismiss(); refreshCustomization(context); });
		popup.showAtLocation(card, Gravity.CENTER, 0, 0);
	}

	private static LinearLayout.LayoutParams miscWeightParams(Context context, float weight, boolean withMargin) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, weight);
		if (withMargin) params.rightMargin = px(context, 7);
		return params;
	}

	private static LinearLayout.LayoutParams miscCardParams(Context context, int ignored) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		params.bottomMargin = px(context, KairokkCustomizationState.compactMode() ? 4 : 8);
		return params;
	}

	private static LinearLayout.LayoutParams miscRowParams(Context context) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 36));
		params.bottomMargin = px(context, KairokkCustomizationState.compactMode() ? 1 : 4);
		return params;
	}

	private static int parseHexColor(String value) {
		if (value == null) return -1;
		String clean = value.trim().replace("#", "");
		if (clean.length() != 6) return -1;
		try { return 0xFF000000 | Integer.parseInt(clean, 16); }
		catch (NumberFormatException ignored) { return -1; }
	}

	private View topBar(Context context) {
		LinearLayout bar = new LinearLayout(context);
		bar.setGravity(Gravity.CENTER_VERTICAL);
		bar.setPadding(px(context, 16), 0, px(context, 13), 0);
		bar.setBackground(box(bar, 0xB51A2636, 0x88445F85, 8));

		if (KairokkCustomizationState.showClientLogo()) {
			ShulkIconView brand = new ShulkIconView(context, ShulkIcon.BRAND, ACCENT, "Kairokk Client GUI");
			bar.addView(brand, new LinearLayout.LayoutParams(px(context, 31), px(context, 31)));
			TextView title = text(context, "C L I E N T", 16, TEXT);
			title.setGravity(Gravity.CENTER_VERTICAL);
			title.setPadding(px(context, 11), 0, px(context, 18), 0);
			bar.addView(title, new LinearLayout.LayoutParams(px(context, 176), ViewGroup.LayoutParams.MATCH_PARENT));
		} else {
			bar.addView(new View(context), new LinearLayout.LayoutParams(px(context, 18), 1));
		}

		String[] navigationTabs = {"VISUALS", "COMBAT", "PLAYER", "WORLD", "PATHFINDER", "MISC"};
		int tabWidth = px(context, 102);
		FrameLayout navigation = new FrameLayout(context);
		LinearLayout tabRow = new LinearLayout(context);
		tabRow.setGravity(Gravity.CENTER_VERTICAL);
		int selectedIndex = 0;
		for (int i = 0; i < navigationTabs.length; i++) {
			String tab = navigationTabs[i];
			boolean selected = tab.equals(selectedTab);
			if (selected) selectedIndex = i;
			TextView view = text(context, tab, 14, selected ? TEXT : MUTED);
			view.setGravity(Gravity.CENTER);
			view.setClickable(true);
			FrameLayout tabCell = new FrameLayout(context);
			tabCell.addView(view, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
			View hoverUnderline = new View(context);
			hoverUnderline.setBackground(pill(hoverUnderline, blendColor(BLUE, MUTED, .65f)));
			hoverUnderline.setAlpha(0f);
			FrameLayout.LayoutParams hoverParams = new FrameLayout.LayoutParams(tabWidth - px(context, 24), px(context, 3), Gravity.LEFT | Gravity.BOTTOM);
			hoverParams.leftMargin = px(context, 12); hoverParams.bottomMargin = px(context, 1);
			tabCell.addView(hoverUnderline, hoverParams);
			attachAnimatedTabHover(view, tab, hoverUnderline);
			int tabIndex = i;
			view.setOnClickListener(clicked -> {
				if (contentTransitionRunning || tab.equals(selectedTab)) return;
				contentTransitionRunning = true;
				int oldIndex = indexOf(navigationTabs, selectedTab);
				selectedTab = tab;
				for (TextView item : tabs) {
					boolean active = item.getText().toString().equals(selectedTab);
					item.setTextColor(customTextColor(active ? TEXT : MUTED));
				}
				FrameLayout.LayoutParams indicatorLayout = (FrameLayout.LayoutParams) navigation.getChildAt(1).getLayoutParams();
				int inset = px(context, 12);
				int start = Math.max(0, oldIndex) * tabWidth + inset;
				int end = tabIndex * tabWidth + inset;
				ValueAnimator slide = ValueAnimator.ofFloat(start, end);
				slide.setDuration(KairokkCustomizationState.menuAnimations() ? 230 : 1);
				slide.setInterpolator(new BezierInterpolator(.22f, .8f, .24f, 1f));
				slide.addUpdateListener(frame -> {
					indicatorLayout.leftMargin = Math.round(((Number) frame.getAnimatedValue()).floatValue());
					navigation.getChildAt(1).setLayoutParams(indicatorLayout);
				});
				slide.addListener(new AnimatorListener() {
					@Override public void onAnimationEnd(Animator animator) { showTopTab(context); }
				});
				slide.start();
			});
			tabs.add(view);
			tabRow.addView(tabCell, new LinearLayout.LayoutParams(tabWidth, ViewGroup.LayoutParams.MATCH_PARENT));
		}
		navigation.addView(tabRow, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		View underline = new View(context);
		underline.setBackground(pill(underline, BLUE));
		paletteBlueViews.add(underline);
		FrameLayout.LayoutParams underlineParams = new FrameLayout.LayoutParams(tabWidth - px(context, 24), px(context, 3), Gravity.LEFT | Gravity.BOTTOM);
		underlineParams.leftMargin = selectedIndex * tabWidth + px(context, 12);
		underlineParams.bottomMargin = px(context, 1);
		navigation.addView(underline, underlineParams);
		bar.addView(navigation, new LinearLayout.LayoutParams(tabWidth * navigationTabs.length, ViewGroup.LayoutParams.MATCH_PARENT));

		bar.addView(new View(context), new LinearLayout.LayoutParams(0, 1, 1));
		ShulkIconView search = icon(context, ShulkIcon.SEARCH, "Search");
		search.setClickable(true);
		attachAnimatedToolHover(search, BLUE_LIGHT);
		search.setOnClickListener(view -> showNotice(context, search, "Search is coming soon."));
		bar.addView(search, new LinearLayout.LayoutParams(px(context, 39), ViewGroup.LayoutParams.MATCH_PARENT));
		ShulkIconView settings = icon(context, ShulkIcon.SETTINGS, "Reconnect Kairokk dashboard");
		settings.setClickable(true);
		attachAnimatedToolHover(settings, 0xFF8BB8FF);
		settings.setOnClickListener(view -> {
			if (KairokkClient.realtime() == null) { showNotice(context, settings, "Dashboard bridge is still starting."); return; }
			KairokkClient.realtime().reconnect();
			showNotice(context, settings, "Dashboard reconnect requested — check the connection status in a moment.");
		});
		bar.addView(settings, new LinearLayout.LayoutParams(px(context, 39), ViewGroup.LayoutParams.MATCH_PARENT));
		ShulkIconView close = icon(context, ShulkIcon.CLOSE, "Close client GUI");
		close.setClickable(true);
		attachAnimatedToolHover(close, 0xFFFF7188);
		close.setOnClickListener(view -> Core.executeOnMainThread(() -> Minecraft.getInstance().setScreen(parent)));
		bar.addView(close, new LinearLayout.LayoutParams(px(context, 39), ViewGroup.LayoutParams.MATCH_PARENT));
		return bar;
	}

	private static int indexOf(String[] values, String value) {
		for (int i = 0; i < values.length; i++) if (values[i].equals(value)) return i;
		return 0;
	}

	private void attachAnimatedTabHover(TextView view, String tab, View hoverUnderline) {
		int[] displayedColor = {customTextColor(tab.equals(selectedTab) ? TEXT : MUTED)};
		ValueAnimator[] running = new ValueAnimator[1];
		KairokkUiSounds.attachSelection(view);
		view.setOnHoverListener((target, event) -> {
			int action = event.getActionMasked();
			if (action != MotionEvent.ACTION_HOVER_ENTER && action != MotionEvent.ACTION_HOVER_EXIT) return false;
			boolean entered = action == MotionEvent.ACTION_HOVER_ENTER;
			if (entered) KairokkUiSounds.play(KairokkUiSounds.HOVER);
			if (running[0] != null) running[0].cancel();
			int fromColor = displayedColor[0];
			int toColor = customTextColor(entered ? BLUE_LIGHT : (tab.equals(selectedTab) ? TEXT : MUTED));
			float fromScale = view.getScaleX();
			float toScale = entered ? 1.045f : 1f;
			float fromAlpha = hoverUnderline.getAlpha(), toAlpha = entered ? 1f : 0f;
			ValueAnimator animation = ValueAnimator.ofFloat(0f, 1f);
			animation.setDuration(entered ? 145 : 180);
			animation.setInterpolator(new BezierInterpolator(.2f, .75f, .25f, 1f));
			animation.addUpdateListener(frame -> {
				float progress = ((Number) frame.getAnimatedValue()).floatValue();
				displayedColor[0] = blendColor(fromColor, toColor, progress);
				view.setTextColor(displayedColor[0]);
				float scale = fromScale + (toScale - fromScale) * progress;
				view.setScaleX(scale); view.setScaleY(scale);
				hoverUnderline.setAlpha(fromAlpha + (toAlpha - fromAlpha) * progress);
			});
			running[0] = animation;
			animation.start();
			return false;
		});
	}

	private void attachAnimatedToolHover(ShulkIconView view, int hoverColor) {
		view.setPalette(MUTED, MUTED, MUTED, 0x667A737D);
		view.setBackground(topToolHoverBackground(view, hoverColor));
		view.setIconAlpha(.80f);
		int[] displayedColor = {MUTED};
		float[] displayedAlpha = {.80f};
		ValueAnimator[] running = new ValueAnimator[1];
		KairokkUiSounds.attachSelection(view);
		view.setOnHoverListener((target, event) -> {
			int action = event.getActionMasked();
			if (action != MotionEvent.ACTION_HOVER_ENTER && action != MotionEvent.ACTION_HOVER_EXIT) return false;
			boolean entered = action == MotionEvent.ACTION_HOVER_ENTER;
			if (entered) KairokkUiSounds.play(KairokkUiSounds.HOVER);
			if (running[0] != null) running[0].cancel();
			float fromScale = view.getScaleX(), toScale = entered ? 1.10f : 1f;
			float fromAlpha = displayedAlpha[0], toAlpha = entered ? 1f : .80f;
			int fromColor = displayedColor[0], toColor = entered ? hoverColor : MUTED;
			ValueAnimator animation = ValueAnimator.ofFloat(0f, 1f);
			animation.setDuration(entered ? 150 : 190);
			animation.setInterpolator(new BezierInterpolator(.2f, .75f, .25f, 1f));
			animation.addUpdateListener(frame -> {
				float progress = ((Number) frame.getAnimatedValue()).floatValue();
				float scale = fromScale + (toScale - fromScale) * progress;
				view.setScaleX(scale); view.setScaleY(scale);
				displayedAlpha[0] = fromAlpha + (toAlpha - fromAlpha) * progress;
				view.setIconAlpha(displayedAlpha[0]);
				displayedColor[0] = blendColor(fromColor, toColor, progress);
				view.setPalette(displayedColor[0], displayedColor[0], displayedColor[0], 0x667A737D);
			});
			running[0] = animation;
			animation.start();
			return false;
		});
	}

	private void showNotice(Context context, View anchor, String message) {
		TextView notice = text(context, message, 12, MUTED);
		notice.setGravity(Gravity.CENTER);
		notice.setBackground(box(notice, 0xF21A2738, 0xFF405B80, 6));
		PopupWindow popup = new PopupWindow(notice, px(context, 200), px(context, 42), true);
		popup.setOutsideTouchable(true);
		popup.showAsDropDown(anchor);
		KairokkUiSounds.play(KairokkUiSounds.OPEN);
	}

	private void showTopTab(Context context) {
		if (rootView == null || workspaceView == null) return;
		contentTransitionRunning = true;
		// Rebuild the root in one operation so a tab click can never leave a
		// second workspace attached behind the newly selected page.
		populateRoot(context);
		KairokkScreenTransitions.fadeContentIn(workspaceView);
		contentTransitionRunning = false;
	}

	private View visualsWorkspace(Context context) {
		LinearLayout workspace = new LinearLayout(context);
		workspace.setOrientation(LinearLayout.HORIZONTAL);
		workspace.setPadding(px(context, 10), px(context, 10), px(context, 10), px(context, 10));
		workspace.setBackground(box(workspace, 0xBD101A26, 0x77506A90, 9));

		LinearLayout sidebar = new LinearLayout(context);
		sidebar.setOrientation(LinearLayout.VERTICAL);
		sidebar.setPadding(px(context, 10), px(context, 10), px(context, 10), px(context, 10));
		sidebar.setBackground(box(sidebar, 0x6D152231, 0x553B587C, 7));
		TextView heading = text(context, "V I S U A L S", 17, TEXT);
		heading.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		sidebar.addView(heading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 29)));
		TextView subtitle = text(context, "SEE MORE. PLAY BETTER.", 9, BLUE_LIGHT);
		subtitle.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 24));
		subtitleParams.bottomMargin = px(context, 10);
		sidebar.addView(subtitle, subtitleParams);
		addVisualNavigation(sidebar, context, ShulkIcon.PLAYER, "Players", "Player ESP, chams, tracers.");
		addVisualNavigation(sidebar, context, ShulkIcon.TARGET, "Mobs", "Mob ESP and tracking.");
		addVisualNavigation(sidebar, context, ShulkIcon.RESOURCE_PACK, "Items", "Dropped items and entities.");
		addVisualNavigation(sidebar, context, ShulkIcon.GLOBE, "World", "Blocks, ores, containers.");
		addVisualNavigation(sidebar, context, ShulkIcon.LAYOUT, "Render", "Shaders, lighting, post FX.");
		addVisualNavigation(sidebar, context, ShulkIcon.OVERLAY, "Overlay", "UI elements and information.");
		LinearLayout.LayoutParams sidebarParams = new LinearLayout.LayoutParams(px(context, 195), ViewGroup.LayoutParams.MATCH_PARENT);
		sidebarParams.rightMargin = px(context, 12);
		workspace.addView(sidebar, sidebarParams);

		LinearLayout main = new LinearLayout(context);
		main.setOrientation(LinearLayout.VERTICAL);
		main.setPadding(px(context, 2), px(context, 2), px(context, 2), px(context, 2));
		visualPageTitle = text(context, selectedCategory.toUpperCase(java.util.Locale.ROOT), 15, TEXT);
		visualPageTitle.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		main.addView(visualPageTitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 29)));
		visualPageSubtitle = text(context, visualDescription(selectedCategory), 10, BLUE_LIGHT);
		visualPageSubtitle.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams mainSubtitleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 23));
		mainSubtitleParams.bottomMargin = px(context, 6);
		main.addView(visualPageSubtitle, mainSubtitleParams);

		visualColumns = new LinearLayout(context);
		visualColumns.setOrientation(LinearLayout.HORIZONTAL);
		visualColumns.setGravity(Gravity.CENTER_VERTICAL);
		showVisualCategory(context);
		main.addView(visualColumns, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
		workspace.addView(main, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
		return workspace;
	}

	private View pathfinderWorkspace(Context context) {
		LinearLayout workspace = new LinearLayout(context);
		workspace.setOrientation(LinearLayout.HORIZONTAL);
		workspace.setPadding(px(context, 10), px(context, 10), px(context, 10), px(context, 10));
		workspace.setBackground(box(workspace, 0xBD101A26, 0x77506A90, 9));

		LinearLayout sidebar = new LinearLayout(context);
		sidebar.setOrientation(LinearLayout.VERTICAL);
		sidebar.setPadding(px(context, 10), px(context, 10), px(context, 10), px(context, 10));
		sidebar.setBackground(box(sidebar, 0x6D152231, 0x553B587C, 7));
		TextView heading = text(context, "P A T H F I N D E R", 15, TEXT);
		heading.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		sidebar.addView(heading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 29)));
		TextView subtitle = text(context, "MOVEMENT INTELLIGENCE", 9, BLUE_LIGHT);
		subtitle.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 24));
		subtitleParams.bottomMargin = px(context, 10);
		sidebar.addView(subtitle, subtitleParams);
		addPathfinderNavigation(sidebar, context, ShulkIcon.COMPASS, "Pathfinder", "Routes, jumps and safety.");
		addPathfinderNavigation(sidebar, context, ShulkIcon.TARGET, "Rotations", "Human-like aim preview.");
		LinearLayout.LayoutParams sidebarParams = new LinearLayout.LayoutParams(px(context, 195), ViewGroup.LayoutParams.MATCH_PARENT);
		sidebarParams.rightMargin = px(context, 12);
		workspace.addView(sidebar, sidebarParams);

		LinearLayout main = new LinearLayout(context);
		main.setOrientation(LinearLayout.VERTICAL);
		main.setPadding(px(context, 2), px(context, 2), px(context, 2), px(context, 2));
		LinearLayout pageHeader = new LinearLayout(context);
		pageHeader.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout headingCopy = new LinearLayout(context);
		headingCopy.setOrientation(LinearLayout.VERTICAL);
		TextView title = text(context, selectedPathfinderPage.toUpperCase(java.util.Locale.ROOT), 15, TEXT);
		title.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		headingCopy.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 29)));
		TextView description = text(context, "Rotations".equals(selectedPathfinderPage)
				? "Tune and inspect the steering used while following a route."
				: "Use the player’s live speed and jump effects when creating safe routes.", 10, BLUE_LIGHT);
		description.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		headingCopy.addView(description, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 23)));
		pageHeader.addView(headingCopy, new LinearLayout.LayoutParams(0, px(context, 52), 1));
		if ("Rotations".equals(selectedPathfinderPage)) {
			View presetChoice = customizationChoice(context, "Preset", PathFinderSettings.rotationPreset,
					new String[]{"Legit", "Balanced", "Aggressive", "Minimal", "Custom"},
					value -> { PathFinderSettings.applyPreset(value); showTopTab(context); });
			LinearLayout.LayoutParams presetChoiceParams = new LinearLayout.LayoutParams(px(context, 160), px(context, 32));
			presetChoiceParams.rightMargin = px(context, 8);
			pageHeader.addView(presetChoice, presetChoiceParams);
			TextView reset = miscAction(context, "↻  Reset", false);
			reset.setOnClickListener(v -> { PathFinderSettings.resetRotations(); showTopTab(context); });
			pageHeader.addView(reset, new LinearLayout.LayoutParams(px(context, 86), px(context, 28)));
			TextView advanced = miscAction(context, PathFinderSettings.advancedMode ? "Advanced: ON" : "Advanced: OFF", PathFinderSettings.advancedMode);
			advanced.setOnClickListener(v -> { PathFinderSettings.advancedMode = !PathFinderSettings.advancedMode; showTopTab(context); });
			LinearLayout.LayoutParams advancedParams = new LinearLayout.LayoutParams(px(context, 112), px(context, 28));
			advancedParams.leftMargin = px(context, 7);
			pageHeader.addView(advanced, advancedParams);
		} else {
			View presetChoice = customizationChoice(context, "Preset", PathfinderOptions.preset,
					new String[]{"Safe", "Balanced", "Fast", "Minimal", "Custom"},
					value -> { PathfinderOptions.applyPreset(value); showTopTab(context); });
			LinearLayout.LayoutParams presetChoiceParams = new LinearLayout.LayoutParams(px(context, 160), px(context, 32));
			pageHeader.addView(presetChoice, presetChoiceParams);
		}
		LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 58));
		headerParams.bottomMargin = px(context, 2);
		main.addView(pageHeader, headerParams);
		main.addView("Rotations".equals(selectedPathfinderPage) ? rotationPage(context) : pathfinderPage(context),
				new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
		workspace.addView(main, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
		return workspace;
	}

	private void addPathfinderNavigation(LinearLayout sidebar, Context context, ShulkIcon icon, String label, String detail) {
		boolean selected = label.equals(selectedPathfinderPage);
		LinearLayout row = new LinearLayout(context);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding(px(context, 8), 0, px(context, 7), 0);
		row.setBackground(selected ? categoryBackground(row) : box(row, 0x00152231, 0x00000000, 6));
		row.addView(new ShulkIconView(context, icon, selected ? TEXT : BLUE_LIGHT, label), new LinearLayout.LayoutParams(px(context, 27), px(context, 38)));
		LinearLayout copy = new LinearLayout(context);
		copy.setOrientation(LinearLayout.VERTICAL);
		copy.setGravity(Gravity.CENTER_VERTICAL);
		TextView name = text(context, label, 11, selected ? TEXT : MUTED);
		TextView explanation = text(context, detail, 8, selected ? BLUE_LIGHT : MUTED);
		copy.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 17)));
		copy.addView(explanation, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 14)));
		LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
		copyParams.leftMargin = px(context, 7);
		row.addView(copy, copyParams);
		row.setClickable(true);
		attachSidebarHover(row, () -> label.equals(selectedPathfinderPage));
		row.setOnClickListener(view -> { if (!label.equals(selectedPathfinderPage)) { selectedPathfinderPage = label; showTopTab(context); } });
		KairokkUiSounds.attach(row);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 46));
		params.bottomMargin = px(context, 4);
		sidebar.addView(row, params);
	}

	private View pathfinderPage(Context context) {
		LinearLayout body = new LinearLayout(context); body.setOrientation(LinearLayout.VERTICAL);
		LinearLayout tabs = new LinearLayout(context); tabs.setBackground(box(tabs, 0x66101822, 0x553B587C, 6));
		for (String section : new String[]{"General", "Skyblock"}) {
			TextView tab = miscAction(context, section, section.equals(selectedPathfinderSection));
			tab.setOnClickListener(v -> { selectedPathfinderSection=section; showTopTab(context); });
			LinearLayout.LayoutParams tabItemParams = new LinearLayout.LayoutParams(px(context, 106), px(context, 32));
			if ("General".equals(section)) tabItemParams.rightMargin = px(context, 8);
			tabs.addView(tab, tabItemParams);
		}
		LinearLayout.LayoutParams tabParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 32)); tabParams.bottomMargin=px(context,8); body.addView(tabs,tabParams);
		LinearLayout columns=new LinearLayout(context); columns.setGravity(Gravity.TOP);
		LinearLayout assists=miscCard(context,ShulkIcon.COMPASS,"MOVEMENT ASSISTS","Automatic actions to keep you moving safely.");
		pathOption(assists,context,"Sprint Jump","Use safe sprint jumps on clear runs.",PathFinderSettings.sprintJump,v->PathFinderSettings.sprintJump=v);
		pathOption(assists,context,"Smart Jump","Plan jumps only when obstacles require them.",PathfinderOptions.smartJump,v->PathfinderOptions.smartJump=v);
		pathOption(assists,context,"Avoid Lava","Exclude lava and dangerous adjacent blocks.",PathfinderOptions.avoidLava,v->PathfinderOptions.avoidLava=v);
		pathUnavailable(assists,context,"Etherwarp","Requires a teleport movement executor.");
		pathUnavailable(assists,context,"AOTE Hops","Requires item ability and mana tracking.");
		pathOption(assists,context,"Safe Walk","Pause if the next walking step has no support.",PathfinderOptions.safeWalk,v->PathfinderOptions.safeWalk=v);
		pathSection(assists,context,"ROUTING","Route calculation and recovery.");
		pathOption(assists,context,"Follow on Calculate","Start walking after a requested route is ready.",PathfinderOptions.autoFollow,v->PathfinderOptions.autoFollow=v);
		pathOption(assists,context,"Recalculate on Stuck","Replan and resume when movement stops.",PathfinderOptions.recalculateOnStuck,v->PathfinderOptions.recalculateOnStuck=v);
		pathUnavailable(assists,context,"Break Through Webs","Block-breaking routes are not supported yet.");
		pathOption(assists,context,"Prefer Safer Routes","Penalise exposed edges during route planning.",PathfinderOptions.saferRoutes,v->PathfinderOptions.saferRoutes=v);
		pathOption(assists,context,"Avoid Water","Keep routes out of water blocks.",PathfinderOptions.avoidWater,v->PathfinderOptions.avoidWater=v);
		LinearLayout tuning=miscCard(context,ShulkIcon.TARGET,"TUNING","Fine tune how Pathfinder moves.");
		pathSlider(tuning,context,"Walk Pace","Throttle forward input; does not change server speed.",Math.round(PathfinderOptions.walkSpeed*100),25,100,v->v+"%",v->PathfinderOptions.walkSpeed=v/100f);
		pathSlider(tuning,context,"Jump Safety Margin","Subtract this distance from calculated jump reach.",Math.round(PathfinderOptions.jumpMargin*100),10,100,v->hundredths(v)+" b",v->PathfinderOptions.jumpMargin=v/100f);
		pathSlider(tuning,context,"Step Height","Maximum ledge treated as a step rather than a jump.",Math.round(PathfinderOptions.stepHeight*100),10,65,v->hundredths(v)+" b",v->PathfinderOptions.stepHeight=v/100f);
		pathSlider(tuning,context,"Max Fall Distance","Maximum drop considered when selecting a neighbour.",Math.round(PathfinderOptions.maxFall),1,5,v->v+" b",v->PathfinderOptions.maxFall=v);
		pathSlider(tuning,context,"Node Render Distance","Hide route geometry beyond this distance.",PathfinderOptions.renderDistance,16,128,v->v+" b",v->PathfinderOptions.renderDistance=v);
		pathSlider(tuning,context,"Path Update Rate","How often the rendered route snapshot refreshes.",PathfinderOptions.updateRate,1,20,v->v+" Hz",v->PathfinderOptions.updateRate=v);
		pathSection(tuning,context,"MOVEMENT MODE","Choose how conservatively to traverse a route.");
		tuning.addView(pathChoice(context,"Mode",PathfinderOptions.movementMode,new String[]{"Balanced","Careful","Fast"},v->PathfinderOptions.movementMode=v),miscRowParams(context));
		pathSection(tuning,context,"RENDER MODE","Choose which route details appear in-game.");
		tuning.addView(pathChoice(context,"Render",PathfinderOptions.renderMode,new String[]{"Full","Line Only","Nodes Only","Hidden"},v->PathfinderOptions.renderMode=v),miscRowParams(context));
		pathSection(tuning,context,"MISCELLANEOUS","SkyBlock-aware movement and screen behaviour.");
		pathOption(tuning,context,"Show in Third Person","Keep the route visible outside first person.",PathfinderOptions.thirdPerson,v->PathfinderOptions.thirdPerson=v);
		pathOption(tuning,context,"Pause While in GUI","Release movement inputs while a menu is open.",PathfinderOptions.pauseInGui,v->PathfinderOptions.pauseInGui=v);
		pathOption(tuning,context,"Clear Path on Disable","Remove route geometry when navigation is stopped.",PathfinderOptions.clearOnDisable,v->PathfinderOptions.clearOnDisable=v);
		LinearLayout visual=miscCard(context,ShulkIcon.PALETTE,"PATH VISUAL","Customise how the path looks in-game.");
		PathViewport preview=new PathViewport(context);
		LinearLayout.LayoutParams previewParams=new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,px(context,155)); previewParams.bottomMargin=px(context,8); visual.addView(preview,previewParams);
		visual.addView(pathColorRow(context,"Path Color",()->PathfinderOptions.pathColor,v->PathfinderOptions.pathColor=v),miscRowParams(context));
		visual.addView(pathColorRow(context,"Node Color",()->PathfinderOptions.nodeColor,v->PathfinderOptions.nodeColor=v),miscRowParams(context));
		visual.addView(pathColorRow(context,"Target Color",()->PathfinderOptions.targetColor,v->PathfinderOptions.targetColor=v),miscRowParams(context));
		visual.addView(pathColorRow(context,"Invalid Color",()->PathfinderOptions.invalidColor,v->PathfinderOptions.invalidColor=v),miscRowParams(context));
		visual.addView(pathChoice(context,"Line Style",PathfinderOptions.lineStyle,new String[]{"Solid","Dashed","Dotted"},v->PathfinderOptions.lineStyle=v),miscRowParams(context));
		pathSlider(visual,context,"Line Width","Thickness of the route line.",Math.round(PathfinderOptions.lineWidth*10),5,50,v->tenths(v),v->PathfinderOptions.lineWidth=v/10f);
		visual.addView(pathChoice(context,"Node Style",PathfinderOptions.nodeStyle,new String[]{"Cube","Diamond","None"},v->PathfinderOptions.nodeStyle=v),miscRowParams(context));
		pathSlider(visual,context,"Node Size","Scale of intermediate route nodes.",Math.round(PathfinderOptions.nodeSize*100),25,200,v->hundredths(v)+"x",v->PathfinderOptions.nodeSize=v/100f);
		pathOption(visual,context,"Glow","Add a soft second stroke to the route.",PathfinderOptions.glow,v->PathfinderOptions.glow=v);
		pathUnavailable(visual,context,"Depth Test","Fixed by the current world rendering pipeline.");
		pathOption(visual,context,"Fade with Distance","Reduce opacity near the render-distance limit.",PathfinderOptions.fade,v->PathfinderOptions.fade=v);
		if ("Skyblock".equals(selectedPathfinderSection)) {
			LinearLayout advanced=miscCard(context,ShulkIcon.SETTINGS,"SKYBLOCK & RECOVERY","Movement limits are calculated from your real attributes.");
			pathOption(advanced,context,"Scan Potion Effects","Use active Speed and Jump Boost in route planning.",PathfinderOptions.scanEffects,v->PathfinderOptions.scanEffects=v);
			pathOption(advanced,context,"Refresh on Effect Change","Replan an active route when movement effects change.",PathfinderOptions.refreshEffects,v->PathfinderOptions.refreshEffects=v);
			pathSlider(advanced,context,"Stuck Timeout","Time without movement before attempting recovery.",PathfinderOptions.stuckTimeout,2,10,v->v+" s",v->PathfinderOptions.stuckTimeout=v);
			TextView note=text(context,"Teleport abilities and block breaking are unavailable. This client never changes your speed or jump attributes.",10,MUTED); note.setGravity(Gravity.TOP); advanced.addView(note,new LinearLayout.LayoutParams(-1,px(context,65)));
			// Skyblock keeps the game-specific movement assists and recovery controls together.
			addPathColumn(columns,context,assists,false); addPathColumn(columns,context,advanced,true);
		} else {
			// General contains reusable route tuning and rendering controls.
			addPathColumn(columns,context,tuning,false); addPathColumn(columns,context,visual,true);
		}
		body.addView(columns,new LinearLayout.LayoutParams(-1,0,1));
		return body;
	}

	private void addPathColumn(LinearLayout body,Context context,LinearLayout card,boolean gap) {
		ScrollView scroll=new ScrollView(context); scroll.setFillViewport(true);
		scroll.addView(card,new ScrollView.LayoutParams(-1,-2));
		LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(0,-1,1); if(gap)params.leftMargin=px(context,8); body.addView(scroll,params);
	}
	private View pathChoice(Context context,String label,String current,String[] choices,java.util.function.Consumer<String> setter){return customizationChoice(context,label,current,choices,v->{setter.accept(v);PathfinderOptions.save();});}
	private void pathSection(LinearLayout card,Context context,String title,String detail) {
		LinearLayout copy=new LinearLayout(context); copy.setOrientation(LinearLayout.VERTICAL); copy.setPadding(0,px(context,5),0,px(context,2));
		copy.addView(text(context,title,11,TEXT)); copy.addView(text(context,detail,8,BLUE_LIGHT));
		card.addView(copy,new LinearLayout.LayoutParams(-1,px(context,30)));
	}
	private void pathOption(LinearLayout card,Context context,String title,String detail,boolean value,java.util.function.Consumer<Boolean> setter) {
		card.addView(pathfinderToggle(context,title,detail,value,v->{setter.accept(v);PathfinderOptions.save();}),new LinearLayout.LayoutParams(-1,px(context,32)));
	}
	private void pathUnavailable(LinearLayout card,Context context,String title,String detail) {
		View row=pathfinderToggle(context,title+" · Unavailable",detail,false,v->{}); row.setEnabled(false); row.setAlpha(.45f); ((LinearLayout)row).getChildAt(1).setEnabled(false); card.addView(row,new LinearLayout.LayoutParams(-1,px(context,32)));
	}
	private void pathSlider(LinearLayout card,Context context,String title,String detail,int value,int min,int max,java.util.function.IntFunction<String> formatter,java.util.function.IntConsumer setter) {
		LinearLayout row=(LinearLayout)itemSlider(context,title,value,min,max,v->formatter.apply(v),v->{setter.accept(v);PathfinderOptions.save();});
		TextView description=text(context,detail,7,MUTED); description.setGravity(Gravity.LEFT|Gravity.CENTER_VERTICAL); row.addView(description,1,new LinearLayout.LayoutParams(-1,px(context,12)));
		card.addView(row,new LinearLayout.LayoutParams(-1,px(context,42)));
	}
	private View pathColorRow(Context context,String title,java.util.function.IntSupplier getter,java.util.function.IntConsumer setter) {
		LinearLayout row=new LinearLayout(context); row.setGravity(Gravity.CENTER_VERTICAL);
		row.addView(text(context,title,10,TEXT),new LinearLayout.LayoutParams(0,-1,1));
		View swatch=new View(context); swatch.setBackground(colorSwatch(swatch,getter.getAsInt())); row.addView(swatch,new LinearLayout.LayoutParams(px(context,20),px(context,20)));
		EditText field=new EditText(context); field.setSingleLine(true); field.setText(hex(getter.getAsInt())); field.setTextSize(textSize(context,9)); field.setTextColor(BLUE_LIGHT); field.setBackground(box(field,0x5517273A,0x553B587C,4)); field.setPadding(px(context,6),0,0,0);
		field.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int b,int c){} public void afterTextChanged(Editable s){} public void onTextChanged(CharSequence s,int a,int b,int c){int color=parseHexColor(s.toString());if(color!=-1){setter.accept(color);swatch.setBackground(colorSwatch(swatch,color));PathfinderOptions.save();}}});
		swatch.setClickable(true);swatch.setOnClickListener(v->showPathColorPicker(context,swatch,field,getter.getAsInt()));
		LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(px(context,94),px(context,26));p.leftMargin=px(context,7);row.addView(field,p);return row;
	}
	private void showPathColorPicker(Context context,View anchor,EditText hexField,int original){
		LinearLayout card=new LinearLayout(context);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(px(context,12),px(context,12),px(context,12),px(context,12));card.setBackground(box(card,0xFF1B293A,0xFF5882BC,8));
		card.addView(text(context,"PATH COLOUR",12,TEXT),new LinearLayout.LayoutParams(-1,px(context,25)));
		float[] initial=hsv(original);ColorField field=new ColorField(context,initial[0],initial[1],initial[2]);HueStrip hue=new HueStrip(context,initial[0]);
		card.addView(field,new LinearLayout.LayoutParams(-1,px(context,150)));
		LinearLayout.LayoutParams hueParams=new LinearLayout.LayoutParams(-1,px(context,22));hueParams.topMargin=px(context,12);
		card.addView(hue,hueParams);
		PopupWindow popup=new PopupWindow(card,px(context,320),px(context,285),true);popup.setOutsideTouchable(true);
		field.setOnColorChanged(color->hexField.setText(hex(color)));hue.setOnHueChanged(value->{field.setHue(value);hexField.setText(hex(field.getColor()));});
		LinearLayout.LayoutParams actionParams=new LinearLayout.LayoutParams(-1,px(context,36));actionParams.topMargin=px(context,14);
		card.addView(new PickerActions(context,()->{field.setColor(original);hue.setHue(field.getHue());hexField.setText(hex(original));},popup::dismiss),actionParams);
		popup.showAtLocation(anchor,Gravity.CENTER,0,0);
	}

	private View rotationPage(Context context) {
		LinearLayout body = new LinearLayout(context);
		body.setOrientation(LinearLayout.VERTICAL);

		// Match the reference hierarchy: three primary columns, with Safety stacked under Response.
		LinearLayout cards = new LinearLayout(context);
		cards.setGravity(Gravity.TOP);
		LinearLayout angle = miscCard(context, ShulkIcon.TARGET, "ANGLE & TIMING", "Control how far and how quickly the camera rotates.");
		if (PathFinderSettings.advancedMode) {
			angle.addView(rotationRangeSlider(context, "Angle", Math.round(PathFinderSettings.minAngle * 10), Math.round(PathFinderSettings.maxAngle * 10), 0, 1800,
					v -> tenths(v) + "°", v -> PathFinderSettings.minAngle = v / 10F, v -> PathFinderSettings.maxAngle = v / 10F), rotationRowParams(context));
			angle.addView(rotationRangeSlider(context, "Time", Math.round(PathFinderSettings.minTime * 100), Math.round(PathFinderSettings.maxTime * 100), 5, 300,
					v -> hundredths(v) + "s", v -> PathFinderSettings.minTime = v / 100F, v -> PathFinderSettings.maxTime = v / 100F), rotationRowParams(context));
			angle.addView(rotationRangeSlider(context, "Rotation Speed", Math.round(PathFinderSettings.minSpeed), Math.round(PathFinderSettings.maxSpeed), 0, 720,
					v -> v + "°/s", v -> PathFinderSettings.minSpeed = v, v -> PathFinderSettings.maxSpeed = v), rotationRowParams(context));
		} else {
			angle.addView(rotationSlider(context, "Rotation Speed", Math.round(PathFinderSettings.maxSpeed), 80, 720, v -> v + "°/s", v -> PathFinderSettings.maxSpeed = v), rotationRowParams(context));
		}
		angle.addView(rotationToggle(context, "Smooth Movement", "Interpolate rotations for natural movement.", PathFinderSettings.smooth > 0.01f,
				value -> { PathFinderSettings.smooth = value ? 0.72f : 0f; PathFinderSettings.rotationPreset = "Custom"; }), rotationRowParams(context));

		LinearLayout movement = miscCard(context, ShulkIcon.GAUGE, "MOVEMENT VARIATION", "Adds natural randomness to your rotations.");
		movement.addView(rotationSlider(context, "Humanization", Math.round(PathFinderSettings.humanization * 100), 0, 100, v -> v + "%", v -> PathFinderSettings.humanization = v / 100F), rotationRowParams(context));
		movement.addView(rotationSlider(context, "Jitter Amount", Math.round(PathFinderSettings.jitterAmplitude * 100), 0, 150, v -> hundredths(v) + "°", v -> PathFinderSettings.jitterAmplitude = v / 100F), rotationRowParams(context));
		movement.addView(rotationSlider(context, "Arc", Math.round(PathFinderSettings.arc * 10), -150, 150, v -> tenths(v) + "°", v -> PathFinderSettings.arc = v / 10F), rotationRowParams(context));
		if (PathFinderSettings.advancedMode) {
			movement.addView(rotationSlider(context, "Step Jitter", Math.round(PathFinderSettings.stepJitter * 100), 0, 100, v -> hundredths(v) + "°", v -> PathFinderSettings.stepJitter = v / 100F), rotationRowParams(context));
			movement.addView(rotationSlider(context, "Mouse Room", Math.round(PathFinderSettings.mouseRoom), 8, 180, v -> v + "°", v -> PathFinderSettings.mouseRoom = v), rotationRowParams(context));
		}

		LinearLayout response = miscCard(context, ShulkIcon.STATUS, "RESPONSE & TARGETING", "Choose how closely the camera must settle on its goal.");
		response.addView(rotationSlider(context, "Aim Tolerance", Math.round(PathFinderSettings.deadzone * 100), 0, 300, v -> hundredths(v) + "°", v -> PathFinderSettings.deadzone = v / 100F), rotationRowParams(context));
		response.addView(rotationSlider(context, "Response Curve", Math.round(PathFinderSettings.curveIntensity * 100), 0, 100, v -> v + "%", v -> PathFinderSettings.curveIntensity = v / 100F), rotationRowParams(context));
		if (PathFinderSettings.advancedMode) {
			response.addView(rotationToggle(context, "No Curve Mode", "Disable response curve shaping.", PathFinderSettings.noCurve,
					value -> { PathFinderSettings.noCurve = value; PathFinderSettings.rotationPreset = "Custom"; }), rotationRowParams(context));
		}

		LinearLayout safety = miscCard(context, ShulkIcon.SETTINGS, "SAFETY", "Keep rotations controlled and predictable.");
		safety.addView(rotationToggle(context, "Limit Vertical Rotations", "Prevent unnatural pitch snaps.", PathFinderSettings.limitVerticalRotations,
				value -> { PathFinderSettings.limitVerticalRotations = value; PathFinderSettings.rotationPreset = "Custom"; }), rotationRowParams(context));
		safety.addView(rotationToggle(context, "Stop Rotating in GUIs", "Pause rotation while a screen is open.", PathFinderSettings.stopRotatingInGuis,
				value -> { PathFinderSettings.stopRotatingInGuis = value; PathFinderSettings.rotationPreset = "Custom"; }), rotationRowParams(context));

		cards.addView(angle, rotationColumnParams(context, true));
		cards.addView(movement, rotationColumnParams(context, true));
		LinearLayout right = new LinearLayout(context);
		right.setOrientation(LinearLayout.VERTICAL);
		right.addView(response, rotationStackParams(context, true));
		right.addView(safety, rotationStackParams(context, false));
		cards.addView(right, rotationColumnParams(context, false));
		LinearLayout.LayoutParams cardsParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		cardsParams.bottomMargin = px(context, 10);
		body.addView(cards, cardsParams);
		body.addView(rotationPreview(context), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
		return body;
	}

	private View rotationPreset(Context context, String name, String detail) {
		boolean selected = name.equals(PathFinderSettings.rotationPreset);
		LinearLayout card = new LinearLayout(context); card.setOrientation(LinearLayout.HORIZONTAL); card.setGravity(Gravity.CENTER_VERTICAL);
		card.setPadding(px(context, 9), 0, px(context, 7), 0);
		card.setBackground(box(card, selected ? 0xA32B5B9C : 0x66152231, selected ? BLUE : 0x553B587C, 6));
		ShulkIcon glyphIcon = switch (name) {
			case "Legit" -> ShulkIcon.COMPASS;
			case "Balanced" -> ShulkIcon.CONTROLS;
			case "Aggressive" -> ShulkIcon.PLAY;
			case "Minimal" -> ShulkIcon.STATUS;
			default -> ShulkIcon.SETTINGS;
		};
		ShulkIconView glyph = new ShulkIconView(context, glyphIcon, selected ? TEXT : BLUE_LIGHT, name + " preset");
		card.addView(glyph, new LinearLayout.LayoutParams(px(context, 22), px(context, 24)));
		LinearLayout copy = new LinearLayout(context); copy.setOrientation(LinearLayout.VERTICAL); copy.setGravity(Gravity.CENTER_VERTICAL);
		TextView title = text(context, name, 10, selected ? TEXT : MUTED);
		TextView subtitle = text(context, detail, 7, selected ? BLUE_LIGHT : MUTED);
		copy.addView(title, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 17)));
		copy.addView(subtitle, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 13)));
		LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
		copyParams.leftMargin = px(context, 7);
		card.addView(copy, copyParams);
		card.setClickable(true); KairokkUiSounds.attach(card);
		card.setOnClickListener(v -> { if (!"Custom".equals(name)) PathFinderSettings.applyPreset(name); else PathFinderSettings.rotationPreset = "Custom"; showTopTab(context); });
		return card;
	}

	private LinearLayout rotationGroup(Context context, String title) {
		LinearLayout group = new LinearLayout(context); group.setOrientation(LinearLayout.VERTICAL);
		TextView label = text(context, title, 10, BLUE_LIGHT); label.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		group.addView(label, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 22)));
		return group;
	}

	private static LinearLayout.LayoutParams rotationColumnParams(Context context, boolean withMargin) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
		if (withMargin) params.rightMargin = px(context, 8);
		return params;
	}

	private static LinearLayout.LayoutParams rotationStackParams(Context context, boolean withMargin) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		if (withMargin) params.bottomMargin = px(context, 8);
		return params;
	}

	private View rotationToggle(Context context, String name, String detail, boolean initial, java.util.function.Consumer<Boolean> setter) {
		LinearLayout row = new LinearLayout(context);
		row.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout copy = new LinearLayout(context);
		copy.setOrientation(LinearLayout.VERTICAL);
		copy.setGravity(Gravity.CENTER_VERTICAL);
		TextView heading = text(context, name, 10, TEXT);
		TextView explanation = text(context, detail, 8, MUTED);
		explanation.setVisibility(KairokkCustomizationState.showModuleDescriptions() ? View.VISIBLE : View.GONE);
		copy.addView(heading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 16)));
		copy.addView(explanation, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 13)));
		row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
		FrameLayout toggle = animatedToggle(context, initial, value -> { setter.accept(value); });
		row.addView(toggle, new LinearLayout.LayoutParams(px(context, 48), px(context, 24)));
		return row;
	}

	private View rotationSlider(Context context, String label, int value, int min, int max,
			java.util.function.Function<Integer, String> formatter, java.util.function.IntConsumer setter) {
		if (PathFinderSettings.advancedMode && rotationMinimum(label) != null) {
			return rotationRangeSlider(context, label, Math.round(rotationMinimum(label)), value, min, max, formatter,
					v -> setRotationMinimum(label, v), setter);
		}
		LinearLayout slider = itemSlider(context, label, value, min, max, formatter, v -> {
			setter.accept(v); setRotationMinimum(label, v); PathFinderSettings.rotationPreset = "Custom";
		});
		addRotationDescription(context, slider, label);
		return slider;
	}

	private Float rotationMinimum(String label) {
		return switch (label) {
			case "Humanization" -> PathFinderSettings.minHumanization * 100;
			case "Jitter Amount" -> PathFinderSettings.minJitterAmplitude * 100;
			case "Arc" -> PathFinderSettings.minArc * 10;
			case "Aim Tolerance" -> PathFinderSettings.minDeadzone * 100;
			case "Response Curve" -> PathFinderSettings.minCurveIntensity * 100;
			default -> null;
		};
	}
	private void setRotationMinimum(String label, int value) {
		switch (label) {
			case "Humanization" -> PathFinderSettings.minHumanization = value / 100f;
			case "Jitter Amount" -> PathFinderSettings.minJitterAmplitude = value / 100f;
			case "Arc" -> PathFinderSettings.minArc = value / 10f;
			case "Aim Tolerance" -> PathFinderSettings.minDeadzone = value / 100f;
			case "Response Curve" -> PathFinderSettings.minCurveIntensity = value / 100f;
		}
	}
	private void addRotationDescription(Context context, LinearLayout slider, String label) {
		String description = switch (label) {
			case "Rotation Speed" -> "How quickly the camera turns toward its goal.";
			case "Humanization" -> "Controls the amount of natural variation per turn.";
			case "Jitter Amount" -> "Adds small deviations along the planned path.";
			case "Arc" -> "Bends the turn upward or downward.";
			case "Aim Tolerance" -> "Allowed camera error before the goal is reached.";
			case "Response Curve" -> "Shapes acceleration and slowing near the goal.";
			case "Angle" -> "Minimum and maximum angular range.";
			case "Time" -> "Minimum and maximum duration of a turn.";
			case "Step Jitter" -> "Adds short variations to the planned turn.";
			case "Mouse Room" -> "Travel available before pausing to reset the mouse.";
			default -> "";
		};
		TextView detail = text(context, description, 7, MUTED);
		detail.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		slider.addView(detail, 1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 12)));
	}
	private static LinearLayout.LayoutParams rotationRowParams(Context context) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 48));
		params.bottomMargin = px(context, 3);
		return params;
	}

	/** Two-thumb range control used wherever a setting has a minimum and maximum. */
	private View rotationRangeSlider(Context context, String label, int initialLow, int initialHigh, int min, int max,
			java.util.function.Function<Integer, String> formatter, java.util.function.IntConsumer lowSetter,
			java.util.function.IntConsumer highSetter) {
		LinearLayout slider = new LinearLayout(context); slider.setOrientation(LinearLayout.VERTICAL);
		LinearLayout heading = new LinearLayout(context); heading.setGravity(Gravity.CENTER_VERTICAL);
		TextView name = text(context, label, 10, TEXT); name.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		TextView lowText = text(context, formatter.apply(initialLow), 8, BLUE_LIGHT); lowText.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		TextView divider = text(context, "–", 8, MUTED); divider.setGravity(Gravity.CENTER);
		TextView highText = text(context, formatter.apply(initialHigh), 8, BLUE_LIGHT); highText.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		heading.addView(name, new LinearLayout.LayoutParams(0, px(context, 17), 1));
		LinearLayout values = new LinearLayout(context); values.setGravity(Gravity.CENTER_VERTICAL);
		values.addView(lowText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(context, 17)));
		LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(context, 17));
		dividerParams.leftMargin = px(context, 5); dividerParams.rightMargin = px(context, 5);
		values.addView(divider, dividerParams);
		values.addView(highText, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(context, 17)));
		heading.addView(values, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, px(context, 17)));
		slider.addView(heading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 18)));

		FrameLayout track = new FrameLayout(context); track.setBackground(pill(track, 0xFF20344C));
		int inset = px(context, 6), thumb = px(context, 11), halfThumb = px(context, 5.5f);
		int[] range = {Math.clamp(initialLow, min, max), Math.clamp(initialHigh, min, max)};
		if (range[0] > range[1]) { int swap = range[0]; range[0] = range[1]; range[1] = swap; }
		View fill = new View(context); fill.setBackground(pill(fill, BLUE)); paletteBlueViews.add(fill);
		FrameLayout.LayoutParams fillParams = new FrameLayout.LayoutParams(0, px(context, 5), Gravity.LEFT | Gravity.CENTER_VERTICAL); track.addView(fill, fillParams);
		View lowThumb = new View(context); lowThumb.setBackground(pill(lowThumb, BLUE_LIGHT)); paletteLightViews.add(lowThumb);
		View highThumb = new View(context); highThumb.setBackground(pill(highThumb, BLUE_LIGHT)); paletteLightViews.add(highThumb);
		FrameLayout.LayoutParams lowParams = new FrameLayout.LayoutParams(thumb, thumb, Gravity.LEFT | Gravity.CENTER_VERTICAL);
		FrameLayout.LayoutParams highParams = new FrameLayout.LayoutParams(thumb, thumb, Gravity.LEFT | Gravity.CENTER_VERTICAL);
		track.addView(lowThumb, lowParams); track.addView(highThumb, highParams);
		Runnable layout = () -> {
			int inner = Math.max(1, track.getWidth() - inset * 2);
			int lowX = Math.round(inner * (range[0] - min) / (float) Math.max(1, max - min));
			int highX = Math.round(inner * (range[1] - min) / (float) Math.max(1, max - min));
			fillParams.leftMargin = inset + lowX; fillParams.width = Math.max(px(context, 2), highX - lowX); fill.setLayoutParams(fillParams);
			lowParams.leftMargin = inset + lowX - halfThumb; lowThumb.setLayoutParams(lowParams);
			highParams.leftMargin = inset + highX - halfThumb; highThumb.setLayoutParams(highParams);
		};
		track.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> layout.run());
		boolean[] movingLow = {true};
		track.setOnTouchListener((view, event) -> {
			int action = event.getActionMasked();
			if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_UP) return true;
			float inner = Math.max(1f, view.getWidth() - inset * 2f);
			int tapped = Math.round(min + clamp((event.getX() - inset) / inner) * (max - min));
			if (action == MotionEvent.ACTION_DOWN) {
				movingLow[0] = Math.abs(tapped - range[0]) <= Math.abs(tapped - range[1]);
				KairokkUiSounds.play(KairokkUiSounds.SELECT);
			}
			if (movingLow[0]) range[0] = Math.min(tapped, range[1]); else range[1] = Math.max(tapped, range[0]);
			lowSetter.accept(range[0]); highSetter.accept(range[1]); PathFinderSettings.rotationPreset = "Custom";
			lowText.setText(formatter.apply(range[0])); highText.setText(formatter.apply(range[1])); layout.run();
			return true;
		});
		slider.addView(track, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 13)));
		addRotationDescription(context, slider, label);
		return slider;
	}
	private static String tenths(int value) { return String.format(java.util.Locale.ROOT, "%.1f", value / 10F); }
	private static String hundredths(int value) { return String.format(java.util.Locale.ROOT, "%.2f", value / 100F); }

	private View pathfinderToggle(Context context, String name, String detail, boolean initial, java.util.function.Consumer<Boolean> setter) {
		LinearLayout row = new LinearLayout(context);row.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout copy=new LinearLayout(context);copy.setOrientation(LinearLayout.VERTICAL);copy.setGravity(Gravity.CENTER_VERTICAL);
		copy.addView(text(context,name,10,TEXT));copy.addView(text(context,detail,7,BLUE_LIGHT));row.addView(copy,new LinearLayout.LayoutParams(0,-1,1));
		FrameLayout toggle = animatedToggle(context, initial, setter);
		row.addView(toggle, new LinearLayout.LayoutParams(px(context, 49), px(context, 27)));
		return row;
	}

	private View rotationPreview(Context context) {
		LinearLayout panel = new LinearLayout(context);
		panel.setOrientation(LinearLayout.VERTICAL);
		panel.setPadding(px(context, 15), px(context, 13), px(context, 15), px(context, 15));
		panel.setBackground(box(panel, 0x82101822, 0x663B506E, 7));
		LinearLayout header = new LinearLayout(context); header.setGravity(Gravity.CENTER_VERTICAL);
		TextView heading = text(context, "│ L I V E   R O T A T I O N   P R E V I E W", 14, TEXT);
		header.addView(heading, new LinearLayout.LayoutParams(0, px(context, 29), 1));
		TextView live = text(context, "●  LIVE", 10, 0xFF70E6A5); live.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		header.addView(live, new LinearLayout.LayoutParams(px(context, 70), px(context, 29)));
		panel.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 29)));
		RotationViewport stage = new RotationViewport(context);
		stage.setBackground(box(stage, 0xFF10151F, 0x553B587C, 6));
		panel.addView(stage, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
		return panel;
	}

	private int workspaceHeight(Context context) {
		int fallback = px(context, 630);
		if (rootView == null || rootView.getHeight() <= 0) return fallback;
		int available = rootView.getHeight() - px(context, 88) - px(context, 14);
		return Math.max(px(context, 330), available);
	}

	private String visualDescription(String category) {
		return switch (category) {
			case "Mobs" -> "Highlight and configure mobs in your world.";
			case "Items" -> "Highlight dropped items and entities.";
			case "World" -> "Configure world and environment visuals.";
			case "Render" -> "Tune rendering and visual effects.";
			case "Overlay" -> "Configure your in-game information layer.";
			default -> "Configure player visuals and entity information.";
		};
	}

	private void addVisualNavigation(LinearLayout sidebar, Context context, ShulkIcon icon, String label, String detail) {
		boolean selected = label.equals(selectedCategory);
		LinearLayout category = new LinearLayout(context);
		category.setGravity(Gravity.CENTER_VERTICAL);
		category.setPadding(px(context, 8), 0, px(context, 7), 0);
		category.setBackground(selected ? categoryBackground(category) : box(category, 0x00152231, 0x00000000, 6));
		ShulkIconView glyph = new ShulkIconView(context, icon, selected ? TEXT : BLUE_LIGHT, label);
		category.addView(glyph, new LinearLayout.LayoutParams(px(context, 27), px(context, 38)));
		LinearLayout labels = new LinearLayout(context);
		labels.setOrientation(LinearLayout.VERTICAL);
		labels.setGravity(Gravity.CENTER_VERTICAL);
		TextView name = text(context, label, 11, selected ? TEXT : MUTED);
		name.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		TextView explanation = text(context, detail, 8, selected ? BLUE_LIGHT : MUTED);
		explanation.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		labels.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 17)));
		labels.addView(explanation, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 14)));
		LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
		labelParams.leftMargin = px(context, 7);
		category.addView(labels, labelParams);
		category.setClickable(true);
		attachSidebarHover(category, () -> label.equals(selectedCategory));
		category.setOnClickListener(view -> {
			if (contentTransitionRunning || label.equals(selectedCategory)) return;
			selectedCategory = label;
			if (visualPageTitle != null) visualPageTitle.setText(label.toUpperCase(java.util.Locale.ROOT));
			if (visualPageSubtitle != null) visualPageSubtitle.setText(visualDescription(label));
			for (int i = 2; i < sidebar.getChildCount(); i++) {
				View child = sidebar.getChildAt(i);
				boolean active = child == category;
				child.setBackground(active ? categoryBackground(child) : box(child, 0x00152231, 0x00000000, 6));
				child.setAlpha(1F); child.setScaleY(1F);
				LinearLayout group = (LinearLayout) child;
				((ShulkIconView) group.getChildAt(0)).setPalette(active ? TEXT : BLUE_LIGHT, TEXT, TEXT, MUTED);
				LinearLayout groupLabels = (LinearLayout) group.getChildAt(1);
				((TextView) groupLabels.getChildAt(0)).setTextColor(active ? TEXT : MUTED);
				((TextView) groupLabels.getChildAt(1)).setTextColor(active ? BLUE_LIGHT : MUTED);
			}
			transitionVisualCategory(context);
		});
		KairokkUiSounds.attachSelection(category);
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 46));
		params.bottomMargin = px(context, 4);
		sidebar.addView(category, params);
	}

	private void showVisualCategory(Context context) {
		visualColumns.removeAllViews();
		if ("Players".equals(selectedCategory)) {
			visualColumns.addView(playerOptions(context), weighted(context, 1, true));
			visualColumns.addView(preview(context), weighted(context, 1, false));
		} else if ("Mobs".equals(selectedCategory)) {
			visualColumns.addView(mobOptions(context), weighted(context, 2, true));
			visualColumns.addView(mobList(context), weighted(context, 3, false));
		} else if ("Items".equals(selectedCategory)) {
			visualColumns.addView(itemVisuals(context), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		} else if ("Overlay".equals(selectedCategory)) {
			visualColumns.addView(overlayWidgets(context), weighted(context, 2, true));
			visualColumns.addView(overlayAppearance(context), weighted(context, 3, false));
		} else {
			TextView placeholder = text(context, selectedCategory + " settings are coming soon.", 16, MUTED);
			placeholder.setGravity(Gravity.CENTER);
			placeholder.setBackground(box(placeholder, 0x82101822, 0x663B506E, 7));
			visualColumns.addView(placeholder, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
		}
	}

	private View itemVisuals(Context context) {
		ScrollView scroll = new ScrollView(context);
		scroll.setFillViewport(true);
		scroll.setSmoothScrollingEnabled(KairokkCustomizationState.smoothScrolling());
		scroll.setEdgeEffectColor(BLUE);
		scroll.setBottomEdgeEffectColor(BLUE);
		LinearLayout body = new LinearLayout(context);
		body.setOrientation(LinearLayout.VERTICAL);
		body.setPadding(px(context, 2), px(context, 2), px(context, 5), px(context, 8));

		LinearLayout firstRow = new LinearLayout(context);
		firstRow.setOrientation(LinearLayout.HORIZONTAL);
		firstRow.setGravity(Gravity.TOP);
		firstRow.addView(itemGeneralCard(context), itemColumnParams(context, true));
		firstRow.addView(itemStyleCard(context), itemColumnParams(context, false));
		LinearLayout secondRow = new LinearLayout(context);
		secondRow.setOrientation(LinearLayout.HORIZONTAL);
		secondRow.setGravity(Gravity.TOP);
		secondRow.addView(itemColoursCard(context), itemColumnParams(context, true));
		secondRow.addView(itemFiltersCard(context), itemColumnParams(context, false));

		int screenWidth = Minecraft.getInstance().getWindow().getScreenWidth();
		if (screenWidth < 1000) {
			body.addView(firstRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 318)));
			body.addView(secondRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 318)));
		} else {
			LinearLayout wideRow = new LinearLayout(context);
			wideRow.setOrientation(LinearLayout.HORIZONTAL);
			wideRow.setGravity(Gravity.TOP);
			wideRow.addView(itemGeneralCard(context), itemWideColumnParams(context, 1, true));
			wideRow.addView(itemStyleCard(context), itemWideColumnParams(context, 1, true));
			wideRow.addView(itemColoursCard(context), itemWideColumnParams(context, 1, true));
			wideRow.addView(itemFiltersCard(context), itemWideColumnParams(context, 1, false));
			body.addView(wideRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 318)));
		}

		body.addView(itemPreviewCard(context), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 225)));
		scroll.addView(body, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
		return scroll;
	}

	private LinearLayout.LayoutParams itemColumnParams(Context context, boolean rightMargin) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
		if (rightMargin) params.rightMargin = px(context, 9);
		return params;
	}

	private LinearLayout.LayoutParams itemRowParams(Context context) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 30));
		params.bottomMargin = px(context, KairokkCustomizationState.compactMode() ? 0 : KairokkCustomizationState.moduleSpacing() / 3F);
		return params;
	}

	private LinearLayout.LayoutParams itemWideColumnParams(Context context, int weight, boolean rightMargin) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
		if (rightMargin) params.rightMargin = px(context, 9);
		return params;
	}

	private LinearLayout itemGeneralCard(Context context) {
		LinearLayout card = miscCard(context, ShulkIcon.RESOURCE_PACK, "GENERAL", "Choose what appears around dropped items.");
		card.addView(itemToggleRow(context, "Item ESP", "Show dropped items.", "itemEsp"), itemRowParams(context));
		card.addView(itemToggleRow(context, "Item Names", "Show item names.", "itemNames"), itemRowParams(context));
		card.addView(itemToggleRow(context, "Item Boxes", "Render boxes around items.", "itemBoxes"), itemRowParams(context));
		card.addView(itemToggleRow(context, "Item Tracers", "Draw lines to items.", "itemTracers"), itemRowParams(context));
		card.addView(itemToggleRow(context, "Item Glow", "Add a soft glow effect.", "itemGlow"), itemRowParams(context));
		card.addView(itemToggleRow(context, "Item Icon", "Show a small icon above items.", "itemIcon"), itemRowParams(context));
		card.addView(itemToggleRow(context, "Item Distance", "Show distance text.", "itemDistance"), itemRowParams(context));
		card.addView(itemToggleRow(context, "Filter by Rarity", "Colour items by rarity.", "filterByRarity"), itemRowParams(context));
		return card;
	}

	private LinearLayout itemStyleCard(Context context) {
		LinearLayout card = miscCard(context, ShulkIcon.LAYOUT, "STYLE", "Shape and tune the item markers.");
		card.addView(itemChoiceRow(context, "Box Style", KairokkItemEspState.boxStyle(), new String[]{"Cornered", "Full", "None"}, value -> KairokkItemEspState.setBoxStyle(value)), itemRowParams(context));
		card.addView(itemSlider(context, "Line Width", KairokkItemEspState.lineWidth(), 5, 30, value -> formatTenths(value) + "px", value -> { KairokkItemEspState.setLineWidth(value); invalidateItemPreview(); }), itemRowParams(context));
		card.addView(itemSlider(context, "Corner Radius", KairokkItemEspState.cornerRadius(), 0, 8, value -> value + "px", value -> { KairokkItemEspState.setCornerRadius(value); invalidateItemPreview(); }), itemRowParams(context));
		card.addView(itemSlider(context, "Glow Strength", KairokkItemEspState.glowStrength(), 0, 100, value -> value + "%", value -> { KairokkItemEspState.setGlowStrength(value); invalidateItemPreview(); }), itemRowParams(context));
		card.addView(itemChoiceRow(context, "Tracer Style", KairokkItemEspState.tracerStyle(), new String[]{"Solid", "Dashed", "Dotted"}, value -> KairokkItemEspState.setTracerStyle(value)), itemRowParams(context));
		card.addView(itemSlider(context, "Tracer Thickness", KairokkItemEspState.tracerThickness(), 1, 4, value -> value + "px", value -> { KairokkItemEspState.setTracerThickness(value); invalidateItemPreview(); }), itemRowParams(context));
		return card;
	}

	private LinearLayout itemColoursCard(Context context) {
		LinearLayout card = miscCard(context, ShulkIcon.PALETTE, "COLOURS", "Click a swatch to cycle its colour.");
		card.addView(itemColourRow(context, "Common", "common"), itemRowParams(context));
		card.addView(itemColourRow(context, "Uncommon", "uncommon"), itemRowParams(context));
		card.addView(itemColourRow(context, "Rare", "rare"), itemRowParams(context));
		card.addView(itemColourRow(context, "Epic", "epic"), itemRowParams(context));
		card.addView(itemColourRow(context, "Legendary", "legendary"), itemRowParams(context));
		card.addView(itemColourRow(context, "Mythic", "mythic"), itemRowParams(context));
		card.addView(itemColourRow(context, "Text", "text"), itemRowParams(context));
		card.addView(itemColourRow(context, "Tracer", "tracer"), itemRowParams(context));
		return card;
	}

	private LinearLayout itemFiltersCard(Context context) {
		LinearLayout card = miscCard(context, ShulkIcon.LIST, "FILTERS", "Choose which item groups are shown.");
		card.addView(itemToggleRow(context, "Show Common", "Show common items.", "showCommon"), itemRowParams(context));
		card.addView(itemToggleRow(context, "Show Uncommon", "Show uncommon items.", "showUncommon"), itemRowParams(context));
		card.addView(itemToggleRow(context, "Show Rare", "Show rare items.", "showRare"), itemRowParams(context));
		card.addView(itemToggleRow(context, "Show Epic", "Show epic items.", "showEpic"), itemRowParams(context));
		card.addView(itemToggleRow(context, "Show Legendary", "Show legendary items.", "showLegendary"), itemRowParams(context));
		card.addView(itemToggleRow(context, "Show Mythic", "Show mythic items.", "showMythic"), itemRowParams(context));
		card.addView(itemToggleRow(context, "Show XP Orbs", "Include experience orbs.", "showXpOrbs"), itemRowParams(context));
		card.addView(itemToggleRow(context, "Show Other Entities", "Include special entities.", "showOtherEntities"), itemRowParams(context));
		return card;
	}

	private LinearLayout itemPreviewCard(Context context) {
		LinearLayout card = miscCard(context, ShulkIcon.VISIBLE, "PREVIEW", "This is how items will appear in-game.");
		itemPreview = new ItemPreview(context);
		itemPreview.setBackground(box(itemPreview, 0xC7182637, 0x44394E70, 7));
		card.addView(itemPreview, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
		return card;
	}

	private void invalidateItemPreview() {
		if (itemPreview != null) itemPreview.invalidate();
	}

	private LinearLayout itemToggleRow(Context context, String label, String detail, String key) {
		LinearLayout row = new LinearLayout(context);
		row.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout labels = new LinearLayout(context);
		labels.setOrientation(LinearLayout.VERTICAL);
		labels.setGravity(Gravity.CENTER_VERTICAL);
		TextView name = text(context, label, 10, TEXT);
		TextView explanation = text(context, detail, 8, MUTED);
		explanation.setVisibility(KairokkCustomizationState.showModuleDescriptions() ? View.VISIBLE : View.GONE);
		labels.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 15)));
		labels.addView(explanation, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 13)));
		row.addView(labels, new LinearLayout.LayoutParams(0, px(context, 30), 1));
		FrameLayout toggle = animatedToggle(context, KairokkItemEspState.enabled(key), enabled -> { KairokkItemEspState.setBoolean(key, enabled); invalidateItemPreview(); });
		row.addView(toggle, new LinearLayout.LayoutParams(px(context, 48), px(context, 24)));
		return row;
	}

	private LinearLayout itemChoiceRow(Context context, String label, String current, String[] choices, java.util.function.Consumer<String> setter) {
		LinearLayout row = new LinearLayout(context);
		row.setGravity(Gravity.CENTER_VERTICAL);
		TextView name = text(context, label, 10, TEXT);
		name.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		row.addView(name, new LinearLayout.LayoutParams(0, px(context, 31), 1));
		LinearLayout choice = new LinearLayout(context);
		choice.setGravity(Gravity.CENTER_VERTICAL);
		choice.setPadding(px(context, 7), 0, px(context, 4), 0);
		choice.setBackground(box(choice, 0x5D223147, 0x885377A4, 5));
		TextView value = text(context, current, 9, TEXT);
		value.setGravity(Gravity.CENTER_VERTICAL);
		choice.addView(value, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
		ShulkIconView arrow = new ShulkIconView(context, ShulkIcon.EXPAND, BLUE_LIGHT, "Choose " + label);
		arrow.setPadding(px(context, 1), px(context, 7), px(context, 1), px(context, 7));
		choice.addView(arrow, new LinearLayout.LayoutParams(px(context, 16), ViewGroup.LayoutParams.MATCH_PARENT));
		choice.setClickable(true);
		KairokkUiSounds.attach(choice);
		choice.setOnClickListener(view -> showCustomizationMenu(context, choice, value, arrow, choices, option -> { setter.accept(option); invalidateItemPreview(); }));
		row.addView(choice, new LinearLayout.LayoutParams(px(context, 104), px(context, 31)));
		return row;
	}

	private LinearLayout itemColourRow(Context context, String label, String slot) {
		LinearLayout row = new LinearLayout(context);
		row.setGravity(Gravity.CENTER_VERTICAL);
		TextView name = text(context, label, 10, TEXT);
		name.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		row.addView(name, new LinearLayout.LayoutParams(0, px(context, 29), 1));
		View swatch = new View(context);
		swatch.setBackground(colorSwatch(swatch, KairokkItemEspState.color(slot)));
		swatch.setClickable(true);
		swatch.setTooltipText("Cycle " + label + " colour");
		KairokkUiSounds.attach(swatch);
		swatch.setOnClickListener(view -> { KairokkItemEspState.cycleColor(slot); swatch.setBackground(colorSwatch(swatch, KairokkItemEspState.color(slot))); invalidateItemPreview(); });
		row.addView(swatch, new LinearLayout.LayoutParams(px(context, 25), px(context, 25)));
		return row;
	}

	private LinearLayout itemSlider(Context context, String label, int initial, int min, int max, java.util.function.Function<Integer, String> formatter, java.util.function.IntConsumer setter) {
		LinearLayout slider = new LinearLayout(context);
		slider.setOrientation(LinearLayout.VERTICAL);
		LinearLayout titleRow = new LinearLayout(context);
		titleRow.setGravity(Gravity.CENTER_VERTICAL);
		TextView name = text(context, label, 10, TEXT);
		TextView amount = text(context, formatter.apply(initial), 9, BLUE_LIGHT);
		amount.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
		titleRow.addView(name, new LinearLayout.LayoutParams(0, px(context, 17), 1));
		titleRow.addView(amount, new LinearLayout.LayoutParams(px(context, 47), px(context, 17)));
		slider.addView(titleRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 18)));
		FrameLayout track = new FrameLayout(context);
		track.setBackground(pill(track, 0xFF20344C));
		// The previous 3px inset placed half of the 11px thumb outside the view.
		int pad = px(context, 6);
		float[] fraction = {(initial - min) / (float) Math.max(1, max - min)};
		View fill = new View(context);
		fill.setBackground(pill(fill, BLUE));
		paletteBlueViews.add(fill);
		FrameLayout.LayoutParams fillParams = new FrameLayout.LayoutParams(0, px(context, 5), Gravity.LEFT | Gravity.CENTER_VERTICAL);
		fillParams.leftMargin = pad;
		track.addView(fill, fillParams);
		View knob = new View(context);
		knob.setBackground(pill(knob, BLUE_LIGHT));
		paletteLightViews.add(knob);
		FrameLayout.LayoutParams knobParams = new FrameLayout.LayoutParams(px(context, 11), px(context, 11), Gravity.LEFT | Gravity.CENTER_VERTICAL);
		track.addView(knob, knobParams);
		Runnable update = () -> {
			int inner = Math.max(1, track.getWidth() - pad * 2);
			fillParams.leftMargin = pad;
			fillParams.width = Math.round(inner * fraction[0]);
			fill.setLayoutParams(fillParams);
			knobParams.leftMargin = pad + Math.round(inner * fraction[0]) - px(context, 5);
			knob.setLayoutParams(knobParams);
		};
		track.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> update.run());
		track.setOnTouchListener((view, event) -> {
			int action = event.getActionMasked();
			if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP) {
				if (action == MotionEvent.ACTION_DOWN) KairokkUiSounds.play(KairokkUiSounds.SELECT);
				float inner = Math.max(1F, view.getWidth() - pad * 2F);
				fraction[0] = clamp((event.getX() - pad) / inner);
				int value = Math.round(min + fraction[0] * (max - min));
				setter.accept(value);
				amount.setText(formatter.apply(value));
				update.run();
			}
			return true;
		});
		slider.addView(track, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 12)));
		return slider;
	}

	private static String formatTenths(int value) {
		return String.format(java.util.Locale.ROOT, "%.1f", value / 10F);
	}

	private View mobOptions(Context context) {
		LinearLayout panel = new LinearLayout(context);
		panel.setOrientation(LinearLayout.VERTICAL);
		panel.setPadding(px(context, 16), px(context, 10), px(context, 16), px(context, 10));
		panel.setBackground(box(panel, 0x82101822, 0x663B506E, 7));
		addMobToggleSetting(panel, context, "Mob ESP", "Highlight mobs in your world.");
		addMobChoiceSetting(panel, context);
		addColorSetting(panel, context, "Box Color", "Set the color of mob boxes.");
		addMobToggleSetting(panel, context, "Outline", "Add an outline to the boxes.");
		addMobToggleSetting(panel, context, "Name Tags", "Display mob names.");
		addMobToggleSetting(panel, context, "Distance", "Show distance to mobs.");
		addMobToggleSetting(panel, context, "Health", "Show mob health.");
		addMobToggleSetting(panel, context, "Tracers", "Draw lines to mobs.");
		addMobToggleSetting(panel, context, "Chams", "Render mobs with a solid material.");
		addMobToggleSetting(panel, context, "Ignore Invisible", "Skip invisible mobs.");
		return panel;
	}

	private View overlayWidgets(Context context) {
		KairokkOverlayManager manager = KairokkOverlayManager.get();
		LinearLayout panel = new LinearLayout(context);
		panel.setOrientation(LinearLayout.VERTICAL);
		panel.setPadding(px(context, 16), px(context, 10), px(context, 16), px(context, 10));
		panel.setBackground(box(panel, 0x82101822, 0x663B506E, 7));
		LinearLayout header = new LinearLayout(context);
		header.setGravity(Gravity.CENTER_VERTICAL);
		TextView title = text(context, "O V E R L A Y   W I D G E T S", 14, TEXT);
		header.addView(title, new LinearLayout.LayoutParams(0, px(context, 29), 1));
		TextView reset = overlayButton(context, "Reset", false);
		reset.setOnClickListener(view -> { manager.restoreDefaults(); transitionVisualCategory(context); });
		header.addView(reset, new LinearLayout.LayoutParams(px(context, 62), px(context, 29)));
		panel.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 31)));
		TextView hint = text(context, "Toggle and configure what appears on your overlay.", 11, MUTED);
		hint.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		panel.addView(hint, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 23)));
		TextView master = overlayButton(context, "Overlay: " + (manager.rendererEnabled() ? "On" : "Off"), manager.rendererEnabled());
		master.setOnClickListener(view -> { manager.setRendererEnabled(!manager.rendererEnabled()); transitionVisualCategory(context); });
		panel.addView(master, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 29)));
		for (KairokkOverlayManager.Widget widget : manager.widgets()) {
			LinearLayout row = new LinearLayout(context);
			row.setGravity(Gravity.CENTER_VERTICAL);
			row.setPadding(px(context, 8), 0, px(context, 7), 0);
			row.setBackground(box(row, 0x6D152231, 0x443B587C, 5));
			ShulkIconView glyph = new ShulkIconView(context, ShulkIcon.forOverlayWidget(widget.typeId), widget.accentColor, widget.name);
			row.addView(glyph, new LinearLayout.LayoutParams(px(context, 25), px(context, 25)));
			LinearLayout labels = new LinearLayout(context);
			labels.setOrientation(LinearLayout.VERTICAL);
			labels.setGravity(Gravity.CENTER_VERTICAL);
			TextView name = text(context, widget.name, 12, TEXT);
			TextView detail = text(context, KairokkOverlayManager.type(widget.typeId).description(), 9, MUTED);
			labels.addView(name, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 18)));
			labels.addView(detail, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 15)));
			LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
			labelParams.leftMargin = px(context, 7);
			row.addView(labels, labelParams);
			FrameLayout toggle = animatedToggle(context, widget.visible, enabled -> { manager.setRendererEnabled(true); manager.toggleVisible(widget.id); transitionVisualCategory(context); });
			row.addView(toggle, new LinearLayout.LayoutParams(px(context, 48), px(context, 24)));
			TextView edit = text(context, "›", 20, BLUE_LIGHT);
			edit.setGravity(Gravity.CENTER);
			row.addView(edit, new LinearLayout.LayoutParams(px(context, 23), ViewGroup.LayoutParams.MATCH_PARENT));
			row.setClickable(true);
			row.setOnClickListener(view -> { manager.select(widget.id); transitionVisualCategory(context); });
			KairokkUiSounds.attach(row);
			LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 38));
			rowParams.bottomMargin = px(context, 3);
			panel.addView(row, rowParams);
		}
		return panel;
	}

	private View overlayAppearance(Context context) {
		KairokkOverlayManager manager = KairokkOverlayManager.get();
		LinearLayout panel = new LinearLayout(context);
		panel.setOrientation(LinearLayout.VERTICAL);
		panel.setPadding(px(context, 15), px(context, 12), px(context, 15), px(context, 12));
		panel.setBackground(box(panel, 0x82101822, 0x663B506E, 7));
		LinearLayout header = new LinearLayout(context);
		header.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout labels = new LinearLayout(context);
		labels.setOrientation(LinearLayout.VERTICAL);
		TextView heading = text(context, "A P P E A R A N C E", 15, TEXT);
		TextView subheading = text(context, "Customise the style of your overlay elements.", 11, MUTED);
		labels.addView(heading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 23)));
		labels.addView(subheading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 17)));
		header.addView(labels, new LinearLayout.LayoutParams(0, px(context, 43), 1));
		TextView edit = overlayButton(context, "✎  Edit Overlay", true);
		edit.setOnClickListener(view -> Core.executeOnMainThread(() -> {
			manager.setRendererEnabled(true);
			Minecraft.getInstance().setScreen(new KairokkOverlayEditScreen(Minecraft.getInstance().screen));
		}));
		header.addView(edit, new LinearLayout.LayoutParams(px(context, 132), px(context, 39)));
		panel.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 48)));

		LinearLayout columns = new LinearLayout(context);
		columns.setGravity(Gravity.TOP);
		columns.addView(overlayTheme(context, manager), weighted(context, 1, true));
		columns.addView(overlayStyle(context, manager), weighted(context, 1, false));
		panel.addView(columns, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
		return panel;
	}

	private View overlayTheme(Context context, KairokkOverlayManager manager) {
		LinearLayout panel = overlaySubpanel(context, "THEME", "Choose a starting look.");
		for (String preset : new String[]{"Default", "Clean", "Vibrant", "Dark"}) {
			TextView option = overlayButton(context, preset, "Default".equals(preset));
			option.setOnClickListener(view -> { manager.applyPreset("Default"); if ("Clean".equals(preset)) manager.cycleSelectedPadding(); if ("Vibrant".equals(preset)) manager.cycleSelectedAccent(); transitionVisualCategory(context); });
			panel.addView(option, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 34)));
		}
		TextView presets = text(context, "PRESETS", 10, MUTED);
		presets.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		LinearLayout.LayoutParams presetParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 24));
		presetParams.topMargin = px(context, 9);
		panel.addView(presets, presetParams);
		for (String preset : KairokkOverlayManager.PRESETS) {
			TextView button = overlayButton(context, preset + (preset.equals(manager.currentPreset()) ? "  ✓" : ""), preset.equals(manager.currentPreset()));
			button.setOnClickListener(view -> { manager.applyPreset(preset); transitionVisualCategory(context); });
			panel.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 27)));
		}
		return panel;
	}

	private View overlayStyle(Context context, KairokkOverlayManager manager) {
		LinearLayout panel = overlaySubpanel(context, "STYLE", "Click an option to cycle it.");
		KairokkOverlayManager.Widget widget = manager.selected();
		if (widget == null) {
			TextView empty = text(context, "Select a widget to edit its appearance.", 11, MUTED);
			panel.addView(empty, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 35)));
			return panel;
		}
		for (String[] entry : new String[][]{{"Background", widget.backgroundVisible ? "Shown" : "Hidden"}, {"Border", widget.borderVisible ? "Rounded" : "None"}, {"Accent", String.format(java.util.Locale.ROOT, "#%06X", widget.accentColor & 0xFFFFFF)}, {"Text", String.format(java.util.Locale.ROOT, "#%06X", widget.textColor & 0xFFFFFF)}, {"Corner Radius", widget.cornerRadius + "px"}, {"Padding", widget.padding + "px"}, {"Alignment", widget.alignment}, {"Grid", manager.gridSpacing() + "px"}}) {
			TextView option = overlaySetting(context, entry[0], entry[1]);
			option.setOnClickListener(view -> { switch (entry[0]) { case "Background" -> manager.toggleSelectedBackground(); case "Border" -> manager.toggleSelectedBorder(); case "Accent" -> manager.cycleSelectedAccent(); case "Text" -> manager.cycleSelectedTextColor(); case "Corner Radius" -> manager.cycleSelectedCornerRadius(); case "Padding" -> manager.cycleSelectedPadding(); case "Alignment" -> manager.cycleSelectedAlignment(); case "Grid" -> manager.cycleGridSpacing(); } transitionVisualCategory(context); });
			panel.addView(option, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 35)));
		}
		TextView remove = overlayButton(context, "Remove selected widget", false);
		remove.setOnClickListener(view -> { manager.removeSelected(); transitionVisualCategory(context); });
		LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 32));
		removeParams.topMargin = px(context, 8);
		panel.addView(remove, removeParams);
		return panel;
	}

	private LinearLayout overlaySubpanel(Context context, String title, String subtitle) {
		LinearLayout panel = new LinearLayout(context);
		panel.setOrientation(LinearLayout.VERTICAL);
		panel.setPadding(px(context, 10), px(context, 9), px(context, 10), px(context, 8));
		panel.setBackground(box(panel, 0x66152231, 0x553B587C, 6));
		TextView heading = text(context, title, 12, TEXT);
		TextView detail = text(context, subtitle, 9, MUTED);
		panel.addView(heading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 19)));
		panel.addView(detail, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 19)));
		return panel;
	}

	private TextView overlaySetting(Context context, String name, String value) {
		TextView option = text(context, name + "\n" + value, 11, TEXT);
		option.setGravity(Gravity.CENTER_VERTICAL);
		option.setPadding(px(context, 10), 0, px(context, 10), 0);
		option.setBackground(box(option, 0x66152231, 0x443B587C, 5));
		KairokkUiSounds.attach(option);
		return option;
	}

	private TextView overlayButton(Context context, String label, boolean selected) {
		TextView button = text(context, label, 11, selected ? TEXT : MUTED);
		button.setGravity(Gravity.CENTER);
		button.setClickable(true);
		button.setBackground(box(button, selected ? 0x86316AC0 : 0x55152231, selected ? BLUE : 0x55415B7C, 5));
		KairokkUiSounds.attach(button);
		return button;
	}

	private void addMobToggleSetting(LinearLayout parent, Context context, String name, String description) {
		LinearLayout row = settingRow(context, name, description);
		FrameLayout toggle = animatedToggle(context, mobSwitches.getOrDefault(name, false), enabled -> mobSwitches.put(name, enabled));
		row.addView(toggle, new LinearLayout.LayoutParams(px(context, 48), px(context, 24)));
		parent.addView(row, settingParams(context));
	}

	private void addMobChoiceSetting(LinearLayout parent, Context context) {
		LinearLayout row = settingRow(context, "Box Style", "Choose the shape of the ESP box.");
		LinearLayout choice = new LinearLayout(context);
		choice.setGravity(Gravity.CENTER_VERTICAL);
		choice.setPadding(px(context, 12), 0, px(context, 8), 0);
		choice.setBackground(box(choice, 0xFF223147, 0x885377A4, 6));
		TextView label = text(context, boxStyle, 13, TEXT);
		label.setGravity(Gravity.CENTER_VERTICAL);
		choice.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
		ShulkIconView arrow = new ShulkIconView(context, ShulkIcon.EXPAND, BLUE_LIGHT, "Choose mob box style");
		arrow.setPadding(px(context, 3), px(context, 9), px(context, 3), px(context, 9));
		choice.addView(arrow, new LinearLayout.LayoutParams(px(context, 20), ViewGroup.LayoutParams.MATCH_PARENT));
		choice.setClickable(true);
		KairokkUiSounds.attach(choice);
		choice.setOnClickListener(view -> showStyleMenu(context, choice, label, arrow));
		row.addView(choice, new LinearLayout.LayoutParams(px(context, 145), px(context, 31)));
		parent.addView(row, settingParams(context));
	}

	private FrameLayout animatedToggle(Context context, boolean initiallyEnabled, java.util.function.Consumer<Boolean> changed) {
		FrameLayout toggle = new FrameLayout(context);
		View thumb = new View(context);
		thumb.setBackground(pill(thumb, 0xFFF5F8FF));
		toggle.addView(thumb, thumbParams(context, initiallyEnabled));
		ShapeDrawable track = pill(toggle, initiallyEnabled ? BLUE : 0xFF435165);
		toggle.setBackground(track);
		int[] trackColor = {initiallyEnabled ? BLUE : 0xFF435165};
		boolean[] enabledState = {initiallyEnabled};
		ValueAnimator[] running = new ValueAnimator[1];
		toggle.setClickable(true);
		KairokkUiSounds.attachSelection(toggle);
		toggle.setOnClickListener(view -> {
			enabledState[0] = !enabledState[0];
			changed.accept(enabledState[0]);
			if (running[0] != null) running[0].cancel();
			int startX = ((FrameLayout.LayoutParams) thumb.getLayoutParams()).leftMargin;
			int endX = px(context, enabledState[0] ? 27 : 3);
			int startColor = trackColor[0];
			int endColor = enabledState[0] ? BLUE : 0xFF435165;
			ValueAnimator animation = ValueAnimator.ofFloat(0F, 1F);
			animation.setDuration(190);
			animation.setInterpolator(new BezierInterpolator(0.2F, 0.75F, 0.25F, 1.0F));
			animation.addUpdateListener(frame -> {
				float progress = ((Number) frame.getAnimatedValue()).floatValue();
				FrameLayout.LayoutParams thumbLayout = (FrameLayout.LayoutParams) thumb.getLayoutParams();
				thumbLayout.leftMargin = Math.round(startX + (endX - startX) * progress);
				thumb.setLayoutParams(thumbLayout);
				trackColor[0] = blendColor(startColor, endColor, progress);
				track.setColor(trackColor[0]);
			});
			running[0] = animation;
			animation.start();
		});
		return toggle;
	}

	private View mobList(Context context) {
		LinearLayout panel = new LinearLayout(context);
		panel.setOrientation(LinearLayout.VERTICAL);
		panel.setPadding(px(context, 15), px(context, 13), px(context, 15), px(context, 13));
		panel.setBackground(box(panel, 0x82101822, 0x663B506E, 7));

		LinearLayout header = new LinearLayout(context);
		header.setGravity(Gravity.CENTER_VERTICAL);
		TextView title = text(context, "│ M O B   L I S T", 14, TEXT);
		header.addView(title, new LinearLayout.LayoutParams(0, px(context, 30), 1));
		TextView include = modeButton(context, "Include", true);
		TextView exclude = modeButton(context, "Exclude", false);
		header.addView(include, new LinearLayout.LayoutParams(px(context, 92), px(context, 31)));
		header.addView(exclude, new LinearLayout.LayoutParams(px(context, 92), px(context, 31)));
		panel.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 32)));

		TextView description = text(context, "Select which mobs to " + (mobIncludeMode ? "highlight." : "hide from ESP."), 11, MUTED);
		description.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		panel.addView(description, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 22)));

		EditText search = new EditText(context);
		search.setHint("Search mobs...");
		search.setSingleLine(true);
		search.setText(mobSearch);
		search.setTextSize(textSize(context, 13));
		search.setTextColor(TEXT);
		search.setHintTextColor(0xFF7587A2);
		search.setPadding(px(context, 12), 0, px(context, 12), 0);
		search.setBackground(box(search, 0xB9152231, 0x77415B7C, 6));
		LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 34));
		searchParams.bottomMargin = px(context, 8);
		panel.addView(search, searchParams);

		LinearLayout lists = new LinearLayout(context);
		lists.setGravity(Gravity.TOP);
		lists.addView(mobColumn(context, "All Mobs", "Hostile", HOSTILE_MOBS), weighted(context, 1, true));
		lists.addView(mobColumn(context, "Animals (Passive)", "Passive", PASSIVE_MOBS), weighted(context, 1, false));
		panel.addView(lists, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

		include.setOnClickListener(view -> {
			if (!mobIncludeMode) { mobIncludeMode = true; transitionVisualCategory(context); }
		});
		exclude.setOnClickListener(view -> {
			if (mobIncludeMode) { mobIncludeMode = false; transitionVisualCategory(context); }
		});
		search.addTextChangedListener(new TextWatcher() {
			@Override public void beforeTextChanged(CharSequence value, int start, int count, int after) { }
			@Override public void onTextChanged(CharSequence value, int start, int before, int count) {
				mobSearch = value.toString();
				lists.removeAllViews();
				lists.addView(mobColumn(context, "All Mobs", "Hostile", HOSTILE_MOBS), weighted(context, 1, true));
				lists.addView(mobColumn(context, "Animals (Passive)", "Passive", PASSIVE_MOBS), weighted(context, 1, false));
			}
			@Override public void afterTextChanged(Editable value) { }
		});
		return panel;
	}

	private static final String[] HOSTILE_MOBS = {"Zombie", "Skeleton", "Creeper", "Spider", "Enderman", "Slime", "Ghast", "Wither Skeleton", "Blaze"};
	private static final String[] PASSIVE_MOBS = {"Cow", "Pig", "Sheep", "Chicken", "Wolf", "Cat", "Horse", "Villager", "Iron Golem"};

	private TextView modeButton(Context context, String label, boolean include) {
		boolean selected = mobIncludeMode == include;
		TextView button = text(context, label, 12, selected ? TEXT : MUTED);
		button.setGravity(Gravity.CENTER);
		button.setClickable(true);
		button.setBackground(box(button, selected ? 0x86316AC0 : 0x55152231, selected ? BLUE : 0x55415B7C, 5));
		KairokkUiSounds.attach(button);
		return button;
	}

	private View mobColumn(Context context, String title, String group, String[] mobs) {
		LinearLayout column = new LinearLayout(context);
		column.setOrientation(LinearLayout.VERTICAL);
		String query = mobSearch.trim().toLowerCase(java.util.Locale.ROOT);
		boolean visible = query.isEmpty() || title.toLowerCase(java.util.Locale.ROOT).contains(query);
		for (String mob : mobs) if (mob.toLowerCase(java.util.Locale.ROOT).contains(query)) visible = true;
		if (visible) column.addView(mobGroupRow(context, title, group, mobs), mobRowParams(context));
		for (String mob : mobs) {
			if (query.isEmpty() || mob.toLowerCase(java.util.Locale.ROOT).contains(query)) column.addView(mobRow(context, mob), mobRowParams(context));
		}
		if (column.getChildCount() == 0) {
			TextView empty = text(context, "No mobs found", 12, MUTED);
			empty.setGravity(Gravity.CENTER);
			column.addView(empty, mobRowParams(context));
		}
		return column;
	}

	private View mobGroupRow(Context context, String title, String group, String[] mobs) {
		LinearLayout row = new LinearLayout(context);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding(px(context, 10), 0, px(context, 9), 0);
		row.setClickable(true);
		row.setBackground(box(row, 0x9B17273A, 0x553B587C, 5));
		MobGlyph glyph = new MobGlyph(context, group.equals("Hostile") ? 0xFF63A85B : 0xFFB18B69, group);
		row.addView(glyph, new LinearLayout.LayoutParams(px(context, 21), px(context, 21)));
		TextView text = text(context, title, 12, TEXT);
		text.setPadding(px(context, 8), 0, 0, 0);
		text.setGravity(Gravity.CENTER_VERTICAL);
		row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
		boolean allEnabled = true;
		for (String mob : mobs) allEnabled &= mobSelections.getOrDefault(mob, false);
		FrameLayout toggle = animatedToggle(context, allEnabled, enabled -> {
			for (String mob : mobs) mobSelections.put(mob, enabled);
			transitionVisualCategory(context);
		});
		row.addView(toggle, new LinearLayout.LayoutParams(px(context, 48), px(context, 24)));
		row.setOnClickListener(view -> {
			boolean enable = !allMobsEnabled(mobs);
			for (String mob : mobs) mobSelections.put(mob, enable);
			transitionVisualCategory(context);
		});
		KairokkUiSounds.attach(row);
		return row;
	}

	private boolean allMobsEnabled(String[] mobs) {
		for (String mob : mobs) if (!mobSelections.getOrDefault(mob, false)) return false;
		return true;
	}

	private View mobRow(Context context, String name) {
		LinearLayout row = new LinearLayout(context);
		row.setGravity(Gravity.CENTER_VERTICAL);
		row.setPadding(px(context, 10), 0, px(context, 9), 0);
		row.setClickable(true);
		row.setBackground(box(row, 0x80152231, 0x443B587C, 5));
		MobGlyph glyph = new MobGlyph(context, colorForMob(name), name);
		row.addView(glyph, new LinearLayout.LayoutParams(px(context, 21), px(context, 21)));
		TextView label = text(context, name, 12, TEXT);
		label.setPadding(px(context, 8), 0, 0, 0);
		label.setGravity(Gravity.CENTER_VERTICAL);
		row.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
		FrameLayout toggle = animatedToggle(context, mobSelections.getOrDefault(name, true), enabled -> {
			mobSelections.put(name, enabled);
			transitionVisualCategory(context);
		});
		row.addView(toggle, new LinearLayout.LayoutParams(px(context, 48), px(context, 24)));
		row.setOnClickListener(view -> {
			mobSelections.put(name, !mobSelections.getOrDefault(name, true));
			transitionVisualCategory(context);
		});
		KairokkUiSounds.attach(row);
		return row;
	}

	private static LinearLayout.LayoutParams mobRowParams(Context context) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 30));
		params.bottomMargin = px(context, 3);
		return params;
	}

	private static int colorForMob(String name) {
		for (int i = 0; i < MOB_NAMES.length; i++) if (MOB_NAMES[i].equals(name)) return MOB_COLORS[i];
		return MUTED;
	}

	private void transitionVisualCategory(Context context) {
		if (visualColumns == null || contentTransitionRunning) return;
		contentTransitionRunning = true;
		if (!KairokkCustomizationState.moduleAnimations()) {
			showVisualCategory(context);
			contentTransitionRunning = false;
			return;
		}
		KairokkScreenTransitions.crossFade(visualColumns, () -> {
			showVisualCategory(context);
			KairokkScreenTransitions.fadeContentIn(visualColumns);
			contentTransitionRunning = false;
		});
	}

	private View playerOptions(Context context) {
		LinearLayout panel = new LinearLayout(context);
		panel.setOrientation(LinearLayout.VERTICAL);
		panel.setPadding(px(context, 16), px(context, 10), px(context, 16), px(context, 10));
		panel.setBackground(box(panel, 0x82101822, 0x663B506E, 7));
		addToggleSetting(panel, context, "Player ESP", "Show players through walls.");
		addChoiceSetting(panel, context, "Box Style", "Choose the shape of the ESP box.", "Corner");
		addColorSetting(panel, context, "Box Color", "Set the color of player boxes.");
		addToggleSetting(panel, context, "Name Tags", "Display player names.");
		addToggleSetting(panel, context, "Distance", "Show distance to players.");
		addToggleSetting(panel, context, "Health", "Show player health.");
		addToggleSetting(panel, context, "Tracers", "Draw lines to players.");
		addToggleSetting(panel, context, "Chams", "Render players with a solid material.");
		return panel;
	}

	private void addToggleSetting(LinearLayout parent, Context context, String name, String description) {
		LinearLayout row = settingRow(context, name, description);
		FrameLayout toggle = new FrameLayout(context);
		View thumb = new View(context);
		thumb.setBackground(pill(thumb, 0xFFF5F8FF));
		boolean initiallyEnabled = switches.getOrDefault(name, false);
		toggle.addView(thumb, thumbParams(context, initiallyEnabled));
		ShapeDrawable track = pill(toggle, initiallyEnabled ? BLUE : 0xFF435165);
		toggle.setBackground(track);
		int[] trackColor = {initiallyEnabled ? BLUE : 0xFF435165};
		ValueAnimator[] running = new ValueAnimator[1];
		toggle.setClickable(true);
		KairokkUiSounds.attachSelection(toggle);
		toggle.setOnClickListener(view -> {
			boolean enabled = !switches.getOrDefault(name, false);
			switches.put(name, enabled);
			if (running[0] != null) running[0].cancel();
			int startX = ((FrameLayout.LayoutParams) thumb.getLayoutParams()).leftMargin;
			int endX = px(context, enabled ? 27 : 3);
			int startColor = trackColor[0];
			int endColor = enabled ? BLUE : 0xFF435165;
			ValueAnimator animation = ValueAnimator.ofFloat(0.0F, 1.0F);
			animation.setDuration(190);
			animation.setInterpolator(new BezierInterpolator(0.2F, 0.75F, 0.25F, 1.0F));
			animation.addUpdateListener(frame -> {
				float progress = ((Number) frame.getAnimatedValue()).floatValue();
				FrameLayout.LayoutParams thumbLayout = (FrameLayout.LayoutParams) thumb.getLayoutParams();
				thumbLayout.leftMargin = Math.round(startX + (endX - startX) * progress);
				thumb.setLayoutParams(thumbLayout);
				trackColor[0] = blendColor(startColor, endColor, progress);
				track.setColor(trackColor[0]);
				toggle.invalidate();
			});
			running[0] = animation;
			animation.start();
			updatePreview();
		});
		row.addView(toggle, new LinearLayout.LayoutParams(px(context, 48), px(context, 24)));
		parent.addView(row, settingParams(context));
	}

	private FrameLayout.LayoutParams thumbParams(Context context, boolean enabled) {
		FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(px(context, 18), px(context, 18), Gravity.CENTER_VERTICAL);
		params.leftMargin = px(context, enabled ? 27 : 3);
		return params;
	}

	private static int blendColor(int from, int to, float progress) {
		int red = Math.round(((from >> 16) & 255) * (1 - progress) + ((to >> 16) & 255) * progress);
		int green = Math.round(((from >> 8) & 255) * (1 - progress) + ((to >> 8) & 255) * progress);
		int blue = Math.round((from & 255) * (1 - progress) + (to & 255) * progress);
		return 0xFF000000 | red << 16 | green << 8 | blue;
	}

	private void addChoiceSetting(LinearLayout parent, Context context, String name, String description, String value) {
		LinearLayout row = settingRow(context, name, description);
		LinearLayout choice = new LinearLayout(context);
		choice.setGravity(Gravity.CENTER_VERTICAL);
		choice.setPadding(px(context, 12), 0, px(context, 8), 0);
		choice.setBackground(box(choice, 0xFF223147, 0x885377A4, 6));
		TextView label = text(context, value, 13, TEXT);
		label.setGravity(Gravity.CENTER_VERTICAL);
		choice.addView(label, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
		ShulkIconView arrow = new ShulkIconView(context, ShulkIcon.EXPAND, BLUE_LIGHT, "Choose box style");
		arrow.setPadding(px(context, 3), px(context, 9), px(context, 3), px(context, 9));
		choice.addView(arrow, new LinearLayout.LayoutParams(px(context, 20), ViewGroup.LayoutParams.MATCH_PARENT));
		choice.setClickable(true);
		choice.setOnClickListener(view -> showStyleMenu(context, choice, label, arrow));
		row.addView(choice, new LinearLayout.LayoutParams(px(context, 145), px(context, 31)));
		parent.addView(row, settingParams(context));
	}

	private void showStyleMenu(Context context, LinearLayout anchor, TextView label, ShulkIconView arrow) {
		KairokkUiSounds.play(KairokkUiSounds.OPEN);
		LinearLayout menu = new LinearLayout(context);
		menu.setOrientation(LinearLayout.VERTICAL);
		menu.setPadding(px(context, 5), px(context, 5), px(context, 5), px(context, 5));
		menu.setBackground(box(menu, 0xF7223044, 0xFF5882BC, 7));
		PopupWindow popup = new PopupWindow(menu, px(context, 145), px(context, 119), true);
		popup.setOutsideTouchable(true);
		popup.setElevation(px(context, 8));
		anchor.setBackground(box(anchor, 0xFF2A4568, BLUE, 6));
		ValueAnimator arrowOpen = ValueAnimator.ofFloat(arrow.getRotation(), 180.0F);
		arrowOpen.setDuration(180);
		arrowOpen.setInterpolator(new BezierInterpolator(0.2F, 0.75F, 0.25F, 1.0F));
		arrowOpen.addUpdateListener(frame -> arrow.setRotation(((Number) frame.getAnimatedValue()).floatValue()));
		arrowOpen.start();
		popup.setOnDismissListener(() -> {
			anchor.setBackground(box(anchor, 0xFF223147, 0x885377A4, 6));
			ValueAnimator arrowClose = ValueAnimator.ofFloat(arrow.getRotation(), 0.0F);
			arrowClose.setDuration(160);
			arrowClose.addUpdateListener(frame -> arrow.setRotation(((Number) frame.getAnimatedValue()).floatValue()));
			arrowClose.start();
		});
		for (String style : new String[]{"Corner", "Full", "None"}) {
			TextView item = text(context, style + (style.equals(boxStyle) ? "   ✓" : ""), 13, style.equals(boxStyle) ? BLUE_LIGHT : TEXT);
			item.setGravity(Gravity.CENTER_VERTICAL);
			item.setPadding(px(context, 12), 0, 0, 0);
			item.setBackground(box(item, style.equals(boxStyle) ? 0x704A94FF : 0x00223044, style.equals(boxStyle) ? 0x774A94FF : 0x00000000, 5));
			item.setClickable(true);
			KairokkUiSounds.attach(item);
			item.setOnClickListener(view -> {
				boxStyle = style;
				label.setText(style);
				updatePreview();
				ValueAnimator close = ValueAnimator.ofFloat(1.0F, 0.0F);
				close.setDuration(120);
				close.addUpdateListener(frame -> {
					float progress = ((Number) frame.getAnimatedValue()).floatValue();
					menu.setAlpha(progress);
					menu.setScaleY(0.88F + progress * 0.12F);
					if (progress <= 0.01F) popup.dismiss();
				});
				close.start();
			});
			menu.addView(item, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 36)));
		}
		menu.setPivotY(0);
		menu.setAlpha(0.0F);
		menu.setScaleY(0.88F);
		popup.showAsDropDown(anchor, 0, px(context, 5));
		ValueAnimator open = ValueAnimator.ofFloat(0.0F, 1.0F);
		open.setDuration(180);
		open.setInterpolator(new BezierInterpolator(0.2F, 0.75F, 0.25F, 1.0F));
		open.addUpdateListener(frame -> {
			float progress = ((Number) frame.getAnimatedValue()).floatValue();
			menu.setAlpha(progress);
			menu.setScaleY(0.88F + progress * 0.12F);
		});
		open.start();
	}

	private void addColorSetting(LinearLayout parent, Context context, String name, String description) {
		LinearLayout row = settingRow(context, name, description);
		View swatch = new View(context);
		swatch.setBackground(colorSwatch(swatch, boxColor));
		swatch.setClickable(true);
		KairokkUiSounds.attach(swatch);
		swatch.setOnClickListener(view -> showColorPicker(context, swatch));
		row.addView(swatch, new LinearLayout.LayoutParams(px(context, 27), px(context, 27)));
		parent.addView(row, settingParams(context));
	}

	private static ShapeDrawable colorSwatch(View view, int color) {
		// Swatches are the value being previewed; do not route their fill through
		// the panel tint used by normal UI surfaces.
		ShapeDrawable drawable = new ShapeDrawable();
		drawable.setShape(ShapeDrawable.RECTANGLE);
		drawable.setColor(color);
		drawable.setCornerRadius(px(view.getContext(), 5));
		drawable.setStroke(px(view.getContext(), 1), BLUE_LIGHT);
		return drawable;
	}

	private void showColorPicker(Context context, View anchor) {
		KairokkUiSounds.play(KairokkUiSounds.OPEN);
		int originalColor = boxColor;
		float[] hsv = hsv(boxColor);
		FrameLayout card = new FrameLayout(context);
		card.setPadding(px(context, 14), px(context, 12), px(context, 14), px(context, 12));
		card.setBackground(box(card, 0xF51A2738, 0xFF5277A6, 9));

		LinearLayout content = new LinearLayout(context);
		content.setOrientation(LinearLayout.VERTICAL);
		card.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		LinearLayout header = new LinearLayout(context);
		header.setGravity(Gravity.CENTER_VERTICAL);
		TextView title = text(context, "BOX COLOR", 13, TEXT);
		title.setText("B O X   C O L O R");
		header.addView(title, new LinearLayout.LayoutParams(0, px(context, 27), 1));
		TextView hex = text(context, hex(boxColor), 12, BLUE_LIGHT);
		hex.setGravity(Gravity.CENTER_VERTICAL | Gravity.RIGHT);
		header.addView(hex, new LinearLayout.LayoutParams(px(context, 70), px(context, 27)));
		content.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 27)));

		HueStrip hueStrip = new HueStrip(context, hsv[0]);
		LinearLayout.LayoutParams hueParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 15));
		hueParams.topMargin = px(context, 8);
		content.addView(hueStrip, hueParams);
		ColorField field = new ColorField(context, hsv[0], hsv[1], hsv[2]);
		LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 128));
		fieldParams.topMargin = px(context, 8);
		content.addView(field, fieldParams);

		final PopupWindow[] popupRef = {null};
		final boolean[] committed = {false};
		PickerActions actions = new PickerActions(context,
			() -> {
				field.setColor(originalColor);
				hueStrip.setHue(field.getHue());
				applyPickerColor(originalColor, anchor, hex);
			},
			() -> {
				committed[0] = true;
				boxColor = field.getColor();
				anchor.setBackground(colorSwatch(anchor, boxColor));
				updatePreview();
				if (popupRef[0] != null) popupRef[0].dismiss();
			});
		LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 32));
		actionsParams.topMargin = px(context, 11);
		content.addView(actions, actionsParams);

		PopupWindow popup = new PopupWindow(card, px(context, 320), px(context, 280), true);
		popupRef[0] = popup;
		popup.setOutsideTouchable(true);
		popup.setElevation(px(context, 10));
		field.setOnColorChanged(color -> applyPickerColor(color, anchor, hex));
		hueStrip.setOnHueChanged(hueValue -> {
			field.setHue(hueValue);
			applyPickerColor(field.getColor(), anchor, hex);
		});
		popup.setOnDismissListener(() -> {
			if (!committed[0]) {
				boxColor = originalColor;
				anchor.setBackground(colorSwatch(anchor, boxColor));
				updatePreview();
			}
		});
		card.setPivotY(0);
		card.setAlpha(0F);
		card.setScaleY(.92F);
		popup.showAtLocation(anchor, Gravity.CENTER, 0, 0);
		ValueAnimator open = ValueAnimator.ofFloat(0F, 1F);
		open.setDuration(190);
		open.setInterpolator(new BezierInterpolator(.2F, .75F, .25F, 1F));
		open.addUpdateListener(frame -> {
			float progress = ((Number) frame.getAnimatedValue()).floatValue();
			card.setAlpha(progress);
			card.setScaleY(.92F + .08F * progress);
		});
		open.start();
	}

	private void applyPickerColor(int color, View anchor, TextView hex) {
		boxColor = color;
		anchor.setBackground(colorSwatch(anchor, color));
		hex.setText(hex(color));
		updatePreview();
	}

	private static float[] hsv(int color) {
		float[] hsv = new float[3];
		Color.RGBToHSV((color >> 16) & 255, (color >> 8) & 255, color & 255, hsv);
		return hsv;
	}

	private static String hex(int color) {
		return String.format(java.util.Locale.ROOT, "#%02X%02X%02X", (color >> 16) & 255, (color >> 8) & 255, color & 255);
	}

	/** Lightweight Minecraft-style pixel sprite used in the mob picker. */
	private static final class MobGlyph extends View {
		private final Paint paint = new Paint();
		private final int color;
		private final String name;

		private MobGlyph(Context context, int color, String name) {
			super(context);
			this.color = color;
			this.name = name;
			setWillNotDraw(false);
			setTooltipText(name);
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			float cell = Math.max(1F, Math.min(getWidth(), getHeight()) / 5F);
			float left = (getWidth() - cell * 5F) / 2F;
			float top = (getHeight() - cell * 5F) / 2F;
			paint.setStyle(Paint.FILL);
			paint.setColor(color);
			// A tiny, intentionally pixelated head silhouette with eyes; it reads as a
			// Minecraft mob thumbnail without loading extra textures into the UI.
			for (int y = 0; y < 5; y++) {
				for (int x = 0; x < 5; x++) {
					if (x == 0 && (y == 0 || y == 4) || x == 4 && (y == 0 || y == 4)) continue;
					canvas.drawRect(left + x * cell, top + y * cell, left + (x + 1) * cell, top + (y + 1) * cell, paint);
				}
			}
			paint.setColor(0xFF182333);
			canvas.drawRect(left + cell, top + 2 * cell, left + 2 * cell, top + 3 * cell, paint);
			canvas.drawRect(left + 3 * cell, top + 2 * cell, left + 4 * cell, top + 3 * cell, paint);
			paint.setStyle(Paint.STROKE);
			paint.setStrokeWidth(Math.max(1F, cell));
			paint.setColor(0x99FFFFFF);
			canvas.drawRect(left, top, left + cell * 5F, top + cell * 5F, paint);
			paint.setStyle(Paint.FILL);
		}
	}

	/** Lightweight item ESP scene used to preview the selected settings without a second renderer. */
	private static final class ItemPreview extends View {
		private final Paint paint = new Paint();
		private final FontPaint fontPaint = new FontPaint();
		private static final String[] NAMES = {"Cobblestone", "Emerald", "Diamond", "Enchanted Book", "Netherite Ingot", "Dragon Egg", "Experience Orb"};
		private static final String[] RARITIES = {"Common", "Uncommon", "Rare", "Epic", "Legendary", "Mythic", "Mythic"};
		private static final float[] POSITIONS = {.08F, .23F, .38F, .53F, .68F, .82F, .93F};

		private ItemPreview(Context context) {
			super(context);
			setWillNotDraw(false);
			setTooltipText("Item ESP preview");
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			float scale = Math.max(.45F, Math.min(1.6F, Math.min(getWidth() / 760F, getHeight() / 250F)));
			float baseY = getHeight() - px(getContext(), 29);
			if (!KairokkItemEspState.itemEsp()) {
				drawText(canvas, "ITEM ESP DISABLED", getWidth() / 2F, getHeight() / 2F, px(getContext(), 14), KairokkCustomizationState.primary(), true);
				return;
			}
			for (int i = 0; i < NAMES.length; i++) {
				String name = NAMES[i];
				String rarity = RARITIES[i];
				if (!KairokkItemEspState.rarityVisible(rarity)) continue;
				if ("Experience Orb".equals(name) && !KairokkItemEspState.showXpOrbs()) continue;
				if ("Dragon Egg".equals(name) && !KairokkItemEspState.showOtherEntities()) continue;
				float x = Math.max(px(getContext(), 28), Math.min(getWidth() - px(getContext(), 28), POSITIONS[i] * getWidth()));
				float y = baseY - (i % 2) * px(getContext(), 22);
				int color = KairokkItemEspState.color(rarity.toLowerCase(java.util.Locale.ROOT));
				if (KairokkItemEspState.itemGlow()) {
					paint.setStyle(Paint.FILL);
					paint.setColor(withAlpha(color, 12 + KairokkItemEspState.glowStrength() / 4));
					canvas.drawCircle(x, y, px(getContext(), 25) * scale, paint);
				}
				if (KairokkItemEspState.itemTracers()) drawTracer(canvas, getWidth() / 2F, getHeight(), x, y, scale);
				if (KairokkItemEspState.itemBoxes()) drawBox(canvas, x, y, color, scale);
				if (KairokkItemEspState.itemIcon()) drawIcon(canvas, x, y, color, scale);
				if (KairokkItemEspState.itemNames()) drawText(canvas, name, x, y - px(getContext(), 41) * scale, px(getContext(), 11) * scale, KairokkItemEspState.color("text"), true);
				if (KairokkItemEspState.itemDistance()) drawText(canvas, (6 + i * 3) + "m", x, y - px(getContext(), 28) * scale, px(getContext(), 9) * scale, color, true);
			}
		}

		private void drawBox(Canvas canvas, float x, float y, int color, float scale) {
			float halfWidth = px(getContext(), 25) * scale;
			float halfHeight = px(getContext(), 27) * scale;
			float left = x - halfWidth;
			float top = y - halfHeight;
			float right = x + halfWidth;
			float bottom = y + halfHeight;
			paint.setStyle(Paint.STROKE);
			paint.setStrokeWidth(Math.max(1F, KairokkItemEspState.lineWidth() / 10F * scale));
			paint.setColor(color);
			if ("Full".equals(KairokkItemEspState.boxStyle())) {
				canvas.drawRoundRect(left, top, right, bottom, px(getContext(), KairokkItemEspState.cornerRadius()) * scale, paint);
			} else if ("Cornered".equals(KairokkItemEspState.boxStyle())) {
				float length = Math.max(px(getContext(), 7), px(getContext(), 8 + KairokkItemEspState.cornerRadius() * 2)) * scale;
				canvas.drawLine(left, top, left + length, top, paint);
				canvas.drawLine(left, top, left, top + length, paint);
				canvas.drawLine(right - length, top, right, top, paint);
				canvas.drawLine(right, top, right, top + length, paint);
				canvas.drawLine(left, bottom - length, left, bottom, paint);
				canvas.drawLine(left, bottom, left + length, bottom, paint);
				canvas.drawLine(right - length, bottom, right, bottom, paint);
				canvas.drawLine(right, bottom - length, right, bottom, paint);
			}
			paint.setStyle(Paint.FILL);
		}

		private void drawIcon(Canvas canvas, float x, float y, int color, float scale) {
			float half = px(getContext(), 10) * scale;
			paint.setStyle(Paint.FILL);
			paint.setColor(withAlpha(color, 220));
			canvas.drawRoundRect(x - half, y - half, x + half, y + half, px(getContext(), 3) * scale, paint);
			paint.setColor(withAlpha(0xFFFFFFFF, 85));
			canvas.drawRect(x - half / 2F, y - half / 2F, x + half / 3F, y - half / 5F, paint);
			paint.setColor(withAlpha(0xFF0A1220, 125));
			canvas.drawRect(x - half / 2F, y + half / 4F, x + half / 2F, y + half / 2F, paint);
		}

		private void drawTracer(Canvas canvas, float startX, float startY, float endX, float endY, float scale) {
			paint.setStyle(Paint.STROKE);
			paint.setStrokeWidth(Math.max(1F, KairokkItemEspState.tracerThickness() * scale));
			paint.setColor(KairokkItemEspState.color("tracer"));
			if ("Solid".equals(KairokkItemEspState.tracerStyle())) {
				canvas.drawLine(startX, startY, endX, endY, paint);
			} else {
				float dx = endX - startX;
				float dy = endY - startY;
				float distance = (float) Math.sqrt(dx * dx + dy * dy);
				float step = "Dotted".equals(KairokkItemEspState.tracerStyle()) ? px(getContext(), 8) : px(getContext(), 13);
				for (float position = 0; position < distance; position += step) {
					float progress = position / Math.max(1F, distance);
					float next = Math.min(distance, position + ("Dotted".equals(KairokkItemEspState.tracerStyle()) ? 1F : step * .55F));
					float nextProgress = next / Math.max(1F, distance);
					canvas.drawLine(startX + dx * progress, startY + dy * progress, startX + dx * nextProgress, startY + dy * nextProgress, paint);
				}
			}
			paint.setStyle(Paint.FILL);
		}

		private void drawText(Canvas canvas, String value, float x, float y, float size, int color, boolean centered) {
			paint.setStyle(Paint.FILL);
			paint.setColor(color);
			fontPaint.setFontSize(Math.max(1F, size));
			fontPaint.setAntiAlias(true);
			float drawX = centered ? x - value.length() * size * .29F : x;
			canvas.drawTextRun(value, 0, value.length(), 0, value.length(), drawX, y, false, paint, fontPaint);
		}

		private static int withAlpha(int color, int alpha) {
			return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
		}
	}

	/** Anti-aliased miniature route scene; uses the active route, or a labelled demo. */
	private static final class PathViewport extends View {
		private final Paint paint=new Paint(); private final FontPaint font=new FontPaint();
		private final long started=System.nanoTime();
		PathViewport(Context context){super(context);setWillNotDraw(false);font.setFont(customTypeface());font.setLocale(java.util.Locale.ROOT);font.setAntiAlias(true);}
		@Override protected void onDraw(Canvas canvas){
			super.onDraw(canvas);int save=canvas.save();
			try{
				float w=getWidth(),h=getHeight();canvas.clipRect(0,0,w,h);paint.setShader(null);paint.setAntiAlias(true);paint.setStyle(Paint.FILL);paint.setColor(0xFF091421);canvas.drawRect(0,0,w,h,paint);
				for(int i=-6;i<=6;i++)line(canvas,new float[]{i,-.5f,2},new float[]{i,-.5f,18},0x304F6D91,.6f);
				for(int i=2;i<19;i++)line(canvas,new float[]{-6,-.5f,i},new float[]{6,-.5f,i},0x304F6D91,.6f);
				var route=com.kairokk.client.pathfinder.pathfinding.NavigationManager.getInstance().currentPath();
				java.util.List<float[]> nodes=new java.util.ArrayList<>();
				if(route!=null && route.points().size()>1){
					var first=route.points().getFirst().position();double span=1;for(var p:route.points())span=Math.max(span,p.position().distanceTo(first));
					for(var p:route.points()){var d=p.position().subtract(first);nodes.add(new float[]{(float)(d.x/span*5-2),(float)(d.y/span*3),(float)(d.z/span*7+7)});}
				}else{nodes.add(new float[]{-2,0,4});nodes.add(new float[]{-.9f,0,6});nodes.add(new float[]{.5f,.55f,8});nodes.add(new float[]{-.2f,.55f,10});nodes.add(new float[]{1.9f,.9f,12});}
				boolean lines=!"Nodes Only".equals(PathfinderOptions.renderMode)&&!"Hidden".equals(PathfinderOptions.renderMode), showNodes=!"Line Only".equals(PathfinderOptions.renderMode)&&!"Hidden".equals(PathfinderOptions.renderMode);
				for(int i=1;i<nodes.size();i++){float[] a=nodes.get(i-1),b=nodes.get(i);if(lines)for(int j=0;j<32;j++){
					if("Dashed".equals(PathfinderOptions.lineStyle)&&j%8>=5||"Dotted".equals(PathfinderOptions.lineStyle)&&j%4!=0)continue;
					float[] from=mix(a,b,j/32f),to=mix(a,b,(j+1)/32f);
					if(PathfinderOptions.glow)line(canvas,from,to,(PathfinderOptions.pathColor&0xFFFFFF)|0x35000000,PathfinderOptions.lineWidth+4);
					line(canvas,from,to,PathfinderOptions.pathColor,PathfinderOptions.lineWidth);
				}}
				if(showNodes)for(int i=0;i<nodes.size();i++)cube(canvas,nodes.get(i),i==nodes.size()-1?PathfinderOptions.targetColor:PathfinderOptions.nodeColor,.16f*PathfinderOptions.nodeSize);
				float t=((System.nanoTime()-started)/1_000_000_000f*.35f)%(nodes.size()-1);int index=(int)t;float[] cursor=screen(mix(nodes.get(index),nodes.get(index+1),t-index));paint.setColor(0xFFFFFFFF);paint.setStyle(Paint.FILL);canvas.drawCircle(cursor[0],cursor[1],px(getContext(),3),paint);
				String label=route==null?"LIVE STYLE PREVIEW · DEMO ROUTE":"LIVE STYLE PREVIEW · CURRENT ROUTE";
				font.setFontSize(px(getContext(),8));paint.setColor(0xFF9BBBDD);canvas.drawTextRun(label,0,label.length(),0,label.length(),px(getContext(),8),h-px(getContext(),8),false,paint,font);
			}finally{canvas.restoreToCount(save);if(isShown())postInvalidateOnAnimation();}
		}
		private float[] screen(float[] p){float depth=p[2]+2;return new float[]{getWidth()*.5f+(p[0]-.4f)*getWidth()*.9f/depth,getHeight()*.65f-(p[1]+.25f)*getHeight()*1.6f/depth-(p[2]-4)*getHeight()*.035f};}
		private static float[] mix(float[] a,float[] b,float t){return new float[]{a[0]+(b[0]-a[0])*t,a[1]+(b[1]-a[1])*t,a[2]+(b[2]-a[2])*t};}
		private void line(Canvas c,float[] a,float[] b,int color,float width){float[] x=screen(a),y=screen(b);paint.setStyle(Paint.STROKE);paint.setColor(color);paint.setStrokeWidth(px(getContext(),width));c.drawLine(x[0],x[1],y[0],y[1],paint);}
		private void cube(Canvas c,float[] p,int color,float size){if("None".equals(PathfinderOptions.nodeStyle))return;float[] s=screen(p);if("Diamond".equals(PathfinderOptions.nodeStyle)){float r=px(getContext(),5)*PathfinderOptions.nodeSize;paint.setColor(color);paint.setStrokeWidth(px(getContext(),1));c.drawLine(s[0],s[1]-r,s[0]+r,s[1],paint);c.drawLine(s[0]+r,s[1],s[0],s[1]+r,paint);c.drawLine(s[0],s[1]+r,s[0]-r,s[1],paint);c.drawLine(s[0]-r,s[1],s[0],s[1]-r,paint);return;}
			float[][] v=new float[8][];for(int i=0;i<8;i++)v[i]=new float[]{p[0]+((i&1)==0?-size:size),p[1]+((i&2)==0?-size:size),p[2]+((i&4)==0?-size:size)};
			for(int i=0;i<8;i++)for(int bit=1;bit<=4;bit*=2)if((i&bit)==0)line(c,v[i],v[i|bit],color,1);
		}
	}

	/** Perspective-projected miniature 3D scene driven by the real rotation controller. */
	private static final class RotationViewport extends View {
		private static final Vec3D BLUE_POS = new Vec3D(-2.7f, 0f, 8.2f);
		private static final Vec3D ORANGE_POS = new Vec3D(2.4f, -.05f, 5.1f);
		private static final int[][] FACES = {{0,1,2,3},{4,7,6,5},{0,4,5,1},{3,2,6,7},{1,5,6,2},{0,3,7,4}};
		private static final int[][] EDGES = {{0,1},{1,2},{2,3},{3,0},{4,5},{5,6},{6,7},{7,4},{0,4},{1,5},{2,6},{3,7}};
		private final Paint paint = new Paint();
		private final FontPaint fontPaint = new FontPaint();
		private final RotationController controller = new RotationController();
		private float yaw = -18f, pitch = -2f, dwell, angularSpeed;
		private boolean blueTarget = true;
		private long lastFrame;

		private RotationViewport(Context context) {
			super(context);
			setWillNotDraw(false);
			// Canvas text does not inherit a TextView typeface. Without an explicit
			// collection ArcCanvas aborts the frame before the animation is queued.
			fontPaint.setFont(customTypeface());
			fontPaint.setLocale(java.util.Locale.ROOT);
		}

		@Override protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			float w = getWidth(), h = getHeight(); if (w < 20 || h < 20) return;
			int save = canvas.save();
			try {
				canvas.clipRect(0, 0, w, h);
				paint.setAntiAlias(true); paint.setShader(null); paint.setStyle(Paint.FILL); paint.setColor(0xFF10151F);
				canvas.drawRect(0, 0, w, h, paint);
				long now = System.nanoTime(); float dt = lastFrame == 0 ? 1f / 60f : Math.min(.05f, (now - lastFrame) / 1_000_000_000f); lastFrame = now;
				Vec3D active = blueTarget ? BLUE_POS : ORANGE_POS;
				float targetYaw = (float) Math.toDegrees(Math.atan2(active.x, active.z));
				float targetPitch = (float) -Math.toDegrees(Math.atan2(active.y - .45f, Math.hypot(active.x, active.z)));
				RotationController.Result next = controller.update(yaw, pitch, targetYaw, targetPitch, dt);
				yaw = next.yaw(); pitch = next.pitch(); angularSpeed = (float) Math.hypot(next.yawVelocity(), next.pitchVelocity());
				if (controller.isComplete() && Math.hypot(RotationController.wrap(targetYaw - yaw), targetPitch - pitch) <= Math.max(.01f, controller.aimTolerance())) {
					dwell += dt; float wait = PathFinderSettings.minTime + (PathFinderSettings.maxTime - PathFinderSettings.minTime) * .65f;
					if (dwell >= Math.max(.08f, wait)) { blueTarget = !blueTarget; dwell = 0; controller.reset(); }
				} else dwell = 0;

				drawGrid(canvas, w, h);
				drawPlannedPath(canvas, w, h, blueTarget ? 0xFF70AEFF : 0xFFFF9A6B);
				drawCube(canvas, BLUE_POS, 1.25f, 0xFF4A94FF, blueTarget, w, h);
				drawCube(canvas, ORANGE_POS, 1.55f, 0xFFC96A3D, !blueTarget, w, h);
				float cross = px(getContext(), 7); paint.setColor(0xFFE7EDF7); paint.setStrokeWidth(px(getContext(), 1.2f));
				canvas.drawLine(w/2-cross, h/2, w/2+cross, h/2, paint); canvas.drawLine(w/2, h/2-cross, w/2, h/2+cross, paint);
				paint.setStyle(Paint.FILL);
				String status = controller.isResettingMouse() ? "   •  LIFTING MOUSE" : "";
				drawText(canvas, "Yaw %.1f°   Pitch %.1f°   Speed %.0f°/s%s".formatted(yaw, pitch, angularSpeed, status), px(getContext(), 9), h-px(getContext(), 9), px(getContext(), 9), controller.isResettingMouse() ? 0xFFFFC36B : 0xFFA8B4C8, false);
				drawText(canvas, "Target: " + (blueTarget ? "BLUE" : "ORANGE"), w-px(getContext(), 110), h-px(getContext(), 9), px(getContext(), 9), blueTarget ? 0xFF70AEFF : 0xFFFF9A6B, false);
			} finally {
				canvas.restoreToCount(save);
				if (isShown()) postInvalidateOnAnimation();
			}
		}

		private void drawPlannedPath(Canvas canvas, float w, float h, int color) {
			java.util.List<RotationController.PathSample> samples = controller.samplePlannedPath(48);
			if (samples.isEmpty()) return;
			Projected previous = project(directionPoint(yaw, pitch, 6f), w, h);
			paint.setStyle(Paint.STROKE);
			for (int pass = 0; pass < 2; pass++) {
				paint.setStrokeWidth(px(getContext(), pass == 0 ? 2.3f : .85f));
				paint.setColor(pass == 0 ? ((color & 0x00FFFFFF) | 0x25000000) : ((color & 0x00FFFFFF) | 0xD8000000));
				RotationController.PathSample start = samples.getFirst();
				previous = project(directionPoint(start.yaw(), start.pitch(), 6f), w, h);
				for (RotationController.PathSample sample : samples) {
					Projected point = project(directionPoint(sample.yaw(), sample.pitch(), 6f), w, h);
					if (previous.visible && point.visible) canvas.drawLine(previous.x, previous.y, point.x, point.y, paint);
					previous = point;
				}
			}
			if (previous.visible) {
				paint.setStyle(Paint.FILL); paint.setColor(color);
				canvas.drawCircle(previous.x, previous.y, px(getContext(), 2.2f), paint);
			}
		}

		private Vec3D directionPoint(float directionYaw, float directionPitch, float distance) {
			double yawRadians = Math.toRadians(directionYaw), pitchRadians = Math.toRadians(directionPitch);
			float horizontal = (float) (Math.cos(pitchRadians) * distance);
			return new Vec3D((float) Math.sin(yawRadians) * horizontal,
					.45f - (float) Math.sin(pitchRadians) * distance,
					(float) Math.cos(yawRadians) * horizontal);
		}

		private void drawGrid(Canvas canvas, float w, float h) {
			paint.setStyle(Paint.STROKE); paint.setStrokeWidth(Math.max(.6f, px(getContext(), .45f)));
			for (int x=-12; x<=12; x++) drawWorldLine(canvas, new Vec3D(x,-1.25f,1.3f), new Vec3D(x,-1.25f,22f), 0x355A6E8D, w,h);
			for (int z=2; z<=22; z++) { int alpha=Math.max(12, 62-z*2); drawWorldLine(canvas,new Vec3D(-12,-1.25f,z),new Vec3D(12,-1.25f,z),(alpha<<24)|0x005A6E8D,w,h); }
		}

		private void drawWorldLine(Canvas canvas, Vec3D a, Vec3D b, int color, float w, float h) {
			Projected pa=project(a,w,h), pb=project(b,w,h); if(!pa.visible||!pb.visible)return; paint.setColor(color); canvas.drawLine(pa.x,pa.y,pb.x,pb.y,paint);
		}

		private void drawCube(Canvas canvas, Vec3D center, float size, int base, boolean active, float w, float h) {
			float s=size/2f; Vec3D[] v={new Vec3D(center.x-s,center.y-s,center.z-s),new Vec3D(center.x+s,center.y-s,center.z-s),new Vec3D(center.x+s,center.y+s,center.z-s),new Vec3D(center.x-s,center.y+s,center.z-s),new Vec3D(center.x-s,center.y-s,center.z+s),new Vec3D(center.x+s,center.y-s,center.z+s),new Vec3D(center.x+s,center.y+s,center.z+s),new Vec3D(center.x-s,center.y+s,center.z+s)};
			Projected[] p=new Projected[8]; for(int i=0;i<8;i++)p[i]=project(v[i],w,h);
			java.util.List<Face> faces=new java.util.ArrayList<>();
			for(int i=0;i<FACES.length;i++){int[] f=FACES[i]; if(p[f[0]].visible&&p[f[1]].visible&&p[f[2]].visible&&p[f[3]].visible) faces.add(new Face(f,(p[f[0]].depth+p[f[1]].depth+p[f[2]].depth+p[f[3]].depth)/4f,i));}
			faces.sort((a,b)->Float.compare(b.depth,a.depth)); paint.setStyle(Paint.FILL); paint.setColor(0xFFFFFFFF);
			for(Face face:faces){
				Projected a=p[face.indices[0]],b=p[face.indices[1]],c=p[face.indices[2]],d=p[face.indices[3]];
				java.nio.FloatBuffer vertices=java.nio.FloatBuffer.wrap(new float[]{a.x,a.y,b.x,b.y,c.x,c.y,a.x,a.y,c.x,c.y,d.x,d.y});
				int color=shade(base,face.side);
				java.nio.IntBuffer colors=java.nio.IntBuffer.wrap(new int[]{color,color,color,color,color,color});
				canvas.drawTriangleListMesh(vertices,colors,paint);
			}
			paint.setStyle(Paint.STROKE);paint.setStrokeWidth(active?px(getContext(),2f):px(getContext(),.85f));paint.setColor(active?0xFFE8F3FF:0x88DCE8F8);
			for(int[] edge:EDGES){Projected a=p[edge[0]],b=p[edge[1]];if(a.visible&&b.visible)canvas.drawLine(a.x,a.y,b.x,b.y,paint);}
			if(active){Projected top=project(new Vec3D(center.x,center.y+s+.35f,center.z),w,h);if(top.visible)drawText(canvas,"TARGET",top.x,top.y,px(getContext(),8),base,true);}
		}

		private Projected project(Vec3D point,float w,float h){double yr=Math.toRadians(yaw),pr=Math.toRadians(pitch);float cy=(float)Math.cos(yr),sy=(float)Math.sin(yr);float x=cy*point.x-sy*point.z,z=sy*point.x+cy*point.z;float cp=(float)Math.cos(pr),sp=(float)Math.sin(pr);float y=cp*(point.y-.45f)+sp*z;float depth=-sp*(point.y-.45f)+cp*z;if(depth<=.12f)return new Projected(0,0,depth,false);float focal=(float)(Math.min(w,h*.95f)/Math.tan(Math.toRadians(70)/2));return new Projected(w/2+x/depth*focal,h/2-y/depth*focal,depth,true);}
		private static int shade(int color,int face){float factor=switch(face){case 3->1.14f;case 2,4->.88f;case 1->.72f;default->1f;};int r=Math.min(255,Math.round(((color>>16)&255)*factor)),g=Math.min(255,Math.round(((color>>8)&255)*factor)),b=Math.min(255,Math.round((color&255)*factor));return 0xFF000000|(r<<16)|(g<<8)|b;}
		private void drawText(Canvas canvas,String value,float x,float y,float size,int color,boolean rightOrCentered){paint.setColor(color);paint.setStyle(Paint.FILL);fontPaint.setFontSize(Math.max(1,size));fontPaint.setAntiAlias(true);float drawX=rightOrCentered?x-value.length()*size*.29f:x;canvas.drawTextRun(value,0,value.length(),0,value.length(),drawX,y,false,paint,fontPaint);}
		private record Vec3D(float x,float y,float z){} private record Projected(float x,float y,float depth,boolean visible){} private record Face(int[] indices,float depth,int side){}
	}

	private static final class ColorField extends View {
		private final Paint paint = new Paint();
		private LinearGradient[] fieldRows;
		private float fieldHue = Float.NaN;
		private float fieldWidth;
		private float hue;
		private float saturation;
		private float value;
		private java.util.function.IntConsumer colorChanged;

		private ColorField(Context context, float hue, float saturation, float value) {
			super(context);
			this.hue = hue;
			this.saturation = saturation;
			this.value = value;
			setWillNotDraw(false);
		}

		private void setOnColorChanged(java.util.function.IntConsumer listener) {
			colorChanged = listener;
		}

		private void setColor(int color) {
			float[] hsv = hsv(color);
			hue = hsv[0];
			saturation = hsv[1];
			value = hsv[2];
			invalidate();
			if (colorChanged != null) colorChanged.accept(getColor());
		}

		private void setHue(float hue) {
			this.hue = (hue + 360F) % 360F;
			invalidate();
			if (colorChanged != null) colorChanged.accept(getColor());
		}

		private float getHue() { return hue; }
		private int getColor() { return 0xFF000000 | Color.HSVToColor(new float[] {hue, saturation, value}); }

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			paint.setAntiAlias(true);
			float width = getWidth();
			float height = getHeight();
			int hueColor = 0xFF000000 | Color.HSVToColor(new float[] {hue, 1F, 1F});
			paint.setStyle(Paint.FILL);
			paint.setColor(0xFFFFFFFF);
			// Opaque scanlines avoid transparent shader compositing losing the hue.
			float radius = px(getContext(), 7);
			int rows = Math.max(1, (int) Math.ceil(height));
			boolean rebuild = fieldRows == null || fieldRows.length != rows || fieldHue != hue || fieldWidth != width;
			if (rebuild) {
				fieldRows = new LinearGradient[rows];
				fieldHue = hue;
				fieldWidth = width;
			}
			for (int row = 0; row < rows; row++) {
				float top = row * height / rows;
				float bottom = (row + 1) * height / rows;
				float brightness = 1F - row / (float) Math.max(1, rows - 1);
				int grey = Math.round(255 * brightness);
				int tint = 0xFF000000 | (Math.round(((hueColor >> 16) & 255) * brightness) << 16)
						| (Math.round(((hueColor >> 8) & 255) * brightness) << 8)
						| Math.round((hueColor & 255) * brightness);
				float edge = Math.min(top, height - bottom);
				float inset = edge < radius ? radius - (float) Math.sqrt(radius * radius - (radius - edge) * (radius - edge)) : 0F;
				if (rebuild) fieldRows[row] = new LinearGradient(0F, 0F, width, 0F, 0xFF000000 | (grey << 16) | (grey << 8) | grey, tint, Shader.TileMode.CLAMP, null);
				paint.setShader(fieldRows[row]);
				canvas.drawRect(inset, top, width - inset, bottom, paint);
			}
			paint.setShader(null);
			paint.setStyle(Paint.STROKE);
			paint.setStrokeWidth(px(getContext(), 2));
			paint.setColor(0xFFF6FAFF);
			float markerX = Math.max(px(getContext(), 6), Math.min(width - px(getContext(), 6), saturation * width));
			float markerY = Math.max(px(getContext(), 6), Math.min(height - px(getContext(), 6), (1F - value) * height));
			canvas.drawCircle(markerX, markerY, px(getContext(), 6), paint);
			paint.setStyle(Paint.FILL);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			int action = event.getActionMasked();
			if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP) {
				saturation = clamp(event.getX() / Math.max(1F, getWidth()));
				value = 1F - clamp(event.getY() / Math.max(1F, getHeight()));
				invalidate();
				if (colorChanged != null) colorChanged.accept(getColor());
				return true;
			}
			return true;
		}
	}

	private static final class HueStrip extends View {
		private final Paint paint = new Paint();
		private float hue;
		private java.util.function.Consumer<Float> hueChanged;

		private HueStrip(Context context, float hue) {
			super(context);
			this.hue = hue;
			setWillNotDraw(false);
		}

		private void setOnHueChanged(java.util.function.Consumer<Float> listener) {
			hueChanged = listener;
		}

		private void setHue(float hue) {
			this.hue = (hue + 360F) % 360F;
			invalidate();
		}

		@Override
		protected void onDraw(Canvas canvas) {
			super.onDraw(canvas);
			paint.setAntiAlias(true);
			// Draw explicit segments instead of relying on the multi-stop shader;
			// that shader is not rendered consistently by ModernUI in popup views.
			int[] colors = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
			paint.setStyle(Paint.FILL);
			float segmentWidth = getWidth() / 6F;
			for (int i = 0; i < 6; i++) {
				float left = i * segmentWidth;
				float right = (i + 1) * segmentWidth + (i == 5 ? 0F : 1F);
				paint.setShader(new LinearGradient(left, 0F, right, 0F, colors[i], colors[i + 1], Shader.TileMode.CLAMP, null));
				canvas.drawRect(left, 0F, right, getHeight(), paint);
			}
			paint.setShader(null);
			paint.setStyle(Paint.STROKE);
			paint.setStrokeWidth(px(getContext(), 2));
			paint.setColor(0xFF152238);
			canvas.drawRoundRect(1F, 1F, getWidth() - 1F, getHeight() - 1F, px(getContext(), 6), paint);
			paint.setColor(0xFFF6FAFF);
			float x = Math.max(px(getContext(), 5), Math.min(getWidth() - px(getContext(), 5), hue / 360F * getWidth()));
			canvas.drawRoundRect(x - px(getContext(), 3), 0F, x + px(getContext(), 3), getHeight(), px(getContext(), 3), paint);
			paint.setStyle(Paint.FILL);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			int action = event.getActionMasked();
			if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP) {
				hue = clamp(event.getX() / Math.max(1F, getWidth())) * 360F;
				invalidate();
				if (hueChanged != null) hueChanged.accept(hue);
				return true;
			}
			return true;
		}
	}

	private static final class PickerActions extends LinearLayout {
		private final Runnable reset;
		private final Runnable done;

		private PickerActions(Context context, Runnable reset, Runnable done) {
			super(context);
			this.reset = reset;
			this.done = done;
			setOrientation(HORIZONTAL);
			setGravity(Gravity.CENTER_VERTICAL);
			setClickable(true);
			TextView resetLabel = text(context, "Reset", 12, TEXT);
			resetLabel.setGravity(Gravity.CENTER);
			resetLabel.setBackground(box(resetLabel, 0xFF243853, 0x885377A4, 6));
			resetLabel.setClickable(true);
			resetLabel.setOnClickListener(view -> reset.run());
			TextView doneLabel = text(context, "Done", 12, TEXT);
			doneLabel.setGravity(Gravity.CENTER);
			doneLabel.setBackground(box(doneLabel, 0xFF243853, 0x885377A4, 6));
			doneLabel.setClickable(true);
			doneLabel.setOnClickListener(view -> done.run());
			addView(resetLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
			LinearLayout.LayoutParams doneParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
			doneParams.leftMargin = px(context, 14);
			addView(doneLabel, doneParams);
		}

		@Override
		public boolean onTouchEvent(MotionEvent event) {
			if (event.getActionMasked() == MotionEvent.ACTION_UP) {
				if (event.getX() < getWidth() / 2F) reset.run();
				else done.run();
			}
			return true;
		}
	}

	private static float clamp(float value) {
		return Math.max(0F, Math.min(1F, value));
	}

	private LinearLayout settingRow(Context context, String name, String description) {
		LinearLayout row = new LinearLayout(context);
		row.setGravity(Gravity.CENTER_VERTICAL);
		LinearLayout copy = new LinearLayout(context);
		copy.setOrientation(LinearLayout.VERTICAL);
		TextView heading = text(context, name, 14, TEXT);
		TextView detail = text(context, description, 11, MUTED);
		detail.setVisibility(KairokkCustomizationState.showModuleDescriptions() ? View.VISIBLE : View.GONE);
		copy.addView(heading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 22)));
		copy.addView(detail, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 17)));
		row.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1));
		return row;
	}

	private LinearLayout.LayoutParams settingParams(Context context) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 48));
		params.bottomMargin = px(context, KairokkCustomizationState.compactMode() ? 0 : KairokkCustomizationState.moduleSpacing() / 3F);
		return params;
	}

	private static LinearLayout.LayoutParams weighted(Context context, int weight, boolean rightMargin) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
		if (rightMargin) params.rightMargin = px(context, 12);
		return params;
	}

	private View preview(Context context) {
		LinearLayout panel = new LinearLayout(context);
		panel.setOrientation(LinearLayout.VERTICAL);
		panel.setPadding(px(context, 15), px(context, 13), px(context, 15), px(context, 15));
		panel.setBackground(box(panel, 0x82101822, 0x663B506E, 7));
		TextView heading = text(context, "│ P R E V I E W", 14, TEXT);
		heading.setTextColor(0xFFF4F0F4);
		panel.addView(heading, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 29)));

		FrameLayout stage = new FrameLayout(context);
		stage.setBackground(box(stage, 0xC7182637, 0x44394E70, 7));
		stage.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
			view.post(() -> capturePreviewBounds(view)));
		panel.addView(stage, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
		updatePreview();
		return panel;
	}

	private void capturePreviewBounds(View stage) {
		if (rootView == null || rootView.getWidth() <= 0 || stage.getWidth() <= 0) return;
		int[] location = new int[2];
		stage.getLocationOnScreen(location);
		previewBounds = new PreviewBounds(location[0], location[1], stage.getWidth(), stage.getHeight(), rootView.getWidth());
	}

	public PreviewBounds previewBounds() {
		return "VISUALS".equals(selectedTab) && "Players".equals(selectedCategory) ? previewBounds : null;
	}

	public record PreviewBounds(int x, int y, int width, int height, int uiWidth) {}
	public PreviewStyle previewStyle() { return previewStyle; }
	public record PreviewStyle(boolean esp, String boxStyle, int color, boolean chams,
		boolean nameTags, boolean distance, boolean health, boolean tracers) {}

	private void updatePreview() {
		int color = boxColor;
		boolean esp = switches.getOrDefault("Player ESP", false);
		previewStyle = new PreviewStyle(esp, boxStyle, color,
			switches.getOrDefault("Chams", false), switches.getOrDefault("Name Tags", false),
			switches.getOrDefault("Distance", false), switches.getOrDefault("Health", false),
			switches.getOrDefault("Tracers", false));
	}

	private static ShulkIconView icon(Context context, ShulkIcon icon, String tooltip) {
		ShulkIconView view = new ShulkIconView(context, icon, MUTED, tooltip);
		view.setPadding(px(context, 6), px(context, 6), px(context, 6), px(context, 6));
		return view;
	}

	private static TextView text(Context context, String value, float size, int color) {
		TextView view = new TextView(context);
		view.setText(value);
		view.setTextSize(textSize(context, size));
		view.setTextColor(customTextColor(color));
		view.setTypeface(customTypeface());
		view.setTextStyle("Bold".equals(KairokkCustomizationState.fontWeight()) ? Typeface.BOLD : Typeface.NORMAL);
		view.setIncludeFontPadding(false);
		return view;
	}

	private static int customTextColor(int color) {
		if (color == TEXT) return KairokkCustomizationState.cssColor("text", KairokkCustomizationState.textColor());
		if (color == MUTED) return KairokkCustomizationState.cssColor("muted", KairokkCustomizationState.mutedColor());
		if (color == ACCENT) return KairokkCustomizationState.cssColor("accent", KairokkCustomizationState.accent());
		if (color == BLUE) return KairokkCustomizationState.cssColor("primary", KairokkCustomizationState.primary());
		if (color == BLUE_LIGHT) return KairokkCustomizationState.cssColor("primaryLight", KairokkCustomizationState.primaryLight());
		return color;
	}

	private static Typeface customTypeface() {
		String family = KairokkCustomizationState.fontFamily();
		if ("Mono".equals(family)) return Typeface.MONOSPACED;
		if ("System".equals(family)) return Typeface.SANS_SERIF;
		try { return Typeface.getSystemFont("Inter"); }
		catch (RuntimeException ignored) { return Typeface.SANS_SERIF; }
	}

	private static ShapeDrawable box(View view, int fill, int outline, float radius) {
		ShapeDrawable drawable = new ShapeDrawable();
		drawable.setShape(ShapeDrawable.RECTANGLE);
		drawable.setColor(KairokkCustomizationState.panelColor(fill));
		float radiusScale = KairokkCustomizationState.roundness() / 8F;
		float customRadius = KairokkCustomizationState.cssPixels("radius", radius);
		if (!KairokkCustomizationState.modernUiElements()) customRadius *= .35F;
		drawable.setCornerRadius(px(view.getContext(), Math.max(0F, customRadius * radiusScale)));
		int stroke = KairokkCustomizationState.modernUiElements() ? KairokkCustomizationState.borderThickness() : 1;
		drawable.setStroke(px(view.getContext(), stroke), KairokkCustomizationState.panelBorderColor(outline));
		return drawable;
	}

	private static ShapeDrawable pill(View view, int color) {
		ShapeDrawable drawable = new ShapeDrawable();
		drawable.setShape(ShapeDrawable.RECTANGLE);
		drawable.setColor(color);
		drawable.setCornerRadius(px(view.getContext(), 14));
		return drawable;
	}

	private static StateListDrawable categoryBackground(View view) {
		StateListDrawable drawable = new StateListDrawable();
		drawable.addState(StateSet.WILD_CARD, box(view, 0x80316AC0, BLUE, 6));
		return drawable;
	}

	/** A quiet, animated hover surface that previews the next sidebar selection. */
	private static void attachSidebarHover(View row, java.util.function.BooleanSupplier active) {
		row.setPivotX(0F);
		row.setPivotY(row.getHeight() / 2F);
		row.setOnHoverListener((view, event) -> {
			if (active.getAsBoolean()) return false;
			if (event.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER) {
				KairokkUiSounds.play(KairokkUiSounds.HOVER);
				view.setBackground(box(view, 0x3A316AC0, 0x665377A4, 6));
				ObjectAnimator.ofFloat(view, View.SCALE_X, view.getScaleX(), 1.018F).setDuration(170).start();
				ObjectAnimator.ofFloat(view, View.SCALE_Y, view.getScaleY(), 1.018F).setDuration(170).start();
				ObjectAnimator.ofFloat(view, View.ALPHA, view.getAlpha(), 1F).setDuration(140).start();
			} else if (event.getActionMasked() == MotionEvent.ACTION_HOVER_EXIT) {
				view.setBackground(box(view, 0x00152231, 0x00000000, 6));
				ObjectAnimator.ofFloat(view, View.SCALE_X, view.getScaleX(), 1F).setDuration(210).start();
				ObjectAnimator.ofFloat(view, View.SCALE_Y, view.getScaleY(), 1F).setDuration(210).start();
			}
			return false;
		});
	}

	private static StateListDrawable topTabHoverBackground(View view) {
		StateListDrawable drawable = new StateListDrawable();
		drawable.addState(StateSet.get(StateSet.VIEW_STATE_PRESSED), box(view, 0x303A79C8, 0, 6));
		drawable.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), box(view, 0x183A79C8, 0, 6));
		drawable.addState(StateSet.WILD_CARD, box(view, 0, 0, 6));
		drawable.setEnterFadeDuration(150);
		drawable.setExitFadeDuration(200);
		return drawable;
	}

	private static StateListDrawable topToolHoverBackground(View view, int color) {
		StateListDrawable drawable = new StateListDrawable();
		drawable.addState(StateSet.get(StateSet.VIEW_STATE_PRESSED), box(view, (color & 0xFFFFFF) | 0x38000000, 0, 7));
		drawable.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), box(view, (color & 0xFFFFFF) | 0x22000000, 0, 7));
		drawable.addState(StateSet.WILD_CARD, box(view, 0, 0, 7));
		drawable.setEnterFadeDuration(160);
		drawable.setExitFadeDuration(210);
		return drawable;
	}

	private static int px(Context context, float value) {
		int width = Minecraft.getInstance().getWindow().getWidth();
		float scale = width < 1000 ? .62F : width / 1250F;
		return Math.round(value * scale * KairokkCustomizationState.uiScale() / 100F);
	}

	private static float textSize(Context context, float value) {
		return px(context, value * KairokkCustomizationState.fontSize() / 14F)
			/ Math.max(.01F, context.getResources().getDisplayMetrics().scaledDensity);
	}
}
