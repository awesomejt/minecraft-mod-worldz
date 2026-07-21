package media.jlt.minecraft.mods.worldz.config;

import java.util.ArrayList;
import java.util.List;

/** Configuration for chunk-shaped sky islands (GOALS 09/37). */
public final class ChunkIslandConfig {
    /** Whether chunk islands generate at all. */
    public boolean enabled;
    /** Probability (0..1) that a given grid cell holds an island. */
    public double spawnChance = 0.35;
    /** Grid-cell edge length in chunks; 1 rolls every chunk independently. */
    public int cellSizeChunks = 1;
    /** Whether a selected island keeps only its top {@link #topOnlyDepthBlocks}, void below. */
    public boolean topOnly;
    /** Depth kept below the real generated surface when {@link #topOnly}. */
    public int topOnlyDepthBlocks = 5;
    /** Probability (0..1) an ordinary scattered island independently resolves top-only (GOALS 37). */
    public double scatteredTopOnlyChance;
    /** Whether a void buffer precedes scattered islands around the starter. */
    public boolean exclusionZoneEnabled;
    /** Exclusion-zone radius in blocks. */
    public int exclusionZoneRadiusBlocks = 256;
    /** Whether the same chunk-island mechanism also applies to the Nether. */
    public boolean applyToNether;
    /** Whether the same chunk-island mechanism also applies to the End. */
    public boolean applyToEnd;
    /**
     * Candidate configured-feature ids force-placed on the reserved geode showcase cell (GOALS
     * 37, DESIGN §29.6) -- config-only, never persisted per-world, matching {@code
     * FloatingIslandsConfig.oreFeatureIds}'s precedent exactly.
     */
    public List<String> geodeFeatureIds = list("minecraft:amethyst_geode");

    /** Creates the disabled default configuration. */
    public ChunkIslandConfig() {
    }

    private static List<String> list(String... values) {
        return new ArrayList<>(List.of(values));
    }
}
