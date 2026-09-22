package com.kairokk.client.pathfinder.pathfinding.follower;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import com.kairokk.client.pathfinder.pathfinding.MovementType;
import com.kairokk.client.pathfinder.pathfinding.Path;
import com.kairokk.client.pathfinder.pathfinding.PathPoint;

public final class PathFollower {
	private final Map<MovementType, MovementExecutor> executors = new EnumMap<>(MovementType.class);
	private Path path;
	private int currentPointIndex;
	private boolean active;
	private double bestStableDistance;
	private int ticksWithoutProgress;
	private MovementType announcedMovement;
	private final HumanRotationController rotation = new HumanRotationController();

	public PathFollower() {
		executors.put(MovementType.WALK, new WalkExecutor(rotation));
		executors.put(MovementType.STEP_UP, new WalkExecutor(rotation));
		executors.put(MovementType.JUMP, new WalkExecutor(rotation));
		executors.put(MovementType.SPRINT_JUMP, new SprintJumpExecutor(rotation));
	}

	public boolean start(Minecraft client, Path newPath) {
		if (client.player == null || newPath == null || newPath.points().isEmpty()) return false;
		path = newPath;
		int nearest = 0;
		double nearestDistance = Double.MAX_VALUE;
		for (int i = 0; i < newPath.points().size(); i++) {
			double distance = client.player.position().distanceToSqr(newPath.points().get(i).position());
			if (distance < nearestDistance) {
				nearestDistance = distance;
				nearest = i;
			}
		}
		currentPointIndex = Math.min(nearest + 1, newPath.points().size() - 1);
		active = true;
		bestStableDistance = Double.POSITIVE_INFINITY;
		ticksWithoutProgress = 0;
		announcedMovement = null;
		return true;
	}

	public FollowResult tick(Minecraft client) {
		if (!active || path == null || client.player == null) return FollowResult.IDLE;
		Vec3 playerPosition = client.player.position();
		while (currentPointIndex < path.points().size()
				&& reachedTarget(playerPosition, currentPointIndex, client.player.onGround())) {
			currentPointIndex++;
			ticksWithoutProgress = 0;
			bestStableDistance = Double.POSITIVE_INFINITY;
		}

		if (currentPointIndex >= path.points().size()) {
			stop(client);
			return FollowResult.COMPLETE;
		}

		// Hopping or shuffling sideways at a stair is not route progress.
		double groundedDistance = playerPosition.distanceTo(path.points().get(currentPointIndex).position());
		if (!client.player.onGround()) {
			// A normal jump is neither stable progress nor evidence of being stuck.
		} else if (groundedDistance < bestStableDistance - 0.20) {
			bestStableDistance = groundedDistance;
			ticksWithoutProgress = 0;
		} else if (++ticksWithoutProgress > Math.min(3, com.kairokk.client.pathfinder.PathfinderOptions.stuckTimeout)*20) {
			stop(client);
			return FollowResult.STUCK;
		}

		PathPoint target = path.points().get(currentPointIndex);
		announceMovement(client, target.movementType());
		MovementExecutor executor = executors.get(target.movementType());
		if (executor == null) {
			stop(client);
			return FollowResult.UNSUPPORTED;
		}
		executor.tick(client, target);
		if (target.movementType() == MovementType.WALK || target.movementType() == MovementType.STEP_UP) {
			boolean preparingJump = currentPointIndex + 1 < path.points().size()
					&& path.points().get(currentPointIndex + 1).movementType() == MovementType.SPRINT_JUMP
					&& playerPosition.distanceTo(target.position()) < 2.5;
			client.options.keySprint.setDown(client.options.keyUp.isDown() && (preparingJump || "Fast".equals(com.kairokk.client.pathfinder.PathfinderOptions.movementMode)));
		}
		return FollowResult.MOVING;
	}

	private boolean reachedTarget(Vec3 playerPosition, int targetIndex, boolean onGround) {
		Vec3 target = path.points().get(targetIndex).position();
		if (path.points().get(targetIndex).movementType() == MovementType.SPRINT_JUMP) {
			if (!onGround) return false;
			// A narrow pad may be landed on near its edge. Once grounded in its
			// block cell, advance instead of jumping at the same pad again.
			if (Math.floor(playerPosition.x) == Math.floor(target.x)
					&& Math.floor(playerPosition.z) == Math.floor(target.z)
					&& Math.abs(playerPosition.y - target.y) < 0.65) return true;
		}
		if (playerPosition.distanceTo(target) <= arrivalTolerance()) return true;
		if (targetIndex <= 0 || Math.abs(playerPosition.y - target.y) > 1.0) return false;
		Vec3 start = path.points().get(targetIndex - 1).position();
		Vec3 segment = new Vec3(target.x - start.x, 0.0, target.z - start.z);
		Vec3 travelled = new Vec3(playerPosition.x - start.x, 0.0, playerPosition.z - start.z);
		if (segment.lengthSqr() <= 1.0e-8 || travelled.dot(segment) < segment.lengthSqr()) return false;
		// Crossing a waypoint's plane off to the side is not reaching the stair.
		return Math.abs(travelled.x * segment.z - travelled.z * segment.x) / Math.sqrt(segment.lengthSqr()) < 0.55;
	}

	private void announceMovement(Minecraft client, MovementType movement) {
		if (movement == announcedMovement) return;
		announcedMovement = movement;
		if (movement == MovementType.SPRINT_JUMP && client.player != null) {
			client.player.sendOverlayMessage(
					Component.literal("SPRINT JUMP").withStyle(ChatFormatting.LIGHT_PURPLE));
		}
	}

	private double arrivalTolerance() {
		MovementExecutor executor = executors.get(path.points().get(currentPointIndex).movementType());
		return executor == null ? 0.48 : executor.arrivalTolerance();
	}

	public void stop(Minecraft client) {
		for (MovementExecutor executor : executors.values()) executor.stop(client);
		active = false;
		announcedMovement = null;
	}

	/** Pause without discarding progress or resetting the route. */
	public void releaseInputs(Minecraft client){client.options.keyUp.setDown(false);client.options.keyJump.setDown(false);client.options.keySprint.setDown(false);}

	public boolean isActive() { return active; }
	public int currentPointIndex() { return currentPointIndex; }

	public enum FollowResult {
		IDLE,
		MOVING,
		COMPLETE,
		STUCK,
		UNSUPPORTED
	}
}
