package com.kairokk.client.pathfinder.pathfinding.follower;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import com.kairokk.client.pathfinder.pathfinding.MovementProfile;
import com.kairokk.client.pathfinder.pathfinding.PathPoint;

/** Executes a straight, pre-validated sprint-jump segment. */
public final class SprintJumpExecutor implements MovementExecutor {
	private static final double LANDING_RESERVE = 1.65;
	private static final double MIN_JUMP_TRAVEL = 3.25;
	private Vec3 activeTarget;
	private boolean finalApproach;
	private final HumanRotationController rotation;

	SprintJumpExecutor(HumanRotationController rotation) {
		this.rotation = rotation;
	}

	@Override
	public void tick(Minecraft client, PathPoint target) {
		if (client.player == null || client.level == null) return;
		if (activeTarget == null || activeTarget.distanceToSqr(target.position()) > 1.0e-6) {
			activeTarget = target.position();
			finalApproach = false;
		}

		Vec3 delta = target.position().subtract(client.player.position());
		double distance = delta.horizontalDistance();
		double horizontalSpeed = client.player.getDeltaMovement().horizontalDistance();
		float yawError = Math.abs(rotation.face(client.player, delta));
		boolean alignedToMove = yawError < 70.0f;
		boolean alignedToSprint = yawError < 25.0f;
		boolean alignedToJump = yawError < 10.0f;

		if (!client.player.onGround()) {
			double remainingFlight = predictRemainingFlightDistance(client.player.getDeltaMovement(),
					client.player.getY(), target.position().y);
			if (distance <= remainingFlight + LANDING_RESERVE) finalApproach = true;
			client.options.keyUp.setDown(!finalApproach && alignedToMove);
			client.options.keySprint.setDown(!finalApproach && alignedToSprint);
			client.options.keyJump.setDown(false);
			return;
		}

		boolean gapJump = needsImmediateGapJump(client, delta);
		MovementProfile profile = MovementProfile.from(client.player);
		double predictedJump = predictFullJumpDistance(horizontalSpeed, profile);
		boolean roomForAnotherJump = !finalApproach && distance > predictedJump + LANDING_RESERVE;
		if ((gapJump || roomForAnotherJump) && alignedToJump) {
			client.options.keyUp.setDown(true);
			// A close one-block gap needs a controlled jump, not a full-speed launch.
			client.options.keySprint.setDown(!gapJump || distance > 3.25);
			client.options.keyJump.setDown(true);
			return;
		}
		if ((gapJump || roomForAnotherJump) && !alignedToJump) {
			client.options.keyUp.setDown(alignedToMove);
			client.options.keySprint.setDown(alignedToSprint);
			client.options.keyJump.setDown(false);
			return;
		}

		finalApproach = true;
		client.options.keyJump.setDown(false);
		client.options.keySprint.setDown(false);
		double brakingDistance = Math.max(0.55, horizontalSpeed * 5.0);
		client.options.keyUp.setDown(distance > brakingDistance && alignedToMove);
	}

	private static double predictFullJumpDistance(double currentHorizontalSpeed, MovementProfile profile) {
		double speed = Math.max(profile.sprintJumpSpeed(), currentHorizontalSpeed);
		double verticalSpeed = profile.jumpVelocity();
		double height = 0.0;
		double distance = 0.0;
		for (int tick = 0; tick < 80; tick++) {
			height += verticalSpeed;
			verticalSpeed = (verticalSpeed - 0.08) * 0.98;
			distance += speed;
			// Maintain the measured sprint velocity instead of decaying to a
			// vanilla constant; this is important for SkyBlock Speed effects.
			speed = speed * 0.91 + profile.sprintJumpSpeed() * 0.09;
			if (tick > 1 && height <= 0.0) break;
		}
		return Math.max(distance, MIN_JUMP_TRAVEL);
	}

	private static double predictRemainingFlightDistance(Vec3 velocity, double playerY, double landingY) {
		double speed = velocity.horizontalDistance();
		double verticalSpeed = velocity.y;
		double height = playerY - landingY;
		double distance = 0.0;
		for (int tick = 0; tick < 20 && (tick < 2 || height > 0.0 || verticalSpeed > 0.0); tick++) {
			height += verticalSpeed;
			verticalSpeed = (verticalSpeed - 0.08) * 0.98;
			distance += speed;
			speed = speed * 0.91 + 0.026;
		}
		return Math.max(distance, 0.0);
	}

	private static boolean needsImmediateGapJump(Minecraft client, Vec3 delta) {
		if (client.player == null || client.level == null || delta.horizontalDistanceSqr() < 0.25) return false;
		int stepX = Math.abs(delta.x) >= Math.abs(delta.z) ? (int) Math.signum(delta.x) : 0;
		int stepZ = stepX == 0 ? (int) Math.signum(delta.z) : 0;
		BlockPos aheadFeet = client.player.blockPosition().offset(stepX, 0, stepZ);
		BlockPos support = aheadFeet.below();
		return client.level.getBlockState(support).getCollisionShape(client.level, support).isEmpty();
	}

	@Override
	public void stop(Minecraft client) {
		client.options.keyUp.setDown(false);
		client.options.keyJump.setDown(false);
		client.options.keySprint.setDown(false);
		activeTarget = null;
		finalApproach = false;
		rotation.reset();
	}

	@Override
	public double arrivalTolerance() {
		return 0.40;
	}
}
