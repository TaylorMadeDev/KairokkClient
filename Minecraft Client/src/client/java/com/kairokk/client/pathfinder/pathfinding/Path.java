package com.kairokk.client.pathfinder.pathfinding;

import java.util.List;

import net.minecraft.core.BlockPos;

import com.kairokk.client.pathfinder.pathfinding.render.PathMarker;

public record Path(
		List<PathPoint> points,
		BlockPos goal,
		double totalDistance,
		PathStatus status,
		List<PathMarker> markers,
		MovementProfile movementProfile) {

	public Path {
		points = List.copyOf(points);
		markers = List.copyOf(markers);
	}

	public Path withStatus(PathStatus newStatus) {
		return new Path(points, goal, totalDistance, newStatus, markers, movementProfile);
	}
}
