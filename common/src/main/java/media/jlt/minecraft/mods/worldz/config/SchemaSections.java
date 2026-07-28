package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.config.schema.BorderSchema;
import media.jlt.minecraft.mods.worldz.config.schema.CaveSchema;
import media.jlt.minecraft.mods.worldz.config.schema.ChaosBiomesSchema;
import media.jlt.minecraft.mods.worldz.config.schema.ChunkIslandSchema;
import media.jlt.minecraft.mods.worldz.config.schema.DeepFlatSchema;
import media.jlt.minecraft.mods.worldz.config.schema.EndBorderSchema;
import media.jlt.minecraft.mods.worldz.config.schema.EndStartSchema;
import media.jlt.minecraft.mods.worldz.config.schema.ExteriorSchema;
import media.jlt.minecraft.mods.worldz.config.schema.FlatSchema;
import media.jlt.minecraft.mods.worldz.config.schema.FloatingIslandsSchema;
import media.jlt.minecraft.mods.worldz.config.schema.ForeverNightSchema;
import media.jlt.minecraft.mods.worldz.config.schema.LayoutSchema;
import media.jlt.minecraft.mods.worldz.config.schema.NetherStartSchema;
import media.jlt.minecraft.mods.worldz.config.schema.OceanIslandSchema;
import media.jlt.minecraft.mods.worldz.config.schema.RisingLavaSchema;
import media.jlt.minecraft.mods.worldz.config.schema.SectionCodec;
import media.jlt.minecraft.mods.worldz.config.schema.SkyIslandSchema;
import media.jlt.minecraft.mods.worldz.config.schema.SingleBiomeSchema;
import media.jlt.minecraft.mods.worldz.config.schema.SpawnSchema;
import media.jlt.minecraft.mods.worldz.config.schema.StackedSchema;
import media.jlt.minecraft.mods.worldz.config.schema.StarterCapsuleSchema;
import media.jlt.minecraft.mods.worldz.config.schema.StarterKitSchema;
import media.jlt.minecraft.mods.worldz.config.schema.StripBandsSchema;
import media.jlt.minecraft.mods.worldz.config.schema.StripSchema;
import media.jlt.minecraft.mods.worldz.config.schema.StripWorldSchema;
import media.jlt.minecraft.mods.worldz.config.schema.StructureDistanceSchema;

/**
 * Registry of {@link SectionCodec}s used by {@link WorldzConfig}'s four orchestration methods
 * (DESIGN §41.8): every one of the 25 sections is now schema-driven (TODO 25.2a-h). This class is
 * the sole indirection between the orchestration methods (which never construct a section class
 * directly) and the {@code config/schema} package.
 *
 * <p>TODO 25.2a converted three sections: {@link #spawn}, {@link #starterKit} and
 * {@link #starterCapsule}. TODO 25.2b converted eight more simple leaf sections: {@link
 * #endBorder}, {@link #strip}, {@link #foreverNight}, {@link #risingLava}, {@link
 * #structureDistance}, {@link #deepFlat}, {@link #stacked}, {@link #flat}. TODO 25.2c converted
 * the two hardest shapes: {@link #border} (one POJO field, two YAML key names, DESIGN R1) and
 * {@link #exterior} (the one cross-section rule, DESIGN R2). TODO 25.2d converted the five
 * biome-list sections: {@link #layout}, {@link #singleBiome}, {@link #chaosBiomes}, {@link
 * #stripWorld} and {@link #stripBands}. TODO 25.2e converted the four island sections: {@link
 * #oceanIsland}, {@link #skyIsland} (nesting {@link #floatingIslands}) and {@link #chunkIsland}.
 * TODO 25.2f converted the chest/kit-preset sections: {@link #cave}, {@link #netherStart} and
 * {@link #endStart} -- the shared {@code chestTier} + three-kit shape, plus Nether-start/End-start's
 * own {@link #starterCapsule}-nesting capsule config. TODO 25.2g converted the root itself
 * ({@code WorldzRootSchema}). TODO 25.2h then retired the legacy path this registry used to fall
 * back to ({@code LegacySections}, deleted) once nothing referenced it anymore.
 */
public final class SchemaSections {
    private SchemaSections() {
    }

    public static SectionCodec<SpawnConfig> spawn(String name) {
        return new SpawnSchema(name);
    }

    public static SectionCodec<StarterKitConfig> starterKit(String name) {
        return new StarterKitSchema(name);
    }

    public static SectionCodec<StarterCapsuleConfig> starterCapsule(
        String name, int minSize, int maxSize, int minHeight, int maxHeight, int minSpacing, int maxSpacing
    ) {
        return new StarterCapsuleSchema(name, minSize, maxSize, minHeight, maxHeight, minSpacing, maxSpacing);
    }

    public static SectionCodec<BorderConfig> border(String name, String objectiveKey, String summaryObjectiveName) {
        return new BorderSchema(name, objectiveKey, summaryObjectiveName);
    }

    public static SectionCodec<EndBorderConfig> endBorder() {
        return new EndBorderSchema("endBorder");
    }

    public static SectionCodec<ExteriorConfig> exterior(
        String name, java.util.function.Function<WorldzConfig, BorderConfig> border, boolean oceanAllowed
    ) {
        return new ExteriorSchema(name, border, oceanAllowed);
    }

    public static SectionCodec<StripConfig> strip() {
        return new StripSchema("strip");
    }

    public static SectionCodec<LayoutConfig> layout() {
        return new LayoutSchema("layout");
    }

    public static SectionCodec<SingleBiomeConfig> singleBiome() {
        return new SingleBiomeSchema("singleBiome");
    }

    public static SectionCodec<ChaosBiomesConfig> chaosBiomes() {
        return new ChaosBiomesSchema("chaosBiomes");
    }

    public static SectionCodec<StripWorldConfig> stripWorld() {
        return new StripWorldSchema("stripWorld");
    }

    public static SectionCodec<StripBandsConfig> stripBands() {
        return new StripBandsSchema("stripWorld.bands");
    }

    public static SectionCodec<OceanIslandConfig> oceanIsland() {
        return new OceanIslandSchema("oceanIsland");
    }

    public static SectionCodec<SkyIslandConfig> skyIsland() {
        return new SkyIslandSchema("skyIsland");
    }

    public static SectionCodec<ChunkIslandConfig> chunkIsland() {
        return new ChunkIslandSchema("chunkIsland");
    }

    public static SectionCodec<FloatingIslandsConfig> floatingIslands(String name) {
        return new FloatingIslandsSchema(name);
    }

    public static SectionCodec<CaveConfig> cave() {
        return new CaveSchema("cave");
    }

    public static SectionCodec<NetherStartConfig> netherStart() {
        return new NetherStartSchema("netherStart");
    }

    public static SectionCodec<EndStartConfig> endStart() {
        return new EndStartSchema("endStart");
    }

    public static SectionCodec<FlatConfig> flat() {
        return new FlatSchema("flat");
    }

    public static SectionCodec<DeepFlatConfig> deepFlat() {
        return new DeepFlatSchema("deepFlat");
    }

    public static SectionCodec<StackedConfig> stacked() {
        return new StackedSchema("stacked");
    }

    public static SectionCodec<ForeverNightConfig> foreverNight() {
        return new ForeverNightSchema("foreverNight");
    }

    public static SectionCodec<RisingLavaConfig> risingLava() {
        return new RisingLavaSchema("risingLava");
    }

    public static SectionCodec<StructureDistanceConfig> structureDistance() {
        return new StructureDistanceSchema("structureDistance");
    }
}
