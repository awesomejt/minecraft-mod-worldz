package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.StructureDistanceConfig;

import java.util.List;

/**
 * The "structures far from spawn" module's resolved settings (GOAL 24, DESIGN §36): a shared
 * runtime rule, not a typed preset, composable with any world type. Every vanilla structure set
 * is held back from generating until a chunk is at least {@link #minDistanceBlocks} (Chebyshev,
 * matching {@link IslandPlan.ExclusionZone}/border convention) from the world's spawn origin,
 * except ids listed in {@link #exemptStructureSets}.
 *
 * @param enabled whether the restriction applies at all
 * @param minDistanceBlocks minimum distance from spawn before a restricted structure set may
 *     generate; zero disables the restriction for every set (kept distinct from {@code enabled}
 *     so a persisted world can distinguish "off" from "off via a zero distance")
 * @param exemptStructureSets structure set ids (e.g. {@code minecraft:strongholds}) always allowed
 *     at their normal vanilla distance, regardless of {@link #minDistanceBlocks}
 */
public record StructureDistancePlan(boolean enabled, int minDistanceBlocks, List<String> exemptStructureSets) {
    /** Validates persisted values even while the module is disabled. */
    public StructureDistancePlan {
        if (minDistanceBlocks < 0) {
            throw new IllegalArgumentException("Structure minimum distance must not be negative.");
        }
        exemptStructureSets = List.copyOf(exemptStructureSets);
    }

    /**
     * Returns a plan with the module switched off.
     *
     * @return disabled plan with safe placeholder values
     */
    public static StructureDistancePlan disabled() {
        return new StructureDistancePlan(false, 2000, List.of());
    }

    /**
     * Resolves a plan from sanitized YAML configuration.
     *
     * @param config sanitized structure-distance configuration
     * @return resolved plan, disabled unless {@link StructureDistanceConfig#enabled} is set
     */
    public static StructureDistancePlan fromConfig(StructureDistanceConfig config) {
        if (!config.enabled) {
            return disabled();
        }
        return new StructureDistancePlan(true, config.minDistanceBlocks, config.exemptStructureSets);
    }

    /**
     * Whether a structure set must be suppressed at a given chunk because it is both restricted
     * (enabled, non-zero distance, not exempted) and too close to spawn.
     *
     * @param structureSetId the structure set's registry id (e.g. {@code minecraft:villages})
     * @param chebyshevDistanceBlocks the chunk's Chebyshev distance from the spawn origin
     * @return {@code true} if the structure set must not generate at this distance
     */
    public boolean isRestricted(String structureSetId, int chebyshevDistanceBlocks) {
        return enabled
            && minDistanceBlocks > 0
            && !exemptStructureSets.contains(structureSetId)
            && chebyshevDistanceBlocks < minDistanceBlocks;
    }
}
