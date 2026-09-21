package com.kairokk.client.pathfinder.pathfinding;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

import com.kairokk.client.pathfinder.pathfinding.follower.PathFollower;
import com.kairokk.client.pathfinder.pathfinding.follower.PathFollower.FollowResult;
import com.kairokk.client.pathfinder.pathfinding.render.MarkerType;
import com.kairokk.client.pathfinder.pathfinding.render.PathMarker;
import com.kairokk.client.pathfinder.PathfinderOptions;

public final class NavigationManager {
	private static final NavigationManager INSTANCE = new NavigationManager();
	private static final int EXPANSIONS_PER_TICK = 700;
	private static final long SEARCH_BUDGET_NANOS = 6_000_000L;

	private final Pathfinder pathfinder = new Pathfinder();
	private final PathFollower follower = new PathFollower();
	private Pathfinder.SearchSession search;
	private Path currentPath;
	private BlockPos requestedGoal;
	private List<BlockPos> debugVisitedNodes = List.of();
	private boolean debugRenderEnabled;
	private boolean resumeAfterSearch;
	private int recoveryAttempts;

	private NavigationManager() {
	}

	public static NavigationManager getInstance() {
		return INSTANCE;
	}

	public void requestPath(Minecraft client, BlockPos requestedGoal) {
		if (client.level == null || client.player == null) return;
		this.requestedGoal = requestedGoal.immutable();
		if(!resumeAfterSearch)recoveryAttempts=0;
		follower.stop(client);
		currentPath = null;
		debugVisitedNodes = List.of();
		MovementProfile profile = MovementProfile.from(client.player);
		search = pathfinder.begin(client.level, client.player.blockPosition(), requestedGoal, profile);
		message(client, Component.literal("Calculating walking path...").withStyle(ChatFormatting.AQUA));
		message(client, Component.literal("Movement profile: " + profile.summary()).withStyle(ChatFormatting.DARK_GRAY));
	}

	public void tick(Minecraft client) {
		if (client.level == null || client.player == null) {
			if (follower.isActive()) follower.stop(client);
			search = null;
			return;
		}
		if(PathfinderOptions.pauseInGui && client.screen!=null){follower.releaseInputs(client);return;}
		if(PathfinderOptions.refreshEffects && follower.isActive() && currentPath!=null){
			MovementProfile old=currentPath.movementProfile(),now=MovementProfile.from(client.player);
			if(Math.abs(old.jumpVelocity()-now.jumpVelocity())>.01||Math.abs(old.sprintJumpSpeed()-now.sprintJumpSpeed())>.02){resumeAfterSearch=true;requestPath(client,requestedGoal);}
		}

		if (search != null) {
			if (search.level() != client.level) {
				search = null;
			} else {
				search.advance(EXPANSIONS_PER_TICK, SEARCH_BUDGET_NANOS);
				if (search.isFinished()) completeSearch(client);
			}
		}

		FollowResult result = follower.tick(client);
		if (result == FollowResult.COMPLETE) {
			recoveryAttempts=0;
			currentPath = currentPath == null ? null : currentPath.withStatus(PathStatus.COMPLETE);
			message(client, Component.literal("Destination reached.").withStyle(ChatFormatting.GREEN));
		} else if (result == FollowResult.STUCK) {
			if(PathfinderOptions.recalculateOnStuck && requestedGoal!=null && recoveryAttempts++<3){resumeAfterSearch=true;requestPath(client,requestedGoal);return;}
			currentPath = currentPath == null ? null : currentPath.withStatus(PathStatus.READY);
			message(client, Component.literal("Path following stopped: no movement progress.").withStyle(ChatFormatting.YELLOW));
		} else if (result == FollowResult.UNSUPPORTED) {
			message(client, Component.literal("Path following stopped: unsupported movement type.").withStyle(ChatFormatting.RED));
		}
	}

	private void completeSearch(Minecraft client) {
		debugVisitedNodes = search.visitedNodes();
		if (!search.succeeded()) {
			currentPath = null;
			message(client, Component.literal("Could not find a valid path to the destination.").withStyle(ChatFormatting.RED));
			message(client, Component.literal(search.failureReason()).withStyle(ChatFormatting.DARK_GRAY));
			search = null;
			return;
		}

		List<PathPoint> points = PathSmoother.simplify(search.level(), search.result(), search.movementProfile());
		double distance = calculateDistance(points);
		List<PathMarker> markers = new ArrayList<>();
		markers.add(new PathMarker(points.getFirst().position(), MarkerType.START, "START"));
		for (int i = 1; i < points.size(); i++) {
			MovementType movement = points.get(i).movementType();
			if (movement != MovementType.WALK) {
				markers.add(new PathMarker(points.get(i - 1).position(), MarkerType.ACTION, movementLabel(movement)));
			}
		}
		markers.add(new PathMarker(points.getLast().position(), MarkerType.GOAL, "GOAL"));
		currentPath = new Path(points, search.resolvedGoal(), distance, PathStatus.READY, markers, search.movementProfile());
		message(client, Component.literal("Path calculated.").withStyle(ChatFormatting.GREEN));
		message(client, Component.literal("Nodes: %d  |  Distance: %.1f blocks"
				.formatted(points.size(), distance)).withStyle(ChatFormatting.GRAY));
		search = null;
		if(resumeAfterSearch || PathfinderOptions.autoFollow){resumeAfterSearch=false;start(client);}
	}

	private static String movementLabel(MovementType movementType) {
		return switch (movementType) {
			case WALK -> "WALK";
			case STEP_UP -> "STEP";
			case JUMP -> "JUMP";
			case SPRINT_JUMP -> "SPRINT JUMP";
		};
	}

	private static double calculateDistance(List<PathPoint> points) {
		double distance = 0.0;
		for (int i = 1; i < points.size(); i++) {
			distance += points.get(i - 1).position().distanceTo(points.get(i).position());
		}
		return distance;
	}

	public boolean start(Minecraft client) {
		if (currentPath == null || currentPath.points().isEmpty()) return false;
		if (follower.start(client, currentPath)) {
			currentPath = currentPath.withStatus(PathStatus.ACTIVE);
			return true;
		}
		return false;
	}

	public void stop(Minecraft client) {
		follower.stop(client);
		resumeAfterSearch=false; recoveryAttempts=0;
		if(PathfinderOptions.clearOnDisable){currentPath=null;search=null;return;}
		if (currentPath != null && currentPath.status() == PathStatus.ACTIVE) {
			currentPath = currentPath.withStatus(PathStatus.READY);
		}
	}

	public void reset(Minecraft client) {
		stop(client);
		search = null;
		currentPath = null;
		debugVisitedNodes = List.of();
		requestedGoal = null;
	}

	public boolean refresh(Minecraft client) {
		if (requestedGoal == null || client.level == null || client.player == null) return false;
		requestPath(client, requestedGoal);
		return true;
	}

	private static void message(Minecraft client, Component component) {
		if (client.player != null) client.player.sendSystemMessage(component);
	}

	public Path currentPath() { return currentPath; }
	public int currentPointIndex() { return follower.isActive() ? follower.currentPointIndex() : 0; }
	public List<BlockPos> debugVisitedNodes() { return debugVisitedNodes; }
	public boolean isDebugRenderEnabled() { return debugRenderEnabled; }
	public boolean toggleDebugRender() { return debugRenderEnabled = !debugRenderEnabled; }
}
