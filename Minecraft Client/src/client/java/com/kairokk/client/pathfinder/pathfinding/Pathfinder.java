package com.kairokk.client.pathfinder.pathfinding;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import com.kairokk.client.pathfinder.PathfinderOptions;
import net.minecraft.tags.FluidTags;

/** A bounded, tick-driven A* search for ordinary walking nodes. */
public final class Pathfinder {
	public static final int MAX_EXPLORED_NODES = 16_000;
	public static final int MAX_HORIZONTAL_RANGE = 128;
	public static final int MAX_VERTICAL_RANGE = 48;
	// A standing player is 0.6 x 1.8 blocks. The small horizontal margin keeps
	// interpolation and server collision rounding from grazing block corners.
	private static final double PLAYER_RADIUS = 0.31;
	private static final double PLAYER_HEIGHT = 1.80;
	private static final int[][] DIRECTIONS = {
			{1, 0}, {-1, 0}, {0, 1}, {0, -1},
			{1, 1}, {1, -1}, {-1, 1}, {-1, -1}
	};

	public SearchSession begin(Level level, BlockPos requestedStart, BlockPos requestedGoal) {
		return begin(level, requestedStart, requestedGoal, MovementProfile.vanilla());
	}

	public SearchSession begin(Level level, BlockPos requestedStart, BlockPos requestedGoal, MovementProfile movementProfile) {
		BlockPos start = findStandableNear(level, requestedStart);
		BlockPos goal = findStandableNear(level, requestedGoal);
		return new SearchSession(level, start, goal, requestedGoal.immutable(), movementProfile);
	}

	public static final class SearchSession {
		private final Level level;
		private final BlockPos start;
		private final BlockPos goal;
		private final BlockPos requestedGoal;
		private final MovementProfile movementProfile;
		private final PriorityQueue<PathNode> open = new PriorityQueue<>();
		private final Map<BlockPos, PathNode> nodes = new HashMap<>();
		private final List<BlockPos> visited = new ArrayList<>();
		private boolean finished;
		private List<PathStep> result;
		private String failureReason;

		private SearchSession(Level level, BlockPos start, BlockPos goal, BlockPos requestedGoal,
				MovementProfile movementProfile) {
			this.level = level;
			this.start = start;
			this.goal = goal;
			this.requestedGoal = requestedGoal;
			this.movementProfile = movementProfile;
			if (start == null) {
				fail("No walkable position exists at the player location.");
			} else if (goal == null) {
				fail("The destination has no safe standing space.");
			} else if (horizontalDistance(start, goal) > MAX_HORIZONTAL_RANGE
					|| Math.abs(start.getY() - goal.getY()) > MAX_VERTICAL_RANGE) {
				fail("The destination is outside the V1 search range.");
			} else {
				PathNode first = new PathNode(start, null, MovementType.WALK, 0.0, heuristic(start, goal));
				open.add(first);
				nodes.put(start, first);
			}
		}

		/** Runs at most maxExpansions and maxNanos of search work. */
		public void advance(int maxExpansions, long maxNanos) {
			if (finished) return;
			long deadline = System.nanoTime() + maxNanos;
			int expanded = 0;
			while (!open.isEmpty() && expanded < maxExpansions && System.nanoTime() < deadline) {
				PathNode current = open.poll();
				if (current.closed) continue;
				current.closed = true;
				expanded++;
				if (visited.size() < 2_500) visited.add(current.position);

				if (current.position.equals(goal)) {
					result = reconstruct(current);
					finished = true;
					return;
				}
				if (nodes.size() >= MAX_EXPLORED_NODES) {
					fail("Search limit reached before a route was found.");
					return;
				}
				for (int[] direction : DIRECTIONS) {
					exploreNeighbour(current, direction[0], direction[1]);
				}
				for (int[] direction : DIRECTIONS) {
					if (direction[0] == 0 || direction[1] == 0) {
						exploreGapJump(current, direction[0], direction[1]);
					}
				}
			}
			if (open.isEmpty()) fail("No connected walking route reaches the destination.");
		}

		private void exploreNeighbour(PathNode current, int dx, int dz) {
			if (dx != 0 && dz != 0 && !diagonalClear(level, current.position, dx, dz)) return;
			BlockPos next = findWalkingNeighbour(level, current.position, dx, dz, movementProfile);
			if (next == null || !withinSearchBounds(next)) return;

			double rise = surfaceY(level, next) - surfaceY(level, current.position);
			if (rise <= 0.08 && !walkingSegmentClear(level, current.position, next)) return;
			double horizontal = dx != 0 && dz != 0 ? Math.sqrt(2.0) : 1.0;
			MovementType movement = next.getY() > current.position.getY() ? MovementType.JUMP : MovementType.WALK;
			exploreCandidate(current, next, movement,
					horizontal + Math.abs(next.getY() - current.position.getY()) * 0.4);
		}

