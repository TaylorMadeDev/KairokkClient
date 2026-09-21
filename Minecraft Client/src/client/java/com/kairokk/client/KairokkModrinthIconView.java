package com.kairokk.client;

import icyllis.modernui.core.Context;
import icyllis.modernui.core.Core;
import icyllis.modernui.graphics.Bitmap;
import icyllis.modernui.graphics.BitmapFactory;
import icyllis.modernui.graphics.Image;
import icyllis.modernui.widget.ImageView;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/** Loads PackDisabler's Modrinth icon without blocking the ModernUI thread. */
final class KairokkModrinthIconView extends ImageView {
	private static final String PROJECT_URL = "https://api.modrinth.com/v2/project/packdisabler-for-hypixel-skyblock";
	private static final String USER_AGENT = "Kairokk/0.1.0 (client add-ons)";
	private static volatile Image cachedImage;
	private static volatile CompletableFuture<?> loading;

	KairokkModrinthIconView(Context context) {
		super(context);
		setScaleType(ScaleType.FIT_CENTER);
		loadAsync();
	}

	private void loadAsync() {
		Image image = cachedImage;
		if (image != null && !image.isClosed()) {
			setImage(image);
			return;
		}
		synchronized (KairokkModrinthIconView.class) {
			if (loading == null) loading = CompletableFuture.runAsync(KairokkModrinthIconView::fetch);
		}
		loading.thenRun(() -> {
			Image loaded = cachedImage;
			if (loaded != null && !loaded.isClosed()) Core.executeOnUiThread(() -> setImage(loaded));
		});
	}

	private static void fetch() {
		try {
			HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
			HttpResponse<String> project = client.send(HttpRequest.newBuilder(URI.create(PROJECT_URL))
					.timeout(Duration.ofSeconds(20)).header("User-Agent", USER_AGENT).build(), HttpResponse.BodyHandlers.ofString());
			if (project.statusCode() != 200) return;
			String iconUrl = com.google.gson.JsonParser.parseString(project.body()).getAsJsonObject().get("icon_url").getAsString();
			HttpResponse<InputStream> imageResponse = client.send(HttpRequest.newBuilder(URI.create(iconUrl))
					.timeout(Duration.ofSeconds(20)).header("User-Agent", USER_AGENT).build(), HttpResponse.BodyHandlers.ofInputStream());
			if (imageResponse.statusCode() != 200) return;
			try (InputStream stream = imageResponse.body(); Bitmap bitmap = BitmapFactory.decodeStream(stream)) {
				if (bitmap != null) cachedImage = Image.createTextureFromBitmap(bitmap);
			}
		} catch (Exception ignored) { }
	}
}
