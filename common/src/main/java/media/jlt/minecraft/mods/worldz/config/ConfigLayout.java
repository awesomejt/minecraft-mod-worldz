package media.jlt.minecraft.mods.worldz.config;

import java.util.List;
import java.util.Optional;

/**
 * The 27-root-key to 16-file mapping (DESIGN §43.2/§43.4.1, TODO 25.7a; {@code kits} added at
 * TODO 25.8b, DESIGN §44.4.4): which {@code config/jlt_worldz/*.yaml} file each {@link
 * media.jlt.minecraft.mods.worldz.config.schema.WorldzRootSchema#declare()} root key is read from.
 *
 * <p>Declarative and hand-written, <strong>not</strong> derived from {@code Setting.applies()}:
 * the two are independently maintained and cross-checked against each other by {@code
 * ConfigLayoutTest} instead, since {@code Applicability} alone (live/world-default/preset-and-which-
 * presets) cannot express a filename. {@link #FILES}'s owned key sets exactly partition all 27
 * root keys -- no duplicates, no gaps -- which is what makes merge order irrelevant once 25.7b
 * wires this into the load path (§43.4.2).
 *
 * <p>Not wired into {@link WorldzConfig#load} yet -- that is TODO 25.7b. This class only builds and
 * is tested against the mapping itself.
 */
public final class ConfigLayout {
    /**
     * All 16 files (DESIGN §43.2's table, plus {@code kits.yaml} at TODO 25.8b/DESIGN §44.4.4).
     * Three are wrapped ({@code runtime.yaml}, {@code world-defaults.yaml}, {@code
     * world-types/worldz.yaml}) plus {@code world-types/strip-world.yaml} until TODO 25.9 folds
     * {@code strip}'s keys into {@code stripWorld} (D10, §43.3); {@code kits.yaml} and the eleven
     * {@code world-types/*.yaml} files are unwrapped, one root key each. Every {@code
     * world-types/*.yaml} filename matches its {@code world_preset/*.json} id, except {@code
     * sky-chunk.yaml}, which owns {@code chunkIsland} -- the one deliberate filename-vs-key
     * mismatch (§43.2 row 17).
     */
    public static final List<ConfigFile> FILES = List.of(
        new ConfigFile("runtime.yaml", List.of("foreverNight", "risingLava", "structureDistance"), false),
        new ConfigFile(
            "world-defaults.yaml",
            List.of("overworldBorder", "netherBorder", "endBorder", "overworldExterior", "netherExterior"),
            false
        ),
        // Inserted here, after world-defaults.yaml, to match CONFIG-RESTRUCTURE.md §3's own file
        // tree (DESIGN §44.4.4).
        new ConfigFile("kits.yaml", List.of("kits"), true),
        new ConfigFile(
            "world-types/worldz.yaml",
            List.of("allowedBiomes", "starter", "naturalBiomes", "layout", "spawn"),
            false
        ),
        new ConfigFile("world-types/strip-world.yaml", List.of("strip", "stripWorld"), false), // 25.9 unwraps
        new ConfigFile("world-types/single-biome.yaml", List.of("singleBiome"), true),
        new ConfigFile("world-types/chaos-biomes.yaml", List.of("chaosBiomes"), true),
        new ConfigFile("world-types/ocean-island.yaml", List.of("oceanIsland"), true),
        new ConfigFile("world-types/sky-island.yaml", List.of("skyIsland"), true),
        new ConfigFile("world-types/sky-chunk.yaml", List.of("chunkIsland"), true),
        new ConfigFile("world-types/cave.yaml", List.of("cave"), true),
        new ConfigFile("world-types/nether-start.yaml", List.of("netherStart"), true),
        new ConfigFile("world-types/end-start.yaml", List.of("endStart"), true),
        new ConfigFile("world-types/flat.yaml", List.of("flat"), true),
        new ConfigFile("world-types/deep-flat.yaml", List.of("deepFlat"), true),
        new ConfigFile("world-types/stacked.yaml", List.of("stacked"), true)
    );

    /**
     * Optional single-file bundle (TODO 25.7b): when {@code config/jlt_worldz/all.yaml} exists, it
     * wins wholesale over every file in {@link #FILES} (Jason, 2026-07-28: keep the bundle, §43.10).
     * Declared here rather than in the loader so both share one name.
     */
    public static final String BUNDLE_FILE = "all.yaml";

    private ConfigLayout() {
    }

    /**
     * Reverse lookup: which file owns a given root key (used by the 25.7c misfile WARN).
     *
     * @param rootKey a {@code WorldzRootSchema.declare()} root-level key
     * @return the owning {@link ConfigFile}, or empty if {@code rootKey} is not one of the 27
     *     known root keys
     */
    public static Optional<ConfigFile> owning(String rootKey) {
        return FILES.stream().filter(file -> file.rootKeys().contains(rootKey)).findFirst();
    }
}
