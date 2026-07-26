package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.EndStartConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@code end_start} typed preset's resolved settings (GOALS 34, DESIGN §32): a guaranteed
 * safe End spawn platform plus a difficulty-tiered starter chest there, Overworld and Nether both
 * otherwise ordinary vanilla terrain. Mirrors {@code NetherStartPlan} (DESIGN §31.5) except for
 * {@code spawnY} -- there is no search, so no search target -- since Jason's decision (DESIGN
 * §32.2) always places a guaranteed platform rather than searching for a natural site first. The
 * capsule shape/lighting fields (GOALS 41's "generalize to end_start... later" follow-up,
 * requested 2026-07-25 after Jason's first real in-game test of config 63 found the original fixed
 * 3x3-footprint/1x1-interior platform "too small" and the chest sitting invisibly underfoot)
 * duplicate {@code NetherStartPlan}'s own fields/bounds rather than sharing them -- matches this
 * project's own precedent (GOALS 41.1: "revisit... true cross-preset sharing later rather than
 * designing that ahead of time") of keeping each preset's plan self-sufficient until a real
 * generalization pass happens.
 *
 * @param enabled whether the End-start shape applies
 * @param chestTier the starter chest's difficulty tier (GOALS 34's own worked example: enough to
 *     make reaching and defeating the Ender Dragon genuinely achievable)
 * @param capsuleSizeBlocks the platform's total exterior footprint width/depth, walls included
 *     (must be odd; interior footprint is this minus 2)
 * @param capsuleHeightBlocks the platform's interior height
 * @param capsuleLightSource which block lights the platform's interior
 * @param capsuleLightSpacingBlocks spacing, in blocks, between embedded/hung light sources
 */
public record EndStartPlan(
    boolean enabled,
    StarterKitTier chestTier,
    int capsuleSizeBlocks,
    int capsuleHeightBlocks,
    LightSource capsuleLightSource,
    int capsuleLightSpacingBlocks
) {
    /** Default platform *exterior* footprint, walls included -- a 5x5 interior, mirroring Nether-start's own now-widened default. */
    public static final int DEFAULT_CAPSULE_SIZE_BLOCKS = 7;
    /** Smallest supported platform footprint -- the original 1x1-interior shape (a 1-thick shell around a single column). */
    public static final int MIN_CAPSULE_SIZE_BLOCKS = 3;
    /** Largest supported platform footprint -- generous but still "a small base", not a hall. */
    public static final int MAX_CAPSULE_SIZE_BLOCKS = 15;
    /** Default platform interior height. */
    public static final int DEFAULT_CAPSULE_HEIGHT_BLOCKS = 3;
    /** Smallest supported platform height -- the original 2-tall shape. */
    public static final int MIN_CAPSULE_HEIGHT_BLOCKS = 2;
    /** Largest supported platform height. */
    public static final int MAX_CAPSULE_HEIGHT_BLOCKS = 8;
    /** Default light spacing. */
    public static final int DEFAULT_CAPSULE_LIGHT_SPACING_BLOCKS = 5;
    /** Smallest supported light spacing. */
    public static final int MIN_CAPSULE_LIGHT_SPACING_BLOCKS = 1;
    /** Largest supported light spacing. */
    public static final int MAX_CAPSULE_LIGHT_SPACING_BLOCKS = 15;

    /** Validates persisted values even while the shape is disabled. */
    public EndStartPlan {
        if (chestTier == null) {
            throw new IllegalArgumentException("Chest tier is required.");
        }
        if (capsuleSizeBlocks < MIN_CAPSULE_SIZE_BLOCKS || capsuleSizeBlocks > MAX_CAPSULE_SIZE_BLOCKS) {
            throw new IllegalArgumentException(
                "Capsule size must be between " + MIN_CAPSULE_SIZE_BLOCKS + " and " + MAX_CAPSULE_SIZE_BLOCKS + "."
            );
        }
        if (capsuleSizeBlocks % 2 == 0) {
            throw new IllegalArgumentException("Capsule size must be odd (a symmetric room needs a true center column).");
        }
        if (capsuleHeightBlocks < MIN_CAPSULE_HEIGHT_BLOCKS || capsuleHeightBlocks > MAX_CAPSULE_HEIGHT_BLOCKS) {
            throw new IllegalArgumentException(
                "Capsule height must be between " + MIN_CAPSULE_HEIGHT_BLOCKS + " and " + MAX_CAPSULE_HEIGHT_BLOCKS + "."
            );
        }
        if (capsuleLightSource == null) {
            throw new IllegalArgumentException("Capsule light source is required.");
        }
        if (capsuleLightSpacingBlocks < MIN_CAPSULE_LIGHT_SPACING_BLOCKS || capsuleLightSpacingBlocks > MAX_CAPSULE_LIGHT_SPACING_BLOCKS) {
            throw new IllegalArgumentException(
                "Capsule light spacing must be between " + MIN_CAPSULE_LIGHT_SPACING_BLOCKS + " and "
                    + MAX_CAPSULE_LIGHT_SPACING_BLOCKS + "."
            );
        }
    }

    /**
     * Returns a plan with the End-start shape switched off.
     *
     * @return disabled plan with safe placeholder values
     */
    public static EndStartPlan disabled() {
        return new EndStartPlan(
            false, StarterKitTier.MEDIUM,
            DEFAULT_CAPSULE_SIZE_BLOCKS, DEFAULT_CAPSULE_HEIGHT_BLOCKS, LightSource.GLOWSTONE, DEFAULT_CAPSULE_LIGHT_SPACING_BLOCKS
        );
    }

    /**
     * Resolves a plan from sanitized YAML configuration.
     *
     * @param config sanitized End-start configuration
     * @return resolved, enabled plan
     */
    public static EndStartPlan fromConfig(EndStartConfig config) {
        return new EndStartPlan(
            true, config.chestTier,
            config.capsule.sizeBlocks, config.capsule.heightBlocks, config.capsule.lightSource, config.capsule.lightSpacingBlocks
        );
    }

    /**
     * Symmetric offsets centered on 0 (always included), then {@code ±spacing}, {@code
     * ±2*spacing}, and so on while still within {@code [-half, half]} -- identical logic to {@code
     * NetherStartPlan.centeredCapsuleOffsets}, duplicated rather than shared for the same
     * self-sufficiency reasoning as this record's other capsule fields. Used by {@code
     * EndStartDeployment} to center the platform's wall/ceiling/floor lights and the lantern grid.
     *
     * @param half how far the offsets may extend from 0 in either direction
     * @param spacing the step between successive offsets
     * @return the symmetric offset list, always including 0
     */
    public static List<Integer> centeredCapsuleOffsets(int half, int spacing) {
        List<Integer> offsets = new ArrayList<>();
        offsets.add(0);
        for (int step = spacing; step <= half; step += spacing) {
            offsets.add(-step);
            offsets.add(step);
        }
        return offsets;
    }
}
