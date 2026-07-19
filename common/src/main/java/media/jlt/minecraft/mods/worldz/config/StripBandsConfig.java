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

    /** Creates the disabled default configuration. */
    public StripBandsConfig() {
    }
}
