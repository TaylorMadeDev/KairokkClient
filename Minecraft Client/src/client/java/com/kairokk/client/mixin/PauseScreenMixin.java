package com.kairokk.client.mixin;

import com.kairokk.client.KairokkPauseScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the in-game world clear behind Minecraft's pause menu. */
@Mixin(PauseScreen.class)
public abstract class PauseScreenMixin {
	@Shadow private boolean showPauseMenu;

	/** Replaces the vanilla button grid with Kairokk's interactive menu controls. */
	@Inject(method = "init", at = @At("HEAD"), cancellable = true)
	private void kairokk$openCustomPauseMenu(CallbackInfo callbackInfo) {
		if (!showPauseMenu) return;
		Minecraft.getInstance().setScreen(new KairokkPauseScreen());
		callbackInfo.cancel();
	}

	@Inject(method = "extractBackground", at = @At("HEAD"), cancellable = true)
	private void kairokk$removePauseBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY,
			float deltaTick, CallbackInfo callbackInfo) {
		callbackInfo.cancel();
	}
}
