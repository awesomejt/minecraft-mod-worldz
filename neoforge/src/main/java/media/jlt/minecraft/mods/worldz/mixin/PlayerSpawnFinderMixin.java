package media.jlt.minecraft.mods.worldz.mixin;

import media.jlt.minecraft.mods.worldz.worldgen.EnvelopedChunkGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.PlayerSpawnFinder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

/**
 * Trusts the cave/Nether-start presets' own resolved spawn/respawn suggestion outright instead of
 * vanilla's radius-jittered {@code PlayerSpawnFinder} search (GOALS 25-27, DESIGN §30.3/§31.4,
 * capsule follow-up DESIGN §31.9).
 *
 * <p>Root cause, confirmed via a real test world's saved NBT plus the real 26.2 decompiled
 * sources ({@code net/minecraft/server/level/PlayerSpawnFinder.java}): {@code findSpawn} never
 * simply trusts its own {@code spawnSuggestion} first -- it tries up to {@code candidateCount}
 * (up to 1024) other positions scattered within the {@code spawnRadius} gamerule's own radius via
 * {@code getLevelRespawnPos} first, and only falls back to the exact suggestion (via {@code
 * fixupSpawnHeight}) if literally every nearby candidate fails. {@code getLevelRespawnPos}
 * discards the *candidate's* own Y entirely and instead scans down from either the real terrain's
 * {@code MOTION_BLOCKING} surface heightmap ({@code hasCeiling() == false} -- every Worldz
 * Overworld, including the cave preset's, DESIGN §30.1 keeps it an ordinary, unmodified Overworld
 * dimension type on purpose) or the chunk generator's own {@code getSpawnHeight()} ({@code
 * hasCeiling() == true} -- true for the Nether, confirmed against the real {@code
 * DimensionTypes.java} bootstrap: {@code hasCeiling} is the *third* constructor argument, not
 * something inferable from height/logicalHeight alone). Either way, ordinary generated terrain
 * almost always has *some* valid nearby candidate, so the suggestion only wins by chance -- for
 * cave, first surfaced as "joined in a Forest treetop, not underground"; for {@code nether_start},
 * as spawning far above/away from a tiny, otherwise-unremarkable-looking guaranteed capsule
 * (2026-07-26, Jason: "config 62 puts me much higher").
 *
 * <p>Flipping {@code hasCeiling} on the shared Overworld dimension type itself was considered and
 * rejected: it also gates {@code Level.canHaveWeather()} (would silently disable weather on the
 * cave world's surface, contradicting GOALS 25's own "ordinary vanilla surface terrain...
 * weather" acceptance wording), {@code NaturalSpawner}'s mob-spawn-height search, and
 * {@code MapItem}'s map rendering -- all real, unrelated behavior changes for a preset whose
 * Overworld is supposed to generate exactly like vanilla above ground (DESIGN §30.1). Bypassing
 * {@code findSpawn} entirely for a cave-preset or {@code nether_start} level avoids all of that:
 * both {@code SpawnOriginManager}'s (cave) and {@code NetherStartDeployment}'s (Nether-start)
 * resolved sites -- natural search result, forced capsule, or automatic low/high-{@code spawnY}
 * capsule alike -- are already verified open-air-with-solid-floor (or an explicitly carved safe
 * shell) at the moment they're computed, so no further vanilla safety search is needed, nearby or
 * otherwise. {@code end_start} deliberately not added here -- unlike the Nether, the End's outer
 * regions are mostly void, so the same-column heightmap candidate {@code getLevelRespawnPos}
 * computes naturally re-finds the guaranteed platform's own solid floor (nothing else occupies
 * that column to intercept the downward scan first); DESIGN §32.1 already designed the platform's
 * shape around exactly this constraint ("must be real, solid, generated terrain occupying its own
 * X/Z column").
 *
 * <p>Identical to the Fabric-side mixin of the same name -- pure Mojang-mapped vanilla code, no
 * loader-specific APIs -- kept as a separate per-loader mixin class rather than shared via
 * {@code common} to match this project's existing per-loader mixin convention.
 *
 * <p>Known, accepted simplification: this also applies to a later bed-based respawn on a
 * cave-preset or {@code nether_start} level, trusting the bed position directly rather than
 * vanilla's own embedded-in-a-wall nudge -- a bed's position is virtually always already safe
 * since the player physically stood there to use it.
 */
@Mixin(PlayerSpawnFinder.class)
abstract class PlayerSpawnFinderMixin {
    @Inject(method = "findSpawn", at = @At("HEAD"), cancellable = true)
    private static void jltWorldz$findSpawn(
        ServerLevel level,
        BlockPos spawnSuggestion,
        CallbackInfoReturnable<CompletableFuture<Vec3>> callback
    ) {
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        if (generator instanceof EnvelopedChunkGenerator enveloped
            && (enveloped.cave().enabled() || enveloped.netherStart().enabled())) {
            callback.setReturnValue(CompletableFuture.completedFuture(Vec3.atBottomCenterOf(spawnSuggestion)));
        }
    }
}
