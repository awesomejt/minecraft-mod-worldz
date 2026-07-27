package media.jlt.minecraft.mods.worldz.logic;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maintained default {@link BiomeRole} mapping for vanilla biomes, pure and
 * independent of the Minecraft biome registry so it stays JUnit-testable.
 * Callers apply an explicit role-override map on top of this default and
 * treat any id absent here (including modded ids) as {@link BiomeRole#LAND}.
 *
 * <p>Also maintains an independent underground/surface classification
 * ({@link #isUnderground(String)}, GOAL 42, DESIGN §37.2) -- a different axis
 * from {@link BiomeRole} (which layout-composition role a biome plays for
 * coordinated terrain shaping), not a fourth {@link BiomeRole} value.
 */
public final class BiomeRoles {
    private static final Set<String> OCEAN_IDS = Set.of(
        "minecraft:ocean",
        "minecraft:deep_ocean",
        "minecraft:warm_ocean",
        "minecraft:lukewarm_ocean",
        "minecraft:deep_lukewarm_ocean",
        "minecraft:cold_ocean",
        "minecraft:deep_cold_ocean",
        "minecraft:frozen_ocean",
        "minecraft:deep_frozen_ocean"
    );

    private static final Set<String> BEACH_IDS = Set.of(
        "minecraft:beach",
        "minecraft:snowy_beach",
        "minecraft:stony_shore"
    );

    /** Vanilla biomes registered at underground/bottom depth bands
     * (verified against the real 26.2 {@code OverworldBiomeBuilder
     * .addUndergroundBiome}/{@code addBottomBiome}, DESIGN §37.0). */
    private static final Set<String> UNDERGROUND_IDS = Set.of(
        "minecraft:dripstone_caves",
        "minecraft:lush_caves",
        "minecraft:sulfur_caves",
        "minecraft:deep_dark"
    );

    private BiomeRoles() {
    }

    /**
     * Returns the maintained default role for a canonical biome id.
     *
     * @param biomeId canonical, namespaced biome id
     * @return the biome's default role; unrecognized ids default to {@link BiomeRole#LAND}
     */
    public static BiomeRole defaultRole(String biomeId) {
        if (OCEAN_IDS.contains(biomeId)) {
            return BiomeRole.OCEAN;
        }
        if (BEACH_IDS.contains(biomeId)) {
            return BiomeRole.BEACH;
        }
        return BiomeRole.LAND;
    }

    /**
     * Resolves a biome's role, preferring an explicit override.
     *
     * @param biomeId canonical, namespaced biome id
     * @param roleOverrides explicit id-to-role overrides
     * @return the overridden role, or the maintained default when absent
     */
    public static BiomeRole resolve(String biomeId, Map<String, BiomeRole> roleOverrides) {
        BiomeRole override = roleOverrides.get(biomeId);
        return override != null ? override : defaultRole(biomeId);
    }

    /**
     * Returns the maintained set of vanilla ocean biome ids (every temperature and depth
     * variant), for callers that need the full pool rather than a per-id role check.
     *
     * @return immutable list of canonical ocean biome ids
     */
    public static List<String> oceanIds() {
        return List.copyOf(OCEAN_IDS);
    }

    /**
     * Reports whether a biome is one of the maintained vanilla underground/bottom-depth
     * biomes (GOAL 42, DESIGN §37.2) -- unrecognized ids (including modded ones) are never
     * underground by default, mirroring {@link #defaultRole(String)}'s own "absent means
     * ordinary" posture. No override parameter: unlike {@link #resolve(String, Map)}, nothing
     * in this codebase populates one for this classification today.
     *
     * @param biomeId canonical, namespaced biome id
     * @return {@code true} for a maintained underground biome id
     */
    public static boolean isUnderground(String biomeId) {
        return UNDERGROUND_IDS.contains(biomeId);
    }

    /**
     * Returns the maintained set of vanilla underground/bottom-depth biome ids, for callers
     * that need the full pool rather than a per-id check.
     *
     * @return immutable list of canonical underground biome ids
     */
    public static List<String> undergroundIds() {
        return List.copyOf(UNDERGROUND_IDS);
    }
}
