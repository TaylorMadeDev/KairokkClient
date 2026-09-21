package com.kairokk.client.pathfinder.pathfinding.follower;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

import com.kairokk.client.pathfinder.pathfinding.PathPoint;
import com.kairokk.client.pathfinder.PathfinderOptions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

public final class WalkExecutor implements MovementExecutor {
	private final HumanRotationController rotation;
	private float paceAccumulator;

	WalkExecutor(HumanRotationController rotation) {
		this.rotation = rotation;
	}

	@Override
	public void tick(Minecraft client, PathPoint target) {
		if (client.player == null) return;
		Vec3 delta = target.position().subtract(client.player.position());
		float yawError = Math.abs(rotation.face(client.player, delta));
		boolean alignedToMove = yawError < 70.0f;
		paceAccumulator+=PathfinderOptions.walkSpeed*("Careful".equals(PathfinderOptions.movementMode)?.65f:1f);
		boolean pace=paceAccumulator>=1f;if(pace)paceAccumulator-=1f;
		boolean supported=true;
		if(PathfinderOptions.safeWalk && client.level!=null && client.player.onGround() && delta.horizontalDistance()>.3){
			Vec3 ahead=client.player.position().add(delta.normalize().scale(.55));BlockPos feet=BlockPos.containing(ahead);
			supported=false;for(int drop=1;drop<=PathfinderOptions.maxFall+1;drop++){BlockPos p=feet.below(drop);if(!client.level.getBlockState(p).getCollisionShape(client.level,p).isEmpty()){supported=true;break;}}
		}
		client.options.keyUp.setDown(alignedToMove && pace && supported);
		client.options.keySprint.setDown("Fast".equals(PathfinderOptions.movementMode)&&alignedToMove);
		client.options.keyJump.setDown(target.movementType() == com.kairokk.client.pathfinder.pathfinding.MovementType.JUMP
				&& alignedToMove && client.player.onGround());
	}

	@Override
	public void stop(Minecraft client) {
		client.options.keyUp.setDown(false);
		client.options.keyJump.setDown(false);
		client.options.keySprint.setDown(false);
		rotation.reset();
	}
}
