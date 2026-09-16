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

	private KairokkScreenTransitions() {
	}

	public static void fadeIn(View root) {
		root.setAlpha(0F);
		Core.getUiHandler().post(() -> {
			ObjectAnimator.ofFloat(root, View.ALPHA, 0F, 1F).setDuration(ENTER_DURATION_MS).start();
		});
	}

	public static void fadeOut(View root, Runnable next) {
		if (root == null) {
			Core.executeOnMainThread(next);
			return;
		}
		Core.getUiHandler().post(() -> {
			ObjectAnimator animation = ObjectAnimator.ofFloat(root, View.ALPHA, root.getAlpha(), 0F)
				.setDuration(EXIT_DURATION_MS);
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
