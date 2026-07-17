package media.jlt.minecraft.mods.worldz.mixin;

import com.mojang.datafixers.DataFixer;
import media.jlt.minecraft.mods.worldz.worldgen.EnvelopedChunkGenerator;
import media.jlt.minecraft.mods.worldz.worldgen.LimitedBiomeSource;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LightChunkGetter;
import net.minecraft.world.level.entity.ChunkStatusUpdateListener;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.SavedDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
 */
@Mixin(ChunkMap.class)
abstract class ChunkMapMixin {
    @Shadow
    @Mutable
    private RandomState randomState;

    @Inject(
        method = "<init>",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/chunk/ChunkGenerator;createState(Lnet/minecraft/core/HolderLookup;"
                + "Lnet/minecraft/world/level/levelgen/RandomState;J)Lnet/minecraft/world/level/chunk/ChunkGeneratorStructureState;"
        )
    )
    private void jltWorldz$useDelegateSettingsInsteadOfDummy(
        ServerLevel level,
        LevelStorageSource.LevelStorageAccess levelStorage,
        DataFixer dataFixer,
        StructureTemplateManager structureManager,
        Executor executor,
        BlockableEventLoop<Runnable> mainThreadExecutor,
        LightChunkGetter chunkGetter,
        ChunkGenerator generator,
        ChunkStatusUpdateListener chunkStatusListener,
        Supplier<SavedDataStorage> overworldDataStorage,
        TicketStorage ticketStorage,
        int serverViewDistance,
        boolean syncWrites,
        CallbackInfo callback
    ) {
        if (generator instanceof EnvelopedChunkGenerator enveloped) {
            if (enveloped.delegate() instanceof NoiseBasedChunkGenerator noiseGenerator) {
                this.randomState = RandomState.create(
                    noiseGenerator.generatorSettings().value(),
                    level.registryAccess().lookupOrThrow(Registries.NOISE),
                    level.getSeed()
                );
            }
            if (enveloped.getBiomeSource() instanceof LimitedBiomeSource source) {
                source.setLayoutSeed(level.getSeed());
            }
        }
    }
}
