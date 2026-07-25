package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitPlan;

/**
 * Immutable, loader-neutral values selected for one new {@code jlt_worldz:cave} world (GOALS
 * 25-26, DESIGN §30): the small per-type field set, not the full generic Customize screen's. No
 * spawn-strategy field -- like {@code sky_island}/{@code ocean_island}, spawn is always resolved
 * by this preset's own mechanism (the underground cavity search, DESIGN §30.3), never vanilla's
 * ordinary strategies.
 *
 * @param spawnDepthY target Y for the underground spawn-cavity search
 * @param sealedSurface whether a solid roof seals off sky access everywhere
 * @param sealedSurfaceY the roof's Y, meaningful only when {@link #sealedSurface} is set
 * @param cavernEnabled whether the mega-cavern is carved around spawn
 * @param cavernRadiusBlocks the mega-cavern's horizontal half-width
 * @param cavernHeightBlocks the mega-cavern's vertical half-height
 * @param chestEnabled whether a starter chest is placed at spawn
 * @param chestTier the starter chest's difficulty tier
 * @param overworldBorder overworld border selection
 * @param netherBorder Nether border selection
 * @param endBorder End border selection (GOALS 17's Overworld-to-End carry-over)
 * @param netherExterior Nether exterior-terrain selection -- no Overworld exterior field, since
 *     a cave world's Overworld always generates ordinary vanilla terrain (DESIGN §30.1)
 */
public record CaveCustomization(
    int spawnDepthY,
    boolean sealedSurface,
    int sealedSurfaceY,
    boolean cavernEnabled,
    int cavernRadiusBlocks,
    int cavernHeightBlocks,
    boolean chestEnabled,
    StarterKitTier chestTier,
    WorldzCustomization.BorderSettings overworldBorder,
    WorldzCustomization.BorderSettings netherBorder,
    WorldzCustomization.EndBorderSettings endBorder,
    WorldzCustomization.ExteriorSettings netherExterior
) {
    /** Validates and canonicalizes customization values. */
    public CaveCustomization {
        if (overworldBorder == null || netherBorder == null || endBorder == null || netherExterior == null) {
            throw new IllegalArgumentException("Border and exterior settings are required.");
        }
        if (chestTier == null) {
            throw new IllegalArgumentException("Chest tier is required.");
        }
        if (netherExterior.mode() == ExteriorMode.OCEAN) {
            throw new IllegalArgumentException("Ocean exterior is only supported in the Overworld.");
        }
        WorldzCustomization.validateAutomaticBoundary(netherExterior, netherBorder, "Nether");
    }

    /**
     * Creates values from the sanitized YAML configuration.
     *
     * @param config sanitized startup configuration
     * @return an immutable customization snapshot
     */
    public static CaveCustomization fromConfig(WorldzConfig config) {
        return new CaveCustomization(
            config.cave.spawnDepthY,
            config.cave.sealedSurface,
            config.cave.sealedSurfaceY,
            config.cave.cavernEnabled,
            config.cave.cavernRadiusBlocks,
            config.cave.cavernHeightBlocks,
            config.cave.chestEnabled,
            config.cave.chestTier,
            WorldzCustomization.BorderSettings.fromConfig(config.overworldBorder),
            WorldzCustomization.BorderSettings.fromConfig(config.netherBorder),
            WorldzCustomization.EndBorderSettings.fromConfig(config.endBorder),
            WorldzCustomization.ExteriorSettings.fromConfig(config.netherExterior)
        );
    }

    /**
     * Parses client text/toggle fields into validated customization values.
     *
     * @param spawnDepthY decimal target depth
     * @param sealedSurface whether the roof is enabled
     * @param sealedSurfaceY decimal roof Y
     * @param cavernEnabled whether the mega-cavern is enabled
     * @param cavernRadiusBlocks decimal horizontal half-width
     * @param cavernHeightBlocks decimal vertical half-height
     * @param chestEnabled whether the starter chest is enabled
     * @param chestTier chest difficulty tier
     * @param overworldBorder validated overworld border values
     * @param netherBorder validated Nether border values
     * @param endBorder validated End border values
     * @param netherExterior validated Nether exterior values
     * @return canonical immutable customization values
     */
    public static CaveCustomization fromText(
        String spawnDepthY,
        boolean sealedSurface,
        String sealedSurfaceY,
        boolean cavernEnabled,
        String cavernRadiusBlocks,
        String cavernHeightBlocks,
        boolean chestEnabled,
        StarterKitTier chestTier,
        WorldzCustomization.BorderSettings overworldBorder,
        WorldzCustomization.BorderSettings netherBorder,
        WorldzCustomization.EndBorderSettings endBorder,
        WorldzCustomization.ExteriorSettings netherExterior
    ) {
        return new CaveCustomization(
            parseInteger(spawnDepthY, "Spawn depth"),
            sealedSurface,
            parseInteger(sealedSurfaceY, "Sealed surface Y"),
            cavernEnabled,
            parseInteger(cavernRadiusBlocks, "Cavern radius"),
            parseInteger(cavernHeightBlocks, "Cavern height"),
            chestEnabled,
            chestTier,
            overworldBorder,
            netherBorder,
            endBorder,
            netherExterior
        );
    }

    /**
     * Resolves this world's cave plan. The sealed-surface block/thickness (Jason, 2026-07-25)
     * stay config-only for now, matching this project's precedent for new, not-yet-screen-exposed
     * options -- the Customize screen always gets the original fixed defaults (stone, 5 thick).
     *
     * @return resolved, enabled plan
     */
    public CavePlan cavePlan() {
        return new CavePlan(
            true, spawnDepthY, sealedSurface, sealedSurfaceY,
            CavePlan.DEFAULT_SEALED_SURFACE_BLOCK, CavePlan.DEFAULT_SEALED_SURFACE_THICKNESS_BLOCKS,
            cavernEnabled, cavernRadiusBlocks, cavernHeightBlocks, chestEnabled, chestTier
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
     * always stays {@link ExteriorMode#NORMAL} -- a cave world's Overworld generates ordinary
     * vanilla terrain (DESIGN §30.1).
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
}
