package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import media.jlt.minecraft.mods.worldz.logic.ExteriorPlan;

/** Persistence codecs for resolved exterior terrain plans. */
final class ExteriorCodecs {
    private static final Codec<ExteriorMode> MODE_CODEC = Codec.STRING.xmap(ExteriorMode::parse, ExteriorMode::serializedName);

    static final Codec<ExteriorPlan.DimensionEnvelope> DIMENSION_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        MODE_CODEC.fieldOf("mode").forGetter(ExteriorPlan.DimensionEnvelope::mode),
        Codec.INT.fieldOf("boundary_radius").forGetter(ExteriorPlan.DimensionEnvelope::boundaryRadiusBlocks),
        Codec.INT.optionalFieldOf("ocean_transition_width", 0)
            .forGetter(ExteriorPlan.DimensionEnvelope::oceanTransitionWidthBlocks)
    ).apply(instance, ExteriorPlan.DimensionEnvelope::new));

    static final Codec<ExteriorPlan> PLAN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        DIMENSION_CODEC.fieldOf("overworld").forGetter(ExteriorPlan::overworld),
        DIMENSION_CODEC.fieldOf("nether").forGetter(ExteriorPlan::nether)
    ).apply(instance, ExteriorPlan::new));

    private ExteriorCodecs() {
    }
}
