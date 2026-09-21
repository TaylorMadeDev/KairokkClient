package com.kairokk.client.pathfinder.pathfinding.render;

import java.util.Optional;

import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

public record PathMarker(Vec3 position, MarkerType type, Optional<String> label, Optional<Identifier> icon) {
	public PathMarker(Vec3 position, MarkerType type, String label) {
		this(position, type, Optional.ofNullable(label), Optional.empty());
	}
}

