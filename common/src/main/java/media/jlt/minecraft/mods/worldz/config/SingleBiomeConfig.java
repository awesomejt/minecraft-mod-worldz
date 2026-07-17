package media.jlt.minecraft.mods.worldz.config;

/**
 * Defaults for the {@code jlt_worldz:single_biome} typed preset (DESIGN
 * §20.2's Phase 2.1 subsection), consulted only when that preset resolves
 * without explicit Customize-screen values.
 */
public final class SingleBiomeConfig {
    /** The one biome that fills the generated world (GOALS 10). */
    public String landBiome = "minecraft:plains";
    /** Optional different biome forced around spawn; empty means same as {@link #landBiome} (GOALS 11). */
    public String starterBiome = "";
    /** Starter-zone radius, only meaningful when {@link #starterBiome} is set. */
    public int starterRadiusBlocks = 256;
    /** Layout-origin and initial-spawn strategy (GOALS 12 uses {@code preferred_natural_biome}). */
    public SpawnConfig spawn = new SpawnConfig();

    /** Creates a config populated with defaults. */
    public SingleBiomeConfig() {
    }
}
