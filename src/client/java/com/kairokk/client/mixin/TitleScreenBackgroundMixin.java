package com.kairokk.client.mixin;

import com.kairokk.client.TitleBackgroundRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class TitleScreenBackgroundMixin {
	@Inject(method = "extractPanorama", at = @At("HEAD"), cancellable = true)
	private void kairokk$replaceTitlePanorama(GuiGraphicsExtractor graphics, float partialTick, CallbackInfo ci) {
		if (!((Object)this instanceof TitleScreen titleScreen)) {
			return;
		}

		TitleBackgroundRenderer.render(graphics, titleScreen.width, titleScreen.height);
		ci.cancel();
	}
}
