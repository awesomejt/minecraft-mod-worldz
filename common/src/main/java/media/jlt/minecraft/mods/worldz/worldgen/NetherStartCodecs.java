package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.logic.LightSource;
import media.jlt.minecraft.mods.worldz.logic.NetherStartPlan;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;

/** Persistence codec for a resolved Nether-start plan (GOALS 27, DESIGN §31, capsule shape DESIGN §31.9). */
final class NetherStartCodecs {
    private static final Codec<StarterKitTier> TIER_CODEC = Codec.STRING.xmap(StarterKitTier::parse, StarterKitTier::serializedName);
    private static final Codec<LightSource> LIGHT_SOURCE_CODEC = Codec.STRING.xmap(LightSource::parse, LightSource::serializedName);

    static final Codec<NetherStartPlan> PLAN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(NetherStartPlan::enabled),
        Codec.INT.fieldOf("spawn_y").forGetter(NetherStartPlan::spawnY),
        TIER_CODEC.fieldOf("chest_tier").forGetter(NetherStartPlan::chestTier),
        Codec.BOOL.fieldOf("force_capsule").forGetter(NetherStartPlan::forceCapsule),
        Codec.INT.fieldOf("capsule_size").forGetter(NetherStartPlan::capsuleSizeBlocks),
        Codec.INT.fieldOf("capsule_height").forGetter(NetherStartPlan::capsuleHeightBlocks),
        LIGHT_SOURCE_CODEC.fieldOf("capsule_light_source").forGetter(NetherStartPlan::capsuleLightSource),
        Codec.INT.fieldOf("capsule_light_spacing").forGetter(NetherStartPlan::capsuleLightSpacingBlocks)
    ).apply(instance, NetherStartPlan::new));

    private NetherStartCodecs() {
    }
}
