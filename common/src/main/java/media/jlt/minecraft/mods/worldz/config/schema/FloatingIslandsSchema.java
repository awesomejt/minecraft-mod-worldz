package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.FloatingIslandsConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.logic.BiomeListSpec;
import media.jlt.minecraft.mods.worldz.logic.IslandShapeProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * Schema for {@link FloatingIslandsConfig} (GOALS 07-08, DESIGN §28; TODO 25.2e, 25.6d). Only
 * {@code enabled} through {@code spawnChance} (the first six declared settings) sanitize in the
 * same order they read/emit; from {@code biomeVariety} onward, {@code sanitizeFloatingIslands}
 * (WorldzConfig :670-747) processes fields in an order that diverges hard from {@code
 * floatingIslandsMap}'s emit order -- most sharply, {@code naturalBiome} is declared/emitted
 * <em>last</em> but its two advisory warnings fire right after {@code biomeVariety}'s own
 * cross-check, long before {@code exclusionZone}/{@code oreDeposits}/{@code lootChest}. Reusing
 * per-setting rules for that whole tail would silently reorder WARN lines for any input tripping
 * more than one of them (the same risk {@link LayoutSchema} documents), so everything from {@code
 * biomeVariety} onward is pushed into {@link #postValidate}, executed by hand in {@code
 * sanitizeFloatingIslands}'s exact original sequence -- including {@code exclusionZone}/{@code
 * oreDeposits}/{@code lootChest}'s own nested sanitize, called manually there rather than via the
 * usual automatic {@code Setting.group}/{@code Setting.section} recursion (each one's own {@code
 * Rule} is overridden to {@link Rule.None} on the outer setting so it isn't sanitized twice, at the
 * wrong position -- DESIGN R4, the same treatment {@code lootKit} already got pre-25.6d).
 *
 * <p>{@code radius: {min, max}} is <em>not</em> part of that divergent tail (TODO 25.6d): {@code
 * minRadiusBlocks}/{@code maxRadiusBlocks} were always among the first six settings that already
 * sanitize automatically in declare/map order, so nesting them changes nothing about when they
 * clamp -- {@code max}'s lower bound is the sibling {@code min}, already clamped by the time this
 * setting's own rule runs (DESIGN R4): an ordinary per-setting {@code
 * IntBuilder.range(ToIntFunction, ToIntFunction)}, unchanged in shape, just renamed and grouped.
 *
 * <p>{@code exclusionZone} reuses the shared {@link ExclusionZoneSchema} (floored at {@code 1},
 * matching the pre-25.6d {@code exclusionZoneRadiusBlocks}' own bound); {@code oreDeposits} and
 * {@code lootChest} are private, non-shared groups (only this section owns them). The two advisory
 * warnings ({@code naturalBiome}+{@code biomeVariety} both set; {@code naturalBiome} alone) change
 * no value at all (DESIGN R4) -- pure {@link #postValidate} side effects, protected only by the
 * WARN-order assertion.
 *
 * <p>Summary is overridden (DESIGN §41.6's table): gated on {@code enabled}, with {@code
 * radius=min..max} merging two settings and two more conditional segments for ore deposits and
 * the loot chest -- not mechanically derivable. Unchanged by TODO 25.6d beyond path strings, since
 * it reads {@link FloatingIslandsConfig}'s own (unrenamed) POJO fields directly.
 */
public final class FloatingIslandsSchema extends SchemaSection<FloatingIslandsConfig> {
    private final RadiusSchema radius;
    private final ExclusionZoneSchema<FloatingIslandsConfig> exclusionZone;
    private final OreDepositsSchema oreDeposits;
    private final LootChestSchema lootChest;

    public FloatingIslandsSchema(String path) {
        super(path, FloatingIslandsConfig::new);
        this.radius = new RadiusSchema(path() + ".radius");
        this.exclusionZone = new ExclusionZoneSchema<>(
            path() + ".exclusionZone", FloatingIslandsConfig::new,
            new Accessor<>(c -> c.exclusionZoneEnabled, (c, v) -> c.exclusionZoneEnabled = v),
            new Accessor<>(c -> c.exclusionZoneRadiusBlocks, (c, v) -> c.exclusionZoneRadiusBlocks = v),
            1, Integer.MAX_VALUE
        );
        this.oreDeposits = new OreDepositsSchema(path() + ".oreDeposits");
        this.lootChest = new LootChestSchema(path() + ".lootChest");
    }

    @Override
    protected List<Setting<FloatingIslandsConfig, ?>> declare() {
        return List.of(
            Setting.<FloatingIslandsConfig>flag("enabled", c -> c.enabled, (c, v) -> c.enabled = v)
                .customizeExposed()
                .doc("Whether scattered islands generate at all.")
                .build(),
            Setting.group("radius", radius)
                .render(radius::summary)
                .customizeChildrenExposed()
                .doc("The range a scattered island's radius is hash-picked from.")
                .build(),
            Setting.<FloatingIslandsConfig>decimal("shapeAmplitude", c -> c.shapeAmplitude, (c, v) -> c.shapeAmplitude = v)
                .range(0.0, IslandShapeProfile.MAX_AMPLITUDE)
                .unit(Unit.FACTOR)
                .customizeExposed()
                .doc("Coastline perturbation strength.")
                .build(),
            Setting.<FloatingIslandsConfig>integer("cellSize", c -> c.cellSizeBlocks, (c, v) -> c.cellSizeBlocks = v)
                .range(WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS, WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS)
                .unit(Unit.BLOCKS)
                .customizeExposed()
                .doc("Grid-cell edge length -- the primary \"how far apart\" knob.")
                .build(),
            Setting.<FloatingIslandsConfig>decimal("spawnChance", c -> c.spawnChance, (c, v) -> c.spawnChance = v)
                .range(0.0, 1.0)
                .unit(Unit.CHANCE)
                .customizeExposed()
                .doc("Probability that a given cell holds an island, independent of spacing.")
                .build(),
            // From here on, sanitize order diverges from declare/map order (class Javadoc): every
            // remaining setting stays a plain, rule-less declaration and the real logic -- in the
            // original imperative sequence -- lives in postValidate below.
            Setting.<FloatingIslandsConfig>flag("biomeVariety", c -> c.biomeVariety, (c, v) -> c.biomeVariety = v)
                .customizeExposed()
                .doc("Whether each island hash-picks its own biome from biomes.")
                .build(),
            Setting.<FloatingIslandsConfig>stringList("biomes", c -> c.islandBiomes, (c, v) -> c.islandBiomes = v)
                .unit(Unit.BIOME_ID)
                .customizeExposed()
                .doc("Candidate biome pool when biomeVariety is true.")
                .build(),
            Setting.group("exclusionZone", exclusionZone)
                .rule(new Rule.None<>())
                .render(exclusionZone::summary)
                .customizeChildrenExposed()
                .doc("Whether a void buffer surrounds the starter island before scattered islands begin.")
                .build(),
            Setting.group("oreDeposits", oreDeposits)
                .rule(new Rule.None<>())
                .render(oreDeposits::summary)
                .doc("Whether each island gets one embedded vanilla ore-vein feature.")
                .build(),
            Setting.group("lootChest", lootChest)
                .rule(new Rule.None<>())
                .render(lootChest::summary)
                .doc("Whether each island gets one placed loot chest.")
                .build(),
            Setting.<FloatingIslandsConfig>flag("naturalBiome", c -> c.naturalBiome, (c, v) -> c.naturalBiome = v)
                .customizeExposed()
                .doc("Whether each island reads the real underlying seed's own biome instead of biomeVariety's pool.")
                .build()
        );
    }

    /**
     * Hand-ordered to match {@code sanitizeFloatingIslands}'s exact tail (class Javadoc):
     * {@code biomes} validation and filtering, then {@code biomeVariety}'s cross-check, then
     * the two advisory warnings (value-preserving, DESIGN R4), then {@code exclusionZone}'s
     * clamp, then {@code oreDeposits}' silent trim and cross-check, then {@code lootChest}'s
     * nested sanitize -- in that order, so any input tripping more than one produces the same
     * WARN sequence as today.
     */
    @Override
    protected void postValidate(FloatingIslandsConfig value, SanitizeContext ctx) {
        BiomeListSpec biomeSpec = BiomeListSpec.parse(value.islandBiomes);
        for (String invalid : biomeSpec.invalidEntries()) {
            ctx.logger().warn("Ignoring invalid " + path() + ".biomes biome '{}'.", invalid);
        }
        for (BiomeListSpec.Entry entry : biomeSpec.entries()) {
            if (entry.tag()) {
                ctx.logger().warn(
                    "Ignoring " + path() + ".biomes biome tag '#{}'; floating islands require concrete biome ids.", entry.id()
                );
            }
        }
        value.islandBiomes = new ArrayList<>(
            biomeSpec.entries().stream().filter(entry -> !entry.tag()).map(BiomeListSpec.Entry::id).toList()
        );
        if (value.biomeVariety && value.islandBiomes.isEmpty()) {
            ctx.logger().warn(path() + ".biomeVariety is set but has no usable biomes; disabling biome variety.");
            value.biomeVariety = false;
        }
        if (value.naturalBiome && value.biomeVariety) {
            // Not a hard conflict -- naturalBiome deliberately takes precedence (DESIGN §28.4) --
            // but honoring the preferred setting silently makes biomeVariety's own pool look
            // configured yet dead, which is confusing to debug from the outside. Log the
            // limitation instead of just picking a winner quietly.
            ctx.logger().warn(
                path() + ".naturalBiome and biomeVariety are both enabled; "
                    + "naturalBiome takes precedence and the biomes pool will be unused."
            );
        }
        if (value.naturalBiome) {
            // Always true whenever floating islands are enabled (the guaranteed village, GOALS 07/
            // DESIGN §28.3, isn't itself optional) -- the village's own island can never honor
            // naturalBiome, since its real vanilla village structure was force-generated for a
            // specific structure-compatible biome.
            ctx.logger().warn(
                path() + ".naturalBiome is enabled: the guaranteed village's own "
                    + "island always keeps its forced, structure-compatible biome instead, since its "
                    + "real vanilla village structure requires it."
            );
        }
        value = exclusionZone.sanitize(value, ctx);
        value = oreDeposits.sanitize(value, ctx);
        value = lootChest.sanitize(value, ctx);
    }

    /**
     * Overridden: gated on {@code enabled} ({@code "<disabled>"}), {@code radius=} merges {@code
     * minRadiusBlocks}/{@code maxRadiusBlocks} into one segment, and {@code oreDeposits=}/{@code
     * lootChest=} are each a conditional segment pairing a flag with its own payload -- not
     * mechanically derivable.
     */
    @Override
    public String summary(FloatingIslandsConfig value) {
        if (!value.enabled) {
            return "<disabled>";
        }
        return "radius=" + value.minRadiusBlocks + ".." + value.maxRadiusBlocks
            + ", shapeAmplitude=" + value.shapeAmplitude
            + ", cellSizeBlocks=" + value.cellSizeBlocks
            + ", spawnChance=" + value.spawnChance
            + ", biomeVariety=" + value.biomeVariety
            + ", naturalBiome=" + value.naturalBiome
            + ", islandBiomes=" + value.islandBiomes
            + ", exclusionZone=" + (value.exclusionZoneEnabled ? "radius=" + value.exclusionZoneRadiusBlocks : "<disabled>")
            + ", oreDeposits=" + (value.oreDepositsEnabled ? "features=" + value.oreFeatureIds : "<disabled>")
            + ", lootChest=" + (value.lootChestEnabled ? lootChest.summary(value) : "<disabled>");
    }

    /**
     * {@code floatingIslands.radius} (TODO 25.6d): {@code min}/{@code max}, a group over this
     * section's own {@link FloatingIslandsConfig} type. Unlike {@code exclusionZone}/{@code
     * oreDeposits}/{@code lootChest} below, this group is <em>not</em> part of the divergent
     * postValidate tail (class Javadoc) -- it keeps the default automatic {@code Setting.group}
     * sanitize, at the same declare-order position {@code minRadiusBlocks} always had.
     */
    private static final class RadiusSchema extends SchemaSection<FloatingIslandsConfig> {
        RadiusSchema(String path) {
            super(path, FloatingIslandsConfig::new);
        }

        @Override
        protected List<Setting<FloatingIslandsConfig, ?>> declare() {
            return List.of(
                Setting.<FloatingIslandsConfig>integer("min", c -> c.minRadiusBlocks, (c, v) -> c.minRadiusBlocks = v)
                    .range(WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS, WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS)
                    .unit(Unit.BLOCKS)
                    .doc("Smallest hash-picked island radius.")
                    .build(),
                Setting.<FloatingIslandsConfig>integer("max", c -> c.maxRadiusBlocks, (c, v) -> c.maxRadiusBlocks = v)
                    .range(c -> c.minRadiusBlocks, c -> WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS)
                    .rangeText("min.." + WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS)
                    .unit(Unit.BLOCKS)
                    .doc("Largest hash-picked island radius; floored at min (DESIGN R4).")
                    .build()
            );
        }

        @Override
        public String summary(FloatingIslandsConfig value) {
            return value.minRadiusBlocks + ".." + value.maxRadiusBlocks;
        }
    }

    /**
     * {@code floatingIslands.oreDeposits} (TODO 25.6d): {@code enabled}/{@code featureIds}, a
     * private (non-shared) group over this section's own {@link FloatingIslandsConfig} type. Both
     * leaves keep {@link Rule.None} at the per-setting level (DESIGN R4) except {@code featureIds}'
     * own silent trim, which -- unlike the manual trim it replaces -- is an ordinary {@link
     * Rule.TrimNonEmpty}, matching {@code chunkIsland.geodeFeatureIds}' own precedent; the
     * enabled+empty cross-check stays in this group's own {@link #postValidate}, invoked by the
     * parent section's manual {@code oreDeposits.sanitize(value, ctx)} call at the original
     * imperative position.
     */
    private static final class OreDepositsSchema extends SchemaSection<FloatingIslandsConfig> {
        OreDepositsSchema(String path) {
            super(path, FloatingIslandsConfig::new);
        }

        @Override
        protected List<Setting<FloatingIslandsConfig, ?>> declare() {
            return List.of(
                Setting.<FloatingIslandsConfig>flag("enabled", c -> c.oreDepositsEnabled, (c, v) -> c.oreDepositsEnabled = v)
                    .customizeExposed()
                    .doc("Whether each island gets one embedded vanilla ore-vein feature.")
                    .build(),
                Setting.<FloatingIslandsConfig>stringList("featureIds", c -> c.oreFeatureIds, (c, v) -> c.oreFeatureIds = v)
                    .rule(new Rule.TrimNonEmpty<>())
                    .doc("Candidate vanilla ore ConfiguredFeature ids one island's deposit is hash-picked from.")
                    .build()
            );
        }

        @Override
        protected void postValidate(FloatingIslandsConfig value, SanitizeContext ctx) {
            if (value.oreDepositsEnabled && value.oreFeatureIds.isEmpty()) {
                ctx.logger().warn(path() + ".enabled is set but has no usable feature ids; disabling ore deposits.");
                value.oreDepositsEnabled = false;
            }
        }

        @Override
        public String summary(FloatingIslandsConfig value) {
            return value.oreDepositsEnabled ? "features=" + value.oreFeatureIds : "<disabled>";
        }
    }

    /**
     * {@code floatingIslands.lootChest} (TODO 25.6d): {@code enabled}/{@code kit}, a private
     * (non-shared) group over this section's own {@link FloatingIslandsConfig} type. {@code kit} is
     * bound through {@link StarterKitSchema#reference} (DESIGN §44.3.5, TODO 25.8d) -- a bare
     * {@code kits} library name (defaulting to {@code floating-islands-loot}) or a full inline
     * definition, either legal -- and keeps its default automatic sanitize (unconditional, regardless
     * of {@code enabled}, matching the pre-25.6d {@code lootKit} setting's own unconditional sanitize)
     * -- no {@link #postValidate} override needed, since there is no cross-field check here at all,
     * only the parent section's manual {@code lootChest.sanitize(value, ctx)} call to place it at
     * the original imperative position rather than this group's own declare-order position.
     */
    private static final class LootChestSchema extends SchemaSection<FloatingIslandsConfig> {
        private final StarterKitSchema kitSchema;

        LootChestSchema(String path) {
            super(path, FloatingIslandsConfig::new);
            this.kitSchema = new StarterKitSchema(path() + ".kit");
        }

        @Override
        protected List<Setting<FloatingIslandsConfig, ?>> declare() {
            return List.of(
                Setting.<FloatingIslandsConfig>flag("enabled", c -> c.lootChestEnabled, (c, v) -> c.lootChestEnabled = v)
                    .customizeExposed()
                    .doc("Whether each island gets one placed loot chest.")
                    .build(),
                StarterKitSchema.<FloatingIslandsConfig>reference(
                        "kit", c -> c.lootKit, (c, v) -> c.lootKit = v, kitSchema, "floating-islands-loot"
                    )
                    .render(kitSchema::summaryOrReference)
                    .doc("The loot chest's contents.")
                    .build()
            );
        }

        @Override
        public String summary(FloatingIslandsConfig value) {
            return value.lootChestEnabled ? kitSchema.summaryOrReference(value.lootKit) : "<disabled>";
        }
    }
}
