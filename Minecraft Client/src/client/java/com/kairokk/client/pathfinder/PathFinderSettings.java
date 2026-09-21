package com.kairokk.client.pathfinder;

/** Runtime defaults for the route preview. They can be exposed in Kairokk's GUI later. */
public final class PathFinderSettings {
	private PathFinderSettings() { }

	public static boolean showGizmo = true;
	public static boolean showLabels = true;
	public static boolean smoothCamera = true;
	public static boolean sprintJump = true;
	public static float minAngle = 0.0f;
	public static float maxAngle = 180.0f;
	public static float minTime = 0.45f;
	public static float maxTime = 1.15f;
	public static float minSpeed = 20.0f;
	public static float maxSpeed = 300.0f;
	public static float targetRate = 240.0f;
	public static float jitterAmplitude = 0.12f;
	public static float smoothJitter = 0.35f;
	public static float stepJitter = 0.0f;
	public static float deadzone = 0.08f;
	public static boolean noCurve;
	public static float curveIntensity = 0.55f;
	public static float smooth = 0.72f;
	public static float mouseRoom = 72.0f;
	public static float arc = 4.0f;
	public static float humanization = 0.42f;
	public static float reactionDelay = 0.055f;
	public static float microMovement = 0.18f;
	public static float minHumanization = 0.42f;
	public static float minJitterAmplitude = 0.12f;
	public static float minArc = 4f;
	public static float minDeadzone = 0.08f;
	public static float minCurveIntensity = 0.55f;
	public static boolean limitVerticalRotations = true;
	public static boolean stopRotatingInGuis = true;
	public static boolean advancedMode;
	public static String rotationPreset = "Legit";
	// Compatibility aliases used by older screens/configs.
	public static float turnSpeed = 16.0f;
	public static float turnAcceleration = 3.0f;
	public static float ribbonWidth = 0.14f;

	public static void resetRotations() { applyPreset("Legit"); }

	public static void applyPreset(String preset) {
		rotationPreset = preset;
		switch (preset) {
			case "Balanced" -> setRotationValues(0f, 180f, .34f, 1.00f, 24f, 340f, 240f, .10f, .32f, .02f, .10f, .50f, .62f, 92f, 2.5f, .32f, .04f, .13f);
			case "Aggressive" -> setRotationValues(0f, 180f, .16f, .55f, 55f, 560f, 300f, .06f, .20f, 0f, .06f, .36f, .35f, 145f, 0f, .18f, .02f, .07f);
			case "Minimal" -> setRotationValues(0f, 180f, .48f, 1.25f, 18f, 250f, 180f, .03f, .18f, 0f, .12f, .44f, .78f, 180f, 0f, .12f, .07f, .04f);
			case "Custom" -> rotationPreset = "Custom";
			default -> setRotationValues(0f, 180f, .42f, 1.18f, 20f, 300f, 240f, .12f, .35f, 0f, .08f, .55f, .72f, 72f, 4f, .42f, .055f, .18f);
		}
	}

	private static void setRotationValues(float minA, float maxA, float minT, float maxT,
			float minS, float maxS, float rate, float jitter, float smoothJ, float stepJ,
			float zone, float curve, float smoothing, float room, float arcAmount,
			float human, float reaction, float micro) {
		minAngle=minA; maxAngle=maxA; minTime=minT; maxTime=maxT; minSpeed=minS; maxSpeed=maxS;
		targetRate=rate; jitterAmplitude=jitter; smoothJitter=smoothJ; stepJitter=stepJ;
		deadzone=zone; curveIntensity=curve; smooth=smoothing; mouseRoom=room; arc=arcAmount;
		humanization=human; reactionDelay=reaction; microMovement=micro; noCurve=false;
		minHumanization=human; minJitterAmplitude=jitter; minArc=arcAmount;
		minDeadzone=zone; minCurveIntensity=curve;
		limitVerticalRotations = true; stopRotatingInGuis = true;
	}
}
