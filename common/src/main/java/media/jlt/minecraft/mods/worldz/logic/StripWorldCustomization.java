package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitPlan;

/**
 * Immutable, loader-neutral values selected for one new {@code jlt_worldz:strip_world}
 * world (GOALS 32, DESIGN §23): the small per-type field set, not the full generic
 * Customize screen's. The corridor's length uses the ordinary border/exterior machinery
 * (below) unmodified; only the width is strip-specific.
 *
 * @param widthRadiusBlocks half-width from the origin; the corridor is twice this wide
 * @param widthMode terrain generated beyond the width -- void or ocean, never normal
 * @param applyToNether whether the same corridor width also applies to the Nether
 * @param spawnStrategy layout-origin and initial-spawn strategy
 * @param overworldBorder overworld border selection -- the corridor's length (GOALS 17-20)
 * @param netherBorder Nether border selection
 * @param endBorder End border selection (GOALS 17's Overworld-to-End carry-over)
 * @param overworldExterior Overworld exterior-terrain selection beyond the length
 * @param netherExterior Nether exterior-terrain selection beyond the length
 */
public record StripWorldCustomization(
    int widthRadiusBlocks,
    ExteriorMode widthMode,
    boolean applyToNether,
    SpawnStrategy spawnStrategy,
    WorldzCustomization.BorderSettings overworldBorder,
    WorldzCustomization.BorderSettings netherBorder,
    WorldzCustomization.EndBorderSettings endBorder,
    WorldzCustomization.ExteriorSettings overworldExterior,
    WorldzCustomization.ExteriorSettings netherExterior
) {
    /** Validates and canonicalizes customization values. */
    public StripWorldCustomization {
        if (widthRadiusBlocks <= 0) {
            throw new IllegalArgumentException("Strip width radius must be positive.");
        }
        if (widthMode == null || widthMode == ExteriorMode.NORMAL) {
            throw new IllegalArgumentException("Strip width mode must be void or ocean.");
        }
        if (spawnStrategy == null || overworldBorder == null || netherBorder == null || endBorder == null
            || overworldExterior == null || netherExterior == null) {
            throw new IllegalArgumentException("Spawn strategy, border, and exterior settings are required.");
        }
        if (netherExterior.mode() == ExteriorMode.OCEAN) {
            throw new IllegalArgumentException("Ocean exterior is only supported in the Overworld.");
        }
        WorldzCustomization.validateAutomaticBoundary(overworldExterior, overworldBorder, "Overworld");
        WorldzCustomization.validateAutomaticBoundary(netherExterior, netherBorder, "Nether");
    }

    /**
     * Creates values from the sanitized YAML configuration.
     *
     * @param config sanitized startup configuration
     * @return an immutable customization snapshot
     */
    public static StripWorldCustomization fromConfig(WorldzConfig config) {
        return new StripWorldCustomization(
            config.strip.widthRadiusBlocks,
            config.strip.widthMode,
            config.strip.applyToNether,
            config.stripWorld.spawn.strategy,
            WorldzCustomization.BorderSettings.fromConfig(config.overworldBorder),
            WorldzCustomization.BorderSettings.fromConfig(config.netherBorder),
            WorldzCustomization.EndBorderSettings.fromConfig(config.endBorder),
            WorldzCustomization.ExteriorSettings.fromConfig(config.overworldExterior),
            WorldzCustomization.ExteriorSettings.fromConfig(config.netherExterior)
        );
    }

    /**
     * Parses client text/toggle fields into validated customization values.
     *
     * @param widthRadiusBlocks decimal half-width
     * @param widthMode void or ocean
     * @param applyToNether whether the same corridor width also applies to the Nether
     * @param spawnStrategy layout-origin and spawn strategy
     * @param overworldBorder validated overworld border values
     * @param netherBorder validated Nether border values
     * @param endBorder validated End border values
     * @param overworldExterior validated Overworld exterior values
     * @param netherExterior validated Nether exterior values
     * @return canonical immutable customization values
     */
    public static StripWorldCustomization fromText(
        String widthRadiusBlocks,
        String widthMode,
        boolean applyToNether,
        SpawnStrategy spawnStrategy,
        WorldzCustomization.BorderSettings overworldBorder,
        WorldzCustomization.BorderSettings netherBorder,
        WorldzCustomization.EndBorderSettings endBorder,
        WorldzCustomization.ExteriorSettings overworldExterior,
        WorldzCustomization.ExteriorSettings netherExterior
    ) {
        return new StripWorldCustomization(
            parseInteger(widthRadiusBlocks, "Strip width radius"),
            ExteriorMode.parse(widthMode),
            applyToNether,
            spawnStrategy,
            overworldBorder,
            netherBorder,
            endBorder,
            overworldExterior,
            netherExterior
        );
    }

    /**
     * Resolves this world's strip plan for one dimension.
     *
     * @param overworld whether to resolve the Overworld rather than the Nether
     * @return resolved plan, disabled for the Nether unless {@link #applyToNether} is set
     */
    public StripPlan stripPlan(boolean overworld) {
        return overworld || applyToNether ? new StripPlan(true, widthRadiusBlocks, widthMode) : StripPlan.disabled();
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
     * Converts both exterior selections to resolved, persisted envelopes.
     *
     * @return immutable resolved exterior plan
     */
    public ExteriorPlan exteriorPlan() {
        return new ExteriorPlan(overworldExterior.toPlan(overworldBorder), netherExterior.toPlan(netherBorder));
    }

    private static int parseInteger(String value, String name) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NullPointerException | NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a whole number.", exception);
        }
    }
}
