package media.jlt.minecraft.mods.worldz.config;

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
    /** Whether a void buffer precedes scattered islands around the starter. */
    public boolean exclusionZoneEnabled;
    /** Exclusion-zone radius in blocks. */
    public int exclusionZoneRadiusBlocks = 256;
    /** Whether the same chunk-island mechanism also applies to the Nether. */
    public boolean applyToNether;
    /** Whether the same chunk-island mechanism also applies to the End. */
    public boolean applyToEnd;

    /** Creates the disabled default configuration. */
    public ChunkIslandConfig() {
    }
}
