package com.kairokk.client.pathfinder.commands;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import com.kairokk.client.pathfinder.pathfinding.NavigationManager;

public final class PathfindCommand {
	private PathfindCommand() {
	}

	public static void register() {
		ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
			dispatcher.register(buildCommand("pathfind"));
			dispatcher.register(buildCommand("pathfinder"));
		});
	}

	private static LiteralArgumentBuilder<FabricClientCommandSource> buildCommand(String name) {
		return literal(name)
						.executes(context -> showUsage(context.getSource()))
						.then(literal("setgoal").executes(context -> setGoal(context.getSource())))
						.then(literal("refresh").executes(context -> refresh(context.getSource())))
						.then(literal("start").executes(context -> start(context.getSource())))
						.then(literal("stop").executes(context -> stop(context.getSource())))
						.then(literal("reset").executes(context -> reset(context.getSource())))
						.then(literal("debug").executes(context -> debug(context.getSource())))
						.then(argument("x", IntegerArgumentType.integer())
								.then(argument("y", IntegerArgumentType.integer())
										.then(argument("z", IntegerArgumentType.integer())
												.executes(context -> calculate(
														context.getSource(),
														IntegerArgumentType.getInteger(context, "x"),
														IntegerArgumentType.getInteger(context, "y"),
														IntegerArgumentType.getInteger(context, "z"))))));
	}

	private static int showUsage(FabricClientCommandSource source) {
		source.sendFeedback(Component.literal("Usage: /pathfind <x> <y> <z> | setgoal | refresh | start | stop | reset | debug")
				.withStyle(ChatFormatting.GRAY));
		return 1;
	}

	private static int refresh(FabricClientCommandSource source) {
		if (!NavigationManager.getInstance().refresh(source.getClient())) {
			source.sendError(Component.literal("No destination is available to refresh. Set a goal first."));
			return 0;
		}
		source.sendFeedback(Component.literal("Refreshing route from your current position.")
				.withStyle(ChatFormatting.AQUA));
		return 1;
	}

	private static int setGoal(FabricClientCommandSource source) {
		Minecraft client = source.getClient();
		if (!(client.hitResult instanceof BlockHitResult blockHit) || blockHit.getType() != HitResult.Type.BLOCK) {
			source.sendError(Component.literal("Look at a block within reach first."));
			return 0;
		}

		BlockPos goal = blockHit.getBlockPos().relative(blockHit.getDirection());
		source.sendFeedback(Component.literal("Goal selected: %d %d %d"
				.formatted(goal.getX(), goal.getY(), goal.getZ())).withStyle(ChatFormatting.AQUA));
		NavigationManager.getInstance().requestPath(client, goal);
		return 1;
	}

	private static int calculate(FabricClientCommandSource source, int x, int y, int z) {
		NavigationManager.getInstance().requestPath(source.getClient(), new BlockPos(x, y, z));
		return 1;
	}

	private static int start(FabricClientCommandSource source) {
		if (!NavigationManager.getInstance().start(source.getClient())) {
			source.sendError(Component.literal("No calculated path is available. Use /pathfind x y z first."));
			return 0;
		}
		source.sendFeedback(Component.literal("Following path.").withStyle(ChatFormatting.GREEN));
		return 1;
	}

	private static int stop(FabricClientCommandSource source) {
		NavigationManager.getInstance().stop(source.getClient());
		source.sendFeedback(Component.literal("Path following stopped.").withStyle(ChatFormatting.YELLOW));
		return 1;
	}

	private static int reset(FabricClientCommandSource source) {
		NavigationManager.getInstance().reset(source.getClient());
		source.sendFeedback(Component.literal("Path cleared.").withStyle(ChatFormatting.YELLOW));
		return 1;
	}

	private static int debug(FabricClientCommandSource source) {
		boolean enabled = NavigationManager.getInstance().toggleDebugRender();
		source.sendFeedback(Component.literal("Path debug rendering: " + (enabled ? "ON" : "OFF"))
				.withStyle(enabled ? ChatFormatting.GOLD : ChatFormatting.GRAY));
		return 1;
	}
}
