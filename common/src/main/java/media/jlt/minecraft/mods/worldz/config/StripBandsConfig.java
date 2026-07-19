package media.jlt.minecraft.mods.worldz.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Optional biome-band variation for the {@code jlt_worldz:strip_world} typed preset
 * (GOALS 36): the strip passes through an ordered (or once-shuffled) sequence of biomes
 * along its length instead of ordinary vanilla terrain. Disabled by default -- a plain
 * strip world (GOALS 32) needs no biome-band configuration at all.
 */
public final class StripBandsConfig {
    /** Whether the strip passes through biome bands instead of ordinary vanilla terrain. */
    public boolean enabled;
    /** Ordered land biome ids walked along the strip's length; repeats once exhausted. */
    public List<String> biomes = new ArrayList<>();
    /** Band width in blocks along the strip's length axis. */
    public int widthBlocks = 128;
    /** Shuffle the sequence once (a fixed permutation, not per-band randomness) instead of using it as given. */
    public boolean seedRandomOrder;
    /**
     * Let vanilla's own river biomes generate where vanilla would place one. Unlike
     * {@code single_biome}/{@code chaos_biomes}, this defaults {@code true}: a band
     * sequence is already a curated, restricted list, so without this a player would need
     * to remember to add water/beach biomes to every band configuration just to get them
     * at all (Jason, 2026-07-19).
     */
    public boolean allowRivers = true;
    /** Let vanilla's own river/ocean-family biomes generate naturally, additive over {@link #allowRivers}. Defaults {@code true} for the same reason as {@link #allowRivers}. */
    public boolean allowOceans = true;
    /** Let vanilla's own beach/stony-shore biomes generate where vanilla would place one. Defaults {@code true} for the same reason as {@link #allowRivers}. */
    public boolean allowBeaches = true;

    /** Creates the disabled default configuration. */
    public StripBandsConfig() {
    }
}
