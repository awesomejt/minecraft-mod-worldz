package media.jlt.minecraft.mods.worldz.logic;

/** Pure coordinate math for the circular starter-biome zone. */
public final class StarterZone {
    private static final int BLOCKS_PER_QUART = 4;

    private StarterZone() {
    }

    /**
     * Converts a block radius using the same four-block quart scale as worldgen.
     *
     * @param radiusBlocks radius in blocks
     * @return radius in quart coordinates
     */
    public static int quartRadius(int radiusBlocks) {
        return Math.floorDiv(radiusBlocks, BLOCKS_PER_QUART);
    }

    /**
     * Tests a quart-coordinate position against the inclusive circular boundary.
     *
     * @param quartX quart X coordinate relative to the origin
     * @param quartZ quart Z coordinate relative to the origin
     * @param radiusBlocks configured radius in blocks
     * @return whether the position is in the starter zone
     */
    public static boolean containsQuart(int quartX, int quartZ, int radiusBlocks) {
        if (radiusBlocks < 0) {
            return false;
        }
        long radius = quartRadius(radiusBlocks);
        long absoluteX = Math.abs((long) quartX);
        long absoluteZ = Math.abs((long) quartZ);
        if (absoluteX > radius || absoluteZ > radius) {
            return false;
        }
        return absoluteX * absoluteX + absoluteZ * absoluteZ <= radius * radius;
    }
}
