package com.kairokk.client;

import java.io.IOException;

import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.Bitmap;
import icyllis.modernui.graphics.BitmapFactory;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.widget.ImageView;

/** Displays the 64x64 favicon returned by Minecraft's server status ping. */
final class ServerIconView extends ImageView {
	private Image icon;

	ServerIconView(Context context) {
		super(context);
		setScaleType(ScaleType.FIT_CENTER);
	}

	void setServerIcon(byte[] bytes) {
		if (bytes == null || bytes.length == 0) return;
		try (Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length)) {
			if (bitmap == null) return;
			Image decoded = Image.createTextureFromBitmap(bitmap);
			if (icon != null && !icon.isClosed()) icon.close();
			icon = decoded;
			setImage(decoded);
		} catch (IOException | RuntimeException ignored) {
			// Invalid server icons should never prevent the server list from loading.
		}
	}

	@Override protected void onDetachedFromWindow() {
		if (icon != null && !icon.isClosed()) icon.close();
		icon = null;
		super.onDetachedFromWindow();
	}
}
