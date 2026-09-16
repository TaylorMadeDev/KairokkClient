package com.kairokk.client.mixin;

import com.kairokk.client.KairokkLoadingRenderer;
import com.kairokk.client.KairokkUiSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.LoadingOverlay;
import net.minecraft.server.packs.resources.ReloadInstance;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.Unique;

/** Replaces Mojang's boot reload artwork while preserving its completion lifecycle. */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
	@Unique private boolean kairokk$completionSoundPlayed;
	@Shadow @Final private Minecraft minecraft;
	@Shadow @Final private ReloadInstance reload;
	@Shadow @Final private boolean fadeIn;
	@Shadow private float currentProgress;
	@Shadow private long fadeOutStart;
	@Shadow private long fadeInStart;

	@Inject(method = "extractRenderState", at = @At("HEAD"), cancellable = true)
	private void kairokk$renderLoadingOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			float deltaTick, CallbackInfo ci) {
		if (fadeInStart == -1L) fadeInStart = Util.getMillis();

		float actualProgress = reload.getActualProgress();
		currentProgress = Math.max(0F, Math.min(1F, currentProgress * 0.95F + actualProgress * 0.05F));
		KairokkLoadingRenderer.render(graphics, currentProgress, kairokk$loadingStatus(currentProgress));

		if (fadeOutStart > -1L) {
			if (!kairokk$completionSoundPlayed) {
				kairokk$completionSoundPlayed = true;
				KairokkUiSounds.play(KairokkUiSounds.NOTIFICATION);
			}
			if (Util.getMillis() - fadeOutStart >= 2000L) {
				minecraft.setOverlay(null);
			}
		}
		ci.cancel();
	}

	@Unique
	private static String kairokk$loadingStatus(float progress) {
		if (progress < 0.08F) return "Preparing Kairokk";
		if (progress < 0.35F) return "Loading game resources";
		if (progress < 0.70F) return "Building your experience";
		if (progress < 0.98F) return "Almost ready";
		return "Welcome aboard";
	}
}
