package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.IslandShapeProfile;

import java.util.ArrayList;
import java.util.List;

/**
 * Scattered small floating islands filling the void beyond a sky island's own footprint (GOALS
 * 08, DESIGN §28). Disabled by default -- a plain sky island (GOALS 05-06) needs no scattered-
 * island configuration at all.
 */
public final class FloatingIslandsConfig {
    /** Whether scattered islands generate at all. */
    public boolean enabled = false;
    /** Smallest hash-picked island radius. */
    public int minRadiusBlocks = 12;
    /** Largest hash-picked island radius. */
    public int maxRadiusBlocks = 32;
    /** Coastline perturbation strength, reusing {@link IslandShapeProfile}. */
    public double shapeAmplitude = IslandShapeProfile.DEFAULT_AMPLITUDE;
    /** Grid-cell edge length -- the primary "how far apart" knob (GOALS 08's "a lot of bridging"). */
    public int cellSizeBlocks = 256;
    /** Probability (0..1) that a given cell holds an island, independent of spacing. */
    public double spawnChance = 0.6;
    /** Whether each island hash-picks its own biome from {@link #islandBiomes} instead of sharing the starter island's. */
    public boolean biomeVariety = true;
    /** Candidate biome pool when {@link #biomeVariety} is {@code true}. */
    public List<String> islandBiomes = list(
        "minecraft:plains", "minecraft:forest", "minecraft:desert", "minecraft:taiga", "minecraft:savanna"
    );
    /** Whether a void buffer surrounds the starter island before scattered islands begin (GOALS 07/08). */
    public boolean exclusionZoneEnabled = true;
    /** Radius of the exclusion-zone buffer, when enabled. */
    public int exclusionZoneRadiusBlocks = 256;
    /** Whether each island gets one embedded vanilla ore-vein feature (GOALS 08, DESIGN §28.2). */
    public boolean oreDepositsEnabled = false;
    /**
     * Candidate vanilla ore {@code ConfiguredFeature} ids one island's deposit is hash-picked
     * from -- config-only, read live at placement time, the same "list stays in config, never
     * persisted" precedent {@code StarterKitPlan}'s own essentials/extras pool already
     * established, not threaded through {@code FloatingIslandsPlan}'s codec.
     */
    public List<String> oreFeatureIds = list(
        "minecraft:ore_coal", "minecraft:ore_iron_small", "minecraft:ore_gold_buried",
        "minecraft:ore_redstone", "minecraft:ore_lapis", "minecraft:ore_diamond_small", "minecraft:ore_emerald"
    );

    /** Creates a config populated with defaults. */
    public FloatingIslandsConfig() {
    }

    private static List<String> list(String... values) {
        return new ArrayList<>(List.of(values));
    }
}
