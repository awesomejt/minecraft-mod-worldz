package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.logic.ChunkIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.IslandPlan;

/** Persistence codec for a resolved chunk-island plan (GOALS 09/37, DESIGN §29). */
final class ChunkIslandCodecs {
    /**
     * Nested rather than two flat fields, matching {@code IslandPlan.ExclusionZone}'s own DFU-
     * ceiling-discipline precedent (DESIGN §26.1) even though this record is nowhere near the
     * ceiling itself -- keeps the shape consistent with every other exclusion-zone consumer.
     */
    private static final Codec<IslandPlan.ExclusionZone> EXCLUSION_ZONE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(IslandPlan.ExclusionZone::enabled),
        Codec.INT.fieldOf("radius").forGetter(IslandPlan.ExclusionZone::radiusBlocks)
    ).apply(instance, IslandPlan.ExclusionZone::new));

    static final Codec<ChunkIslandPlan> PLAN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(ChunkIslandPlan::enabled),
        Codec.DOUBLE.fieldOf("spawn_chance").forGetter(ChunkIslandPlan::spawnChance),
        Codec.INT.fieldOf("cell_size_chunks").forGetter(ChunkIslandPlan::cellSizeChunks),
        Codec.BOOL.fieldOf("top_only").forGetter(ChunkIslandPlan::topOnly),
        Codec.INT.fieldOf("top_only_depth").forGetter(ChunkIslandPlan::topOnlyDepthBlocks),
        EXCLUSION_ZONE_CODEC.fieldOf("exclusion_zone").forGetter(ChunkIslandPlan::exclusionZone)
    ).apply(instance, ChunkIslandPlan::new));

    private ChunkIslandCodecs() {
    }
}
