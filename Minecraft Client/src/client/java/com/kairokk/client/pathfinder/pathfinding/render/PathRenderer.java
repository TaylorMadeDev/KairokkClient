package com.kairokk.client.pathfinder.pathfinding.render;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4fc;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import com.kairokk.client.pathfinder.PathFinderSettings;
import com.kairokk.client.pathfinder.PathfinderOptions;
import net.minecraft.client.Minecraft;
import com.kairokk.client.pathfinder.pathfinding.MovementType;
import com.kairokk.client.pathfinder.pathfinding.NavigationManager;
import com.kairokk.client.pathfinder.pathfinding.Path;
import com.kairokk.client.pathfinder.pathfinding.PathPoint;
import com.kairokk.client.pathfinder.pathfinding.PathStatus;

/**
 * Extracts immutable path data, then draws a camera-facing quad ribbon through
 * Blaze3D. No world objects are read during the draw phase.
 */
public final class PathRenderer {
	private static final PathRenderer INSTANCE = new PathRenderer();
	private static final int ARC_SUBDIVISIONS = 24;
	private static final double SPRINT_JUMP_SPAN = 3.80;
	private static final double FINAL_APPROACH_LENGTH = 1.65;
	private static final double SPRINT_ARC_HEIGHT = 0.92;
	private static final double STEP_ARC_HEIGHT = 0.72;

	private volatile RenderState renderState;
	private float ribbonWidth = 0.14f;
	private long lastExtract;

	private PathRenderer() {
	}

	public static PathRenderer getInstance() {
		return INSTANCE;
	}

	public void extract(LevelExtractionContext context) {
		NavigationManager navigation = NavigationManager.getInstance();
		Path path = navigation.currentPath();
		if (path == null || path.points().isEmpty()) {
			renderState = null;
			return;
		}
		long now=System.nanoTime();if(renderState!=null && now-lastExtract<1_000_000_000L/PathfinderOptions.updateRate)return;lastExtract=now;

		List<RenderPoint> points = path.points().stream()
				.map(point -> new RenderPoint(point.position(), point.movementType()))
				.toList();
		List<MarkerState> markers = path.markers().stream()
				.map(marker -> new MarkerState(marker.position(), marker.type(), marker.label().orElse("")))
				.toList();
		List<Vec3> debugPoints = List.of();
		if (navigation.isDebugRenderEnabled()) {
			List<BlockPos> visited = navigation.debugVisitedNodes();
			int stride = Math.max(1, visited.size() / 600);
			List<Vec3> sampled = new ArrayList<>();
			for (int i = 0; i < visited.size(); i += stride) {
				sampled.add(Vec3.atBottomCenterOf(visited.get(i)).add(0.0, 0.035, 0.0));
			}
			debugPoints = List.copyOf(sampled);
		}
		renderState = new RenderState(points, markers, debugPoints, navigation.currentPointIndex(), path.status());
	}

	public void draw(LevelRenderContext context) {
		if (!PathFinderSettings.showGizmo) return;
		if("Hidden".equals(PathfinderOptions.renderMode))return;
		if(!PathfinderOptions.thirdPerson&&!Minecraft.getInstance().options.getCameraType().isFirstPerson())return;
		RenderState state = renderState;
		if (state == null) return;
		RenderType renderType = RenderTypes.debugQuads();
		VertexConsumer vertices = context.bufferSource().getBuffer(renderType);
		buildGeometry(context, vertices, state);
		context.bufferSource().endBatch(renderType);
	}

