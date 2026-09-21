package com.kairokk.client.pathfinder.pathfinding;

import net.minecraft.world.phys.Vec3;

public record PathPoint(Vec3 position, MovementType movementType) {
	public PathPoint {
		if (position == null || movementType == null) {
			throw new IllegalArgumentException("Path points require a position and movement type");
		}
	}
}

