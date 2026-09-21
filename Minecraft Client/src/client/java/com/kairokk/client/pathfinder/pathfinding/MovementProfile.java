package com.kairokk.client.pathfinder.pathfinding;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * A movement snapshot captured when a route is requested.  SkyBlock sends the
 * normal vanilla Speed and Jump Boost effects, so their actual amplifiers are
 * enough to give the planner the same launch envelope as the player.
 */
public record MovementProfile(
		double jumpVelocity,
		double maxJumpRise,
		double sprintJumpSpeed,
		int speedAmplifier,
		int jumpBoostAmplifier) {
	private static final double BASE_JUMP_VELOCITY = 0.42;
	private static final double BASE_SPRINT_JUMP_SPEED = 0.28;

	public static MovementProfile from(LocalPlayer player) {
		if (!com.kairokk.client.pathfinder.PathfinderOptions.scanEffects) return vanilla();
		MobEffectInstance speed = player.getEffect(MobEffects.SPEED);
		MobEffectInstance jumpBoost = player.getEffect(MobEffects.JUMP_BOOST);
		int speedAmplifier = speed == null ? -1 : speed.getAmplifier();
		int jumpBoostAmplifier = jumpBoost == null ? -1 : jumpBoost.getAmplifier();

		// getSpeed includes every active movement-speed attribute modifier, not
		// only the visible potion. That keeps SkyBlock equipment/stat bonuses in
		// the route calculation too.
		double speedScale = clamp(player.getSpeed() / 0.10, 0.35, 8.0);
		double jumpVelocity = BASE_JUMP_VELOCITY + player.getJumpBoostPower();
		return new MovementProfile(
				jumpVelocity,
				calculateJumpRise(jumpVelocity),
				clamp(BASE_SPRINT_JUMP_SPEED * speedScale, 0.10, 1.60),
				speedAmplifier,
				jumpBoostAmplifier);
	}

	public static MovementProfile vanilla() {
		return new MovementProfile(BASE_JUMP_VELOCITY, calculateJumpRise(BASE_JUMP_VELOCITY),
				BASE_SPRINT_JUMP_SPEED, -1, -1);
	}

	/** Maximum whole-block ledge that can safely be landed on from a jump. */
	public int maxClimbBlocks() {
		return Math.max(1, (int) Math.floor(maxJumpRise - 0.15));
	}

	/** Conservative maximum continuous gap span for one sprint jump. */
	public int maxGapBlocks() {
		return Math.max(2, Math.min(8, (int) Math.floor(predictJumpDistance() - com.kairokk.client.pathfinder.PathfinderOptions.jumpMargin)));
	}

	public double predictJumpDistance() {
		double speed = sprintJumpSpeed;
		double verticalSpeed = jumpVelocity;
		double height = 0.0;
		double distance = 0.0;
		for (int tick = 0; tick < 80; tick++) {
			height += verticalSpeed;
			verticalSpeed = (verticalSpeed - 0.08) * 0.98;
			distance += speed;
			speed = speed * 0.91 + sprintJumpSpeed * 0.09;
			if (tick > 1 && height <= 0.0) break;
		}
		return distance;
	}

	public String summary() {
		String speed = speedAmplifier < 0 ? "Speed none" : "Speed " + (speedAmplifier + 1);
		String jump = jumpBoostAmplifier < 0 ? "Jump none" : "Jump " + (jumpBoostAmplifier + 1);
		return speed + " · " + jump + " · " + maxClimbBlocks() + " block climb";
	}

	private static double calculateJumpRise(double initialVelocity) {
		double velocity = initialVelocity;
		double height = 0.0;
		double maximum = 0.0;
		for (int tick = 0; tick < 80; tick++) {
			height += velocity;
			maximum = Math.max(maximum, height);
			velocity = (velocity - 0.08) * 0.98;
			if (velocity <= 0.0 && tick > 0) break;
		}
		return maximum;
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
