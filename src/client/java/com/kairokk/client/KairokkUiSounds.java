package com.kairokk.client;

import icyllis.modernui.core.Core;
import icyllis.modernui.view.MotionEvent;
import icyllis.modernui.view.View;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

/** Shared native Minecraft sound cues for Kairokk's ModernUI controls. */
public final class KairokkUiSounds {
	public static final SoundEvent CANCEL = register("ui.cancel");
	public static final SoundEvent CLOSE = register("ui.close");
	public static final SoundEvent ERROR = register("ui.error");
	public static final SoundEvent HOVER = register("ui.hover");
	public static final SoundEvent NOTIFICATION = register("ui.notification");
	public static final SoundEvent OPEN = register("ui.open");
	public static final SoundEvent SELECT = register("ui.select");
	public static final SoundEvent SWIPE = register("ui.swipe");

	private KairokkUiSounds() {
	}

	/** Forces sound-event registration during client initialization. */
	public static void initialize() {
	}

	private static SoundEvent register(String path) {
		Identifier id = Identifier.fromNamespaceAndPath("kairokk", path);
		return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
	}

	public static void play(SoundEvent sound) {
		if (sound == null) return;
		Core.executeOnMainThread(() -> {
			Minecraft minecraft = Minecraft.getInstance();
			if (minecraft.getSoundManager() != null) {
				minecraft.getSoundManager().play(SimpleSoundInstance.forUI(sound, 1F, 0.72F));
			}
		});
	}

	/** Adds hover and primary-selection cues without consuming the control's input. */
	public static void attach(View view) {
		view.setOnHoverListener((target, event) -> {
			if (event.getActionMasked() == MotionEvent.ACTION_HOVER_ENTER) play(HOVER);
			return false;
		});
		attachSelection(view);
	}

	/** Adds only the primary-selection cue, allowing a caller to retain a custom hover listener. */
	public static void attachSelection(View view) {
		view.setOnTouchListener((target, event) -> {
			if (event.getActionMasked() == MotionEvent.ACTION_DOWN) play(SELECT);
			return false;
		});
	}
}
