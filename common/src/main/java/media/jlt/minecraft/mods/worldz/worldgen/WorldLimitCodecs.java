package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.logic.ResizeStyle;

/** Minecraft serialization adapters kept separate from the pure limit model. */
final class WorldLimitCodecs {
    private static final Codec<ResizeStyle> RESIZE_STYLE_CODEC = Codec.STRING.xmap(ResizeStyle::parse, ResizeStyle::serializedName);

    static final Codec<WorldLimitPlan.DimensionLimit> DIMENSION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(WorldLimitPlan.DimensionLimit::enabled),
        Codec.INT.fieldOf("initial_radius").forGetter(WorldLimitPlan.DimensionLimit::initialRadiusBlocks),
        Codec.INT.fieldOf("final_radius").forGetter(WorldLimitPlan.DimensionLimit::finalRadiusBlocks),
        Codec.INT.fieldOf("resize_days").forGetter(WorldLimitPlan.DimensionLimit::resizeDays),
        Codec.INT.optionalFieldOf("resize_delay_days", 0).forGetter(WorldLimitPlan.DimensionLimit::resizeDelayDays),
        Codec.INT.optionalFieldOf("resize_rate_blocks", 0).forGetter(WorldLimitPlan.DimensionLimit::resizeRateBlocks),
        Codec.INT.optionalFieldOf("resize_rate_days", 0).forGetter(WorldLimitPlan.DimensionLimit::resizeRateDays),
        Codec.BOOL.fieldOf("ensure_objective").forGetter(WorldLimitPlan.DimensionLimit::ensureObjective),
        RESIZE_STYLE_CODEC.optionalFieldOf("resize_style", ResizeStyle.CONTINUOUS).forGetter(WorldLimitPlan.DimensionLimit::resizeStyle)
    ).apply(instance, WorldLimitPlan.DimensionLimit::new));

    static final Codec<WorldLimitPlan.EndLimit> END_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("carry_from_overworld").forGetter(WorldLimitPlan.EndLimit::carryFromOverworld),
        Codec.INT.fieldOf("minimum_radius").forGetter(WorldLimitPlan.EndLimit::minimumRadiusBlocks)
    ).apply(instance, WorldLimitPlan.EndLimit::new));

    static final Codec<WorldLimitPlan> PLAN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        DIMENSION_CODEC.fieldOf("overworld").forGetter(WorldLimitPlan::overworld),
        DIMENSION_CODEC.fieldOf("nether").forGetter(WorldLimitPlan::nether),
        END_CODEC.optionalFieldOf("end", WorldLimitPlan.EndLimit.disabled()).forGetter(WorldLimitPlan::end)
    ).apply(instance, WorldLimitPlan::new));

    private WorldLimitCodecs() {
    }
}
