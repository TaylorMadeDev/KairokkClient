package com.kairokk.client.pathfinder.pathfinding;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import com.kairokk.client.pathfinder.PathFinderSettings;

final class PathSmoother {
	private static final int MIN_SPRINT_JUMP_RUN = 6;

	private PathSmoother() {
	}

	static List<PathPoint> simplify(Level level, List<PathStep> raw, MovementProfile movementProfile) {
		if (raw.isEmpty()) return List.of();
		List<Integer> kept = stringPull(level, raw);

		List<PathPoint> result = new ArrayList<>();
		for (int i = 0; i < kept.size(); i++) {
			int rawIndex = kept.get(i);
			MovementType movement = MovementType.WALK;
			if (i > 0) {
				int priorRawIndex = kept.get(i - 1);
				movement = raw.get(rawIndex).movementType();
				if (PathFinderSettings.sprintJump && movement == MovementType.WALK
						&& isSafeSprintJumpRun(level, raw, priorRawIndex, rawIndex, movementProfile)) {
					movement = MovementType.SPRINT_JUMP;
				} else if (movement == MovementType.WALK) {
					double rise = Pathfinder.surfaceY(level, raw.get(rawIndex).position())
							- Pathfinder.surfaceY(level, raw.get(priorRawIndex).position());
					if (rise > 0.08 && rise <= com.kairokk.client.pathfinder.PathfinderOptions.stepHeight && (Pathfinder.isStepSupport(level, raw.get(priorRawIndex).position())
							|| Pathfinder.isStepSupport(level, raw.get(rawIndex).position()))) {
						movement = MovementType.STEP_UP;
					} else if (rise > 0.65) movement = MovementType.JUMP;
					else if (rise > 0.08) movement = MovementType.STEP_UP;
				}
			}
			BlockPos block = raw.get(rawIndex).position();
			Vec3 position = new Vec3(block.getX() + 0.5, Pathfinder.surfaceY(level, block) + 0.09, block.getZ() + 0.5);
			result.add(new PathPoint(position, movement));
		}
		return List.copyOf(result);
	}

	/**
	 * Greedily joins the farthest visible walking node. This removes the grid
	 * staircase from A* and gives the follower one natural, continuous heading
	 * across open ground while retaining explicit jump boundaries.
	 */
	private static List<Integer> stringPull(Level level, List<PathStep> raw) {
		List<Integer> kept = new ArrayList<>();
		kept.add(0);
		int cursor = 0;
		while (cursor < raw.size() - 1) {
			if (raw.get(cursor + 1).movementType() != MovementType.WALK) {
				cursor++;
				kept.add(cursor);
				continue;
			}

			int walkEnd = cursor + 1;
			while (walkEnd + 1 < raw.size()
					&& raw.get(walkEnd + 1).movementType() == MovementType.WALK) {
				walkEnd++;
			}

			int farthest = walkEnd;
			while (farthest > cursor + 1
					&& !straightWalkClear(level, raw.get(cursor).position(), raw.get(farthest).position())) {
				farthest--;
			}
			cursor = farthest;
			kept.add(cursor);
		}
		return kept;
	}

	private static boolean straightWalkClear(Level level, BlockPos start, BlockPos end) {
		double dx = end.getX() - start.getX();
		double dz = end.getZ() - start.getZ();
		double distance = Math.sqrt(dx * dx + dz * dz);
		int samples = Math.max(1, (int) Math.ceil(distance * 10.0));
		BlockPos previous = start;
		double previousSurface = Pathfinder.surfaceY(level, start);
		for (int i = 0; i <= samples; i++) {
			double progress = i / (double) samples;
			double x = start.getX() + 0.5 + dx * progress;
			double z = start.getZ() + 0.5 + dz * progress;
			BlockPos sample = findGroundAt(level, x, z, previous.getY());
			if (sample == null) return false;
			double sampleSurface = highestFloorUnderFootprint(level, x, z, sample);
			if (!Double.isFinite(sampleSurface)) return false;
			// Dirt paths, carpets, slabs, and similar blocks differ by fractions of
			// a block. They are continuous walking ground, not a reason to retain a
			// grid-corner waypoint. A full rise is still left as an explicit jump.
			double rise = sampleSurface - previousSurface;
			if (rise > com.kairokk.client.pathfinder.PathfinderOptions.stepHeight
					|| rise < -com.kairokk.client.pathfinder.PathfinderOptions.maxFall) return false;
			if (!Pathfinder.hasPlayerClearanceAt(level, x, sampleSurface, z)) return false;
			int stepX = sample.getX() - previous.getX();
			int stepZ = sample.getZ() - previous.getZ();
			previous = sample;
			previousSurface = sampleSurface;
		}
		return true;
	}

	/**
	 * Uses the highest walkable floor touched by the player's footprint. Dirt
	 * paths are 1/16 lower than grass; checking only the centre makes the side of
	 * the neighbouring grass look like a wall and forces needless grid turns.
	 */
	private static double highestFloorUnderFootprint(Level level, double x, double z, BlockPos center) {
		double highest = Double.NEGATIVE_INFINITY;
		double radius = 0.30;
		double[] offsets = {-radius, radius};
		for (double offsetX : offsets) for (double offsetZ : offsets) {
			BlockPos corner = findGroundAt(level, x + offsetX, z + offsetZ, center.getY());
			if (corner == null) return Double.NaN;
			highest = Math.max(highest, Pathfinder.surfaceY(level, corner));
		}
		return Math.max(highest, Pathfinder.surfaceY(level, center));
	}

	/** Finds the actual walkable surface under a straight-line sample. */
	private static BlockPos findGroundAt(Level level, double x, double z, int preferredFeetY) {
		int blockX = (int) Math.floor(x);
		int blockZ = (int) Math.floor(z);
		int[] offsets = {0, 1, -1, 2, -2};
		for (int offset : offsets) {
			BlockPos candidate = new BlockPos(blockX, preferredFeetY + offset, blockZ);
			if (Pathfinder.isStandable(level, candidate)) return candidate;
		}
		return null;
	}

	private static boolean isSafeSprintJumpRun(Level level, List<PathStep> raw, int from, int to,
			MovementProfile movementProfile) {
		if (to - from < MIN_SPRINT_JUMP_RUN) return false;
		BlockPos start = raw.get(from).position();
		BlockPos end = raw.get(to).position();
		if (start.getY() != end.getY()) return false;
		for (int i = from + 1; i <= to; i++) {
			if (raw.get(i).movementType() != MovementType.WALK
					|| raw.get(i).position().getY() != start.getY()
					|| !Pathfinder.hasJumpClearance(level, raw.get(i).position(), movementProfile)) return false;
		}
		return true;
	}

}
