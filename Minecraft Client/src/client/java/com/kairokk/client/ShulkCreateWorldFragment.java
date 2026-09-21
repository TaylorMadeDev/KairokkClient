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
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.Difficulty;

/** Focused Shulk world setup UI with the commonly used creation controls. */
public final class ShulkCreateWorldFragment extends Fragment {
	private static final int WIDTH_DP = 620;
	private static final int FOOTER_HEIGHT_DP = 54;
	private final Screen parent;
	private EditText name;
	private EditText seed;
	private Button modeButton;
	private Button difficultyButton;
	private Button commandsButton;
	private Button structuresButton;
	private Button bonusButton;
	private Button worldTypeButton;
	private WorldCreationUiState.SelectedGameMode mode = WorldCreationUiState.SelectedGameMode.SURVIVAL;
	private ShulkCreateWorldRequest.WorldType worldType = ShulkCreateWorldRequest.WorldType.NORMAL;
	private Difficulty difficulty = Difficulty.NORMAL;
	private boolean commands;
	private boolean structures = true;
	private boolean bonusChest;

	public ShulkCreateWorldFragment(Screen parent) {
		this.parent = parent;
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

		TextView eyebrow = text(context, "K A I R O K K   ·   WORLD SETUP", 13, 0xFF3D8BFF);
		eyebrow.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		panel.addView(eyebrow, full(context, 24, 4));
		TextView heading = text(context, "Create New World", 30, 0xFFF4F0F2);
		heading.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		panel.addView(heading, full(context, 46, 14));

		panel.addView(label(context, "WORLD NAME"), full(context, 22, 5));
		name = input(context, "New World", "Name your world");
		panel.addView(name, full(context, 48, 14));

		LinearLayout firstRow = new LinearLayout(context);
		firstRow.setOrientation(LinearLayout.HORIZONTAL);
		firstRow.addView(control(context, "GAME MODE", modeButton = button(context, "Survival", this::cycleMode)), half(context, true));
		firstRow.addView(control(context, "DIFFICULTY", difficultyButton = button(context, "Normal", this::cycleDifficulty)), half(context, false));
		panel.addView(firstRow, full(context, 76, 12));

		LinearLayout toggles = new LinearLayout(context);
		toggles.setOrientation(LinearLayout.HORIZONTAL);
		commandsButton = button(context, "Commands: Off", this::toggleCommands);
		structuresButton = button(context, "Structures: On", this::toggleStructures);
		bonusButton = button(context, "Bonus Chest: Off", this::toggleBonus);
		toggles.addView(commandsButton, third(context));
		toggles.addView(structuresButton, third(context));
		toggles.addView(bonusButton, third(context));
		panel.addView(toggles, full(context, 44, 15));

		panel.addView(label(context, "WORLD TYPE"), full(context, 22, 5));
		worldTypeButton = button(context, worldType.displayName(), this::openWorldTypes);
		panel.addView(worldTypeButton, full(context, 48, 14));

		panel.addView(label(context, "SEED  ·  OPTIONAL"), full(context, 22, 5));
		seed = input(context, "", "Leave blank for a random seed");
		panel.addView(seed, full(context, 48, 18));

		TextView note = text(context, "World type controls terrain, biomes, and the shape of the world.", 13, 0xFF817984);
		note.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		panel.addView(note, full(context, 26, 16));

		LinearLayout actions = new LinearLayout(context);
		actions.setOrientation(LinearLayout.HORIZONTAL);
		Button back = button(context, "Back", this::goBack);
		Button create = button(context, "Create World", this::createWorld);
		create.setBackground(primaryBackground(create));
		actions.addView(back, half(context, true));
		actions.addView(create, half(context, false));
		panel.addView(actions, full(context, 48, 0));

		ScrollView scroll = new ScrollView(context);
		scroll.setClipToPadding(false);
		scroll.addView(panel, new ScrollView.LayoutParams(px(context, WIDTH_DP), ViewGroup.LayoutParams.WRAP_CONTENT));
		int availableHeight = Math.max(px(context, 250), Minecraft.getInstance().getWindow().getScreenHeight() - px(context, FOOTER_HEIGHT_DP + 34));
		FrameLayout.LayoutParams scrollParams = new FrameLayout.LayoutParams(px(context, WIDTH_DP), availableHeight, Gravity.CENTER);
		scrollParams.bottomMargin = px(context, 34);
		root.addView(scroll, scrollParams);
		ShulkTitleFragment.addFooter(root, context);
		KairokkScreenTransitions.fadeIn(root);
		return root;
	}

