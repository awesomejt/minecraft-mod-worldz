package media.jlt.minecraft.mods.worldz.logic;

/** Pure vertical profile calculations for generated ocean and void exteriors. */
public final class ExteriorTerrainProfile {
    private static final int OCEAN_DEPTH = 16;

    private ExteriorTerrainProfile() {
    }

    /** Layers used by the block-level ocean generator. */
    public enum OceanLayer {
        /** Unbreakable bottom layer. */
        BEDROCK,
        /** Solid seabed. */
        STONE,
        /** Water below sea level. */
        WATER,
        /** Air at and above sea level. */
        AIR
    }

    /**
     * Chooses a stable deep-ocean floor within the dimension's build height.
     *
     * @param minimumY inclusive minimum build Y
     * @param maximumY inclusive maximum build Y
     * @param seaLevel first air block above the water
     * @return inclusive seabed top Y
     */
    public static int oceanFloorY(int minimumY, int maximumY, int seaLevel) {
        return Math.max(minimumY, Math.min(seaLevel - OCEAN_DEPTH - 1, maximumY));
    }

    /**
     * Classifies one block in the generated ocean profile.
     *
     * @param y block Y
     * @param minimumY inclusive minimum build Y
     * @param maximumY inclusive maximum build Y
     * @param seaLevel first air block above the water
     * @return generated layer
     */
    public static OceanLayer oceanLayerAt(int y, int minimumY, int maximumY, int seaLevel) {
        if (y == minimumY) {
            return OceanLayer.BEDROCK;
        }
        if (y <= oceanFloorY(minimumY, maximumY, seaLevel)) {
            return OceanLayer.STONE;
        }
        if (y < seaLevel) {
            return OceanLayer.WATER;
        }
        return OceanLayer.AIR;
    }

    /**
     * Returns the first free height for an exterior column.
     *
     * @param mode void or ocean exterior mode
     * @param oceanFloorHeightmap whether the query ignores water
     * @param minimumY inclusive minimum build Y
     * @param maximumY inclusive maximum build Y
     * @param seaLevel first air block above the water
     * @return first free block Y
     */
    public static int baseHeight(
        ExteriorMode mode,
        boolean oceanFloorHeightmap,
        int minimumY,
        int maximumY,
        int seaLevel
    ) {
        if (mode == ExteriorMode.VOID) {
            return minimumY;
        }
        if (mode != ExteriorMode.OCEAN) {
            throw new IllegalArgumentException("Exterior profile requires void or ocean mode.");
        }
        return oceanFloorHeightmap
            ? oceanFloorY(minimumY, maximumY, seaLevel) + 1
            : Math.max(minimumY, Math.min(seaLevel, maximumY + 1));
    }
}
