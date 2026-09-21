package com.kairokk.client;

import icyllis.modernui.R;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.graphics.drawable.ImageDrawable;
import icyllis.modernui.mc.ImageStore;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.view.PointerIcon;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.widget.ImageView;
import icyllis.modernui.widget.TextView;

/** Reusable, state-aware FontAwesome image used throughout ModernUI screens. */
public final class ShulkIconView extends ImageView {
	private ShulkIcon icon;

	public ShulkIconView(Context context, ShulkIcon icon, int tint, String description) {
		super(context);
		setScaleType(ScaleType.CENTER_INSIDE);
		setIcon(icon);
		int normal = KairokkCustomizationState.colorizeIcons() ? tint : 0xFFB7C3D5;
		setPalette(normal, 0xFFFFFFFF, normal, 0x667A737D);
		setDescription(description);
	}

	public void setIcon(ShulkIcon value) {
		icon = value;
		setImage(load(value));
	}

	public ShulkIcon getIcon() { return icon; }

	public void setPalette(int normal, int hover, int selected, int disabled) {
		if (!KairokkCustomizationState.colorizeIcons()) {
			normal = 0xFFB7C3D5;
			selected = normal;
		}
		setImageTintList(new ColorStateList(
			new int[][]{
				new int[]{-R.attr.state_enabled},
				new int[]{R.attr.state_selected},
				new int[]{R.attr.state_hovered},
				StateSet.WILD_CARD
			},
			new int[]{disabled, selected, hover, normal}
		));
	}

	public void setIconAlpha(float alpha) {
		setImageAlpha(Math.max(0F, Math.min(1F, alpha)));
	}

	public void setDescription(String description) {
		setTooltipText(description == null || description.isBlank() ? null : description);
		setTag(description);
	}

	/** Adds the same mapped icon to an existing text button without changing its layout contract. */
	public static void decorate(TextView target, ShulkIcon icon, int sizePx, int normal, int hover, int selected, int disabled,
			String description) {
		Image image = load(icon);
		if (image == null) return;
		ImageDrawable drawable = new ImageDrawable(image);
		drawable.setBounds(0, 0, sizePx, sizePx);
		ColorStateList colors = new ColorStateList(
			new int[][]{
				new int[]{-R.attr.state_enabled},
				new int[]{R.attr.state_selected},
				new int[]{R.attr.state_hovered},
				StateSet.WILD_CARD
			},
			new int[]{disabled, selected, hover, normal}
		);
		drawable.setTintList(colors);
		target.setCompoundDrawables(drawable, null, null, null);
		target.setCompoundDrawableTintList(colors);
		target.setCompoundDrawablePadding(Math.max(4, sizePx / 2));
		// ModernUI anchors a start compound drawable to the content inset while
		// independently centering the label. Give both sides the same inset so
		// the label stays centered and the icon sits comfortably inside the
		// button instead of hugging its left border.
		int iconInset = Math.max(sizePx + Math.round(sizePx * .35f), sizePx + 5);
		target.setPadding(iconInset, target.getPaddingTop(), iconInset, target.getPaddingBottom());
		if (description != null && !description.isBlank()) target.setTooltipText(description);
	}

	private static Image load(ShulkIcon value) {
		if (value == null) return null;
		try {
			return ImageStore.getInstance().getOrCreate(value.resource());
		} catch (RuntimeException exception) {
			if (value == ShulkIcon.WARNING) return null;
			try {
				return ImageStore.getInstance().getOrCreate(ShulkIcon.WARNING.resource());
			} catch (RuntimeException ignored) {
				return null;
			}
		}
	}

	@Override
	public PointerIcon onResolvePointerIcon(MotionEvent event) {
		return isClickable() ? PointerIcon.getSystemIcon(PointerIcon.TYPE_HAND) : super.onResolvePointerIcon(event);
	}
}
