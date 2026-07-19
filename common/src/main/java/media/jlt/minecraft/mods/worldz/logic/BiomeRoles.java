package media.jlt.minecraft.mods.worldz.logic;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maintained default {@link BiomeRole} mapping for vanilla biomes, pure and
 * independent of the Minecraft biome registry so it stays JUnit-testable.
 * Callers apply an explicit role-override map on top of this default and
 * treat any id absent here (including modded ids) as {@link BiomeRole#LAND}.
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
}
