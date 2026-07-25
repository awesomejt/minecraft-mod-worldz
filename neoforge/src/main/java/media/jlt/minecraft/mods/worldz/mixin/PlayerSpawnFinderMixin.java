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
 * Trusts the cave preset's own resolved spawn/respawn suggestion outright instead of vanilla's
 * radius-jittered {@code PlayerSpawnFinder} search (GOALS 25-26, DESIGN §30.3).
 *
 * <p>Root cause, confirmed via a real test world's saved NBT plus the real 26.2 decompiled
 * sources ({@code net/minecraft/server/level/PlayerSpawnFinder.java}): {@code findSpawn}'s
 * per-candidate check, {@code getLevelRespawnPos}, discards the suggested position's Y entirely
 * and recomputes it from the real terrain's own {@code MOTION_BLOCKING} surface heightmap
 * whenever {@code level.dimensionType().hasCeiling()} is {@code false} -- true for every Worldz
 * Overworld, including the cave preset's (DESIGN §30.1 keeps it an ordinary, unmodified Overworld
 * dimension type on purpose). {@code SpawnOriginManager.resolveCaveOrigin}'s underground cavity
 * search was already correct and its result was already persisted into the level's
 * {@code Data.spawn} field (confirmed directly from a test world's level.dat) -- the player was
 * simply placed by this class's own separate search instead, which never consults that field's Y
 * outside the {@code hasCeiling() == true} branch.
 *
 * <p>Flipping {@code hasCeiling} on the shared Overworld dimension type itself was considered and
 * rejected: it also gates {@code Level.canHaveWeather()} (would silently disable weather on the
 * cave world's surface, contradicting GOALS 25's own "ordinary vanilla surface terrain...
 * weather" acceptance wording), {@code NaturalSpawner}'s mob-spawn-height search, and
 * {@code MapItem}'s map rendering -- all real, unrelated behavior changes for a preset whose
 * Overworld is supposed to generate exactly like vanilla above ground (DESIGN §30.1). Bypassing
 * {@code findSpawn} entirely for a cave-preset level avoids all of that: both
 * {@code resolveCaveOrigin}'s natural-cavity result and its {@code buildCaveCapsule} fallback are
 * already verified open-air-with-solid-floor (or an explicitly carved safe shell) at the moment
 * they're computed, so no further vanilla safety search is needed.
 *
 * <p>Identical to the Fabric-side mixin of the same name -- pure Mojang-mapped vanilla code, no
 * loader-specific APIs -- kept as a separate per-loader mixin class rather than shared via
 * {@code common} to match this project's existing per-loader mixin convention.
 *
 * <p>Known, accepted simplification: this also applies to a later bed-based respawn on a
 * cave-preset level, trusting the bed position directly rather than vanilla's own
 * embedded-in-a-wall nudge -- a bed's position is virtually always already safe since the player
 * physically stood there to use it.
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
        if (generator instanceof EnvelopedChunkGenerator enveloped && enveloped.cave().enabled()) {
            callback.setReturnValue(CompletableFuture.completedFuture(Vec3.atBottomCenterOf(spawnSuggestion)));
        }
    }
}
