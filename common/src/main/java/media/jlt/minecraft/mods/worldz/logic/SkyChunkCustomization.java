package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitPlan;

/**
 * Immutable, loader-neutral values selected for one new {@code jlt_worldz:sky_chunk} world
 * (GOALS 09/37, DESIGN §29): the small per-type field set, not the full generic Customize
 * screen's. Like {@code sky_island} there is no spawn-strategy field (the starter chunk island
 * only ever exists at the origin) and no separate Overworld Exterior field ({@link
 * ChunkIslandPlan} unconditionally supplies the entire Overworld exterior itself, mirroring
 * DESIGN §27.9's precedent).
 *
 * @param spawnChance probability (0..1) that a given grid cell holds an island
 * @param cellSizeChunks grid-cell edge length in chunks
 * @param topOnly whether a selected island keeps only its top {@link #topOnlyDepthBlocks}
 * @param topOnlyDepthBlocks depth kept below the real generated surface when {@link #topOnly}
 * @param exclusionZoneEnabled whether a void buffer precedes scattered islands around the starter
 * @param exclusionZoneRadiusBlocks exclusion-zone radius in blocks
 * @param scatteredTopOnlyChance probability (0..1) an ordinary scattered island independently
 *     resolves top-only instead of full-column (GOALS 37)
 * @param applyToNether whether the same chunk-island mechanism also applies to the Nether
 * @param applyToEnd whether the same chunk-island mechanism also applies to the End (DESIGN
 *     §29.5 -- unlike sky island's Phase 10.5 End-skip, this is a genuinely new capability there)
 * @param overworldBorder optional Overworld size limit, composed on top of the chunk-island grid
 * @param netherBorder Nether border selection
 * @param endBorder End border selection (GOALS 17's Overworld-to-End carry-over)
 * @param netherExterior Nether exterior-terrain selection
 */
