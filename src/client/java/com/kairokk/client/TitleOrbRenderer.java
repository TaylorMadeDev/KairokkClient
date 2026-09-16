package com.kairokk.client;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import icyllis.modernui.mc.MuiModApi;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;

public final class TitleOrbRenderer {
	private static final RenderPipeline RADIAL_ORB_PIPELINE = RenderPipelines.register(
		RenderPipeline.builder()
			.withLocation(Identifier.fromNamespaceAndPath("kairokk", "pipeline/title_orb"))
			.withVertexShader(Identifier.withDefaultNamespace("core/position_tex_color"))
			.withFragmentShader(Identifier.fromNamespaceAndPath("kairokk", "core/title_orb"))
			.withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
			.withUniform("Projection", UniformType.UNIFORM_BUFFER)
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT))
			.withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS)
			.build()
	);

	private TitleOrbRenderer() {
	}

	/** Ensures the pipeline is registered before Minecraft loads client shaders. */
	public static void initialize() {
	}

	public static void drawSoftOrb(GuiGraphicsExtractor graphics, int centerX, int centerY, int radius, int color, int alpha) {
		if (radius <= 0 || alpha <= 0) {
			return;
		}

		drawOrbLayer(graphics, centerX, centerY, Math.round(radius * 3.0F), color, Math.max(1, alpha / 12));
		drawOrbLayer(graphics, centerX, centerY, Math.round(radius * 1.8F), color, Math.max(1, alpha / 5));
		drawOrbLayer(graphics, centerX, centerY, radius, color, alpha);
	}

	private static void drawOrbLayer(GuiGraphicsExtractor graphics, int centerX, int centerY, int radius, int color, int alpha) {
		int tint = (Math.min(alpha, 255) << 24) | (color & 0xFFFFFF);
		float left = centerX - radius;
		float top = centerY - radius;
		float right = centerX + radius;
		float bottom = centerY + radius;
		Matrix3x2f pose = new Matrix3x2f(graphics.pose());
		ScreenRectangle scissor = MuiModApi.get().peekScissorStack(graphics);
		MuiModApi.get().submitGuiElementRenderState(
			graphics, new RadialOrbRenderState(RADIAL_ORB_PIPELINE, pose, left, top, right, bottom, tint, scissor)
		);
	}

	private record RadialOrbRenderState(
		RenderPipeline pipeline,
		Matrix3x2f pose,
		float left,
		float top,
		float right,
		float bottom,
		int color,
		@Nullable ScreenRectangle scissorArea,
		@Nullable ScreenRectangle bounds
	) implements GuiElementRenderState {
		private RadialOrbRenderState(
			RenderPipeline pipeline,
			Matrix3x2f pose,
			float left,
			float top,
			float right,
			float bottom,
			int color,
			@Nullable ScreenRectangle scissorArea
		) {
			this(
				pipeline,
				pose,
				left,
				top,
				right,
				bottom,
				color,
				scissorArea,
				getBounds(left, top, right, bottom, pose, scissorArea)
			);
		}

		@Override
		public TextureSetup textureSetup() {
			return TextureSetup.noTexture();
		}

		@Override
		public void buildVertices(VertexConsumer consumer) {
			consumer.addVertexWith2DPose(pose, left, top).setUv(0.0F, 0.0F).setColor(color);
			consumer.addVertexWith2DPose(pose, left, bottom).setUv(0.0F, 1.0F).setColor(color);
			consumer.addVertexWith2DPose(pose, right, bottom).setUv(1.0F, 1.0F).setColor(color);
			consumer.addVertexWith2DPose(pose, right, top).setUv(1.0F, 0.0F).setColor(color);
		}

		private static @Nullable ScreenRectangle getBounds(
			float left, float top, float right, float bottom, Matrix3x2f pose, @Nullable ScreenRectangle scissorArea
		) {
			ScreenRectangle bounds = new ScreenRectangle(
				(int)Math.floor(left), (int)Math.floor(top), (int)Math.ceil(right - left), (int)Math.ceil(bottom - top)
			).transformMaxBounds(pose);
			return scissorArea == null ? bounds : scissorArea.intersection(bounds);
		}
	}
}
