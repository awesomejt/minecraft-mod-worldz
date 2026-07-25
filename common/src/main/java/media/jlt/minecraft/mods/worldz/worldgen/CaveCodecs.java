package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.logic.CavePlan;
import media.jlt.minecraft.mods.worldz.logic.SealedSurfaceBlock;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;

/** Persistence codec for a resolved cave plan (GOALS 25-26, DESIGN §30). */
final class CaveCodecs {
    private static final Codec<StarterKitTier> TIER_CODEC = Codec.STRING.xmap(StarterKitTier::parse, StarterKitTier::serializedName);
    private static final Codec<SealedSurfaceBlock> SEALED_SURFACE_BLOCK_CODEC =
        Codec.STRING.xmap(SealedSurfaceBlock::parse, SealedSurfaceBlock::serializedName);

    static final Codec<CavePlan> PLAN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(CavePlan::enabled),
        Codec.INT.fieldOf("spawn_depth_y").forGetter(CavePlan::spawnDepthY),
        Codec.BOOL.fieldOf("sealed_surface").forGetter(CavePlan::sealedSurface),
        Codec.INT.fieldOf("sealed_surface_y").forGetter(CavePlan::sealedSurfaceY),
        SEALED_SURFACE_BLOCK_CODEC.fieldOf("sealed_surface_block").forGetter(CavePlan::sealedSurfaceBlock),
        Codec.INT.fieldOf("sealed_surface_thickness").forGetter(CavePlan::sealedSurfaceThicknessBlocks),
        Codec.BOOL.fieldOf("cavern_enabled").forGetter(CavePlan::cavernEnabled),
        Codec.INT.fieldOf("cavern_radius").forGetter(CavePlan::cavernRadiusBlocks),
        Codec.INT.fieldOf("cavern_height").forGetter(CavePlan::cavernHeightBlocks),
        Codec.BOOL.fieldOf("chest_enabled").forGetter(CavePlan::chestEnabled),
        TIER_CODEC.fieldOf("chest_tier").forGetter(CavePlan::chestTier)
    ).apply(instance, CavePlan::new));

    private CaveCodecs() {
    }
}
