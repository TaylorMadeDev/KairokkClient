package com.kairokk.client.pathfinder.pathfinding;

import net.minecraft.core.BlockPos;

final class PathNode implements Comparable<PathNode> {
	final BlockPos position;
	PathNode parent;
	MovementType incomingMovement;
	double gCost;
	final double hCost;
	boolean closed;

	PathNode(BlockPos position, PathNode parent, MovementType incomingMovement, double gCost, double hCost) {
		this.position = position.immutable();
		this.parent = parent;
		this.incomingMovement = incomingMovement;
		this.gCost = gCost;
		this.hCost = hCost;
	}

	double fCost() {
		return gCost + hCost;
	}

	@Override
	public int compareTo(PathNode other) {
		int byF = Double.compare(fCost(), other.fCost());
		return byF != 0 ? byF : Double.compare(hCost, other.hCost);
	}
}

