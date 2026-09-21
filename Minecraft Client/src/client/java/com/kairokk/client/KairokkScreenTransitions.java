package com.kairokk.client;

import icyllis.modernui.animation.Animator;
import icyllis.modernui.animation.AnimatorListener;
import icyllis.modernui.animation.ObjectAnimator;
import icyllis.modernui.core.Core;
import icyllis.modernui.view.View;
import net.minecraft.client.Minecraft;

/** Small shared enter/exit transitions for Kairokk's ModernUI screens. */
public final class KairokkScreenTransitions {
	private static final long ENTER_DURATION_MS = 220L;
	private static final long EXIT_DURATION_MS = 140L;
	private static final long CONTENT_OUT_DURATION_MS = 110L;
	private static final long CONTENT_IN_DURATION_MS = 170L;

	private KairokkScreenTransitions() {
	}

	private static long duration(long base) {
		if (!KairokkCustomizationState.menuAnimations()) return 0L;
		return Math.max(1L, Math.round(base * 100.0 / KairokkCustomizationState.animationSpeed()));
	}

	public static void fadeIn(View root) {
		root.setAlpha(0F);
		Core.getUiHandler().post(() -> {
			ObjectAnimator.ofFloat(root, View.ALPHA, 0F, 1F).setDuration(duration(ENTER_DURATION_MS)).start();
		});
	}

	/** Fades a live panel away before its caller swaps the contents. */
	public static void crossFade(View current, Runnable swap) {
		if (current == null) {
			Core.getUiHandler().post(swap);
			return;
		}
		Core.getUiHandler().post(() -> {
			ObjectAnimator animation = ObjectAnimator.ofFloat(current, View.ALPHA, current.getAlpha(), 0F)
				.setDuration(duration(CONTENT_OUT_DURATION_MS));
			animation.addListener(new AnimatorListener() {
				@Override
				public void onAnimationEnd(Animator animator) {
					swap.run();
				}
			});
			animation.start();
		});
	}

	/** Brings a newly swapped panel into view with a small, clean fade. */
	public static void fadeContentIn(View view) {
		view.setAlpha(0F);
		Core.getUiHandler().post(() ->
			ObjectAnimator.ofFloat(view, View.ALPHA, 0F, 1F).setDuration(duration(CONTENT_IN_DURATION_MS)).start());
	}

	public static void fadeOut(View root, Runnable next) {
		if (root == null) {
			Core.executeOnMainThread(next);
			return;
		}
		Core.getUiHandler().post(() -> {
			ObjectAnimator animation = ObjectAnimator.ofFloat(root, View.ALPHA, root.getAlpha(), 0F)
				.setDuration(duration(EXIT_DURATION_MS));
			animation.addListener(new AnimatorListener() {
				@Override
				public void onAnimationEnd(Animator animator) {
					Core.executeOnMainThread(next);
				}
			});
			animation.start();
		});
	}

	public static void returnTo(View root, net.minecraft.client.gui.screens.Screen parent) {
		fadeOut(root, () -> Minecraft.getInstance().setScreen(parent));
	}
}
