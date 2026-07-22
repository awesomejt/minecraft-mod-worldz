package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.logic.FlatLayerSpec;
import media.jlt.minecraft.mods.worldz.logic.FlatPlan;

/** Persistence codec for a resolved flat plan (GOAL 15, DESIGN §33.2). */
final class FlatCodecs {
    private static final Codec<FlatLayerSpec> LAYER_CODEC = Codec.STRING.xmap(FlatLayerSpec::parse, FlatLayerSpec::format);

    static final Codec<FlatPlan> PLAN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(FlatPlan::enabled),
        LAYER_CODEC.listOf().fieldOf("layers").forGetter(FlatPlan::layers),
        Codec.STRING.fieldOf("biome").forGetter(FlatPlan::biome),
        Codec.BOOL.fieldOf("decoration").forGetter(FlatPlan::decoration),
        Codec.STRING.listOf().fieldOf("structure_overrides").forGetter(FlatPlan::structureOverrides)
    ).apply(instance, FlatPlan::new));

    private FlatCodecs() {
    }
}