	private void buildGeometry(LevelRenderContext context, VertexConsumer vertices, RenderState state) {
		PoseStack poses = context.poseStack();
		Vec3 camera = context.levelState().cameraRenderState.pos;
		poses.pushPose();
		poses.translate(-camera.x, -camera.y, -camera.z);
		Matrix4fc matrix = poses.last().pose();
		for (int i = 0; i < state.points().size() - 1; i++) {
			Color color = segmentColor(state, i);
			RenderPoint from = state.points().get(i);
			RenderPoint to = state.points().get(i + 1);
			double distance=from.position().distanceTo(camera);if(distance>PathfinderOptions.renderDistance)continue;
			if(PathfinderOptions.fade)color=new Color(color.r,color.g,color.b,color.a*(float)Math.clamp(1-distance/PathfinderOptions.renderDistance,.1,1));
			if(!"Line Only".equals(PathfinderOptions.renderMode)&&!"None".equals(PathfinderOptions.nodeStyle))addNode(matrix,vertices,from.position().add(0,.14,0),.12*PathfinderOptions.nodeSize,color(PathfinderOptions.nodeColor,.9f));
			if("Nodes Only".equals(PathfinderOptions.renderMode))continue;
			if(PathfinderOptions.glow)addPathSegment(matrix,vertices,from,to,camera,PathfinderOptions.lineWidth*.055,new Color(color.r,color.g,color.b,color.a*.12f));
			addPathSegment(matrix, vertices, from, to, camera,
					PathfinderOptions.lineWidth*.025, color);
			if (PathFinderSettings.showLabels
					&& (i == 0 || to.movementType() != state.points().get(i).movementType())) {
				Vec3 middle = lerp(from.position(), to.position(), 0.5).add(0.0, 0.58, 0.0);
				addSmallLabel(matrix, vertices, movementLabel(to.movementType()), middle, camera);
			}
		}

		for (MarkerState marker : state.markers()) {
			if (marker.type() == MarkerType.GOAL) {
				addMarker(matrix, vertices, marker.position().add(0.0, 0.64, 0.0), 0.30,
						color(PathfinderOptions.targetColor,.94f));
			} else if (marker.type() == MarkerType.START) {
				addMarker(matrix, vertices, marker.position().add(0.0, 0.18, 0.0), 0.16,
						new Color(0.12f, 0.92f, 0.86f, 0.88f));
			} else if (marker.type() == MarkerType.ACTION) {
				Color actionColor = new Color(0.94f, 0.26f, 0.78f, 0.94f);
				addMarker(matrix, vertices, marker.position().add(0.0, 0.30, 0.0), 0.20, actionColor);
			}
		}

		for (Vec3 debugPoint : state.debugPoints()) {
			addHorizontalDiamond(matrix, vertices, debugPoint, 0.055,
					color(PathfinderOptions.invalidColor,.38f));
		}
		poses.popPose();
	}

	private static String movementLabel(MovementType movement) {
		return switch (movement) {
			case WALK -> "WALK";
			case STEP_UP -> "STEP";
			case JUMP -> "JUMP";
			case SPRINT_JUMP -> "SPRINT JUMP";
		};
	}

	private static void addSmallLabel(Matrix4fc matrix, VertexConsumer vertices, String label, Vec3 center, Vec3 camera) {
		Vec3 facing = camera.subtract(center);
		Vec3 right = Vec3.Y_AXIS.cross(facing);
		if (right.lengthSqr() < 1.0e-8) right = Vec3.X_AXIS;
		right = right.normalize();
		Vec3 toward = facing.lengthSqr() < 1.0e-8 ? Vec3.Z_AXIS : facing.normalize();
		double pixel = 0.018;
		double advance = pixel * 4.0;
		double width = Math.max(pixel * 3.0, label.length() * advance - pixel);
		Vec3 cursor = center.subtract(right.scale(width * 0.5)).add(toward.scale(0.01));
		Color text = new Color(0.90f, 0.96f, 1.0f, 0.96f);
		for (int index = 0; index < label.length(); index++) {
			String glyph = smallGlyph(label.charAt(index));
			for (int row = 0; row < 5; row++) for (int column = 0; column < 3; column++) {
				if (glyph.charAt(row * 3 + column) != '1') continue;
				Vec3 p = cursor.add(right.scale(column * pixel)).add(0.0, (4 - row) * pixel, 0.0);
				addLabelQuad(matrix, vertices, p, right, pixel, text);
			}
			cursor = cursor.add(right.scale(advance));
		}
	}

	private static void addLabelQuad(Matrix4fc matrix, VertexConsumer vertices, Vec3 p, Vec3 right,
			double size, Color color) {
		Vec3 up = new Vec3(0.0, size, 0.0);
		Vec3 across = right.scale(size * 0.84);
		vertex(matrix, vertices, p, color);
		vertex(matrix, vertices, p.add(across), color);
		vertex(matrix, vertices, p.add(across).add(up), color);
		vertex(matrix, vertices, p.add(up), color);
	}

	private static String smallGlyph(char c) {
		return switch (c) {
			case 'A' -> "010101111101101"; case 'E' -> "111100110100111";
			case 'I' -> "111010010010111"; case 'J' -> "001001001101010";
			case 'K' -> "101101110101101"; case 'M' -> "101111111101101";
			case 'N' -> "101111111111101"; case 'P' -> "110101110100100";
			case 'R' -> "110101110101101"; case 'S' -> "111100111001111";
			case 'T' -> "111010010010010"; case 'U' -> "101101101101111";
			case 'W' -> "101101111111101"; case 'L' -> "100100100100111";
			case ' ' -> "000000000000000"; default -> "111001010000010";
		};
	}

