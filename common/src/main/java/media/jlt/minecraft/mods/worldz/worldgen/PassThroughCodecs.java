package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Persistence codec for the three vanilla pass-through toggles (DESIGN §20.5, GOALS 13/14),
 * nested under one field instead of three flat ones. Freed two slots in {@link
 * LimitedBiomeSource#CODEC} for Phase 10's new {@code sky_island} field -- that codec's {@code
 * instance.group(...)} was already at the 14-field {@code Function14} ceiling (same DFU-version
 * limit {@code IslandPlan} hit in Phase 9, DESIGN §26.1), and these three independent booleans
 * are always encoded together (see {@code LimitedBiomeSource.CODEC}'s forGetter), so nesting them
 * loses nothing real -- any world with one of the three present has always had all three.
 */
final class PassThroughCodecs {
    static final Codec<Flags> FLAGS_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.BOOL.fieldOf("allow_rivers").forGetter(Flags::allowRivers),
        Codec.BOOL.fieldOf("allow_oceans").forGetter(Flags::allowOceans),
        Codec.BOOL.fieldOf("allow_beaches").forGetter(Flags::allowBeaches)
    ).apply(instance, Flags::new));

    private PassThroughCodecs() {
    }

    /**
     * The three independent pass-through toggles, grouped only for wire-format compactness --
     * {@link LimitedBiomeSource} still stores and reads them as three flat fields internally.
     *
     * @param allowRivers let vanilla's own river biomes generate naturally
     * @param allowOceans let vanilla's own river/ocean-family biomes generate naturally
     * @param allowBeaches let vanilla's own beach/stony-shore biomes generate naturally
     */
    record Flags(boolean allowRivers, boolean allowOceans, boolean allowBeaches) {
    }
}
