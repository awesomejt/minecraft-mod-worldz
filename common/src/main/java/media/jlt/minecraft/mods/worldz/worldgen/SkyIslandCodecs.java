package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.logic.SkyIslandPlan;

/** Persistence codec for a resolved sky-island plan (DESIGN §27.9). */
final class SkyIslandCodecs {
    static final Codec<SkyIslandPlan> PLAN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(SkyIslandPlan::enabled),
        Codec.INT.fieldOf("radius").forGetter(SkyIslandPlan::radiusBlocks),
        Codec.DOUBLE.fieldOf("shape_amplitude").forGetter(SkyIslandPlan::shapeAmplitude),
        Codec.STRING.fieldOf("island_biome").forGetter(SkyIslandPlan::islandBiome),
        Codec.INT.fieldOf("surface_y").forGetter(SkyIslandPlan::surfaceY),
        Codec.INT.fieldOf("thickness").forGetter(SkyIslandPlan::thicknessBlocks)
    ).apply(instance, SkyIslandPlan::new));

    private SkyIslandCodecs() {
    }
}
