package com.kairokk.client.mixin;

import com.kairokk.client.KairokkMusicPlayer;
import net.minecraft.client.sounds.MusicManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps vanilla's random menu music silent while Kairokk's title soundtrack is active. */
@Mixin(MusicManager.class)
public abstract class MusicManagerMixin {
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void kairokk$preventOverlappingMenuMusic(CallbackInfo ci) {
		if (KairokkMusicPlayer.getInstance().isRunning()) {
			ci.cancel();
		}
	}
}
