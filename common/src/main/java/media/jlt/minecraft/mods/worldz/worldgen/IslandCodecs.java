package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.logic.IslandFluid;
import media.jlt.minecraft.mods.worldz.logic.IslandPlan;

/**
 * Persistence codec for a resolved ocean-island plan. {@code exclusionZone} is a nested codec
 * matching {@link IslandPlan}'s own nested record -- {@code RecordCodecBuilder.create}'s {@code
 * instance.group(...)} tops out at 14 fields in this DFU version (DESIGN §26.1).
 */
final class IslandCodecs {
    private static final Codec<IslandFluid> FLUID_CODEC = Codec.STRING.xmap(IslandFluid::parse, IslandFluid::serializedName);

    private static final Codec<IslandPlan.ExclusionZone> EXCLUSION_ZONE_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(IslandPlan.ExclusionZone::enabled),
        Codec.INT.fieldOf("radius").forGetter(IslandPlan.ExclusionZone::radiusBlocks)
    ).apply(instance, IslandPlan.ExclusionZone::new));

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
        Codec.BOOL.fieldOf("has_land").forGetter(IslandPlan::hasLand),
        Codec.BOOL.fieldOf("synthetic_land").forGetter(IslandPlan::syntheticLand),
        EXCLUSION_ZONE_CODEC.fieldOf("exclusion_zone").forGetter(IslandPlan::exclusionZone),
        FLUID_CODEC.fieldOf("fluid").forGetter(IslandPlan::fluid)
    ).apply(instance, IslandPlan::new));

    private IslandCodecs() {
    }
}