	private static Color segmentColor(RenderState state, int segmentIndex) {
		MovementType movement = state.points().get(segmentIndex + 1).movementType();
		if (state.status() == PathStatus.COMPLETE) return new Color(0.22f, 0.72f, 0.52f, 0.34f);
		if (state.status() == PathStatus.ACTIVE && segmentIndex < Math.max(0, state.currentPointIndex() - 1)) {
			return new Color(0.20f, 0.72f, 0.76f, 0.30f);
		}
		return color(PathfinderOptions.pathColor,.88f);
	}
	private static Color color(int argb,float opacity){return new Color((argb>>16&255)/255f,(argb>>8&255)/255f,(argb&255)/255f,opacity);}

	private static void addPathSegment(Matrix4fc matrix, VertexConsumer vertices, RenderPoint from, RenderPoint to,
			Vec3 camera, double halfWidth, Color color) {
		if (to.movementType() == MovementType.SPRINT_JUMP) {
			addSprintJumpTrajectory(matrix, vertices, from.position(), to.position(), camera, halfWidth, color);
		} else if (to.movementType() == MovementType.JUMP) {
			addJumpArc(matrix, vertices, from.position(), to.position(), STEP_ARC_HEIGHT,
					camera, halfWidth, color);
		} else if (to.movementType() == MovementType.STEP_UP) {
			addStepCurve(matrix, vertices, from.position(), to.position(), camera, halfWidth, color);
		} else {
			addRibbonSegment(matrix, vertices, from.position(), to.position(), camera, halfWidth, color);
		}
	}

	private static void addStepCurve(Matrix4fc matrix, VertexConsumer vertices, Vec3 start, Vec3 end,
			Vec3 camera, double halfWidth, Color color) {
		Vec3 previous = start;
		for (int step = 1; step <= ARC_SUBDIVISIONS; step++) {
			double t = step / (double) ARC_SUBDIVISIONS;
			double eased = t * t * (3.0 - 2.0 * t);
			Vec3 current = new Vec3(start.x + (end.x - start.x) * t,
					start.y + (end.y - start.y) * eased, start.z + (end.z - start.z) * t);
			addRibbonSegment(matrix, vertices, previous, current, camera, halfWidth, color);
			previous = current;
		}
	}

	private static void addSprintJumpTrajectory(Matrix4fc matrix, VertexConsumer vertices, Vec3 start, Vec3 end,
			Vec3 camera, double halfWidth, Color color) {
		double horizontalDistance = horizontalDistance(start, end);
		if (horizontalDistance < 3.0) {
			addJumpArc(matrix, vertices, start, end, SPRINT_ARC_HEIGHT, camera, halfWidth, color);
			return;
		}

		double jumpableDistance = Math.max(0.0, horizontalDistance - FINAL_APPROACH_LENGTH);
		int jumpCount = Math.max(1, (int) Math.floor(jumpableDistance / SPRINT_JUMP_SPAN));
		double archedDistance = Math.min(horizontalDistance,
				Math.min(jumpableDistance, jumpCount * SPRINT_JUMP_SPAN));
		double arcSpan = archedDistance / jumpCount;
		for (int jump = 0; jump < jumpCount; jump++) {
			double fromProgress = (jump * arcSpan) / horizontalDistance;
			double toProgress = ((jump + 1) * arcSpan) / horizontalDistance;
			addJumpArc(matrix, vertices, lerp(start, end, fromProgress), lerp(start, end, toProgress),
					SPRINT_ARC_HEIGHT, camera, halfWidth, color);
		}

		if (archedDistance < horizontalDistance - 1.0e-6) {
			addRibbonSegment(matrix, vertices, lerp(start, end, archedDistance / horizontalDistance), end,
					camera, halfWidth, color);
		}
	}

	private static void addJumpArc(Matrix4fc matrix, VertexConsumer vertices, Vec3 start, Vec3 end,
			double arcHeight, Vec3 camera, double halfWidth, Color color) {
		Vec3 previous = start;
		for (int step = 1; step <= ARC_SUBDIVISIONS; step++) {
			double progress = step / (double) ARC_SUBDIVISIONS;
			Vec3 base = lerp(start, end, progress);
			double lift = 4.0 * arcHeight * progress * (1.0 - progress);
			Vec3 current = base.add(0.0, lift, 0.0);
			addRibbonSegment(matrix, vertices, previous, current, camera, halfWidth, color);
			previous = current;
		}
	}

	private static Vec3 lerp(Vec3 start, Vec3 end, double progress) {
		return new Vec3(
				start.x + (end.x - start.x) * progress,
				start.y + (end.y - start.y) * progress,
				start.z + (end.z - start.z) * progress);
	}

	private static double horizontalDistance(Vec3 start, Vec3 end) {
		double dx = end.x - start.x;
		double dz = end.z - start.z;
		return Math.sqrt(dx * dx + dz * dz);
	}

