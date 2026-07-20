package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitPlan;

import java.util.List;

/**
 * Immutable, loader-neutral values selected for one new {@code jlt_worldz:sky_island} world
 * (GOALS 05, DESIGN §27): the small per-type field set, not the full generic Customize screen's.
 * Like {@code ocean_island} there is no spawn-strategy field (the island only ever exists at the
 * origin) and no separate Overworld Exterior field ({@link SkyIslandPlan} unconditionally
 * supplies the entire Overworld exterior itself, DESIGN §27.9).
 *
 * @param islandBiome the one biome that fills the island's interior
 * @param radiusBlocks configured (unperturbed) island radius
 * @param shapeAmplitude coastline perturbation strength
 * @param surfaceY the island's walkable surface Y (GOALS 05 default 64, slime rule)
 * @param thicknessBlocks how many blocks of solid ground extend below {@code surfaceY}
 * @param chestTier the necessities-chest difficulty tier (GOALS 05, DESIGN §27.8)
 * @param applyToNether whether the Nether is also a sky island, reusing this same shape
 *     (GOALS 06, DESIGN §27.6)
 * @param overworldBorder optional Overworld size limit, composed on top of the island shape
 * @param netherBorder Nether border selection
 * @param endBorder End border selection (GOALS 17's Overworld-to-End carry-over)
 * @param netherExterior Nether exterior-terrain selection
 */
public record SkyIslandCustomization(
    String islandBiome,
    int radiusBlocks,
    double shapeAmplitude,
    int surfaceY,
    int thicknessBlocks,
    StarterKitTier chestTier,
    boolean applyToNether,
    WorldzCustomization.BorderSettings overworldBorder,
    WorldzCustomization.BorderSettings netherBorder,
    WorldzCustomization.EndBorderSettings endBorder,
    WorldzCustomization.ExteriorSettings netherExterior
) {
    /** Validates and canonicalizes customization values. */
    public SkyIslandCustomization {
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
        if (thicknessBlocks < SkyIslandPlan.MIN_THICKNESS_BLOCKS || thicknessBlocks > SkyIslandPlan.MAX_THICKNESS_BLOCKS) {
            throw new IllegalArgumentException(
                "Slab thickness must be between " + SkyIslandPlan.MIN_THICKNESS_BLOCKS
                    + " and " + SkyIslandPlan.MAX_THICKNESS_BLOCKS + "."
            );
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
    public static SkyIslandCustomization fromConfig(WorldzConfig config) {
        return new SkyIslandCustomization(
            config.skyIsland.islandBiome,
            config.skyIsland.radiusBlocks,
            config.skyIsland.shapeAmplitude,
            config.skyIsland.surfaceY,
            config.skyIsland.thicknessBlocks,
            config.skyIsland.chestTier,
            config.skyIsland.applyToNether,
            WorldzCustomization.BorderSettings.fromConfig(config.overworldBorder),
            WorldzCustomization.BorderSettings.fromConfig(config.netherBorder),
            WorldzCustomization.EndBorderSettings.fromConfig(config.endBorder),
            WorldzCustomization.ExteriorSettings.fromConfig(config.netherExterior)
        );
    }

    /**
     * Parses client text/toggle fields into validated customization values.
     *
     * @param islandBiome the one biome that fills the island's interior
     * @param radiusBlocks decimal island radius
     * @param shapeAmplitude decimal coastline perturbation strength
     * @param surfaceY decimal surface Y
     * @param thicknessBlocks decimal slab thickness
     * @param chestTier the necessities-chest difficulty tier
     * @param applyToNether whether the Nether is also a sky island
     * @param overworldBorder validated Overworld border values
     * @param netherBorder validated Nether border values
     * @param endBorder validated End border values
     * @param netherExterior validated Nether exterior values
     * @return canonical immutable customization values
     */
    public static SkyIslandCustomization fromText(
        String islandBiome,
        String radiusBlocks,
        String shapeAmplitude,
        String surfaceY,
        String thicknessBlocks,
        StarterKitTier chestTier,
        boolean applyToNether,
        WorldzCustomization.BorderSettings overworldBorder,
        WorldzCustomization.BorderSettings netherBorder,
        WorldzCustomization.EndBorderSettings endBorder,
        WorldzCustomization.ExteriorSettings netherExterior
    ) {
        return new SkyIslandCustomization(
            islandBiome,
            parseInteger(radiusBlocks, "Island radius"),
            parseDouble(shapeAmplitude, "Island shape amplitude"),
            parseInteger(surfaceY, "Surface Y"),
            parseInteger(thicknessBlocks, "Slab thickness"),
            chestTier,
            applyToNether,
            overworldBorder,
            netherBorder,
            endBorder,
            netherExterior
        );
    }

    /**
     * Resolves this world's sky island plan.
     *
     * @return immutable, always-enabled sky island plan
     */
    public SkyIslandPlan skyIslandPlan() {
        return new SkyIslandPlan(true, radiusBlocks, shapeAmplitude, islandBiome, surfaceY, thicknessBlocks, chestTier);
    }

    /**
     * Resolves the Nether's own sky island plan (GOALS 06, DESIGN §27.6): the same shape as the
     * Overworld's, but disabled entirely unless {@link #applyToNether} is set. {@code islandBiome}/
     * {@code chestTier} are unused placeholders on the Nether side -- the block palette is fixed
     * (netherrack-family, DESIGN §27.6) and only the Overworld ever gets a starter chest.
     *
     * @return resolved Nether sky island plan, disabled unless {@link #applyToNether}
     */
    public SkyIslandPlan netherSkyIslandPlan() {
        return applyToNether
            ? new SkyIslandPlan(true, radiusBlocks, shapeAmplitude, islandBiome, surfaceY, thicknessBlocks, chestTier)
            : SkyIslandPlan.disabled();
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
     * side always stays {@code normal} -- {@link SkyIslandPlan} supplies the entire Overworld
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