		private void exploreGapJump(PathNode current, int dx, int dz) {
			if (!PathfinderOptions.smartJump || !com.kairokk.client.pathfinder.PathFinderSettings.sprintJump || "Careful".equals(PathfinderOptions.movementMode)) return;
			if (!hasJumpClearance(level, current.position, movementProfile)) return;
			for (int span = 2; span <= movementProfile.maxGapBlocks(); span++) {
				BlockPos gap = current.position.offset(dx * (span - 1), 0, dz * (span - 1));
				if (isStandable(level, gap) || !hasJumpClearance(level, gap, movementProfile)) break;
				BlockPos landing = findWalkingNeighbour(level, current.position.offset(dx * span, 0, dz * span),
						0, 0, movementProfile);
				if (landing == null || !withinSearchBounds(landing)) continue;
				double rise = surfaceY(level, landing) - surfaceY(level, current.position);
				if (rise > movementProfile.maxJumpRise() - 0.15 || !hasJumpClearance(level, landing, movementProfile)) continue;
				exploreCandidate(current, landing, MovementType.SPRINT_JUMP, span / movementProfile.sprintJumpSpeed());
			}
		}

		private void exploreCandidate(PathNode current, BlockPos next, MovementType movementType, double edgeCost) {
			if (PathfinderOptions.saferRoutes) {
				for (Direction direction : Direction.Plane.HORIZONTAL) {
					BlockPos support=next.relative(direction).below();
					if(level.getBlockState(support).getCollisionShape(level,support).isEmpty())edgeCost+=.35;
				}
			}
			double tentativeG = current.gCost + edgeCost;
			PathNode known = nodes.get(next);
			if (known == null) {
				PathNode created = new PathNode(next, current, movementType, tentativeG, heuristic(next, goal));
				nodes.put(next, created);
				open.add(created);
			} else if (!known.closed && tentativeG < known.gCost) {
				open.remove(known);
				known.parent = current;
				known.incomingMovement = movementType;
				known.gCost = tentativeG;
				open.add(known);
			}
		}

		private boolean withinSearchBounds(BlockPos position) {
			return horizontalDistance(start, position) <= MAX_HORIZONTAL_RANGE
					&& Math.abs(start.getY() - position.getY()) <= MAX_VERTICAL_RANGE;
		}

		private void fail(String reason) {
			failureReason = reason;
			finished = true;
		}

		public boolean isFinished() { return finished; }
		public boolean succeeded() { return result != null; }
		public List<PathStep> result() { return result == null ? List.of() : List.copyOf(result); }
		public List<BlockPos> visitedNodes() { return List.copyOf(visited); }
		public String failureReason() { return failureReason; }
		public BlockPos requestedGoal() { return requestedGoal; }
		public BlockPos resolvedGoal() { return goal; }
		public Level level() { return level; }
		public MovementProfile movementProfile() { return movementProfile; }
	}

	private static List<PathStep> reconstruct(PathNode end) {
		List<PathStep> result = new ArrayList<>();
		for (PathNode node = end; node != null; node = node.parent) {
			result.add(new PathStep(node.position, node.incomingMovement));
		}
		Collections.reverse(result);
		return List.copyOf(result);
	}

	private static BlockPos findStandableNear(Level level, BlockPos origin) {
		int[] yOffsets = {0, 1, -1, 2, -2, 3, -3};
		for (int yOffset : yOffsets) {
			BlockPos candidate = origin.offset(0, yOffset, 0);
			if (isStandable(level, candidate)) return candidate.immutable();
		}
		return null;
	}

	private static BlockPos findWalkingNeighbour(Level level, BlockPos current, int dx, int dz, MovementProfile profile) {
		int climbs=PathfinderOptions.smartJump?profile.maxClimbBlocks():0;
		int drops=Math.round(PathfinderOptions.maxFall);
		int[] yOffsets = new int[climbs + 1 + drops];
		yOffsets[0] = 0;
		for (int y = 1; y <= climbs; y++) yOffsets[y] = y;
		for (int y = 1; y <= drops; y++) yOffsets[climbs+y] = -y;
		for (int yOffset : yOffsets) {
			BlockPos candidate = current.offset(dx, yOffset, dz);
			if (!isStandable(level, candidate)) continue;
			double rise = surfaceY(level, candidate) - surfaceY(level, current);
			if (rise <= 0.08 || rise <= profile.maxJumpRise() - 0.15
					&& hasJumpClearance(level, current, profile) && hasJumpClearance(level, candidate, profile)) {
				return candidate.immutable();
			}
		}
		return null;
	}

