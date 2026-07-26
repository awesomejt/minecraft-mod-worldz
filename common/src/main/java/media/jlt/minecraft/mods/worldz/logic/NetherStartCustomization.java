package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitPlan;

/**
 * Immutable, loader-neutral values selected for one new {@code jlt_worldz:nether_start} world
 * (GOALS 27, DESIGN §31): the small per-type field set, not the full generic Customize screen's.
 * No spawn-strategy field -- spawn is always resolved by this preset's own mechanism (the safe-site
 * search, DESIGN §31.4), never vanilla's ordinary strategies. No Overworld-exterior field, same
 * reasoning as {@code cave}: the Overworld always generates ordinary vanilla terrain (DESIGN §31.5).
 *
 * @param spawnY target Y for the safe-site search
 * @param chestTier the starter chest's difficulty tier
 * @param overworldBorder Overworld border selection
 * @param netherBorder Nether border selection
 * @param endBorder End border selection (GOALS 17's Overworld-to-End carry-over)
 * @param netherExterior Nether exterior-terrain selection
 */
public record NetherStartCustomization(
    int spawnY,
    StarterKitTier chestTier,
    WorldzCustomization.BorderSettings overworldBorder,
    WorldzCustomization.BorderSettings netherBorder,
    WorldzCustomization.EndBorderSettings endBorder,
    WorldzCustomization.ExteriorSettings netherExterior
) {
    /** Validates and canonicalizes customization values. */
    public NetherStartCustomization {
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
    public static NetherStartCustomization fromConfig(WorldzConfig config) {
        return new NetherStartCustomization(
            config.netherStart.spawnY,
            config.netherStart.chestTier,
            WorldzCustomization.BorderSettings.fromConfig(config.overworldBorder),
            WorldzCustomization.BorderSettings.fromConfig(config.netherBorder),
            WorldzCustomization.EndBorderSettings.fromConfig(config.endBorder),
            WorldzCustomization.ExteriorSettings.fromConfig(config.netherExterior)
        );
    }

    /**
     * Parses client text/toggle fields into validated customization values.
     *
     * @param spawnY decimal target search depth
     * @param chestTier chest difficulty tier
     * @param overworldBorder validated Overworld border values
     * @param netherBorder validated Nether border values
     * @param endBorder validated End border values
     * @param netherExterior validated Nether exterior values
     * @return canonical immutable customization values
     */
    public static NetherStartCustomization fromText(
        String spawnY,
        StarterKitTier chestTier,
        WorldzCustomization.BorderSettings overworldBorder,
        WorldzCustomization.BorderSettings netherBorder,
        WorldzCustomization.EndBorderSettings endBorder,
        WorldzCustomization.ExteriorSettings netherExterior
    ) {
        return new NetherStartCustomization(
            parseInteger(spawnY, "Spawn Y"), chestTier, overworldBorder, netherBorder, endBorder, netherExterior
        );
    }

    /**
     * Resolves this world's Nether-start plan. The capsule's shape/lighting and {@code
     * forceCapsule} (GOALS 41, DESIGN §31.9) are not yet exposed on this Customize screen -- only
     * the YAML config path ({@link #fromConfig}) can set them today; a world created here always
     * gets the compiled-in capsule defaults. Deferred, not forgotten: DESIGN §31.9 flags full
     * Customize-screen exposure as part of the later cross-preset generalization pass (GOALS 41.1)
     * rather than one-off UI work scoped to Nether-start alone.
     *
     * @return resolved, enabled plan
     */
    public NetherStartPlan netherStartPlan() {
        return new NetherStartPlan(
            true, spawnY, chestTier, false,
            NetherStartPlan.DEFAULT_CAPSULE_SIZE_BLOCKS, NetherStartPlan.DEFAULT_CAPSULE_HEIGHT_BLOCKS,
            LightSource.GLOWSTONE, NetherStartPlan.DEFAULT_CAPSULE_LIGHT_SPACING_BLOCKS
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
     * always stays {@link ExteriorMode#NORMAL} -- a Nether-start world's Overworld generates
     * ordinary vanilla terrain (DESIGN §31.5).
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
