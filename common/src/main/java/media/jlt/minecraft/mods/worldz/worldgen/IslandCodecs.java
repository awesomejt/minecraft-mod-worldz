package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.logic.IslandPlan;

/** Persistence codec for a resolved ocean-island plan. */
final class IslandCodecs {
    static final Codec<IslandPlan> PLAN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(IslandPlan::enabled),
        Codec.INT.fieldOf("radius").forGetter(IslandPlan::radiusBlocks),
        Codec.DOUBLE.fieldOf("shape_amplitude").forGetter(IslandPlan::shapeAmplitude),
        Codec.STRING.fieldOf("island_biome").forGetter(IslandPlan::islandBiome),
        Codec.INT.fieldOf("shore_width").forGetter(IslandPlan::shoreWidthBlocks),
        Codec.INT.fieldOf("ocean_shallow_width").forGetter(IslandPlan::oceanShallowWidthBlocks),
        Codec.INT.fieldOf("ocean_deepen_width").forGetter(IslandPlan::oceanDeepenWidthBlocks),
        Codec.INT.fieldOf("ocean_shallow_depth").forGetter(IslandPlan::oceanShallowDepthBlocks),
        Codec.INT.fieldOf("ocean_deep_depth").forGetter(IslandPlan::oceanDeepDepthBlocks),
        Codec.INT.fieldOf("ocean_region_scale").forGetter(IslandPlan::oceanRegionScaleBlocks),
        Codec.BOOL.fieldOf("exclusion_zone_enabled").forGetter(IslandPlan::exclusionZoneEnabled),
        Codec.INT.fieldOf("exclusion_zone_radius").forGetter(IslandPlan::exclusionZoneRadiusBlocks),
        Codec.BOOL.fieldOf("has_land").forGetter(IslandPlan::hasLand)
    ).apply(instance, IslandPlan::new));

    private IslandCodecs() {
    }
}