	static boolean diagonalClear(Level level, BlockPos current, int dx, int dz) {
		return hasBodyClearance(level, current.offset(dx, 0, 0))
				&& hasBodyClearance(level, current.offset(0, 0, dz))
				&& walkingSegmentClear(level, current, current.offset(dx, 0, dz));
	}

	/** Checks the complete player-sized volume swept between two walking nodes. */
	static boolean walkingSegmentClear(Level level, BlockPos start, BlockPos end) {
		double startX = start.getX() + .5, startZ = start.getZ() + .5;
		double endX = end.getX() + .5, endZ = end.getZ() + .5;
		double dx = endX - startX, dz = endZ - startZ;
		double distance = Math.hypot(dx, dz);
		int samples = Math.max(1, (int) Math.ceil(distance * 10.0));
		for (int i = 0; i <= samples; i++) {
			double t = i / (double) samples;
			double x = startX + dx * t, z = startZ + dz * t;
			BlockPos floorCell = BlockPos.containing(x, start.getY(), z);
			double feetY = surfaceY(level, floorCell);
			if (!hasPlayerClearanceAt(level, x, feetY, z)) return false;
		}
		return true;
	}

	static boolean hasPlayerClearanceAt(Level level, double x, double feetY, double z) {
		BlockPos center = BlockPos.containing(x, feetY + .05, z);
		if (!level.isLoaded(center) || !level.isLoaded(center.above())) return false;
		AABB playerBox = new AABB(
				x - PLAYER_RADIUS, feetY + 1.0e-4, z - PLAYER_RADIUS,
				x + PLAYER_RADIUS, feetY + PLAYER_HEIGHT, z + PLAYER_RADIUS);
		return level.noCollision(playerBox);
	}

	static boolean isStandable(Level level, BlockPos feet) {
		if (!level.isInWorldBounds(feet) || !level.isLoaded(feet)) return false;
		if (!hasBodyClearance(level, feet)) return false;
		if(PathfinderOptions.avoidLava)for(Direction d:Direction.Plane.HORIZONTAL){if(level.getFluidState(feet.relative(d)).is(FluidTags.LAVA)||level.getFluidState(feet.relative(d).below()).is(FluidTags.LAVA))return false;}

		BlockPos supportPos = feet.below();
		VoxelShape support = level.getBlockState(supportPos).getCollisionShape(level, supportPos);
		if (support.isEmpty()) return false;
		double top = support.max(Direction.Axis.Y);
		return top >= 0.5 && top <= 1.001;
	}

	static double surfaceY(Level level, BlockPos feet) {
		BlockPos supportPos = feet.below();
		VoxelShape support = level.getBlockState(supportPos).getCollisionShape(level, supportPos);
		return support.isEmpty() ? feet.getY() : supportPos.getY() + support.max(Direction.Axis.Y);
	}

	static boolean isStepSupport(Level level, BlockPos feet) {
		var block = level.getBlockState(feet.below()).getBlock();
		return block instanceof StairBlock || block instanceof SlabBlock;
	}

	private static boolean hasBodyClearance(Level level, BlockPos feet) {
		BlockPos head = feet.above();
		return level.isLoaded(feet)
				&& level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
				&& level.getBlockState(head).getCollisionShape(level, head).isEmpty()
				&& fluidAllowed(level,feet) && fluidAllowed(level,head);
	}
	private static boolean fluidAllowed(Level level,BlockPos pos){var fluid=level.getFluidState(pos);return fluid.isEmpty()||fluid.is(FluidTags.WATER)&&!PathfinderOptions.avoidWater;}

	static boolean hasJumpClearance(Level level, BlockPos feet, MovementProfile profile) {
		if (!hasBodyClearance(level, feet)) return false;
		int highestOccupiedBlock = Math.max(2, (int) Math.ceil(profile.maxJumpRise() + 1.8) - 1);
		for (int y = 2; y <= highestOccupiedBlock; y++) {
			BlockPos upper = feet.above(y);
			if (!level.isLoaded(upper)
					|| !level.getBlockState(upper).getCollisionShape(level, upper).isEmpty()
					|| !level.getFluidState(upper).isEmpty()) return false;
		}
		return true;
	}

	private static double heuristic(BlockPos a, BlockPos b) {
		double dx = b.getX() - a.getX();
		double dy = b.getY() - a.getY();
		double dz = b.getZ() - a.getZ();
		return Math.sqrt(dx * dx + dz * dz) + Math.abs(dy) * 0.4;
	}

	private static double horizontalDistance(BlockPos a, BlockPos b) {
		double dx = b.getX() - a.getX();
		double dz = b.getZ() - a.getZ();
		return Math.sqrt(dx * dx + dz * dz);
	}
}
