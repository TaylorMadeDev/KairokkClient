package com.kairokk.client;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Util;

/** Shared title backdrop used by both the vanilla replacement hook and the ModernUI title screen. */
public final class TitleBackgroundRenderer {
	private static final Orb[] ORBS = {
		new Orb(0.06F, 0.15F, 8.0F, 0.010F, 0.014F, 0.2F, 132),
		new Orb(0.13F, 0.72F, 5.0F, 0.016F, 0.010F, 2.7F, 104),
		new Orb(0.20F, 0.41F, 13.0F, 0.008F, 0.018F, 4.1F, 148),
		new Orb(0.27F, 0.90F, 7.0F, 0.014F, 0.012F, 1.3F, 122),
		new Orb(0.32F, 0.23F, 5.0F, 0.019F, 0.008F, 5.4F, 118),
		new Orb(0.37F, 0.59F, 10.0F, 0.011F, 0.015F, 3.0F, 138),
		new Orb(0.43F, 0.08F, 6.0F, 0.017F, 0.011F, 0.8F, 112),
		new Orb(0.48F, 0.78F, 16.0F, 0.007F, 0.020F, 4.7F, 158),
		new Orb(0.53F, 0.35F, 7.0F, 0.015F, 0.009F, 2.1F, 128),
		new Orb(0.58F, 0.95F, 5.0F, 0.021F, 0.008F, 5.9F, 106),
		new Orb(0.63F, 0.16F, 11.0F, 0.010F, 0.017F, 1.8F, 145),
		new Orb(0.69F, 0.67F, 6.0F, 0.018F, 0.011F, 3.7F, 120),
		new Orb(0.74F, 0.46F, 18.0F, 0.006F, 0.022F, 0.5F, 154),
		new Orb(0.79F, 0.87F, 8.0F, 0.013F, 0.013F, 4.4F, 126),
		new Orb(0.84F, 0.27F, 5.0F, 0.020F, 0.009F, 2.4F, 112),
		new Orb(0.90F, 0.57F, 12.0F, 0.009F, 0.019F, 5.1F, 142),
		new Orb(0.95F, 0.12F, 7.0F, 0.016F, 0.010F, 1.1F, 120),
		new Orb(0.98F, 0.81F, 5.0F, 0.022F, 0.008F, 3.3F, 102),
		new Orb(0.23F, 0.05F, 4.0F, 0.023F, 0.007F, 5.6F, 92),
		new Orb(0.40F, 0.48F, 5.0F, 0.018F, 0.010F, 2.9F, 108),
		new Orb(0.57F, 0.70F, 4.0F, 0.024F, 0.007F, 0.9F, 96),
		new Orb(0.72F, 0.03F, 6.0F, 0.019F, 0.011F, 4.9F, 114),
		new Orb(0.87F, 0.95F, 4.0F, 0.025F, 0.006F, 2.0F, 92)
	};

	private TitleBackgroundRenderer() {
	}

	public static void render(GuiGraphicsExtractor graphics, int width, int height) {
		double time = KairokkCustomizationState.animatedBackground() ? Util.getMillis() / 1000.0 : 0.0;
		int shortestSide = Math.min(width, height);
		int alpha = Math.round(KairokkCustomizationState.backgroundOpacity() * 2.55F);

		if ("Solid".equals(KairokkCustomizationState.backgroundStyle())) {
			graphics.fill(0, 0, width, height, alpha << 24 | 0x080F1D);
		} else if ("Ambient".equals(KairokkCustomizationState.backgroundStyle())) {
			graphics.fillGradient(0, 0, width, height, alpha << 24 | 0x0B1830, alpha << 24 | 0x050912);
		} else {
			graphics.fillGradient(0, 0, width, height, alpha << 24 | 0x080F1D, alpha << 24 | 0x050912);
		}
		TitleOrbRenderer.drawSoftOrb(graphics, width / 2, (int) (height * 0.35F),
			Math.round(shortestSide * 0.14F), 0x164F9A, Math.round(50 * alpha / 255F));

		float uiScale = Math.max(0.78F, Math.min(1.65F, shortestSide / 360.0F));
		if (!KairokkCustomizationState.particleEffects()) return;
		for (Orb orb : ORBS) {
			double loop = orb.baseY - time * orb.speed;
			loop -= Math.floor(loop);

			float x = orb.baseX * width
				+ (float) Math.sin(time * (0.34 + orb.speed * 8.0) + orb.phase) * orb.drift * width;
			float y = (float) (loop * (height + 44.0) - 22.0);
			float breath = 1.0F + (float) Math.sin(time * 0.72 + orb.phase * 1.7F) * 0.10F;
			int radius = Math.max(1, Math.round(orb.size * uiScale * breath * 0.34F));
			int blue = ((int) (orb.phase * 10.0F) & 1) == 0 ? 0x2A78D0 : 0x164F9A;
			TitleOrbRenderer.drawSoftOrb(graphics, Math.round(x), Math.round(y), radius, blue,
				Math.round(orb.alpha * alpha / 255F));
		}
	}

	private record Orb(float baseX, float baseY, float size, float speed, float drift, float phase, int alpha) {
	}
}
