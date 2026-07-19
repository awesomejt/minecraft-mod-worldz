package media.jlt.minecraft.mods.worldz.logic;

/** Pure vertical profile calculations for generated ocean and void exteriors. */
public final class ExteriorTerrainProfile {
    /** Fixed seabed depth below sea level used by every non-island ocean exterior. */
    public static final int OCEAN_DEPTH = 16;

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
     * Chooses a stable ocean floor within the dimension's build height, using the fixed
     * default depth every non-island exterior has always used.
     *
     * @param minimumY inclusive minimum build Y
     * @param maximumY inclusive maximum build Y
     * @param seaLevel first air block above the water
     * @return inclusive seabed top Y
     */
    public static int oceanFloorY(int minimumY, int maximumY, int seaLevel) {
        return oceanFloorY(minimumY, maximumY, seaLevel, OCEAN_DEPTH);
    }

    /**
     * Chooses a stable ocean floor within the dimension's build height at an explicit depth
     * (the ocean island's shallow-to-deep gradient, DESIGN §24.5, varies this per column
     * instead of using the fixed {@link #OCEAN_DEPTH}).
     *
     * @param minimumY inclusive minimum build Y
     * @param maximumY inclusive maximum build Y
     * @param seaLevel first air block above the water
     * @param depthBlocks seabed depth below sea level
     * @return inclusive seabed top Y
     */
    public static int oceanFloorY(int minimumY, int maximumY, int seaLevel, int depthBlocks) {
        return Math.max(minimumY, Math.min(seaLevel - depthBlocks - 1, maximumY));
    }

    /**
     * Classifies one block in the generated ocean profile using the fixed default depth.
     *
     * @param y block Y
     * @param minimumY inclusive minimum build Y
     * @param maximumY inclusive maximum build Y
     * @param seaLevel first air block above the water
     * @return generated layer
     */
    public static OceanLayer oceanLayerAt(int y, int minimumY, int maximumY, int seaLevel) {
        return oceanLayerAt(y, minimumY, maximumY, seaLevel, OCEAN_DEPTH);
    }

    /**
     * Classifies one block in the generated ocean profile at an explicit depth.
     *
     * @param y block Y
     * @param minimumY inclusive minimum build Y
     * @param maximumY inclusive maximum build Y
     * @param seaLevel first air block above the water
     * @param depthBlocks seabed depth below sea level
     * @return generated layer
     */
    public static OceanLayer oceanLayerAt(int y, int minimumY, int maximumY, int seaLevel, int depthBlocks) {
        if (y == minimumY) {
            return OceanLayer.BEDROCK;
        }
        if (y <= oceanFloorY(minimumY, maximumY, seaLevel, depthBlocks)) {
            return OceanLayer.STONE;
        }
        if (y < seaLevel) {
            return OceanLayer.WATER;
        }
        return OceanLayer.AIR;
    }

    /**
     * Returns the first free height for an exterior column using the fixed default depth.
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
        return baseHeight(mode, oceanFloorHeightmap, minimumY, maximumY, seaLevel, OCEAN_DEPTH);
    }

    /**
     * Returns the first free height for an exterior column at an explicit ocean depth.
     *
     * @param mode void or ocean exterior mode
     * @param oceanFloorHeightmap whether the query ignores water
     * @param minimumY inclusive minimum build Y
     * @param maximumY inclusive maximum build Y
     * @param seaLevel first air block above the water
     * @param depthBlocks seabed depth below sea level, only meaningful for ocean mode
     * @return first free block Y
     */
    public static int baseHeight(
        ExteriorMode mode,
        boolean oceanFloorHeightmap,
        int minimumY,
        int maximumY,
        int seaLevel,
        int depthBlocks
    ) {
        if (mode == ExteriorMode.VOID) {
            return minimumY;
        }
        if (mode != ExteriorMode.OCEAN) {
            throw new IllegalArgumentException("Exterior profile requires void or ocean mode.");
        }
        return oceanFloorHeightmap
            ? oceanFloorY(minimumY, maximumY, seaLevel, depthBlocks) + 1
            : Math.max(minimumY, Math.min(seaLevel, maximumY + 1));
    }
}
