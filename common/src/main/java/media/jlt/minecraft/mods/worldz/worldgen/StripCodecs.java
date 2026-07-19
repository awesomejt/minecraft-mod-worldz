package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;
import media.jlt.minecraft.mods.worldz.logic.StripPlan;

/** Persistence codec for a resolved strip-world plan. */
final class StripCodecs {
    private static final Codec<ExteriorMode> WIDTH_MODE_CODEC = Codec.STRING.xmap(ExteriorMode::parse, ExteriorMode::serializedName);

    static final Codec<StripPlan> PLAN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(StripPlan::enabled),
        Codec.INT.fieldOf("width_radius").forGetter(StripPlan::widthRadiusBlocks),
        WIDTH_MODE_CODEC.fieldOf("width_mode").forGetter(StripPlan::widthMode)
    ).apply(instance, StripPlan::new));

    private StripCodecs() {
    }
}
