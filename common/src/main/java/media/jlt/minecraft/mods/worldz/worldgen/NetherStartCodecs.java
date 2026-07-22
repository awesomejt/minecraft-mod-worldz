package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.logic.NetherStartPlan;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;

/** Persistence codec for a resolved Nether-start plan (GOALS 27, DESIGN §31). */
final class NetherStartCodecs {
    private static final Codec<StarterKitTier> TIER_CODEC = Codec.STRING.xmap(StarterKitTier::parse, StarterKitTier::serializedName);

    static final Codec<NetherStartPlan> PLAN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(NetherStartPlan::enabled),
        Codec.INT.fieldOf("spawn_y").forGetter(NetherStartPlan::spawnY),
        TIER_CODEC.fieldOf("chest_tier").forGetter(NetherStartPlan::chestTier)
    ).apply(instance, NetherStartPlan::new));

    private NetherStartCodecs() {
    }
}
