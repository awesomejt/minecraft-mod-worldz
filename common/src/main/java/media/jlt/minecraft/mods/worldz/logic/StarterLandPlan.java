package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

/**
 * Persisted settings for reinforcing natural terrain around the starter zone.
 *
 * @param enabled whether terrain reinforcement is active
 * @param transitionWidthBlocks outward natural-terrain blend width
 * @param foundationDepthBlocks repair depth below the natural ocean floor
 * @param profileVersion persisted terrain-shaping revision
 */
public record StarterLandPlan(
    boolean enabled,
    int transitionWidthBlocks,
    int foundationDepthBlocks,
    int profileVersion
) {
    /** Original flat-floor profile used through Worldz 0.1.4. */
    public static final int LEGACY_PROFILE_VERSION = 1;
    /** Relief-preserving profile used by newly created worlds. */
    public static final int RELIEF_PROFILE_VERSION = 2;
    /** Profile revision assigned to new configuration and Customize values. */
    public static final int CURRENT_PROFILE_VERSION = RELIEF_PROFILE_VERSION;

    /** Validates the persisted settings. */
    public StarterLandPlan {
        if (transitionWidthBlocks < 0 || transitionWidthBlocks > WorldzConfig.MAX_STARTER_LAND_TRANSITION_BLOCKS) {
            throw new IllegalArgumentException("Starter-land transition is outside the supported range.");
        }
        if (foundationDepthBlocks < 0 || foundationDepthBlocks > WorldzConfig.MAX_STARTER_LAND_FOUNDATION_DEPTH_BLOCKS) {
            throw new IllegalArgumentException("Starter-land foundation depth is outside the supported range.");
        }
        if (profileVersion < LEGACY_PROFILE_VERSION) {
            throw new IllegalArgumentException("Starter-land profile version must be positive.");
        }
    }

    /**
     * Creates values using the current terrain profile.
     *
     * @param enabled whether terrain reinforcement is active
     * @param transitionWidthBlocks outward natural-terrain blend width
     * @param foundationDepthBlocks repair depth below the natural ocean floor
     */
    public StarterLandPlan(boolean enabled, int transitionWidthBlocks, int foundationDepthBlocks) {
        this(enabled, transitionWidthBlocks, foundationDepthBlocks, CURRENT_PROFILE_VERSION);
    }

    /**
     * Returns a compatibility plan that never changes terrain.
     *
     * @return disabled plan
     */
    public static StarterLandPlan disabled() {
        return new StarterLandPlan(false, 0, 0, CURRENT_PROFILE_VERSION);
    }

    /**
     * Copies sanitized YAML defaults.
     *
     * @param config sanitized configuration
     * @return immutable plan for a newly created world
     */
    public static StarterLandPlan fromConfig(WorldzConfig config) {
        return new StarterLandPlan(
            config.ensureStarterLand,
            config.starterLandTransitionBlocks,
            config.starterLandFoundationDepthBlocks,
            CURRENT_PROFILE_VERSION
        );
    }

    /**
     * Parses editable starter-land numeric fields.
     *
     * @param enabled whether terrain reinforcement is active
     * @param transitionWidthBlocks decimal outward blend width
     * @param foundationDepthBlocks decimal foundation repair depth
     * @return validated immutable plan
     */
    public static StarterLandPlan fromText(
        boolean enabled,
        String transitionWidthBlocks,
        String foundationDepthBlocks
    ) {
        return new StarterLandPlan(
            enabled,
            parseInteger(transitionWidthBlocks, "Starter-land transition"),
            parseInteger(foundationDepthBlocks, "Starter-land foundation depth"),
            CURRENT_PROFILE_VERSION
        );
    }

    private static int parseInteger(String value, String name) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NullPointerException | NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a whole number.", exception);
        }
    }
}
