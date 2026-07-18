package media.jlt.minecraft.mods.worldz.mixin;

import com.mojang.datafixers.DataFixer;
import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.worldgen.EnvelopedChunkGenerator;
import media.jlt.minecraft.mods.worldz.worldgen.LimitedBiomeSource;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Fixes {@code ChunkMap}'s {@code RandomState} fallback for
 * {@code EnvelopedChunkGenerator}. Vanilla only builds a real,
 * delegate-settings-based {@code RandomState} when the top-level generator
 * is directly {@code instanceof NoiseBasedChunkGenerator}; otherwise it
 * silently falls back to {@code RandomState.create(NoiseGeneratorSettings.dummy(), ...)}
 * -- a zero-density router and air surface rule. Worldz's
 * {@code EnvelopedChunkGenerator} wraps a real {@code NoiseBasedChunkGenerator}
 * by composition rather than extending it ({@code NoiseBasedChunkGenerator}
 * is {@code final} in 26.2, so extension is not possible in Java at all),
 * so that check always failed for every Worldz world (DESIGN's "Known
 * caveats", {@code MEMORY.md}'s "Known Risks"). Confirmed in-game (world
 * "Worldz14"): the lower part of the world was almost entirely lava
 * (vanilla's aquifer fluid picker still applies its normal below-Y-54 lava
 * rule, fed by a degenerate density field) and caves were mostly absent
 * instead of vanilla's usual winding systems (real cave shape comes from
 * the router's own noise, which the dummy router has none of).
 *
 * <p>Also resolves {@link LimitedBiomeSource#setLayoutSeed(long)} with the real world
 * seed here, the earliest point it is available to Worldz's coordinated-layout sampling
 * (DESIGN §20.4): {@code BiomeSource} codecs decode from {@code RegistryOps}, which has
 * no seed-aware hook, so the seed baked into a freshly created world's plan is a
 * placeholder until this injection runs. No persistence of its own is needed --
 * {@code ServerLevel.getSeed()} already returns the same value on every load.
 *
 * <p><b>2026-07-17 correction:</b> the original fix used {@code @Inject} at
 * {@code @At("INVOKE")} on {@code generator.createState(...)}, reassigning
 * {@code this.randomState} there. That correctly fixes every <i>later</i> read of the
 * field (actual terrain generation), confirmed by a clean bottom-of-world check -- but
 * not the {@code createState(...)} call itself: decompiled source shows
 * {@code this.chunkGeneratorState = generator.createState(structureSets, this.randomState,
 * levelSeed);}, reading the field inline as an argument. An {@code @At("INVOKE")} callback
 * fires immediately before the {@code INVOKE} instruction, which is *after* the argument
 * list -- including that {@code GETFIELD} -- has already been evaluated onto the stack, so
 * the reassignment always landed one instruction too late for this specific call.
 * {@code ChunkGeneratorStructureState} (built once here, governing every structure
 * placement decision for the whole level) was still built from the dummy
 * {@code RandomState} regardless -- consistent with in-game reports of villages/structures
 * generating detached from the real (correctly-generated) terrain around them. Switched to
 * {@code @Redirect} so the value passed into {@code createState(...)} is chosen explicitly
 * rather than depending on field-read timing.
 */
@Mixin(ChunkMap.class)
abstract class ChunkMapMixin {
    @Shadow
    @Mutable
    private RandomState randomState;

    @Redirect(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/ChunkGenerator;createState(Lnet/minecraft/core/HolderLookup;"
                + "Lnet/minecraft/world/level/levelgen/RandomState;J)Lnet/minecraft/world/level/chunk/ChunkGeneratorStructureState;"
        )
    )
    private ChunkGeneratorStructureState jltWorldz$createStateWithDelegateSettings(
        ChunkGenerator generator,
        HolderLookup<StructureSet> structureSets,
        RandomState randomState,
        long legacyLevelSeed,
        ServerLevel level
    ) {
        if (generator instanceof EnvelopedChunkGenerator enveloped) {
            if (enveloped.delegate() instanceof NoiseBasedChunkGenerator noiseGenerator) {
                this.randomState = RandomState.create(
                    noiseGenerator.generatorSettings().value(),
                    level.registryAccess().lookupOrThrow(Registries.NOISE),
                    level.getSeed()
                );
                // TEMPORARY DIAGNOSTIC (2026-07-17, remove after the floating-structure
                // investigation concludes): ground-truth log of the identity this mixin just
                // assigned, to compare against EnvelopedChunkGenerator's own
                // jltWorldzDiag$trackRandomState logs.
                WorldzCommon.LOGGER.warn(
                    "[DIAG] ChunkMapMixin fixed randomState: identity={}, thread={}, dimension={}",
                    System.identityHashCode(this.randomState), Thread.currentThread().getName(), level.dimension().identifier()
                );
            }
            if (enveloped.getBiomeSource() instanceof LimitedBiomeSource source) {
                source.setLayoutSeed(level.getSeed());
            }
        }
        // Pass this.randomState explicitly (now fixed above when applicable) rather than
        // the redirect's own captured randomState argument, which is this call's original,
        // possibly-stale value.
        return generator.createState(structureSets, this.randomState, legacyLevelSeed);
    }
}
