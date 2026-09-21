package com.kairokk.client.pathfinder;

import java.util.ArrayList;
import java.util.List;

/** Shared, planned yaw/pitch solver used by gameplay steering and the live preview. */
public final class RotationController {
	private float yawVelocity, pitchVelocity, jitterPhase, steppedJitter;
	private int stepCounter;
	private boolean planned;
	private float startYaw, startPitch, endYaw, endPitch, elapsed, duration;
	private float reactionRemaining, mouseUsed, mouseResetRemaining;
	private float planArc, planHuman, planJitter, planCurve, planTolerance, planSpeed, phase;
	private float planSmooth, planStepJitter, planMicro;
	private boolean tracking;
	private float trackedYaw, trackedPitch;

	public Result update(float yaw, float pitch, float targetYaw, float targetPitch, float deltaSeconds) {
		float dt = Math.clamp(deltaSeconds, 0.001f, 0.10f);
		if (!planned || Math.abs(wrap(targetYaw - endYaw)) > .45f || Math.abs(targetPitch - endPitch) > .45f) {
			beginPlan(yaw, pitch, targetYaw, targetPitch);
		}
		int steps = Math.clamp((int) Math.ceil(dt * Math.max(20f, PathFinderSettings.targetRate)), 1, 32);
		float stepDt = dt / steps;
		Result result = new Result(yaw, pitch, yawVelocity, pitchVelocity);
		for (int i = 0; i < steps; i++) result = updateStep(result.yaw, result.pitch, stepDt);
		return result;
	}

	/**
	 * Continuously follows a moving target without rebuilding an eased animation.
	 *
	 * A waypoint's bearing changes a little on every movement tick. Treating each
	 * change as a brand-new animation repeatedly re-applies the reaction delay and
	 * zero-speed ease-in, which makes navigation alternate between turning and
	 * walking. This critically damped tracker preserves angular velocity while the
	 * target moves, so corners remain smooth and a new path can still turn quickly.
	 */
	public Result updateTracking(float yaw, float pitch, float targetYaw, float targetPitch, float deltaSeconds) {
		float dt = Math.clamp(deltaSeconds, 0.001f, 0.10f);
		float pitchLimit = PathFinderSettings.limitVerticalRotations ? 75f : 89f;
		float limitedPitch = Math.clamp(targetPitch, -pitchLimit, pitchLimit);
		if (!tracking) {
			tracking = true;
			trackedYaw = yaw + wrap(targetYaw - yaw);
			trackedPitch = limitedPitch;
			yawVelocity = pitchVelocity = 0f;
		} else {
			// Filter the aim point itself. This absorbs sub-degree waypoint drift
			// without introducing latency when the route takes a real corner.
			float targetBlend = 1f - (float) Math.exp(-dt * 14f);
			trackedYaw += wrap(targetYaw - trackedYaw) * targetBlend;
			trackedPitch += (limitedPitch - trackedPitch) * targetBlend;
		}

		float maxSpeed = Math.max(80f, PathFinderSettings.maxSpeed);
		float smoothTime = lerp(.075f, .30f, Math.clamp(PathFinderSettings.smooth, 0f, 1f));
		AxisResult yawResult = smoothDampAngle(yaw, trackedYaw, yawVelocity, smoothTime, maxSpeed, dt);
		AxisResult pitchResult = smoothDamp(pitch, trackedPitch, pitchVelocity, smoothTime * 1.18f, maxSpeed * .72f, dt);
		yawVelocity = yawResult.velocity;
		pitchVelocity = pitchResult.velocity;

		float tolerance = Math.max(.01f, PathFinderSettings.deadzone);
		float nextYaw = Math.abs(wrap(targetYaw - yawResult.value)) <= tolerance && Math.abs(yawVelocity) < 1f
				? targetYaw : yawResult.value;
		float nextPitch = Math.abs(limitedPitch - pitchResult.value) <= tolerance && Math.abs(pitchVelocity) < 1f
				? limitedPitch : pitchResult.value;
		return new Result(wrap(nextYaw), Math.clamp(nextPitch, -pitchLimit, pitchLimit), yawVelocity, pitchVelocity);
	}

	private static AxisResult smoothDampAngle(float current, float target, float velocity,
			float smoothTime, float maxSpeed, float dt) {
		return smoothDamp(current, current + wrap(target - current), velocity, smoothTime, maxSpeed, dt);
	}