	private void cycleMode() {
		mode = switch (mode) {
			case SURVIVAL -> WorldCreationUiState.SelectedGameMode.CREATIVE;
			case CREATIVE -> WorldCreationUiState.SelectedGameMode.HARDCORE;
			default -> WorldCreationUiState.SelectedGameMode.SURVIVAL;
		};
		modeButton.setText(title(mode.name()));
		if (mode == WorldCreationUiState.SelectedGameMode.HARDCORE) {
			difficulty = Difficulty.HARD;
			commands = false;
			difficultyButton.setText("Hard (locked)");
			commandsButton.setText("Commands: Off");
		} else {
			difficultyButton.setText(title(difficulty.name()));
		}
	}

	private void cycleDifficulty() {
		if (mode == WorldCreationUiState.SelectedGameMode.HARDCORE) return;
		difficulty = switch (difficulty) {
			case PEACEFUL -> Difficulty.EASY;
			case EASY -> Difficulty.NORMAL;
			case NORMAL -> Difficulty.HARD;
			case HARD -> Difficulty.PEACEFUL;
		};
		difficultyButton.setText(title(difficulty.name()));
	}

	private void toggleCommands() {
		if (mode == WorldCreationUiState.SelectedGameMode.HARDCORE) return;
		commands = !commands;
		commandsButton.setText("Commands: " + (commands ? "On" : "Off"));
	}

	private void toggleStructures() {
		structures = !structures;
		structuresButton.setText("Structures: " + (structures ? "On" : "Off"));
	}

	private void toggleBonus() {
		bonusChest = !bonusChest;
		bonusButton.setText("Bonus Chest: " + (bonusChest ? "On" : "Off"));
	}

	private void createWorld() {
		String worldName = name.getText().toString().trim();
		if (worldName.isEmpty()) worldName = "New World";
		String finalName = worldName;
		String finalSeed = seed.getText().toString().trim();
		Core.executeOnMainThread(() -> ShulkCreateWorldRequest.create(parent, finalName, finalSeed,
			mode, difficulty, commands, structures, bonusChest, worldType));
	}

	private void openWorldTypes() {
		Screen current = Minecraft.getInstance().screen;
		if (current != null) Core.executeOnMainThread(() -> Minecraft.getInstance().setScreen(new ShulkWorldTypeScreen(current, this)));
	}

	public ShulkCreateWorldRequest.WorldType getWorldType() {
		return worldType;
	}

	public void setWorldType(ShulkCreateWorldRequest.WorldType worldType) {
		this.worldType = worldType;
		if (worldTypeButton != null) worldTypeButton.setText(worldType.displayName());
	}

	private void goBack() {
		Core.executeOnMainThread(() -> Minecraft.getInstance().setScreen(parent));
	}

	private static View control(Context context, String label, Button button) {
		LinearLayout box = new LinearLayout(context);
		box.setOrientation(LinearLayout.VERTICAL);
		box.addView(label(context, label), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 24)));
		box.addView(button, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, 46)));
		return box;
	}

	private static EditText input(Context context, String value, String hint) {
		EditText input = new EditText(context);
		input.setText(value);
		input.setHint(hint);
		input.setSingleLine(true);
		input.setTextSize(textSize(context, 18));
		input.setTextColor(0xFFF3EEF1);
		input.setHintTextColor(0xFF68616B);
		input.setPadding(px(context, 14), 0, px(context, 14), 0);
		input.setBackground(inputBackground(input));
		return input;
	}

	private static TextView label(Context context, String value) {
		TextView label = text(context, value, 12, 0xFF8B838E);
		label.setGravity(Gravity.LEFT | Gravity.CENTER_VERTICAL);
		return label;
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

	private static LinearLayout.LayoutParams third(Context context) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
		params.leftMargin = px(context, 4);
		params.rightMargin = px(context, 4);
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

	private static StateListDrawable primaryBackground(View view) {
		StateListDrawable background = new StateListDrawable();
		background.addState(StateSet.get(StateSet.VIEW_STATE_PRESSED), shape(view, 0xFF286BD8, 0xFF286BD8, 6));
		background.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), shape(view, 0xFF5B9DFF, 0xFF5B9DFF, 6));
		background.addState(StateSet.WILD_CARD, shape(view, 0xFF4B94FF, 0xFF4B94FF, 6));
		background.setEnterFadeDuration(110);
		background.setExitFadeDuration(150);
		return background;
	}

	private static StateListDrawable inputBackground(View view) {
		StateListDrawable background = new StateListDrawable();
		background.addState(StateSet.get(StateSet.VIEW_STATE_FOCUSED), shape(view, 0xFF0E0C10, 0xFF3D8BFF, 6));
		background.addState(StateSet.WILD_CARD, shape(view, 0xFF0E0C10, 0xFF39313B, 6));
		background.setEnterFadeDuration(100);
		background.setExitFadeDuration(130);
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

	private static String title(String value) {
		return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
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
