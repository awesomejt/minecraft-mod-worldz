package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.logic.StarterLandPlan;

/** Minecraft serialization adapter for the pure starter-land plan. */
final class StarterLandCodecs {
    static final Codec<StarterLandPlan> PLAN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(StarterLandPlan::enabled),
        Codec.INT.fieldOf("transition_width").forGetter(StarterLandPlan::transitionWidthBlocks),
        Codec.INT.fieldOf("foundation_depth").forGetter(StarterLandPlan::foundationDepthBlocks)
    ).apply(instance, StarterLandPlan::new));

    private StarterLandCodecs() {
    }
}
