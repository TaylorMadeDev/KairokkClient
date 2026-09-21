package com.kairokk.client;

import icyllis.modernui.R;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.graphics.drawable.StateListDrawable;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.EditText;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.Minecraft;

final class ShulkServerUi {
	static final int FOOTER_HEIGHT_DP = 54;

	private ShulkServerUi() { }

	static TextView text(Context context, String value, float size, int color) {
		TextView text = new TextView(context);
		text.setText(value);
		text.setTextSize(textSize(context, size));
		text.setTextColor(color);
		text.setIncludeFontPadding(false);
		text.setGravity(Gravity.CENTER);
		return text;
	}

	static EditText input(Context context, String value, String hint) {
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

	static Button button(Context context, String value, Runnable action) {
		Button button = new Button(context);
		button.setText(value);
		button.setTextSize(textSize(context, 17));
		button.setTextColor(0xFFF3EEF1);
		button.setGravity(Gravity.CENTER);
		button.setIncludeFontPadding(false);
		button.setBackground(buttonBackground(button));
		KairokkUiSounds.attach(button);
		ShulkIcon icon = ShulkIcon.forAction(value);
		if (icon != null) ShulkIconView.decorate(button, icon, px(context, 16), 0xFFF3EEF1, 0xFFFFFFFF, 0xFFFFFFFF, 0x667A737D, value);
		button.setOnClickListener(ignored -> action.run());
		return button;
	}

	static Button primaryButton(Context context, String value, Runnable action) {
		Button button = button(context, value, action);
		button.setBackground(primaryBackground(button));
		return button;
	}

	static LinearLayout.LayoutParams full(Context context, int height, int bottom) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, px(context, height));
		params.bottomMargin = px(context, bottom);
		return params;
	}

	static LinearLayout.LayoutParams weighted(Context context, int height, boolean left) {
		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, px(context, height), 1);
		if (left) params.rightMargin = px(context, 5); else params.leftMargin = px(context, 5);
		return params;
	}

	static ShapeDrawable shape(View view, int fill, int outline, float radius) {
		ShapeDrawable drawable = new ShapeDrawable();
		drawable.setShape(ShapeDrawable.RECTANGLE);
		drawable.setColor(fill);
		drawable.setCornerRadius(px(view.getContext(), radius));
		drawable.setStroke(px(view.getContext(), 1), outline);
		return drawable;
	}

	static StateListDrawable rowBackground(View view, boolean selected) {
		StateListDrawable background = new StateListDrawable();
		background.addState(StateSet.get(StateSet.VIEW_STATE_PRESSED), shape(view, 0xFF131B2B, 0xFF3D8BFF, 7));
		background.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), shape(view, 0xFF151B28, 0xFF3A2B31, 7));
		background.addState(StateSet.WILD_CARD, shape(view, selected ? 0xFF101826 : 0xB8121015,
			selected ? 0xFF3D8BFF : 0xFF26344A, 7));
		background.setEnterFadeDuration(120);
		background.setExitFadeDuration(150);
		return background;
	}

	private static StateListDrawable buttonBackground(View view) {
		StateListDrawable background = new StateListDrawable();
		background.addState(StateSet.get(StateSet.VIEW_STATE_PRESSED), shape(view, 0xFF286BD8, 0xFF286BD8, 6));
		background.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), shape(view, 0xFF4B94FF, 0xFF4B94FF, 6));
		background.addState(StateSet.WILD_CARD, shape(view, 0xFF171419, 0xFF2A3446, 6));
		background.setEnterFadeDuration(110);
		background.setExitFadeDuration(140);
		return background;
	}

	private static StateListDrawable primaryBackground(View view) {
		StateListDrawable background = new StateListDrawable();
		background.addState(new int[]{-R.attr.state_enabled}, shape(view, 0xFF171419, 0xFF2A3446, 6));
		background.addState(StateSet.get(StateSet.VIEW_STATE_PRESSED), shape(view, 0xFF286BD8, 0xFF286BD8, 6));
		background.addState(StateSet.get(StateSet.VIEW_STATE_HOVERED), shape(view, 0xFF5B9DFF, 0xFF5B9DFF, 6));
		background.addState(StateSet.WILD_CARD, shape(view, 0xFF4B94FF, 0xFF4B94FF, 6));
		background.setEnterFadeDuration(110);
		background.setExitFadeDuration(140);
		return background;
	}

	private static StateListDrawable inputBackground(View view) {
		StateListDrawable background = new StateListDrawable();
		background.addState(StateSet.get(StateSet.VIEW_STATE_FOCUSED), shape(view, 0xFF0D0B10, 0xFF3D8BFF, 6));
		background.addState(StateSet.WILD_CARD, shape(view, 0xFF0D0B10, 0xFF39313B, 6));
		background.setEnterFadeDuration(120);
		background.setExitFadeDuration(120);
		return background;
	}

	static int px(Context context, float value) {
		int screenWidth = Minecraft.getInstance().getWindow().getScreenWidth();
		float scale = Math.max(0.80F, Math.min(0.90F, screenWidth / 855.0F));
		return Math.round(value * scale);
	}

	private static float textSize(Context context, float value) {
		return px(context, value) / Math.max(0.01F, context.getResources().getDisplayMetrics().scaledDensity);
	}
}
