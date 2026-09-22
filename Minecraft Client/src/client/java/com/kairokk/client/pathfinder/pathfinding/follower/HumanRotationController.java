package com.kairokk.client.pathfinder.pathfinding.follower;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import com.kairokk.client.pathfinder.PathFinderSettings;
import com.kairokk.client.pathfinder.RotationController;

/** Eased yaw steering shared by every movement executor. */
final class HumanRotationController {
	private final RotationController controller = new RotationController();

	/** Turns one smooth tick toward delta and returns the remaining yaw error. */
	float face(LocalPlayer player, Vec3 delta) {
		if (delta.horizontalDistanceSqr() < 1.0e-8) return 0.0f;
		if (PathFinderSettings.stopRotatingInGuis && Minecraft.getInstance().screen != null) {
			controller.reset();
			return 0.0f;
		}
		float targetYaw = (float) Math.toDegrees(Math.atan2(-delta.x, delta.z));
		// Looking up and down at every stair waypoint makes the camera fight
		// movement. Keep navigation level while yaw follows the horizontal route.
		float targetPitch = 0f;
		RotationController.Result result = controller.updateTracking(player.getYRot(), player.getXRot(), targetYaw, targetPitch, 0.05f);
		player.setYRot(result.yaw());
		player.setXRot(result.pitch());
		// Keep the local model and camera in the same orientation while the
		// controller eases toward the next waypoint.
		player.setYHeadRot(result.yaw());
		player.setYBodyRot(result.yaw());
		return RotationController.wrap(targetYaw - player.getYRot());
	}

	void reset() {
		controller.reset();
	}
}