public record SkyChunkCustomization(
    double spawnChance,
    int cellSizeChunks,
    boolean topOnly,
    int topOnlyDepthBlocks,
    boolean exclusionZoneEnabled,
    int exclusionZoneRadiusBlocks,
    double scatteredTopOnlyChance,
    boolean applyToNether,
    boolean applyToEnd,
    WorldzCustomization.BorderSettings overworldBorder,
    WorldzCustomization.BorderSettings netherBorder,
    WorldzCustomization.EndBorderSettings endBorder,
    WorldzCustomization.ExteriorSettings netherExterior
) {
    /** Validates and canonicalizes customization values. */
    public SkyChunkCustomization {
        if (spawnChance < 0.0 || spawnChance > 1.0) {
            throw new IllegalArgumentException("Chunk island spawn chance must be between 0 and 1.");
        }
        if (cellSizeChunks < 1 || cellSizeChunks > WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS / 16) {
            throw new IllegalArgumentException("Chunk island cell size must be at least 1 chunk and reasonably bounded.");
        }
        if (topOnlyDepthBlocks < 1) {
            throw new IllegalArgumentException("Chunk island top-only depth must be positive.");
        }
        if (scatteredTopOnlyChance < 0.0 || scatteredTopOnlyChance > 1.0) {
            throw new IllegalArgumentException("Chunk island scattered top-only chance must be between 0 and 1.");
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
    public static SkyChunkCustomization fromConfig(WorldzConfig config) {
        return new SkyChunkCustomization(
            config.chunkIsland.spawnChance,
            config.chunkIsland.cellSizeChunks,
            config.chunkIsland.topOnly,
            config.chunkIsland.topOnlyDepthBlocks,
            config.chunkIsland.exclusionZoneEnabled,
            config.chunkIsland.exclusionZoneRadiusBlocks,
            config.chunkIsland.scatteredTopOnlyChance,
            config.chunkIsland.applyToNether,
            config.chunkIsland.applyToEnd,
            WorldzCustomization.BorderSettings.fromConfig(config.overworldBorder),
            WorldzCustomization.BorderSettings.fromConfig(config.netherBorder),
            WorldzCustomization.EndBorderSettings.fromConfig(config.endBorder),
            WorldzCustomization.ExteriorSettings.fromConfig(config.netherExterior)
        );
    }

    /**
     * Parses client text/toggle fields into validated customization values.
     *
     * @param spawnChance decimal spawn probability
     * @param cellSizeChunks decimal grid-cell edge length in chunks
     * @param topOnly whether a selected island keeps only its top slice
     * @param topOnlyDepthBlocks decimal depth kept below the surface
     * @param exclusionZoneEnabled whether a void buffer precedes scattered islands
     * @param exclusionZoneRadiusBlocks decimal exclusion-zone radius
     * @param scatteredTopOnlyChance decimal probability an ordinary scattered island
     *     independently resolves top-only
     * @param applyToNether whether the same mechanism also applies to the Nether
     * @param applyToEnd whether the same mechanism also applies to the End
     * @param overworldBorder validated Overworld border values
     * @param netherBorder validated Nether border values
     * @param endBorder validated End border values
     * @param netherExterior validated Nether exterior values
     * @return canonical immutable customization values
     */
    public static SkyChunkCustomization fromText(
        String spawnChance,
        String cellSizeChunks,
        boolean topOnly,
        String topOnlyDepthBlocks,
        boolean exclusionZoneEnabled,
        String exclusionZoneRadiusBlocks,
        String scatteredTopOnlyChance,
        boolean applyToNether,
        boolean applyToEnd,
        WorldzCustomization.BorderSettings overworldBorder,
        WorldzCustomization.BorderSettings netherBorder,
        WorldzCustomization.EndBorderSettings endBorder,
        WorldzCustomization.ExteriorSettings netherExterior
    ) {
        return new SkyChunkCustomization(
            parseDouble(spawnChance, "Chunk island spawn chance"),
            parseInteger(cellSizeChunks, "Chunk island cell size"),
            topOnly,
            parseInteger(topOnlyDepthBlocks, "Chunk island top-only depth"),
            exclusionZoneEnabled,
            parseInteger(exclusionZoneRadiusBlocks, "Chunk island exclusion zone radius"),
            parseDouble(scatteredTopOnlyChance, "Chunk island scattered top-only chance"),
            applyToNether,
            applyToEnd,
            overworldBorder,
            netherBorder,
            endBorder,
            netherExterior
        );
    }

    /**
     * Resolves this world's Overworld chunk island plan.
     *
     * @return immutable, always-enabled chunk island plan
     */
    public ChunkIslandPlan chunkIslandPlan() {
        return new ChunkIslandPlan(
            true, spawnChance, cellSizeChunks, topOnly, topOnlyDepthBlocks,
            new IslandPlan.ExclusionZone(exclusionZoneEnabled, exclusionZoneRadiusBlocks), scatteredTopOnlyChance
        );
    }

    /**
     * Resolves the Nether's own chunk island plan (DESIGN §29.5): the same grid shape as the
     * Overworld's, but disabled entirely unless {@link #applyToNether} is set.
     *
     * @return resolved Nether chunk island plan, disabled unless {@link #applyToNether}
     */
    public ChunkIslandPlan netherChunkIslandPlan() {
        return applyToNether ? chunkIslandPlan() : ChunkIslandPlan.disabled();
    }

    /**
     * Resolves the End's own chunk island plan (DESIGN §29.5): the same grid shape as the
     * Overworld's, but disabled entirely unless {@link #applyToEnd} is set.
     *
     * @return resolved End chunk island plan, disabled unless {@link #applyToEnd}
     */
    public ChunkIslandPlan endChunkIslandPlan() {
        return applyToEnd ? chunkIslandPlan() : ChunkIslandPlan.disabled();
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
     * side always stays {@code normal} -- the chunk-island branch in {@code
     * EnvelopedChunkGenerator.effectiveModeAt} supplies the entire Overworld exterior itself,
     * ahead of this shared mechanism ever being consulted.
     *
     * @return immutable resolved exterior plan
     */
    public ExteriorPlan exteriorPlan() {
        return new ExteriorPlan(ExteriorPlan.DimensionEnvelope.normal(), netherExterior.toPlan(netherBorder));
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
