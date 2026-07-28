package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.FloatingIslandsConfig;
import media.jlt.minecraft.mods.worldz.config.StarterKitConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.logic.BiomeListSpec;
import media.jlt.minecraft.mods.worldz.logic.IslandShapeProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * Schema for {@link FloatingIslandsConfig} (GOALS 07-08, DESIGN §28; TODO 25.2e). Only
 * {@code enabled} through {@code spawnChance} (the first six declared settings) sanitize in the
 * same order they read/emit; from {@code biomeVariety} onward, {@code sanitizeFloatingIslands}
 * (WorldzConfig :670-747) processes fields in an order that diverges hard from {@code
 * floatingIslandsMap}'s emit order -- most sharply, {@code naturalBiome} is declared/emitted
 * <em>last</em> but its two advisory warnings fire right after {@code biomeVariety}'s own
 * cross-check, long before {@code exclusionZoneRadiusBlocks}/{@code oreFeatureIds}/{@code
 * oreDepositsEnabled}/{@code lootKit}. Reusing per-setting rules for that whole tail would
 * silently reorder WARN lines for any input tripping more than one of them (the same risk
 * {@link LayoutSchema} documents), so everything from {@code biomeVariety} onward is pushed into
 * {@link #postValidate}, executed by hand in {@code sanitizeFloatingIslands}'s exact original
 * sequence -- including {@code lootKit}'s nested sanitize, called manually there rather than via
 * the usual automatic {@code Setting.section} recursion (its {@code Rule} is overridden to
 * {@link Rule.None} so it isn't sanitized twice).
 *
 * <p>{@code maxRadiusBlocks}'s lower bound is the sibling {@code minRadiusBlocks}, already clamped
 * by the time this setting's own rule runs (DESIGN R4): an ordinary per-setting {@code
 * IntBuilder.range(ToIntFunction, ToIntFunction)}, since this one pair does <em>not</em> diverge
 * from map order. The two advisory warnings ({@code naturalBiome}+{@code biomeVariety} both set;
 * {@code naturalBiome} alone) change no value at all (DESIGN R4) -- pure {@link #postValidate}
 * side effects, protected only by the WARN-order assertion.
 *
 * <p>Summary is overridden (DESIGN §41.6's table): gated on {@code enabled}, with {@code
 * radius=min..max} merging two settings and two more conditional segments for ore deposits and
 * the loot chest -- not mechanically derivable.
 */
public final class FloatingIslandsSchema extends SchemaSection<FloatingIslandsConfig> {
    private final StarterKitSchema lootKitSchema;

    public FloatingIslandsSchema(String path) {
        super(path, FloatingIslandsConfig::new);
        this.lootKitSchema = new StarterKitSchema(path() + ".lootKit");
    }

    @Override
    protected List<Setting<FloatingIslandsConfig, ?>> declare() {
        return List.of(
            Setting.<FloatingIslandsConfig>flag("enabled", c -> c.enabled, (c, v) -> c.enabled = v)
                .doc("Whether scattered islands generate at all.")
                .build(),
            Setting.<FloatingIslandsConfig>integer("minRadiusBlocks", c -> c.minRadiusBlocks, (c, v) -> c.minRadiusBlocks = v)
                .range(WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS, WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Smallest hash-picked island radius.")
                .build(),
            Setting.<FloatingIslandsConfig>integer("maxRadiusBlocks", c -> c.maxRadiusBlocks, (c, v) -> c.maxRadiusBlocks = v)
                .range(c -> c.minRadiusBlocks, c -> WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS)
                .rangeText("minRadiusBlocks.." + WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Largest hash-picked island radius; floored at minRadiusBlocks (DESIGN R4).")
                .build(),
            Setting.<FloatingIslandsConfig>decimal("shapeAmplitude", c -> c.shapeAmplitude, (c, v) -> c.shapeAmplitude = v)
                .range(0.0, IslandShapeProfile.MAX_AMPLITUDE)
                .doc("Coastline perturbation strength.")
                .build(),
            Setting.<FloatingIslandsConfig>integer("cellSizeBlocks", c -> c.cellSizeBlocks, (c, v) -> c.cellSizeBlocks = v)
                .range(WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS, WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Grid-cell edge length -- the primary \"how far apart\" knob.")
                .build(),
            Setting.<FloatingIslandsConfig>decimal("spawnChance", c -> c.spawnChance, (c, v) -> c.spawnChance = v)
                .range(0.0, 1.0)
                .unit(Unit.CHANCE)
                .doc("Probability that a given cell holds an island, independent of spacing.")
                .build(),
            // From here on, sanitize order diverges from declare/map order (class Javadoc): every
            // remaining setting stays a plain, rule-less declaration and the real logic -- in the
            // original imperative sequence -- lives in postValidate below.
            Setting.<FloatingIslandsConfig>flag("biomeVariety", c -> c.biomeVariety, (c, v) -> c.biomeVariety = v)
                .doc("Whether each island hash-picks its own biome from islandBiomes.")
                .build(),
            Setting.<FloatingIslandsConfig>stringList("islandBiomes", c -> c.islandBiomes, (c, v) -> c.islandBiomes = v)
                .unit(Unit.BIOME_ID)
                .doc("Candidate biome pool when biomeVariety is true.")
                .build(),
            Setting.<FloatingIslandsConfig>flag(
                    "exclusionZoneEnabled", c -> c.exclusionZoneEnabled, (c, v) -> c.exclusionZoneEnabled = v
                )
                .doc("Whether a void buffer surrounds the starter island before scattered islands begin.")
                .build(),
            Setting.<FloatingIslandsConfig>integer(
                    "exclusionZoneRadiusBlocks", c -> c.exclusionZoneRadiusBlocks, (c, v) -> c.exclusionZoneRadiusBlocks = v
                )
                .unit(Unit.BLOCKS)
                .doc("Radius of the exclusion-zone buffer, when enabled.")
                .build(),
            Setting.<FloatingIslandsConfig>flag("oreDepositsEnabled", c -> c.oreDepositsEnabled, (c, v) -> c.oreDepositsEnabled = v)
                .doc("Whether each island gets one embedded vanilla ore-vein feature.")
                .build(),
            Setting.<FloatingIslandsConfig>stringList("oreFeatureIds", c -> c.oreFeatureIds, (c, v) -> c.oreFeatureIds = v)
                .doc("Candidate vanilla ore ConfiguredFeature ids one island's deposit is hash-picked from.")
                .build(),
            Setting.<FloatingIslandsConfig>flag("lootChestEnabled", c -> c.lootChestEnabled, (c, v) -> c.lootChestEnabled = v)
                .doc("Whether each island gets one placed loot chest.")
                .build(),
            Setting.<FloatingIslandsConfig, StarterKitConfig>section(
                    "lootKit", c -> c.lootKit, (c, v) -> c.lootKit = v, lootKitSchema
                )
                .rule(new Rule.None<>())
                .doc("The loot chest's contents.")
                .build(),
            Setting.<FloatingIslandsConfig>flag("naturalBiome", c -> c.naturalBiome, (c, v) -> c.naturalBiome = v)
                .doc("Whether each island reads the real underlying seed's own biome instead of biomeVariety's pool.")
                .build()
        );
    }

    /**
     * Hand-ordered to match {@code sanitizeFloatingIslands}'s exact tail (class Javadoc):
     * {@code islandBiomes} validation and filtering, then {@code biomeVariety}'s cross-check, then
     * the two advisory warnings (value-preserving, DESIGN R4), then {@code exclusionZoneRadiusBlocks}
     * 's clamp, then {@code oreFeatureIds}' silent trim, then {@code oreDepositsEnabled}'s
     * cross-check, then {@code lootKit}'s nested sanitize -- in that order, so any input tripping
     * more than one produces the same WARN sequence as today.
     */
    @Override
    protected void postValidate(FloatingIslandsConfig value, SanitizeContext ctx) {
        BiomeListSpec biomeSpec = BiomeListSpec.parse(value.islandBiomes);
        for (String invalid : biomeSpec.invalidEntries()) {
            ctx.logger().warn("Ignoring invalid " + path() + ".islandBiomes biome '{}'.", invalid);
        }
        for (BiomeListSpec.Entry entry : biomeSpec.entries()) {
            if (entry.tag()) {
                ctx.logger().warn(
                    "Ignoring " + path() + ".islandBiomes biome tag '#{}'; floating islands require concrete biome ids.", entry.id()
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
                    + "naturalBiome takes precedence and the islandBiomes pool will be unused."
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
        value.exclusionZoneRadiusBlocks = new Rule.IntRange<FloatingIslandsConfig>(owner -> 1, owner -> Integer.MAX_VALUE)
            .apply(value, value.exclusionZoneRadiusBlocks, path() + ".exclusionZoneRadiusBlocks", ctx);

        value.oreFeatureIds = new ArrayList<>(value.oreFeatureIds.stream().map(String::trim).filter(id -> !id.isEmpty()).toList());
        if (value.oreDepositsEnabled && value.oreFeatureIds.isEmpty()) {
            ctx.logger().warn(path() + ".oreDepositsEnabled is set but has no usable feature ids; disabling ore deposits.");
            value.oreDepositsEnabled = false;
        }
        value.lootKit = lootKitSchema.sanitize(value.lootKit, ctx);
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
            + ", lootChest=" + (value.lootChestEnabled ? lootKitSchema.summary(value.lootKit) : "<disabled>");
    }
}
