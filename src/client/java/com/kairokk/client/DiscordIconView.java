package com.kairokk.client;

import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Bitmap;
import icyllis.modernui.graphics.BitmapFactory;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.PointerIcon;
import icyllis.modernui.widget.ImageView;

import java.io.IOException;
import java.io.InputStream;

/** Footer button that displays the supplied DiscordLogo.png asset. */
public final class DiscordIconView extends ImageView {
	private static Image sharedLogo;
	private int iconColor = 0xFFB6B0BA;

	public DiscordIconView(Context context) {
		super(context);
		setClickable(true);
		setScaleType(ScaleType.FIT_CENTER);
		setImage(loadLogo());
		setImageTintList(ColorStateList.valueOf(iconColor));
	}

	private static synchronized Image loadLogo() {
		if (sharedLogo != null && !sharedLogo.isClosed()) return sharedLogo;
		try (InputStream stream = DiscordIconView.class.getResourceAsStream("/assets/kairokk/textures/icons/discord.png")) {
			if (stream == null) throw new IOException("Missing bundled Discord logo");
			try (Bitmap bitmap = BitmapFactory.decodeStream(stream)) {
				sharedLogo = Image.createTextureFromBitmap(bitmap);
				return sharedLogo;
			}
		} catch (IOException exception) {
			return null;
		}
	}

	public int getIconColor() { return iconColor; }
	public void setIconColor(int value) {
		iconColor = value;
		setImageTintList(ColorStateList.valueOf(value));
	}

	@Override
	public PointerIcon onResolvePointerIcon(MotionEvent event) {
		return PointerIcon.getSystemIcon(PointerIcon.TYPE_HAND);
	}

}
