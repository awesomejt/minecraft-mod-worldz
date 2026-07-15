package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.logic.BiomeRole;
import media.jlt.minecraft.mods.worldz.logic.LayoutMode;
import media.jlt.minecraft.mods.worldz.logic.WorldLayoutPlan;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Minecraft serialization adapter for the pure coordinated-layout plan. */
final class LayoutCodecs {
    private static final Codec<LayoutMode> MODE_CODEC = Codec.STRING.xmap(LayoutMode::parse, LayoutMode::serializedName);
    private static final Codec<BiomeRole> ROLE_CODEC = Codec.STRING.xmap(BiomeRole::parse, BiomeRole::serializedName);

    private static final Codec<WorldLayoutPlan.BiomeWeight> WEIGHT_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.STRING.fieldOf("id").forGetter(WorldLayoutPlan.BiomeWeight::biomeId),
        Codec.DOUBLE.fieldOf("weight").forGetter(WorldLayoutPlan.BiomeWeight::weight)
    ).apply(instance, WorldLayoutPlan.BiomeWeight::new));

    private static final Codec<List<WorldLayoutPlan.BiomeWeight>> WEIGHT_LIST_CODEC = WEIGHT_CODEC.listOf();
    private static final Codec<Map<String, BiomeRole>> ROLE_OVERRIDES_CODEC = Codec.unboundedMap(Codec.STRING, ROLE_CODEC);

    static final Codec<WorldLayoutPlan> PLAN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        MODE_CODEC.fieldOf("mode").forGetter(WorldLayoutPlan::mode),
        Codec.LONG.fieldOf("seed").forGetter(WorldLayoutPlan::seed),
        Codec.INT.fieldOf("region_scale").forGetter(WorldLayoutPlan::regionScaleBlocks),
        Codec.DOUBLE.fieldOf("ocean_coverage").forGetter(WorldLayoutPlan::oceanCoverageFraction),
        Codec.INT.fieldOf("coast_blend_width").forGetter(WorldLayoutPlan::coastBlendWidthBlocks),
        WEIGHT_LIST_CODEC.optionalFieldOf("land_biomes", List.of()).forGetter(WorldLayoutPlan::landBiomes),
        WEIGHT_LIST_CODEC.optionalFieldOf("ocean_biomes", List.of()).forGetter(WorldLayoutPlan::oceanBiomes),
        WEIGHT_LIST_CODEC.optionalFieldOf("beach_biomes", List.of()).forGetter(WorldLayoutPlan::beachBiomes),
        Codec.STRING.optionalFieldOf("single_biome").forGetter(WorldLayoutPlan::singleBiome),
        ROLE_OVERRIDES_CODEC.optionalFieldOf("role_overrides", Map.of()).forGetter(WorldLayoutPlan::roleOverrides),
        Codec.INT.optionalFieldOf("layout_origin_x", 0).forGetter(WorldLayoutPlan::layoutOriginBlockX),
        Codec.INT.optionalFieldOf("layout_origin_z", 0).forGetter(WorldLayoutPlan::layoutOriginBlockZ),
        Codec.INT.optionalFieldOf("algorithm_revision", WorldLayoutPlan.LEGACY_MODE_REVISION)
            .forGetter(WorldLayoutPlan::algorithmRevision)
    ).apply(instance, WorldLayoutPlan::new));

    private LayoutCodecs() {
    }
}
