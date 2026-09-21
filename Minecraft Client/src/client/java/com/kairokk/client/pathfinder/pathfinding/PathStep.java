package com.kairokk.client.pathfinder.pathfinding;

import net.minecraft.core.BlockPos;

/** Internal A* result edge; movementType describes travel into position. */
record PathStep(BlockPos position, MovementType movementType) {
	PathStep {
		position = position.immutable();
	}
}

