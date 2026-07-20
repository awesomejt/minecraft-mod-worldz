package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitPlan;

import java.util.List;

/**
 * Immutable, loader-neutral values selected for one new {@code jlt_worldz:ocean_island} world
 * (GOALS 01, 02, 03, 04; DESIGN §24, §25): the small per-type field set, not the full generic
 * Customize screen's. Unlike every other typed preset, there is no spawn-strategy field (the
 * island only ever exists artificially at the origin -- see DESIGN §24.8) and no Overworld
 * Exterior field ({@link IslandPlan} unconditionally supplies the entire Overworld exterior
 * itself). {@link #islandSource} is this preset's own analogous choice (DESIGN §25.1):
 * {@code ARTIFICIAL} keeps every field above meaningful exactly as before; {@code CHEST_BOAT}
 * (GOALS 03) makes {@code islandBiome}/{@code radiusBlocks}/{@code shapeAmplitude} unused
 * placeholders instead (land is entirely absent -- see {@link IslandPlan#hasLand}).
 *
 * @param islandSource how the land is sourced (DESIGN §25.1)
 * @param islandBiome the one biome that fills the island's interior
 * @param radiusBlocks configured (unperturbed) island radius
 * @param shapeAmplitude coastline perturbation strength
 * @param shoreWidthBlocks width of the beach/stony-shore ring; also the height-taper width
 * @param oceanShallowWidthBlocks width of the shallow ocean band immediately beyond the shore
 * @param oceanDeepenWidthBlocks width over which the seabed ramps from shallow to deep
 * @param oceanShallowDepthBlocks seabed depth below sea level in the shallow band
 * @param oceanDeepDepthBlocks seabed depth below sea level once fully deep
 * @param oceanRegionScaleBlocks grid-cell edge length for the ocean biome's per-region pick
 * @param exclusionZoneEnabled whether shaping releases beyond the exclusion radius (GOALS 04)
 * @param exclusionZoneRadiusBlocks radius beyond which shaping releases, when enabled
 * @param overworldBorder optional Overworld size limit, composed on top of the island shape
 * @param netherBorder Nether border selection
 * @param endBorder End border selection (GOALS 17's Overworld-to-End carry-over)
 * @param netherExterior Nether exterior-terrain selection
 */
