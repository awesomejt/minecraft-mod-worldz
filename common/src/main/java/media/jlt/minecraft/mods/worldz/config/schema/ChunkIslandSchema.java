package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.ChunkIslandConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

import java.util.List;

/**
 * Schema for {@link ChunkIslandConfig} (GOALS 09/37; TODO 25.2e, 25.6d). Every sanitized field
 * still processes in the same order it reads/emits, so no {@link #postValidate} is needed at all --
 * unlike {@link FloatingIslandsSchema}, this section's shared {@code exclusionZone} is never
 * rendered in the summary at all (not even a merged segment; {@code chunkIslandSummary} :2114-2126
 * simply never mentions it), so the {@code exclusionZone} group {@link Setting} is {@link
 * Setting.Builder#hiddenFromSummary()} rather than needing {@link OceanIslandSchema}'s
 * paired-render trick (moot anyway, since {@link #summary} is fully hand-overridden below and
 * never delegates through it).
 *
 * <p>{@code topOnly: {enabled, depth, scatteredChance}} (TODO 25.6d) pulls {@code
 * scatteredTopOnlyChance} out of its old position (after {@code exclusionZoneRadiusBlocks}) to sit
 * next to {@code topOnly}/{@code topOnlyDepthBlocks}, so the group's three members are contiguous
 * in {@link #declare()} -- the same kind of emit-order change DESIGN §42.3 documents for {@code
 * oceanIsland.fluid}. None of the three has any cross-field dependency on the others or on {@code
 * exclusionZone}, so the reorder changes nothing about when/whether anything clamps.
 *
 * <p>{@code exclusionZone} reuses the shared {@link ExclusionZoneSchema}, matching this section's
 * pre-25.6d bound of {@code [0, WorldzConfig.MAX_BORDER_RADIUS_BLOCKS]} -- the one call site whose
 * floor is {@code 0}, not {@code 1}. {@code applyTo: {nether, end}} groups the two mirrored
 * dimension flags (unlike {@code skyIsland}/{@code strip}, which each keep a single bare {@code
 * applyToNether} -- a one-member group would be noise, DESIGN §42.3's other noted asymmetry).
 *
 * <p>{@code geodeFeatureIds} uses {@link Rule.TrimNonEmpty}: a silent trim-and-drop with no
 * fallback and no warning, unlike every other list rule in this package.
 *
 * <p>Summary is overridden (DESIGN §41.6's table): gated on {@code enabled}, with {@code
 * topOnlyDepthBlocks} a conditional trailing segment shown only when {@code topOnly} is set
 * (:2121) -- both the gate and the conditional segment could, in isolation, use {@code
 * includeInSummaryWhen}, but since the top-level gate itself has no generic mechanism in this
 * framework (see {@code StripSchema}/{@code StripBandsSchema}'s own overrides), the whole line is
 * simplest written by hand, verbatim against {@code chunkIslandSummary}. Unchanged by TODO 25.6d
 * beyond path strings, since it reads {@link ChunkIslandConfig}'s own (unrenamed) POJO fields
 * directly.
 */
public final class ChunkIslandSchema extends SchemaSection<ChunkIslandConfig> {
    private final TopOnlySchema topOnly;
    private final ExclusionZoneSchema<ChunkIslandConfig> exclusionZone;
    private final ApplyToSchema applyTo;

    public ChunkIslandSchema(String path) {
        super(path, ChunkIslandConfig::new);
        this.topOnly = new TopOnlySchema(path() + ".topOnly");
        this.exclusionZone = new ExclusionZoneSchema<>(
            path() + ".exclusionZone", ChunkIslandConfig::new,
            new Accessor<>(c -> c.exclusionZoneEnabled, (c, v) -> c.exclusionZoneEnabled = v),
            new Accessor<>(c -> c.exclusionZoneRadiusBlocks, (c, v) -> c.exclusionZoneRadiusBlocks = v),
            0, WorldzConfig.MAX_BORDER_RADIUS_BLOCKS
        );
        this.applyTo = new ApplyToSchema(path() + ".applyTo");
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
            Setting.group("topOnly", topOnly)
                .render(topOnly::summary)
                .doc("Whether/how deep a selected island keeps only its top slice, void below.")
                .build(),
            Setting.group("exclusionZone", exclusionZone)
                .hiddenFromSummary()
                .doc("Whether a void buffer precedes scattered islands around the starter.")
                .build(),
            Setting.group("applyTo", applyTo)
                .render(applyTo::summary)
                .doc("Whether the same chunk-island mechanism also applies to the Nether/End.")
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

    /**
     * {@code chunkIsland.topOnly} (TODO 25.6d): {@code enabled}/{@code depth}/{@code
     * scatteredChance}, a group over this section's own {@link ChunkIslandConfig} type. No
     * cross-field dependency between the three (class Javadoc), so all three keep their ordinary
     * per-setting rules and this group needs no {@link #postValidate} override.
     */
    private static final class TopOnlySchema extends SchemaSection<ChunkIslandConfig> {
        TopOnlySchema(String path) {
            super(path, ChunkIslandConfig::new);
        }

        @Override
        protected List<Setting<ChunkIslandConfig, ?>> declare() {
            return List.of(
                Setting.<ChunkIslandConfig>flag("enabled", c -> c.topOnly, (c, v) -> c.topOnly = v)
                    .doc("Whether a selected island keeps only its top depth, void below.")
                    .build(),
                Setting.<ChunkIslandConfig>integer("depth", c -> c.topOnlyDepthBlocks, (c, v) -> c.topOnlyDepthBlocks = v)
                    .range(1, WorldzConfig.MAX_BORDER_RADIUS_BLOCKS)
                    .unit(Unit.BLOCKS)
                    .doc("Depth kept below the real generated surface when enabled.")
                    .build(),
                Setting.<ChunkIslandConfig>decimal(
                        "scatteredChance", c -> c.scatteredTopOnlyChance, (c, v) -> c.scatteredTopOnlyChance = v
                    )
                    .range(0.0, 1.0)
                    .unit(Unit.CHANCE)
                    .doc("Probability an ordinary scattered island independently resolves top-only.")
                    .build()
            );
        }

        @Override
        public String summary(ChunkIslandConfig value) {
            return "enabled=" + value.topOnly
                + ", depth=" + value.topOnlyDepthBlocks
                + ", scatteredChance=" + value.scatteredTopOnlyChance;
        }
    }

    /**
     * {@code chunkIsland.applyTo} (TODO 25.6d): {@code nether}/{@code end}, a group over this
     * section's own {@link ChunkIslandConfig} type.
     */
    private static final class ApplyToSchema extends SchemaSection<ChunkIslandConfig> {
        ApplyToSchema(String path) {
            super(path, ChunkIslandConfig::new);
        }

        @Override
        protected List<Setting<ChunkIslandConfig, ?>> declare() {
            return List.of(
                Setting.<ChunkIslandConfig>flag("nether", c -> c.applyToNether, (c, v) -> c.applyToNether = v)
                    .doc("Whether the same chunk-island mechanism also applies to the Nether.")
                    .build(),
                Setting.<ChunkIslandConfig>flag("end", c -> c.applyToEnd, (c, v) -> c.applyToEnd = v)
                    .doc("Whether the same chunk-island mechanism also applies to the End.")
                    .build()
            );
        }

        @Override
        public String summary(ChunkIslandConfig value) {
            return "nether=" + value.applyToNether + ", end=" + value.applyToEnd;
        }
    }
}
