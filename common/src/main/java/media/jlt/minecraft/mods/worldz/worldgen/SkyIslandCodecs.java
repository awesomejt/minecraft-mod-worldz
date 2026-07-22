package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.logic.FloatingIslandsPlan;
import media.jlt.minecraft.mods.worldz.logic.IslandPlan;
import media.jlt.minecraft.mods.worldz.logic.SkyIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;

/**
 * Persistence codec for a resolved sky-island plan (DESIGN §27.9). {@code floatingIslands} nests
 * entirely inside this codec's own group rather than adding a new top-level {@code
 * LimitedBiomeSource} field, since that codec was already at 13 of its 14-field DFU ceiling after
 * {@code sky_island} landed (DESIGN §28.4).
 */
final class SkyIslandCodecs {
    private static final Codec<StarterKitTier> TIER_CODEC = Codec.STRING.xmap(StarterKitTier::parse, StarterKitTier::serializedName);

    private static final Codec<IslandPlan.ExclusionZone> EXCLUSION_ZONE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(IslandPlan.ExclusionZone::enabled),
        Codec.INT.fieldOf("radius").forGetter(IslandPlan.ExclusionZone::radiusBlocks)
    ).apply(instance, IslandPlan.ExclusionZone::new));

    private static final Codec<FloatingIslandsPlan> FLOATING_ISLANDS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(FloatingIslandsPlan::enabled),
        Codec.INT.fieldOf("min_radius").forGetter(FloatingIslandsPlan::minRadiusBlocks),
        Codec.INT.fieldOf("max_radius").forGetter(FloatingIslandsPlan::maxRadiusBlocks),
        Codec.DOUBLE.fieldOf("shape_amplitude").forGetter(FloatingIslandsPlan::shapeAmplitude),
        Codec.INT.fieldOf("cell_size").forGetter(FloatingIslandsPlan::cellSizeBlocks),
        Codec.DOUBLE.fieldOf("spawn_chance").forGetter(FloatingIslandsPlan::spawnChance),
        Codec.BOOL.fieldOf("biome_variety").forGetter(FloatingIslandsPlan::biomeVariety),
        Codec.STRING.listOf().fieldOf("island_biomes").forGetter(FloatingIslandsPlan::islandBiomes),
        EXCLUSION_ZONE_CODEC.fieldOf("exclusion_zone").forGetter(FloatingIslandsPlan::exclusionZone),
        Codec.BOOL.fieldOf("ore_deposits_enabled").forGetter(FloatingIslandsPlan::oreDepositsEnabled),
        Codec.BOOL.fieldOf("loot_chest_enabled").forGetter(FloatingIslandsPlan::lootChestEnabled),
        Codec.BOOL.fieldOf("natural_biome").forGetter(FloatingIslandsPlan::naturalBiome)
    ).apply(instance, FloatingIslandsPlan::new));

    static final Codec<SkyIslandPlan> PLAN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(SkyIslandPlan::enabled),
        Codec.INT.fieldOf("radius").forGetter(SkyIslandPlan::radiusBlocks),
        Codec.DOUBLE.fieldOf("shape_amplitude").forGetter(SkyIslandPlan::shapeAmplitude),
        Codec.STRING.fieldOf("island_biome").forGetter(SkyIslandPlan::islandBiome),
        Codec.INT.fieldOf("surface_y").forGetter(SkyIslandPlan::surfaceY),
        Codec.INT.fieldOf("thickness").forGetter(SkyIslandPlan::thicknessBlocks),
        TIER_CODEC.fieldOf("chest_tier").forGetter(SkyIslandPlan::chestTier),
        FLOATING_ISLANDS_CODEC.fieldOf("floating_islands").forGetter(SkyIslandPlan::floatingIslands),
        EXCLUSION_ZONE_CODEC.fieldOf("biome_exclusion_zone").forGetter(SkyIslandPlan::biomeExclusionZone)
    ).apply(instance, SkyIslandPlan::new));

    private SkyIslandCodecs() {
    }
}
