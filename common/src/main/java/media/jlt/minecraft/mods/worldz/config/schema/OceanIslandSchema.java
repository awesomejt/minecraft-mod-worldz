package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.OceanIslandConfig;
import media.jlt.minecraft.mods.worldz.config.StarterKitConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.logic.IslandFluid;
import media.jlt.minecraft.mods.worldz.logic.IslandShapeProfile;
import media.jlt.minecraft.mods.worldz.logic.IslandSource;

import java.util.List;

/**
 * Schema for {@link OceanIslandConfig} (GOALS 01, 02, 03, 04, 28, 31; DESIGN §24, §25, §26; TODO
 * 25.2e). {@code islandBiome} uses {@link Rule.BiomeIdOrDefault}: an invalid or blank value is
 * silently blanked (or warned once, if invalid rather than blank) exactly like {@link
 * Rule.BiomeId}, but a still-empty result then warns a <em>second</em> time citing the original
 * raw value and falls back to the section's own default -- {@code sanitizeOceanIsland} :437-442.
 *
 * <p>{@code exclusionZoneEnabled}/{@code exclusionZoneRadiusBlocks} render as one collapsed
 * {@code exclusionZone=} summary segment (DESIGN §41.6's table) without any {@code summary()}
 * override: the two settings' {@code includeInSummaryWhen} predicates are exact complements of
 * each other ({@code owner -> !owner.exclusionZoneEnabled} and {@code owner ->
 * owner.exclusionZoneEnabled}), so exactly one of the two ever contributes a segment, both sharing
 * the label {@code "exclusionZone"} -- proving TODO 25.2e's "shared exclusion-zone pair" shape
 * that 25.6 later collapses for real. The rest of the summary is fully derived; no override.
 */
public final class OceanIslandSchema extends SchemaSection<OceanIslandConfig> {
    public OceanIslandSchema(String path) {
        super(path, OceanIslandConfig::new);
    }

    @Override
    protected List<Setting<OceanIslandConfig, ?>> declare() {
        StarterKitSchema starterKitSchema = new StarterKitSchema(path() + ".starterKit");
        return List.of(
            Setting.<OceanIslandConfig, IslandSource>enumeration(
                    "islandSource", c -> c.islandSource, (c, v) -> c.islandSource = v,
                    IslandSource::parse, IslandSource::serializedName, IslandSource.ARTIFICIAL
                )
                .doc("How the land is sourced: artificial, natural (seed), or chest-boat/none.")
                .build(),
            Setting.<OceanIslandConfig, IslandFluid>enumeration(
                    "fluid", c -> c.fluid, (c, v) -> c.fluid = v,
                    IslandFluid::parse, IslandFluid::serializedName, IslandFluid.WATER
                )
                .doc("The exterior/ocean gradient's fluid: water, lava, or none.")
                .build(),
            Setting.<OceanIslandConfig>text("islandBiome", c -> c.islandBiome, (c, v) -> c.islandBiome = v)
                .rule(new Rule.BiomeIdOrDefault<>(
                    "Ignoring invalid " + path() + ".islandBiome '{}'.",
                    "Invalid " + path() + ".islandBiome '{}'; using the default instead.",
                    () -> new OceanIslandConfig().islandBiome
                ))
                .unit(Unit.BIOME_ID)
                .doc("The one biome that fills the island's interior.")
                .build(),
            Setting.<OceanIslandConfig>integer("radiusBlocks", c -> c.radiusBlocks, (c, v) -> c.radiusBlocks = v)
                .range(WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS, WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Configured (unperturbed) island radius.")
                .build(),
            Setting.<OceanIslandConfig>decimal("shapeAmplitude", c -> c.shapeAmplitude, (c, v) -> c.shapeAmplitude = v)
                .range(0.0, IslandShapeProfile.MAX_AMPLITUDE)
                .doc("Coastline perturbation strength.")
                .build(),
            Setting.<OceanIslandConfig>integer("shoreWidthBlocks", c -> c.shoreWidthBlocks, (c, v) -> c.shoreWidthBlocks = v)
                .min(1)
                .unit(Unit.BLOCKS)
                .doc("Width of the beach/stony-shore ring; also the terrain-height taper width.")
                .build(),
            Setting.<OceanIslandConfig>integer(
                    "oceanShallowWidthBlocks", c -> c.oceanShallowWidthBlocks, (c, v) -> c.oceanShallowWidthBlocks = v
                )
                .min(0)
                .unit(Unit.BLOCKS)
                .doc("Width of the shallow ocean band immediately beyond the shore.")
                .build(),
            Setting.<OceanIslandConfig>integer(
                    "oceanDeepenWidthBlocks", c -> c.oceanDeepenWidthBlocks, (c, v) -> c.oceanDeepenWidthBlocks = v
                )
                .min(0)
                .unit(Unit.BLOCKS)
                .doc("Width over which the seabed ramps from shallow to deep.")
                .build(),
            Setting.<OceanIslandConfig>integer(
                    "oceanShallowDepthBlocks", c -> c.oceanShallowDepthBlocks, (c, v) -> c.oceanShallowDepthBlocks = v
                )
                .min(1)
                .unit(Unit.BLOCKS)
                .doc("Seabed depth below sea level in the shallow band.")
                .build(),
            Setting.<OceanIslandConfig>integer("oceanDeepDepthBlocks", c -> c.oceanDeepDepthBlocks, (c, v) -> c.oceanDeepDepthBlocks = v)
                .min(1)
                .unit(Unit.BLOCKS)
                .doc("Seabed depth below sea level once fully deep.")
                .build(),
            Setting.<OceanIslandConfig>integer(
                    "oceanRegionScaleBlocks", c -> c.oceanRegionScaleBlocks, (c, v) -> c.oceanRegionScaleBlocks = v
                )
                .min(1)
                .unit(Unit.BLOCKS)
                .doc("Grid-cell edge length for the ocean biome's per-region pick.")
                .build(),
            Setting.<OceanIslandConfig>flag("exclusionZoneEnabled", c -> c.exclusionZoneEnabled, (c, v) -> c.exclusionZoneEnabled = v)
                .label("exclusionZone")
                .render(enabled -> "<disabled>")
                .includeInSummaryWhen(owner -> !owner.exclusionZoneEnabled)
                .doc("Whether island/ocean shaping releases beyond exclusionZoneRadiusBlocks.")
                .build(),
            Setting.<OceanIslandConfig>integer(
                    "exclusionZoneRadiusBlocks", c -> c.exclusionZoneRadiusBlocks, (c, v) -> c.exclusionZoneRadiusBlocks = v
                )
                .min(1)
                .unit(Unit.BLOCKS)
                .label("exclusionZone")
                .render(radius -> "radius=" + radius)
                .includeInSummaryWhen(owner -> owner.exclusionZoneEnabled)
                .doc("Radius beyond which shaping releases, when enabled.")
                .build(),
            Setting.<OceanIslandConfig, StarterKitConfig>section(
                    "starterKit", c -> c.starterKit, (c, v) -> c.starterKit = v, starterKitSchema
                )
                .render(starterKitSchema::summary)
                .doc("Chest-boat starter kit, consulted only when islandSource is chest_boat.")
                .build()
        );
    }
}
