package media.jlt.minecraft.mods.worldz.mixin;

import media.jlt.minecraft.mods.worldz.worldgen.EnvelopedChunkGenerator;
import net.minecraft.core.Registry;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldDimensions;
import net.minecraft.world.level.storage.PrimaryLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Marks a {@code flat}/{@code deep_flat} Overworld as {@code SpecialWorldProperty.FLAT} the same
 * way vanilla's own superflat preset is (GOAL 15/16, 2026-07-26 follow-up to Jason's config 67
 * retest: "low spawn has a very dark horizon").
 *
 * <p>Root cause, confirmed against real decompiled sources: {@code
 * ClientLevel.ClientLevelData.getHorizonHeight} -- which gates {@code SkyRenderer}'s "dark disc"
 * plane (the black band Jason saw at the horizon) -- returns the level's real minimum Y when
 * {@code isFlat} is set, but falls back to a hardcoded sea level of {@code 63.0} otherwise. {@code
 * shouldRenderDarkDisc} renders the disc whenever the player's eye Y is below whichever value
 * {@code getHorizonHeight} returns, so an ordinary (non-flat) world only ever sees it below sea
 * level -- normally hidden by real terrain around the player. A classic-flat world with a low
 * layer stack (config 67 spawns at Y -60) has no terrain to hide behind, so the disc becomes
 * visible the moment {@code getHorizonHeight} is stuck at sea level instead of the world's real
 * floor. {@code isFlat} itself is {@code PrimaryLevelData.isFlatWorld()}, computed exactly once at
 * world creation by {@code WorldDimensions.specialWorldProperty} as a plain {@code generator
 * instanceof FlatLevelSource} check -- {@code jlt_worldz}'s own {@code flat}/{@code deep_flat}
 * presets never satisfy that check, since their real generator is always {@code
 * EnvelopedChunkGenerator} (wrapping a delegate, with {@code FlatPlan}/{@code DeepFlatPlan} as an
 * internal field vanilla has no way to see), so a Worldz flat world always fell into vanilla's
 * ordinary (sea-level-63) branch regardless of its own configured surface height.
 *
 * <p>Targets {@code WorldDimensions.bake} (not the private {@code specialWorldProperty} helper
 * directly) since it's the nearest public method returning the already-computed {@code
 * WorldDimensions.Complete} -- cheaper and simpler than re-deriving the overworld's {@code
 * LevelStem} a second time just to duplicate vanilla's own instanceof check. Deep-flat is included
 * here alongside classic flat: its own surface is exactly as uniformly flat (GOAL 16), so a
 * low-{@code surfaceY} deep-flat world would hit the identical dark-disc bug, even though it
 * wasn't the config Jason actually retested. Runs once at world creation/load (both dedicated and
 * integrated-server startup), not per-frame, so the extra {@code instanceof} check here is free.
 */
@Mixin(WorldDimensions.class)
abstract class WorldDimensionsMixin {
    @Inject(method = "bake", at = @At("RETURN"), cancellable = true)
    private void jltWorldz$bake(Registry<LevelStem> baseDimensions, CallbackInfoReturnable<WorldDimensions.Complete> callback) {
        WorldDimensions.Complete result = callback.getReturnValue();
        if (result.specialWorldProperty() != PrimaryLevelData.SpecialWorldProperty.NONE) {
            return;
        }
        LevelStem overworld = result.dimensions().getValue(LevelStem.OVERWORLD);
        if (overworld != null && overworld.generator() instanceof EnvelopedChunkGenerator enveloped
            && (enveloped.flat().enabled() || enveloped.deepFlat().enabled())) {
            callback.setReturnValue(new WorldDimensions.Complete(result.dimensions(), PrimaryLevelData.SpecialWorldProperty.FLAT));
        }
    }
}
