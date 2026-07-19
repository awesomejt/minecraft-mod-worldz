package media.jlt.minecraft.mods.worldz.logic;

/**
 * Pure geometry for a natural-looking (non-circular) ocean island (GOALS 01, DESIGN §24).
 * A small number of sine harmonics with seed-hashed phases perturb the circular radius by
 * angle, giving a deterministic "lumpy circle" coastline instead of a disc. Every other piece
 * of island logic (biome classification, terrain height, the shore ring, the ocean gradient)
 * is defined in terms of {@link #distanceFromShore}, so none of them can ever disagree about
 * where the coastline actually is.
 */
public final class IslandShapeProfile {
    /** Number of sine harmonics summed for the coastline perturbation. */
    private static final int HARMONICS = 4;
    /** Sum of {@code 1/k} for {@code k} in {@code 1..HARMONICS}, used to normalize the total. */
    private static final double HARMONIC_WEIGHT_SUM = harmonicWeightSum();
    /** Fixture-verified default perturbation strength; see DESIGN §24.3. */
    public static final double DEFAULT_AMPLITUDE = 0.3;
    /** Hard clamp: no combination of harmonics can shrink the radius below 40% of base. */
    public static final double MAX_AMPLITUDE = 0.6;
    /** Required first-free block height relative to sea level, matching {@link StarterLandProfile}. */
    public static final int BLOCKS_ABOVE_SEA_LEVEL = StarterLandProfile.BLOCKS_ABOVE_SEA_LEVEL;
    /** Coordinate scale applied to seeded vanilla surface noise for broad relief. */
    public static final double RELIEF_NOISE_SCALE = StarterLandProfile.RELIEF_NOISE_SCALE;
    private static final int NATURAL_RELIEF_REFERENCE_DEPTH = 32;
    private static final int NATURAL_RELIEF_DIVISOR = 4;
    private static final int MAX_NATURAL_RELIEF_BLOCKS = 8;
    private static final int MAX_NOISE_RELIEF_BLOCKS = 8;

    private IslandShapeProfile() {
    }

    /**
     * Computes the perturbed island radius in one direction.
     *
     * @param baseRadiusBlocks configured (unperturbed) island radius
     * @param amplitude perturbation strength, clamped to {@code 0..MAX_AMPLITUDE}
     * @param angleRadians direction from the origin
     * @param seed sampling seed (the world's real seed, resolved live -- see DESIGN §24.2)
     * @return effective radius in that direction, always positive
     */
    public static double radiusAt(double baseRadiusBlocks, double amplitude, double angleRadians, long seed) {
        double clampedAmplitude = Math.clamp(amplitude, 0.0, MAX_AMPLITUDE);
        double perturbation = 0.0;
        for (int harmonic = 1; harmonic <= HARMONICS; harmonic++) {
            double weight = 1.0 / (harmonic * HARMONIC_WEIGHT_SUM);
            double phase = hash01(seed, harmonic) * 2.0 * Math.PI;
            perturbation += weight * Math.sin(harmonic * angleRadians + phase);
        }
        return Math.max(0.0, baseRadiusBlocks * (1.0 + clampedAmplitude * perturbation));
    }

    /**
     * Computes the signed distance from the perturbed coastline.
     *
     * @param x block X relative to the origin
     * @param z block Z relative to the origin
     * @param baseRadiusBlocks configured (unperturbed) island radius
     * @param amplitude perturbation strength
     * @param seed sampling seed
     * @return negative inside the island, positive outside, zero at the coastline
     */
    public static double distanceFromShore(int x, int z, double baseRadiusBlocks, double amplitude, long seed) {
        double distance = Math.hypot(x, z);
        double angle = Math.atan2(z, x);
        return distance - radiusAt(baseRadiusBlocks, amplitude, angle, seed);
    }

    /**
     * Returns a smooth terrain-raising strength: one throughout the island interior, smoothly
     * tapering to zero across the shore ring, matching {@code StarterLandProfile.strengthAt}'s
     * exact smoothstep shape.
     *
     * @param distanceFromShore signed shore distance (see {@link #distanceFromShore})
     * @param shoreWidthBlocks shore-ring width; also the height-taper width
     * @return value in the inclusive range zero through one
     */
    public static double strengthAt(double distanceFromShore, int shoreWidthBlocks) {
        if (distanceFromShore <= 0.0) {
            return 1.0;
        }
        if (shoreWidthBlocks <= 0 || distanceFromShore >= shoreWidthBlocks) {
            return 0.0;
        }
        double progress = distanceFromShore / shoreWidthBlocks;
        double smoothstep = progress * progress * (3.0 - 2.0 * progress);
        return 1.0 - smoothstep;
    }

    /**
     * Calculates the first-free height after applying the island's guaranteed-land raise,
     * reusing {@code StarterLandProfile}'s relief-blend approach (natural ocean-floor relief
     * plus seeded broad noise) so the island's surface reads consistently with every other
     * Worldz-guaranteed land, just shaped by shore distance instead of a raw circular one.
     *
     * @param distanceFromShore signed shore distance (see {@link #distanceFromShore})
     * @param shoreWidthBlocks shore-ring width; also the height-taper width
     * @param naturalHeight delegate generator's first-free height
     * @param seaLevel generator sea level
     * @param reliefNoise seeded broad noise sample, normally in the range -1 through 1
     * @return natural or smoothly raised first-free height
     */
    public static int targetHeight(
        double distanceFromShore,
        int shoreWidthBlocks,
        int naturalHeight,
        int seaLevel,
        double reliefNoise
    ) {
        int shapedMinimum = reliefMinimumHeight(naturalHeight, seaLevel, reliefNoise);
        if (naturalHeight >= shapedMinimum) {
            return naturalHeight;
        }
        double strength = strengthAt(distanceFromShore, shoreWidthBlocks);
        double lift = (shapedMinimum - naturalHeight) * strength;
        return naturalHeight + (int) Math.round(lift);
    }

    private static int reliefMinimumHeight(int naturalHeight, int seaLevel, double reliefNoise) {
        int referenceHeight = seaLevel - NATURAL_RELIEF_REFERENCE_DEPTH;
        int naturalRelief = Math.clamp(
            Math.floorDiv(naturalHeight - referenceHeight, NATURAL_RELIEF_DIVISOR),
            0,
            MAX_NATURAL_RELIEF_BLOCKS
        );
        double normalizedNoise = Math.clamp((reliefNoise + 1.0) * 0.5, 0.0, 1.0);
        int noiseRelief = (int) Math.round(normalizedNoise * MAX_NOISE_RELIEF_BLOCKS);
        return seaLevel + BLOCKS_ABOVE_SEA_LEVEL + naturalRelief + noiseRelief;
    }

    private static double harmonicWeightSum() {
        double sum = 0.0;
        for (int harmonic = 1; harmonic <= HARMONICS; harmonic++) {
            sum += 1.0 / harmonic;
        }
        return sum;
    }

    private static double hash01(long seed, long salt) {
        long h = splitmix64(seed);
        h = splitmix64(h ^ salt);
        return (h >>> 11) * 0x1.0p-53;
    }

    private static long splitmix64(long x) {
        x += 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }
}
