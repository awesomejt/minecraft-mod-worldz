package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.OceanIslandConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.logic.IslandFluid;
import media.jlt.minecraft.mods.worldz.logic.IslandShapeProfile;
import media.jlt.minecraft.mods.worldz.logic.IslandSource;

import java.util.List;

/**
 * Schema for {@link OceanIslandConfig} (GOALS 01, 02, 03, 04, 28, 31; DESIGN §24, §25, §26; TODO
 * 25.2e, 25.6d). {@code islandBiome} uses {@link Rule.BiomeIdOrDefault}: an invalid or blank value is
 * silently blanked (or warned once, if invalid rather than blank) exactly like {@link
 * Rule.BiomeId}, but a still-empty result then warns a <em>second</em> time citing the original
 * raw value and falls back to the section's own default -- {@code sanitizeOceanIsland} :437-442.
 *
 * <p>{@code island: {source, biome, radius, shapeAmplitude}} is a group over this section's own
 * {@link OceanIslandConfig} type (DESIGN §42.2, TODO 25.6d): every field stays flat on {@link
 * OceanIslandConfig} itself, only the YAML shape and key names change. {@code fluid} moves out from
 * between {@code islandSource}/{@code islandBiome} to keep {@code island}'s members contiguous
 * (DESIGN §42.3's noted emit-order change) -- ocean island gets an {@code island:} group (unlike
 * {@code skyIsland}, whose section name already supplies the prefix) because it also owns a sibling
 * {@code ocean:} group, needing disambiguation.
 *
 * <p>{@code exclusionZone} is the shared {@link ExclusionZoneSchema}, floored at {@code 1} with an
 * unbounded ceiling, matching the pre-25.6d {@code exclusionZoneRadiusBlocks}' own {@code min(1)}.
 * Its former paired-{@code includeInSummaryWhen} rendering trick is now {@link
 * ExclusionZoneSchema#summary}, so this section's own {@code summary()} stays fully derived (no
 * override) exactly as it was pre-25.6d.
 */
public final class OceanIslandSchema extends SchemaSection<OceanIslandConfig> {
    private final IslandSchema island;
    private final OceanSchema ocean;
    private final ExclusionZoneSchema<OceanIslandConfig> exclusionZone;

    public OceanIslandSchema(String path) {
        super(path, OceanIslandConfig::new);
        this.island = new IslandSchema(path() + ".island");
        this.ocean = new OceanSchema(path() + ".ocean");
        this.exclusionZone = new ExclusionZoneSchema<>(
            path() + ".exclusionZone", OceanIslandConfig::new,
            new Accessor<>(c -> c.exclusionZoneEnabled, (c, v) -> c.exclusionZoneEnabled = v),
            new Accessor<>(c -> c.exclusionZoneRadiusBlocks, (c, v) -> c.exclusionZoneRadiusBlocks = v),
            1, Integer.MAX_VALUE
        );
    }

    @Override
    protected List<Setting<OceanIslandConfig, ?>> declare() {
        StarterKitSchema starterKitSchema = new StarterKitSchema(path() + ".starterKit");
        return List.of(
            Setting.group("island", island)
                .render(island::summary)
                .doc("How the island's land is sourced and shaped.")
                .build(),
            Setting.<OceanIslandConfig, IslandFluid>enumeration(
                    "fluid", c -> c.fluid, (c, v) -> c.fluid = v,
                    IslandFluid::parse, IslandFluid::serializedName, IslandFluid.WATER
                )
                .doc("The exterior/ocean gradient's fluid: water, lava, or none.")
                .build(),
            Setting.<OceanIslandConfig>integer("shoreWidth", c -> c.shoreWidthBlocks, (c, v) -> c.shoreWidthBlocks = v)
                .min(1)
                .unit(Unit.BLOCKS)
                .doc("Width of the beach/stony-shore ring; also the terrain-height taper width.")
                .build(),
            Setting.group("ocean", ocean)
                .render(ocean::summary)
                .doc("The exterior ocean's shallow/deep gradient shape.")
                .build(),
            Setting.group("exclusionZone", exclusionZone)
                .render(exclusionZone::summary)
                .doc("Whether/where island and ocean shaping releases to the seed's own terrain.")
                .build(),
            StarterKitSchema.<OceanIslandConfig>reference(
                    "starterKit", c -> c.starterKit, (c, v) -> c.starterKit = v, starterKitSchema, "ocean-island-default"
                )
                .render(starterKitSchema::summaryOrReference)
                .doc("Chest-boat starter kit, consulted only when islandSource is chest_boat.")
                .build()
        );
    }

