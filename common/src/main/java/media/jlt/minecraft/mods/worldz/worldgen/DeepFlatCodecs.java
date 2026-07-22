package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.logic.DeepFlatPlan;
import media.jlt.minecraft.mods.worldz.logic.FlatLayerSpec;

/** Persistence codec for a resolved deep-flat plan (GOAL 16, DESIGN §33.4). */
final class DeepFlatCodecs {
    private static final Codec<FlatLayerSpec> LAYER_CODEC = Codec.STRING.xmap(FlatLayerSpec::parse, FlatLayerSpec::format);

    static final Codec<DeepFlatPlan> PLAN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(DeepFlatPlan::enabled),
        Codec.INT.fieldOf("surface_y").forGetter(DeepFlatPlan::surfaceY),
        LAYER_CODEC.listOf().fieldOf("cap_layers").forGetter(DeepFlatPlan::capLayers),
        Codec.BOOL.fieldOf("rivers_enabled").forGetter(DeepFlatPlan::riversEnabled),
        Codec.INT.fieldOf("river_exclusion_radius").forGetter(DeepFlatPlan::riverExclusionRadiusBlocks)
    ).apply(instance, DeepFlatPlan::new));

    private DeepFlatCodecs() {
    }
}
