package com.kairokk.client.pathfinder.pathfinding.follower;

import net.minecraft.client.Minecraft;
import com.kairokk.client.pathfinder.pathfinding.PathPoint;

public interface MovementExecutor {
	void tick(Minecraft client, PathPoint target);
	void stop(Minecraft client);

	default double arrivalTolerance() {
		return 0.48;
	}
}