	private static void addRibbonSegment(Matrix4fc matrix, VertexConsumer vertices, Vec3 a, Vec3 b,
			Vec3 camera, double halfWidth, Color color) {
		if(a.distanceTo(camera)>PathfinderOptions.renderDistance&&b.distanceTo(camera)>PathfinderOptions.renderDistance)return;
		if("Solid".equals(PathfinderOptions.lineStyle)){addSolidRibbon(matrix,vertices,a,b,camera,halfWidth,color);return;}
		double length=a.distanceTo(b),period="Dotted".equals(PathfinderOptions.lineStyle)?.22:.65;
		double visible="Dotted".equals(PathfinderOptions.lineStyle)?.05:.4;
		// World-distance phase keeps dashes continuous through subdivided jump arcs.
		double phase=a.x+a.z+a.y;int steps=Math.max(1,(int)Math.ceil(length/.025));
		for(int i=0;i<steps;i++){double offset=(phase+length*i/steps)%period;if(offset<0)offset+=period;if(offset<visible)addSolidRibbon(matrix,vertices,lerp(a,b,i/(double)steps),lerp(a,b,(i+1)/(double)steps),camera,halfWidth,color);}
	}
	private static void addSolidRibbon(Matrix4fc matrix, VertexConsumer vertices, Vec3 a, Vec3 b,
			Vec3 camera, double halfWidth, Color color) {
		Vec3 direction = b.subtract(a);
		if (direction.lengthSqr() < 1.0e-8) return;
		Vec3 overlap = direction.normalize().scale(0.012);
		a = a.subtract(overlap);
		b = b.add(overlap);
		direction = b.subtract(a);
		Vec3 midpoint = a.add(b).scale(0.5);
		Vec3 side = direction.cross(camera.subtract(midpoint));
		if (side.lengthSqr() < 1.0e-8) side = direction.cross(Vec3.Y_AXIS);
		if (side.lengthSqr() < 1.0e-8) side = Vec3.X_AXIS;
		side = side.normalize().scale(halfWidth);
		vertex(matrix, vertices, a.subtract(side), color);
		vertex(matrix, vertices, a.add(side), color);
		vertex(matrix, vertices, b.add(side), color);
		vertex(matrix, vertices, b.subtract(side), color);
	}

	private static void addMarker(Matrix4fc matrix, VertexConsumer vertices, Vec3 center, double radius, Color color) {
		addDiamond(matrix, vertices, center, new Vec3(radius, 0, 0), new Vec3(0, radius * 1.35, 0), color);
		addDiamond(matrix, vertices, center, new Vec3(0, 0, radius), new Vec3(0, radius * 1.35, 0), color);
		addHorizontalDiamond(matrix, vertices, center, radius, color);
	}
	private static void addNode(Matrix4fc matrix,VertexConsumer vertices,Vec3 center,double radius,Color color){
		if("Diamond".equals(PathfinderOptions.nodeStyle)){addMarker(matrix,vertices,center,radius,color);return;}
		Vec3[] v=new Vec3[8];for(int i=0;i<8;i++)v[i]=center.add((i&1)==0?-radius:radius,(i&2)==0?-radius:radius,(i&4)==0?-radius:radius);
		int[][] faces={{0,1,3,2},{4,6,7,5},{0,4,5,1},{2,3,7,6},{0,2,6,4},{1,5,7,3}};
		for(int[] f:faces)for(int index:f)vertex(matrix,vertices,v[index],color);
	}

	private static void addHorizontalDiamond(Matrix4fc matrix, VertexConsumer vertices, Vec3 center,
			double radius, Color color) {
		addDiamond(matrix, vertices, center, new Vec3(radius, 0, 0), new Vec3(0, 0, radius), color);
	}

	private static void addDiamond(Matrix4fc matrix, VertexConsumer vertices, Vec3 center,
			Vec3 axisA, Vec3 axisB, Color color) {
		vertex(matrix, vertices, center.add(axisA), color);
		vertex(matrix, vertices, center.add(axisB), color);
		vertex(matrix, vertices, center.subtract(axisA), color);
		vertex(matrix, vertices, center.subtract(axisB), color);
	}

	private static void vertex(Matrix4fc matrix, VertexConsumer vertices, Vec3 position, Color color) {
		vertices.addVertex(matrix, (float) position.x, (float) position.y, (float) position.z)
				.setColor(color.r(), color.g(), color.b(), color.a());
	}

	public float ribbonWidth() { return ribbonWidth; }
	public void setRibbonWidth(float width) { ribbonWidth = Math.clamp(width, 0.04f, 0.50f); }
	public static void close() { }

	private record RenderState(List<RenderPoint> points, List<MarkerState> markers, List<Vec3> debugPoints,
			int currentPointIndex, PathStatus status) {
	}
	private record RenderPoint(Vec3 position, MovementType movementType) { }
	private record MarkerState(Vec3 position, MarkerType type, String label) { }
	private record Color(float r, float g, float b, float a) { }
}