	/** Critically damped spring with an explicit angular speed limit. */
	private static AxisResult smoothDamp(float current, float target, float velocity,
			float smoothTime, float maxSpeed, float dt) {
		float time = Math.max(.025f, smoothTime);
		float omega = 2f / time;
		float x = omega * dt;
		float decay = 1f / (1f + x + .48f * x * x + .235f * x * x * x);
		float change = current - target;
		float originalTarget = target;
		float maxChange = maxSpeed * time;
		change = Math.clamp(change, -maxChange, maxChange);
		target = current - change;
		float temp = (velocity + omega * change) * dt;
		float nextVelocity = (velocity - omega * temp) * decay;
		float value = target + (change + temp) * decay;
		if ((originalTarget - current > 0f) == (value > originalTarget)) {
			value = originalTarget;
			nextVelocity = 0f;
		}
		return new AxisResult(value, nextVelocity);
	}

	private void beginPlan(float yaw, float pitch, float targetYaw, float targetPitch) {
		planned = true;
		startYaw = yaw; startPitch = pitch;
		endYaw = yaw + wrap(targetYaw - yaw);
		float pitchLimit = PathFinderSettings.limitVerticalRotations ? 75f : 89f;
		endPitch = Math.clamp(targetPitch, -pitchLimit, pitchLimit);
		elapsed = mouseUsed = mouseResetRemaining = 0f;
		planArc = pick(PathFinderSettings.minArc, PathFinderSettings.arc);
		planHuman = pick(PathFinderSettings.minHumanization, PathFinderSettings.humanization);
		planJitter = pick(PathFinderSettings.minJitterAmplitude, PathFinderSettings.jitterAmplitude);
		planCurve = PathFinderSettings.noCurve ? 0f : pick(PathFinderSettings.minCurveIntensity, PathFinderSettings.curveIntensity);
		planTolerance = pick(PathFinderSettings.minDeadzone, PathFinderSettings.deadzone);
		planSpeed = Math.max(1f, pick(PathFinderSettings.minSpeed, PathFinderSettings.maxSpeed));
		planSmooth = PathFinderSettings.noCurve ? 0f : PathFinderSettings.smooth;
		planStepJitter = PathFinderSettings.stepJitter; planMicro = PathFinderSettings.microMovement;
		phase = java.util.concurrent.ThreadLocalRandom.current().nextFloat() * 6.283185f;
		float distance = (float) Math.hypot(endYaw - startYaw, endPitch - startPitch);
		float nominalSpeed = Math.max(1f, planSpeed);
		float variation = 1f + planHuman * .10f
				* (float) Math.sin(Math.toRadians(targetYaw * 2.17f + targetPitch * 1.31f));
		duration = Math.clamp(distance / nominalSpeed * variation,
				Math.min(PathFinderSettings.minTime, PathFinderSettings.maxTime),
				Math.max(PathFinderSettings.minTime, PathFinderSettings.maxTime));
		duration = Math.max(.05f, Math.max(duration, distance / planSpeed));
		reactionRemaining = Math.max(0f, PathFinderSettings.reactionDelay);
	}

	private Result updateStep(float yaw, float pitch, float dt) {
		if (reactionRemaining > 0f) {
			reactionRemaining -= dt;
			return stopped(yaw, pitch, dt, 15f);
		}
		if (mouseResetRemaining > 0f) {
			mouseResetRemaining -= dt;
			if (mouseResetRemaining <= 0f) mouseUsed = 0f;
			return stopped(yaw, pitch, dt, 18f);
		}

		float nextElapsed = Math.min(duration, elapsed + dt);
		PathSample waypoint = sample(nextElapsed / duration);
		float distance = (float) Math.hypot(wrap(waypoint.yaw - yaw), waypoint.pitch - pitch);
		// Advance along the planned curve instead of smoothing velocity across it.
		if (distance > planSpeed * dt) {
			float low = elapsed, high = nextElapsed;
			for (int i = 0; i < 16; i++) {
				float middle = (low + high) * .5f;
				PathSample candidate = sample(middle / duration);
				if (Math.hypot(wrap(candidate.yaw - yaw), candidate.pitch - pitch) > planSpeed * dt) high = middle;
				else low = middle;
			}
			nextElapsed = low; waypoint = sample(nextElapsed / duration);
			distance = (float) Math.hypot(wrap(waypoint.yaw - yaw), waypoint.pitch - pitch);
		}
		if (mouseUsed + distance >= Math.max(8f, PathFinderSettings.mouseRoom) && progress() < .98f) {
			mouseUsed = 0f;
			mouseResetRemaining = lerp(.065f, .17f, Math.clamp(planHuman, 0f, 1f));
			return stopped(yaw, pitch, dt, 20f);
		}
		elapsed = nextElapsed; mouseUsed += distance;
		yawVelocity = wrap(waypoint.yaw - yaw) / dt;
		pitchVelocity = (waypoint.pitch - pitch) / dt;
		return new Result(waypoint.yaw, waypoint.pitch, yawVelocity, pitchVelocity);
	}

