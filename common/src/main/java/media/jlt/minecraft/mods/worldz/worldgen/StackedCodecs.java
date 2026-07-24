package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.logic.StackedLayerSpec;
import media.jlt.minecraft.mods.worldz.logic.StackedPlan;

/** Persistence codec for a resolved stacked-biome-layers plan (GOAL 35, DESIGN §34.1). */
final class StackedCodecs {
    private static final Codec<StackedLayerSpec> LAYER_CODEC =
        Codec.STRING.xmap(StackedLayerSpec::parse, StackedLayerSpec::format);

    static final Codec<StackedPlan> PLAN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(StackedPlan::enabled),
        LAYER_CODEC.listOf().fieldOf("layers").forGetter(StackedPlan::layers),
        Codec.BOOL.fieldOf("seed_randomized_order").forGetter(StackedPlan::seedRandomizedOrder),
        // Optional with a zero default (DESIGN §34.7) for save-compat with worlds generated
        // before relief existed -- they resume as perfectly flat, exactly as before.
        Codec.INT.optionalFieldOf("relief_blocks", 0).forGetter(StackedPlan::reliefBlocks)
    ).apply(instance, StackedPlan::new));

    private StackedCodecs() {
    }
}
