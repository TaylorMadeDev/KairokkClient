package com.kairokk.client.mixin;

import com.kairokk.client.ShulkCreateWorldRequest;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import net.minecraft.resources.ResourceKey;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies Shulk's setup choices while retaining Minecraft's world-gen/data-pack flow. */
@Mixin(CreateWorldScreen.class)
public abstract class CreateWorldScreenMixin {
	@Shadow public abstract WorldCreationUiState getUiState();
	@Shadow private void onCreate() { }

	@Inject(method = "init", at = @At("TAIL"))
	private void shulk$submitCustomWorld(CallbackInfo ci) {
		ShulkCreateWorldRequest.Request request = ShulkCreateWorldRequest.take();
		if (request == null) return;
		WorldCreationUiState state = getUiState();
		state.setName(request.name());
		state.setGameMode(request.gameMode());
		state.setDifficulty(request.difficulty());
		state.setAllowCommands(request.commands());
		state.setSeed(request.seed());
		state.setGenerateStructures(request.structures());
		state.setBonusChest(request.bonusChest());
		WorldCreationUiState.WorldTypeEntry worldType = findWorldType(state, request.worldType().preset());
		if (worldType != null) state.setWorldType(worldType);
		onCreate();
	}

	private static WorldCreationUiState.WorldTypeEntry findWorldType(WorldCreationUiState state,
			ResourceKey<WorldPreset> preset) {
		for (WorldCreationUiState.WorldTypeEntry entry : state.getNormalPresetList()) {
			if (entry.preset().unwrapKey().map(preset::equals).orElse(false)) return entry;
		}
		for (WorldCreationUiState.WorldTypeEntry entry : state.getAltPresetList()) {
			if (entry.preset().unwrapKey().map(preset::equals).orElse(false)) return entry;
		}
		return null;
	}
}