	private Result stopped(float yaw, float pitch, float dt, float braking) {
		float retain = Math.max(0f, 1f - dt * braking);
		yawVelocity *= retain; pitchVelocity *= retain;
		return new Result(yaw, pitch, yawVelocity, pitchVelocity);
	}

	private PathSample sample(float rawT) {
		float t = Math.clamp(rawT, 0f, 1f), eased;
		if (planSmooth == 0f && planCurve == 0f) eased = t;
		else {
			float smoothStep = t * t * (3f - 2f * t);
			eased = lerp(t, smoothStep, Math.clamp(planSmooth + planCurve * .35f, 0f, 1f));
		}
		float oneMinus = 1f - eased;
		float controlYaw = (startYaw + endYaw) * .5f;
		// Minecraft pitch is negative when looking up: positive Arc bends upward.
		float controlPitch = (startPitch + endPitch) * .5f - planArc;
		float pathYaw = oneMinus * oneMinus * startYaw + 2f * oneMinus * eased * controlYaw + eased * eased * endYaw;
		float pathPitch = oneMinus * oneMinus * startPitch + 2f * oneMinus * eased * controlPitch + eased * eased * endPitch;
		float envelope = (float) Math.sin(Math.PI * eased);
		float noise = ((float) Math.sin(eased * 12f + phase) * planJitter
				+ (float) Math.sin(eased * 25f + phase) * planStepJitter
				+ (float) Math.sin(eased * 19f + phase) * planMicro) * planHuman * envelope;
		return new PathSample(wrap(pathYaw + noise), Math.clamp(pathPitch + noise * .38f, -89f, 89f));
	}

	public List<PathSample> sampleRemainingPath(int count) {
		List<PathSample> path = new ArrayList<>();
		if (!planned) return path;
		float from = duration <= .001f ? 1f : Math.clamp(elapsed / duration, 0f, 1f);
		int points = Math.max(2, count);
		for (int i = 0; i < points; i++) path.add(sample(lerp(from, 1f, i / (float) (points - 1))));
		return path;
	}

	/** Keep the entire planned curve visible rather than shrinking it to its straight end. */
	public List<PathSample> samplePlannedPath(int count) {
		List<PathSample> path = new ArrayList<>();
		if (!planned) return path;
		int points = Math.max(2, count);
		for (int i = 0; i < points; i++) path.add(sample(i / (float) (points - 1)));
		return path;
	}

	public boolean isComplete() { return planned && elapsed >= duration; }
	public float aimTolerance() { return planTolerance; }
	private static float pick(float low, float high) {
		if (!PathFinderSettings.advancedMode) return high;
		return lerp(Math.min(low, high), Math.max(low, high), java.util.concurrent.ThreadLocalRandom.current().nextFloat());
	}
	public boolean isResettingMouse() { return mouseResetRemaining > 0f; }
	public float progress() { return duration <= .001f ? 1f : Math.clamp(elapsed / duration, 0f, 1f); }
	public void reset() {
		yawVelocity = pitchVelocity = jitterPhase = steppedJitter = 0f;
		elapsed = duration = reactionRemaining = mouseUsed = mouseResetRemaining = 0f;
		stepCounter = 0; planned = false; tracking = false;
	}
	public static float wrap(float degrees) { float v = degrees % 360f; if (v >= 180f) v -= 360f; if (v < -180f) v += 360f; return v; }
	private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
	public record Result(float yaw, float pitch, float yawVelocity, float pitchVelocity) {}
	public record PathSample(float yaw, float pitch) {}
	private record AxisResult(float value, float velocity) {}
}