    /**
     * {@code oceanIsland.island} (TODO 25.6d): {@code source}/{@code biome}/{@code radius}/{@code
     * shapeAmplitude}, a group over {@link OceanIslandSchema}'s own {@link OceanIslandConfig} type
     * rather than a shared/parameterized class -- there is only ever one owner shape. Private,
     * exactly like {@code BorderSchema.ResizeSchema}.
     */
    private static final class IslandSchema extends SchemaSection<OceanIslandConfig> {
        IslandSchema(String path) {
            super(path, OceanIslandConfig::new);
        }

        @Override
        protected List<Setting<OceanIslandConfig, ?>> declare() {
            return List.of(
                Setting.<OceanIslandConfig, IslandSource>enumeration(
                        "source", c -> c.islandSource, (c, v) -> c.islandSource = v,
                        IslandSource::parse, IslandSource::serializedName, IslandSource.ARTIFICIAL
                    )
                    .doc("How the land is sourced: artificial, natural (seed), or chest-boat/none.")
                    .build(),
                Setting.<OceanIslandConfig>text("biome", c -> c.islandBiome, (c, v) -> c.islandBiome = v)
                    .rule(new Rule.BiomeIdOrDefault<>(
                        "Ignoring invalid " + path() + ".biome '{}'.",
                        "Invalid " + path() + ".biome '{}'; using the default instead.",
                        () -> new OceanIslandConfig().islandBiome
                    ))
                    .unit(Unit.BIOME_ID)
                    .doc("The one biome that fills the island's interior.")
                    .build(),
                Setting.<OceanIslandConfig>integer("radius", c -> c.radiusBlocks, (c, v) -> c.radiusBlocks = v)
                    .range(WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS, WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS)
                    .unit(Unit.BLOCKS)
                    .doc("Configured (unperturbed) island radius.")
                    .build(),
                Setting.<OceanIslandConfig>decimal("shapeAmplitude", c -> c.shapeAmplitude, (c, v) -> c.shapeAmplitude = v)
                    .range(0.0, IslandShapeProfile.MAX_AMPLITUDE)
                    .unit(Unit.FACTOR)
                    .doc("Coastline perturbation strength.")
                    .build()
            );
        }
    }

    /**
     * {@code oceanIsland.ocean} (TODO 25.6d): {@code shallowWidth}/{@code deepenWidth}/{@code
     * shallowDepth}/{@code deepDepth}/{@code regionScale}, a group over {@link OceanIslandSchema}'s
     * own {@link OceanIslandConfig} type. Private, exactly like {@link IslandSchema}.
     */
    private static final class OceanSchema extends SchemaSection<OceanIslandConfig> {
        OceanSchema(String path) {
            super(path, OceanIslandConfig::new);
        }

        @Override
        protected List<Setting<OceanIslandConfig, ?>> declare() {
            return List.of(
                Setting.<OceanIslandConfig>integer(
                        "shallowWidth", c -> c.oceanShallowWidthBlocks, (c, v) -> c.oceanShallowWidthBlocks = v
                    )
                    .min(0)
                    .unit(Unit.BLOCKS)
                    .doc("Width of the shallow ocean band immediately beyond the shore.")
                    .build(),
                Setting.<OceanIslandConfig>integer(
                        "deepenWidth", c -> c.oceanDeepenWidthBlocks, (c, v) -> c.oceanDeepenWidthBlocks = v
                    )
                    .min(0)
                    .unit(Unit.BLOCKS)
                    .doc("Width over which the seabed ramps from shallow to deep.")
                    .build(),
                Setting.<OceanIslandConfig>integer(
                        "shallowDepth", c -> c.oceanShallowDepthBlocks, (c, v) -> c.oceanShallowDepthBlocks = v
                    )
                    .min(1)
                    .unit(Unit.BLOCKS)
                    .doc("Seabed depth below sea level in the shallow band.")
                    .build(),
                Setting.<OceanIslandConfig>integer("deepDepth", c -> c.oceanDeepDepthBlocks, (c, v) -> c.oceanDeepDepthBlocks = v)
                    .min(1)
                    .unit(Unit.BLOCKS)
                    .doc("Seabed depth below sea level once fully deep.")
                    .build(),
                Setting.<OceanIslandConfig>integer(
                        "regionScale", c -> c.oceanRegionScaleBlocks, (c, v) -> c.oceanRegionScaleBlocks = v
                    )
                    .min(1)
                    .unit(Unit.BLOCKS)
                    .doc("Grid-cell edge length for the ocean biome's per-region pick.")
                    .build()
            );
        }
    }
}
