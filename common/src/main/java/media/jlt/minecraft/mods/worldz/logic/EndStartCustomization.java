package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitPlan;

/**
 * Immutable, loader-neutral values selected for one new {@code jlt_worldz:end_start} world
 * (GOALS 34, DESIGN §32): the small per-type field set, not the full generic Customize screen's.
 * No spawn-strategy field -- spawn is always resolved by this preset's own mechanism (the
 * guaranteed platform, DESIGN §32.4), never vanilla's ordinary strategies. No exterior field at
 * all -- unlike {@code nether_start} (whose Nether stays ordinary vanilla but the preset itself
 * targets the Nether), {@code end_start} leaves the Overworld, the Nether, *and* the End all
 * ordinary vanilla terrain (DESIGN §32.3), so there is nothing for an exterior selection to
 * apply to.
 *
 * @param chestTier the starter chest's difficulty tier
 * @param overworldBorder Overworld border selection
 * @param netherBorder Nether border selection
 * @param endBorder End border selection (GOALS 17's Overworld-to-End carry-over)
 */
public record EndStartCustomization(
    StarterKitTier chestTier,
    WorldzCustomization.BorderSettings overworldBorder,
    WorldzCustomization.BorderSettings netherBorder,
    WorldzCustomization.EndBorderSettings endBorder
) {
    /** Validates and canonicalizes customization values. */
    public EndStartCustomization {
        if (overworldBorder == null || netherBorder == null || endBorder == null) {
            throw new IllegalArgumentException("Border settings are required.");
        }
        if (chestTier == null) {
            throw new IllegalArgumentException("Chest tier is required.");
        }
    }

    /**
     * Creates values from the sanitized YAML configuration.
     *
     * @param config sanitized startup configuration
     * @return an immutable customization snapshot
     */
    public static EndStartCustomization fromConfig(WorldzConfig config) {
        return new EndStartCustomization(
            config.endStart.chestTier,
            WorldzCustomization.BorderSettings.fromConfig(config.overworldBorder),
            WorldzCustomization.BorderSettings.fromConfig(config.netherBorder),
            WorldzCustomization.EndBorderSettings.fromConfig(config.endBorder)
        );
    }

    /**
     * Resolves this world's End-start plan.
     *
     * @return resolved, enabled plan
     */
    public EndStartPlan endStartPlan() {
        return new EndStartPlan(true, chestTier);
    }

    /**
     * Converts all three border selections to the world-persisted plan.
     *
     * @return immutable codec-backed world-limit plan
     */
    public WorldLimitPlan worldLimitPlan() {
        return new WorldLimitPlan(overworldBorder.toPlan(), netherBorder.toPlan(), endBorder.toPlan());
    }
}
