package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.logic.EndStartPlan;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;

/** Persistence codec for a resolved End-start plan (GOALS 34, DESIGN §32). */
final class EndStartCodecs {
    private static final Codec<StarterKitTier> TIER_CODEC = Codec.STRING.xmap(StarterKitTier::parse, StarterKitTier::serializedName);

    static final Codec<EndStartPlan> PLAN_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("enabled").forGetter(EndStartPlan::enabled),
        TIER_CODEC.fieldOf("chest_tier").forGetter(EndStartPlan::chestTier)
    ).apply(instance, EndStartPlan::new));

    private EndStartCodecs() {
    }
}
