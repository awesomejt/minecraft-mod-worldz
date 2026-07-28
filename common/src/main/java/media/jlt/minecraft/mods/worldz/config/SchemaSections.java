package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.config.schema.DeepFlatSchema;
import media.jlt.minecraft.mods.worldz.config.schema.EndBorderSchema;
import media.jlt.minecraft.mods.worldz.config.schema.FlatSchema;
import media.jlt.minecraft.mods.worldz.config.schema.ForeverNightSchema;
import media.jlt.minecraft.mods.worldz.config.schema.RisingLavaSchema;
import media.jlt.minecraft.mods.worldz.config.schema.SectionCodec;
import media.jlt.minecraft.mods.worldz.config.schema.SpawnSchema;
import media.jlt.minecraft.mods.worldz.config.schema.StackedSchema;
import media.jlt.minecraft.mods.worldz.config.schema.StarterCapsuleSchema;
import media.jlt.minecraft.mods.worldz.config.schema.StarterKitSchema;
import media.jlt.minecraft.mods.worldz.config.schema.StripSchema;
import media.jlt.minecraft.mods.worldz.config.schema.StructureDistanceSchema;

/**
 * Registry of {@link SectionCodec}s used by {@link WorldzConfig}'s four orchestration methods
 * (DESIGN §41.8): the schema-driven implementation for sections converted so far, falling back
 * to {@link LegacySections} for everything else. Production code is indifferent to which one it
 * gets -- as more sections convert in TODO 25.2b-h, only this class changes; the orchestration
 * methods themselves do not.
 *
 * <p>TODO 25.2a converted three sections: {@link #spawn}, {@link #starterKit} and
 * {@link #starterCapsule}. TODO 25.2b converted eight more simple leaf sections: {@link
 * #endBorder}, {@link #strip}, {@link #foreverNight}, {@link #risingLava}, {@link
 * #structureDistance}, {@link #deepFlat}, {@link #stacked}, {@link #flat}. Everything else below
 * still resolves to a {@link LegacySections} adapter -- present here so the calling convention
 * (and the differential test's "current mix") is already uniform, ready for 25.2c onward to flip
 * entries one at a time.
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
        return LegacySections.border(name, objectiveKey, summaryObjectiveName);
    }

    public static SectionCodec<EndBorderConfig> endBorder() {
        return new EndBorderSchema("endBorder");
    }

    public static SectionCodec<ExteriorConfig> exterior(
        String name, java.util.function.Function<WorldzConfig, BorderConfig> border, boolean oceanAllowed
    ) {
        return LegacySections.exterior(name, border, oceanAllowed);
    }

    public static SectionCodec<StripConfig> strip() {
        return new StripSchema("strip");
    }

    public static SectionCodec<LayoutConfig> layout() {
        return LegacySections.layout();
    }

    public static SectionCodec<SingleBiomeConfig> singleBiome() {
        return LegacySections.singleBiome();
    }

    public static SectionCodec<ChaosBiomesConfig> chaosBiomes() {
        return LegacySections.chaosBiomes();
    }

    public static SectionCodec<StripWorldConfig> stripWorld() {
        return LegacySections.stripWorld();
    }

    public static SectionCodec<OceanIslandConfig> oceanIsland() {
        return LegacySections.oceanIsland();
    }

    public static SectionCodec<SkyIslandConfig> skyIsland() {
        return LegacySections.skyIsland();
    }

    public static SectionCodec<ChunkIslandConfig> chunkIsland() {
        return LegacySections.chunkIsland();
    }

    public static SectionCodec<CaveConfig> cave() {
        return LegacySections.cave();
    }

    public static SectionCodec<NetherStartConfig> netherStart() {
        return LegacySections.netherStart();
    }

    public static SectionCodec<EndStartConfig> endStart() {
        return LegacySections.endStart();
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
