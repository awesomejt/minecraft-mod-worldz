package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.ChunkIslandConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

import java.util.List;

/**
 * Schema for {@link ChunkIslandConfig} (GOALS 09/37; TODO 25.2e). Every sanitized field processes
 * in the same order it reads/emits, so no {@link #postValidate} is needed at all -- unlike {@link
 * FloatingIslandsSchema}, this section's shared {@code exclusionZoneEnabled}/{@code
 * exclusionZoneRadiusBlocks} pair is never rendered in the summary at all (not even a merged
 * segment; {@code chunkIslandSummary} :2114-2126 simply never mentions it), so both settings are
 * {@link Setting.Builder#hiddenFromSummary()} rather than needing {@link OceanIslandSchema}'s
 * paired-render trick.
 *
 * <p>{@code geodeFeatureIds} uses {@link Rule.TrimNonEmpty}: a silent trim-and-drop with no
 * fallback and no warning, unlike every other list rule in this package.
 *
 * <p>Summary is overridden (DESIGN §41.6's table): gated on {@code enabled}, with {@code
 * topOnlyDepthBlocks} a conditional trailing segment shown only when {@code topOnly} is set
 * (:2121) -- both the gate and the conditional segment could, in isolation, use {@code
 * includeInSummaryWhen}, but since the top-level gate itself has no generic mechanism in this
 * framework (see {@code StripSchema}/{@code StripBandsSchema}'s own overrides), the whole line is
 * simplest written by hand, verbatim against {@code chunkIslandSummary}.
 */
public final class ChunkIslandSchema extends SchemaSection<ChunkIslandConfig> {
    public ChunkIslandSchema(String path) {
        super(path, ChunkIslandConfig::new);
    }

    @Override
    protected List<Setting<ChunkIslandConfig, ?>> declare() {
        return List.of(
            Setting.<ChunkIslandConfig>flag("enabled", c -> c.enabled, (c, v) -> c.enabled = v)
                .doc("Whether chunk islands generate at all.")
                .build(),
            Setting.<ChunkIslandConfig>decimal("spawnChance", c -> c.spawnChance, (c, v) -> c.spawnChance = v)
                .range(0.0, 1.0)
                .unit(Unit.CHANCE)
                .doc("Probability that a given grid cell holds an island.")
                .build(),
            Setting.<ChunkIslandConfig>integer("cellSizeChunks", c -> c.cellSizeChunks, (c, v) -> c.cellSizeChunks = v)
                .range(1, WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS / 16)
                .unit(Unit.CHUNKS)
                .doc("Grid-cell edge length in chunks; 1 rolls every chunk independently.")
                .build(),
            Setting.<ChunkIslandConfig>flag("topOnly", c -> c.topOnly, (c, v) -> c.topOnly = v)
                .doc("Whether a selected island keeps only its top topOnlyDepthBlocks, void below.")
                .build(),
            Setting.<ChunkIslandConfig>integer("topOnlyDepthBlocks", c -> c.topOnlyDepthBlocks, (c, v) -> c.topOnlyDepthBlocks = v)
                .range(1, WorldzConfig.MAX_BORDER_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Depth kept below the real generated surface when topOnly.")
                .build(),
            Setting.<ChunkIslandConfig>flag(
                    "exclusionZoneEnabled", c -> c.exclusionZoneEnabled, (c, v) -> c.exclusionZoneEnabled = v
                )
                .hiddenFromSummary()
                .doc("Whether a void buffer precedes scattered islands around the starter.")
                .build(),
            Setting.<ChunkIslandConfig>integer(
                    "exclusionZoneRadiusBlocks", c -> c.exclusionZoneRadiusBlocks, (c, v) -> c.exclusionZoneRadiusBlocks = v
                )
                .range(0, WorldzConfig.MAX_BORDER_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS)
                .hiddenFromSummary()
                .doc("Exclusion-zone radius in blocks.")
                .build(),
            Setting.<ChunkIslandConfig>decimal(
                    "scatteredTopOnlyChance", c -> c.scatteredTopOnlyChance, (c, v) -> c.scatteredTopOnlyChance = v
                )
                .range(0.0, 1.0)
                .unit(Unit.CHANCE)
                .doc("Probability an ordinary scattered island independently resolves top-only.")
                .build(),
            Setting.<ChunkIslandConfig>flag("applyToNether", c -> c.applyToNether, (c, v) -> c.applyToNether = v)
                .doc("Whether the same chunk-island mechanism also applies to the Nether.")
                .build(),
            Setting.<ChunkIslandConfig>flag("applyToEnd", c -> c.applyToEnd, (c, v) -> c.applyToEnd = v)
                .doc("Whether the same chunk-island mechanism also applies to the End.")
                .build(),
            Setting.<ChunkIslandConfig>stringList("geodeFeatureIds", c -> c.geodeFeatureIds, (c, v) -> c.geodeFeatureIds = v)
                .rule(new Rule.TrimNonEmpty<>())
                .doc("Candidate configured-feature ids force-placed on the reserved geode showcase cell.")
                .build()
        );
    }

    /**
     * Overridden: {@code enabled} gates the whole line and is itself excluded from the surviving
     * segments, and {@code topOnlyDepthBlocks} is a conditional trailing segment -- not
     * mechanically derivable (class Javadoc).
     */
    @Override
    public String summary(ChunkIslandConfig value) {
        if (!value.enabled) {
            return "<disabled>";
        }
        return "spawnChance=" + value.spawnChance
            + ", cellSizeChunks=" + value.cellSizeChunks
            + ", topOnly=" + value.topOnly
            + (value.topOnly ? ", topOnlyDepthBlocks=" + value.topOnlyDepthBlocks : "")
            + ", scatteredTopOnlyChance=" + value.scatteredTopOnlyChance
            + ", applyToNether=" + value.applyToNether
            + ", applyToEnd=" + value.applyToEnd
            + ", geodeFeatureIds=" + value.geodeFeatureIds;
    }
}