public record OceanIslandCustomization(
    IslandSource islandSource,
    String islandBiome,
    int radiusBlocks,
    double shapeAmplitude,
    int shoreWidthBlocks,
    int oceanShallowWidthBlocks,
    int oceanDeepenWidthBlocks,
    int oceanShallowDepthBlocks,
    int oceanDeepDepthBlocks,
    int oceanRegionScaleBlocks,
    boolean exclusionZoneEnabled,
    int exclusionZoneRadiusBlocks,
    WorldzCustomization.BorderSettings overworldBorder,
    WorldzCustomization.BorderSettings netherBorder,
    WorldzCustomization.EndBorderSettings endBorder,
    WorldzCustomization.ExteriorSettings netherExterior
) {
    /** Validates and canonicalizes customization values. */
    public OceanIslandCustomization {
        islandBiome = requireSingleBiomeId(islandBiome, "Island biome");
        if (radiusBlocks < WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS || radiusBlocks > WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS) {
            throw new IllegalArgumentException(
                "Island radius must be between " + WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS
                    + " and " + WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS + "."
            );
        }
        if (shapeAmplitude < 0.0 || shapeAmplitude > IslandShapeProfile.MAX_AMPLITUDE) {
            throw new IllegalArgumentException(
                "Island shape amplitude must be between 0 and " + IslandShapeProfile.MAX_AMPLITUDE + "."
            );
        }
        if (shoreWidthBlocks <= 0) {
            throw new IllegalArgumentException("Shore width must be positive.");
        }
        if (oceanShallowWidthBlocks < 0 || oceanDeepenWidthBlocks < 0) {
            throw new IllegalArgumentException("Ocean gradient widths must not be negative.");
        }
        if (oceanShallowDepthBlocks <= 0 || oceanDeepDepthBlocks <= 0) {
            throw new IllegalArgumentException("Ocean gradient depths must be positive.");
        }
        if (oceanRegionScaleBlocks <= 0) {
            throw new IllegalArgumentException("Ocean region scale must be positive.");
        }
        if (exclusionZoneRadiusBlocks <= 0) {
            throw new IllegalArgumentException("Exclusion zone radius must be positive.");
        }
        if (overworldBorder == null || netherBorder == null || endBorder == null || netherExterior == null) {
            throw new IllegalArgumentException("Border and exterior settings are required.");
        }
        if (netherExterior.mode() == ExteriorMode.OCEAN) {
            throw new IllegalArgumentException("Ocean exterior is only supported in the Overworld.");
        }
    }

    /**
     * Creates values from the sanitized YAML configuration.
     *
     * @param config sanitized startup configuration
     * @return an immutable customization snapshot
     */
    public static OceanIslandCustomization fromConfig(WorldzConfig config) {
        return new OceanIslandCustomization(
            config.oceanIsland.islandSource,
            config.oceanIsland.islandBiome,
            config.oceanIsland.radiusBlocks,
            config.oceanIsland.shapeAmplitude,
            config.oceanIsland.shoreWidthBlocks,
            config.oceanIsland.oceanShallowWidthBlocks,
            config.oceanIsland.oceanDeepenWidthBlocks,
            config.oceanIsland.oceanShallowDepthBlocks,
            config.oceanIsland.oceanDeepDepthBlocks,
            config.oceanIsland.oceanRegionScaleBlocks,
            config.oceanIsland.exclusionZoneEnabled,
            config.oceanIsland.exclusionZoneRadiusBlocks,
            WorldzCustomization.BorderSettings.fromConfig(config.overworldBorder),
            WorldzCustomization.BorderSettings.fromConfig(config.netherBorder),
            WorldzCustomization.EndBorderSettings.fromConfig(config.endBorder),
            WorldzCustomization.ExteriorSettings.fromConfig(config.netherExterior)
        );
    }

    /**
     * Parses client text/toggle fields into validated customization values.
     *
     * @param islandSource how the land is sourced
     * @param islandBiome the one biome that fills the island's interior
     * @param radiusBlocks decimal island radius
     * @param shapeAmplitude decimal coastline perturbation strength
     * @param shoreWidthBlocks decimal shore-ring width
     * @param oceanShallowWidthBlocks decimal shallow-band width
     * @param oceanDeepenWidthBlocks decimal deepening-band width
     * @param oceanShallowDepthBlocks decimal shallow-band depth
     * @param oceanDeepDepthBlocks decimal deep-band depth
     * @param oceanRegionScaleBlocks decimal ocean biome region scale
     * @param exclusionZoneEnabled whether shaping releases beyond the exclusion radius
     * @param exclusionZoneRadiusBlocks decimal exclusion-zone radius
     * @param overworldBorder validated Overworld border values
     * @param netherBorder validated Nether border values
     * @param endBorder validated End border values
     * @param netherExterior validated Nether exterior values
     * @return canonical immutable customization values
     */
    public static OceanIslandCustomization fromText(
        IslandSource islandSource,
        String islandBiome,
        String radiusBlocks,
        String shapeAmplitude,
        String shoreWidthBlocks,
        String oceanShallowWidthBlocks,
        String oceanDeepenWidthBlocks,
        String oceanShallowDepthBlocks,
        String oceanDeepDepthBlocks,
        String oceanRegionScaleBlocks,
        boolean exclusionZoneEnabled,
        String exclusionZoneRadiusBlocks,
        WorldzCustomization.BorderSettings overworldBorder,
        WorldzCustomization.BorderSettings netherBorder,
        WorldzCustomization.EndBorderSettings endBorder,
        WorldzCustomization.ExteriorSettings netherExterior
    ) {
        return new OceanIslandCustomization(
            islandSource,
            islandBiome,
            parseInteger(radiusBlocks, "Island radius"),
            parseDouble(shapeAmplitude, "Island shape amplitude"),
            parseInteger(shoreWidthBlocks, "Shore width"),
            parseInteger(oceanShallowWidthBlocks, "Ocean shallow width"),
            parseInteger(oceanDeepenWidthBlocks, "Ocean deepen width"),
            parseInteger(oceanShallowDepthBlocks, "Ocean shallow depth"),
            parseInteger(oceanDeepDepthBlocks, "Ocean deep depth"),
            parseInteger(oceanRegionScaleBlocks, "Ocean region scale"),
            exclusionZoneEnabled,
            parseInteger(exclusionZoneRadiusBlocks, "Exclusion zone radius"),
            overworldBorder,
            netherBorder,
            endBorder,
            netherExterior
        );
    }

    /**
     * Resolves this world's island plan. {@code CHEST_BOAT} (GOALS 03) resolves with
     * {@link IslandPlan#hasLand} {@code false} and placeholder island-shape values (radius,
     * amplitude, biome) that are never actually consulted -- see DESIGN §25.2. {@code NATURAL}
     * (GOALS 02) resolves with {@link IslandPlan#syntheticLand} {@code false} and a placeholder
     * amplitude/biome (the real seed provides both) -- see DESIGN §25.4.
     *
     * @return immutable, always-enabled island plan
     */
    public IslandPlan islandPlan() {
        boolean hasLand = islandSource != IslandSource.CHEST_BOAT;
        boolean syntheticLand = islandSource != IslandSource.NATURAL;
        return new IslandPlan(
            true,
            hasLand ? radiusBlocks : WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS,
            syntheticLand ? shapeAmplitude : 0.0,
            syntheticLand ? islandBiome : "minecraft:plains",
            shoreWidthBlocks,
            oceanShallowWidthBlocks,
            oceanDeepenWidthBlocks,
            oceanShallowDepthBlocks,
            oceanDeepDepthBlocks,
            oceanRegionScaleBlocks,
            exclusionZoneEnabled,
            exclusionZoneRadiusBlocks,
            hasLand,
            syntheticLand
        );
    }

    /**
     * Converts all three border selections to the world-persisted plan.
     *
     * @return immutable codec-backed world-limit plan
     */
    public WorldLimitPlan worldLimitPlan() {
        return new WorldLimitPlan(overworldBorder.toPlan(), netherBorder.toPlan(), endBorder.toPlan());
    }

    /**
     * Converts the Nether exterior selection to a resolved, persisted envelope. The Overworld
     * side always stays {@code normal} -- {@link IslandPlan} supplies the entire Overworld
     * exterior itself, independent of this shared mechanism.
     *
     * @return immutable resolved exterior plan
     */
    public ExteriorPlan exteriorPlan() {
        return new ExteriorPlan(ExteriorPlan.DimensionEnvelope.normal(), netherExterior.toPlan(netherBorder));
    }

    private static String requireSingleBiomeId(String value, String name) {
        BiomeListSpec spec = BiomeListSpec.parse(List.of(value == null ? "" : value));
        if (spec.entries().size() != 1 || spec.entries().getFirst().tag()) {
            throw new IllegalArgumentException(name + " must be one biome ID, not a tag.");
        }
        return spec.entries().getFirst().id();
    }

    private static int parseInteger(String value, String name) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NullPointerException | NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a whole number.", exception);
        }
    }

    private static double parseDouble(String value, String name) {
        try {
            return Double.parseDouble(value.trim());
        } catch (NullPointerException | NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a number.", exception);
        }
    }
}
