package media.jlt.minecraft.mods.worldz.mixin.client;

import media.jlt.minecraft.mods.worldz.client.ChaosBiomesPresetEditor;
import media.jlt.minecraft.mods.worldz.client.SingleBiomePresetEditor;
import media.jlt.minecraft.mods.worldz.client.WorldzPresetEditor;
import net.minecraft.client.gui.screens.worldselection.PresetEditor;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.presets.WorldPreset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Supplies Fabric's missing modded-preset editor lookup hook. */
@Mixin(WorldCreationUiState.class)
abstract class WorldCreationUiStateMixin {
    @Inject(method = "getPresetEditor", at = @At("HEAD"), cancellable = true)
    private void jltWorldz$getPresetEditor(CallbackInfoReturnable<PresetEditor> callback) {
        WorldCreationUiState state = (WorldCreationUiState)(Object)this;
        Holder<WorldPreset> preset = state.getWorldType().preset();
        if (preset == null) {
            return;
        }
        if (preset.unwrapKey().filter(WorldzPresetEditor.WORLDZ_PRESET::equals).isPresent()) {
            callback.setReturnValue(WorldzPresetEditor.INSTANCE);
        } else if (preset.unwrapKey().filter(SingleBiomePresetEditor.SINGLE_BIOME_PRESET::equals).isPresent()) {
            callback.setReturnValue(SingleBiomePresetEditor.INSTANCE);
        } else if (preset.unwrapKey().filter(ChaosBiomesPresetEditor.CHAOS_BIOMES_PRESET::equals).isPresent()) {
            callback.setReturnValue(ChaosBiomesPresetEditor.INSTANCE);
        }
    }
}
