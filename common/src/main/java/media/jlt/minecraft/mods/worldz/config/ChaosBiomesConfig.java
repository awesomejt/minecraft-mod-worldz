package media.jlt.minecraft.mods.worldz.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Defaults for the {@code jlt_worldz:chaos_biomes} typed preset (DESIGN
 * §20.11's Phase 4.1 subsection), consulted only when that preset resolves
 * without explicit Customize-screen values.
 */
public final class ChaosBiomesConfig {
    /** Weighted land biome entries ({@code id} or {@code id@weight}) shuffled per region (GOALS 33). */
    public List<String> biomes = new ArrayList<>(List.of(
        "minecraft:desert", "minecraft:jungle", "minecraft:ice_spikes", "minecraft:badlands", "minecraft:taiga"
    ));
    /** Grid-cell edge length in blocks; smaller means more frequent biome changes. */
    public int regionScaleBlocks = 512;
    /** Optional biome forced in a circular zone around spawn; empty means chaos starts at spawn. */
    public String starterBiome = "";
    /** Starter-zone radius, only meaningful when {@link #starterBiome} is set. */
    public int starterRadiusBlocks = 256;
    /** Layout-origin and initial-spawn strategy. */
    public SpawnConfig spawn = new SpawnConfig();
    /** Let vanilla's own river biomes generate where vanilla would place one (GOALS 33's rivers/oceans option). */
    public boolean allowRivers = false;
    /** Let vanilla's own river/ocean-family biomes generate naturally, additive over {@link #allowRivers}. */
    public boolean allowOceans = false;

    /** Creates a config populated with defaults. */
    public ChaosBiomesConfig() {
    }
}
