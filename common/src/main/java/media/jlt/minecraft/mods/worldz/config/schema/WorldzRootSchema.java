package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.BorderConfig;
import media.jlt.minecraft.mods.worldz.config.CaveConfig;
import media.jlt.minecraft.mods.worldz.config.ChaosBiomesConfig;
import media.jlt.minecraft.mods.worldz.config.ChunkIslandConfig;
import media.jlt.minecraft.mods.worldz.config.DeepFlatConfig;
import media.jlt.minecraft.mods.worldz.config.EndBorderConfig;
import media.jlt.minecraft.mods.worldz.config.EndStartConfig;
import media.jlt.minecraft.mods.worldz.config.ExteriorConfig;
import media.jlt.minecraft.mods.worldz.config.FlatConfig;
import media.jlt.minecraft.mods.worldz.config.ForeverNightConfig;
import media.jlt.minecraft.mods.worldz.config.LayoutConfig;
import media.jlt.minecraft.mods.worldz.config.NetherStartConfig;
import media.jlt.minecraft.mods.worldz.config.OceanIslandConfig;
import media.jlt.minecraft.mods.worldz.config.RisingLavaConfig;
import media.jlt.minecraft.mods.worldz.config.SchemaSections;
import media.jlt.minecraft.mods.worldz.config.SingleBiomeConfig;
import media.jlt.minecraft.mods.worldz.config.SkyIslandConfig;
import media.jlt.minecraft.mods.worldz.config.SpawnConfig;
import media.jlt.minecraft.mods.worldz.config.StackedConfig;
import media.jlt.minecraft.mods.worldz.config.StarterKitConfig;
import media.jlt.minecraft.mods.worldz.config.StripWorldConfig;
import media.jlt.minecraft.mods.worldz.config.StructureDistanceConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

import java.util.List;
import java.util.Map;

/**
 * Root schema for {@link WorldzConfig} itself (DESIGN §41.7; TODO 25.2g). Declares the {@code
 * kits} library first of all (TODO 25.8b, DESIGN §44.4.3 -- load-bearing declaration order), one
 * top-level scalar ({@code allowedBiomes}), the {@code starter}/{@code naturalBiomes} groups
 * (TODO 25.6b), then a {@code Setting.section(...)} entry for each of the 22 current sections, in
 * today's exact {@code toYaml()} emit order (DESIGN §41.1's ordering invariant) -- the same order
 * {@code WorldzConfig.parse} checked {@code containsKey} in and {@code WorldzConfig.sanitize}
 * processed fields in (re-verified against the current code, not DESIGN's line numbers, which
 * predate 25.2a-f). {@code naturalBiomes} moved up next to {@code starter} at 25.6b (DESIGN §42.3),
 * out of its pre-25.6 position after {@code structureDistance}.
 *
 * <p>Every nested section comes from {@link SchemaSections}, the same registry {@code
 * WorldzConfig}'s four orchestration methods called directly before this refactor -- so this
 * wraps that registry rather than duplicating it, and production behavior is unchanged rather than
 * merely reproduced. Each entry is typed {@link SectionCodec} (not {@link SchemaSection}), since
 * that is what the registry's factory methods return; {@code Setting.section}, {@code
 * Codecs.section} and {@code Rule.Nested} were widened from {@code SchemaSection} to {@code
 * SectionCodec} to accept them directly, with no cast.
 *
 * <p>This section's own {@link #path()} is {@code ""}: a root-level key has no dotted prefix.
 * {@link SchemaSection} was adjusted (TODO 25.2g) to omit the leading {@code "."} that implies, so
 * warning/exception text for the one remaining scalar (e.g. {@code "Ignoring invalid allowed
 * biome or tag '{}'."}) matches today's exactly; every other (non-root) section already had a
 * non-empty path, so that change is behavior-identical for them.
 *
 * <p>{@code summary()} is overridden rather than derived: the {@code starter=}/{@code
 * naturalBiomes=} segments delegate to the {@link #starter}/{@link #naturalBiomes} group instances'
 * own summaries (TODO 25.6b), and {@code starter}'s nested {@code land=} sub-segment still folds
 * three separate fields ({@code ensureStarterLand}, {@code starterLandTransitionBlocks}, {@code
 * starterLandFoundationDepthBlocks}) into one segment via {@link StarterLandSchema}'s own override,
 * which no single {@link Setting}'s renderer can express -- the same "not mechanically derivable"
 * shape as {@code BorderSchema}, {@code ExteriorSchema} and {@code SkyIslandSchema} (DESIGN §41.6's
 * table).
 */
public final class WorldzRootSchema extends SchemaSection<WorldzConfig> {
    /** The {@code kits} library's shared per-entry schema (DESIGN §44.4.2), bound by both {@link
     * Codecs#sectionMap} and {@link Rule.NestedMap} below -- one instance, matching {@code
     * KitReferenceTest}'s own convention (TODO 25.8a) of a single {@code StarterKitSchema("kits")}. */
    private final StarterKitSchema kitEntry = new StarterKitSchema("kits");
    private final StarterLandSchema starterLand = new StarterLandSchema("starter.land");
    private final StarterSchema<WorldzConfig> starter = new StarterSchema<>(
        "starter", WorldzConfig::new,
        new Accessor<>(c -> c.starterBiome, (c, v) -> c.starterBiome = v),
        new Accessor<>(c -> c.starterRadiusBlocks, (c, v) -> c.starterRadiusBlocks = v),
        starterLand
    );
    private final NaturalBiomesSchema<WorldzConfig> naturalBiomes = new NaturalBiomesSchema<>(
        "naturalBiomes", WorldzConfig::new,
        new Accessor<>(c -> c.allowRivers, (c, v) -> c.allowRivers = v),
        new Accessor<>(c -> c.allowOceans, (c, v) -> c.allowOceans = v)
    );
    private final SectionCodec<BorderConfig> overworldBorder =
        SchemaSections.border("overworldBorder", "ensureEndPortal", "endPortal");
    private final SectionCodec<BorderConfig> netherBorder =
        SchemaSections.border("netherBorder", "ensureBlazeAccess", "blazeAccess");
    private final SectionCodec<EndBorderConfig> endBorder = SchemaSections.endBorder();
    private final SectionCodec<ExteriorConfig> overworldExterior =
        SchemaSections.exterior("overworldExterior", c -> c.overworldBorder, true);
    private final SectionCodec<ExteriorConfig> netherExterior =
        SchemaSections.exterior("netherExterior", c -> c.netherBorder, false);
    private final SectionCodec<LayoutConfig> layout = SchemaSections.layout();
    private final SectionCodec<SpawnConfig> spawn = SchemaSections.spawn("spawn");
    private final SectionCodec<SingleBiomeConfig> singleBiome = SchemaSections.singleBiome();
    private final SectionCodec<ChaosBiomesConfig> chaosBiomes = SchemaSections.chaosBiomes();
    private final SectionCodec<StripWorldConfig> stripWorld = SchemaSections.stripWorld();
    private final SectionCodec<OceanIslandConfig> oceanIsland = SchemaSections.oceanIsland();
    private final SectionCodec<SkyIslandConfig> skyIsland = SchemaSections.skyIsland();
    private final SectionCodec<ChunkIslandConfig> chunkIsland = SchemaSections.chunkIsland();
    private final SectionCodec<CaveConfig> cave = SchemaSections.cave();
    private final SectionCodec<NetherStartConfig> netherStart = SchemaSections.netherStart();
    private final SectionCodec<EndStartConfig> endStart = SchemaSections.endStart();
    private final SectionCodec<FlatConfig> flat = SchemaSections.flat();
    private final SectionCodec<DeepFlatConfig> deepFlat = SchemaSections.deepFlat();
    private final SectionCodec<StackedConfig> stacked = SchemaSections.stacked();
    private final SectionCodec<ForeverNightConfig> foreverNight = SchemaSections.foreverNight();
    private final SectionCodec<RisingLavaConfig> risingLava = SchemaSections.risingLava();
    private final SectionCodec<StructureDistanceConfig> structureDistance = SchemaSections.structureDistance();

    public WorldzRootSchema() {
        super("", WorldzConfig::new);
    }

    @Override
    protected List<Setting<WorldzConfig, ?>> declare() {
        return List.of(
            // Declared FIRST -- load-bearing (DESIGN §44.4.3, pinned by
            // WorldzRootSchemaTest.kitsIsDeclaredFirst): SchemaSection.sanitize runs settings in
            // declaration order, so the kits library is fully sanitized before any preset's own
            // Rule.KitReference reads it through ctx.root().kits. The setter merges rather than
            // replaces (c.kits.putAll(v), never c.kits = v) -- both at parse time (a user kits.yaml
            // entry adds to/overrides the shipped 14, never deletes them) and at sanitize time,
            // where getter and rule both hand back the very same map instance, making the setter's
            // self-putAll(map, itself) a deliberate no-op (the Setting.group / 25.6a precedent this
            // cites, DESIGN §44.4.2).
            new Setting.PlainBuilder<WorldzConfig, Map<String, StarterKitConfig>>(
                "kits", new Accessor<>(c -> c.kits, (c, v) -> c.kits.putAll(v)), Codecs.sectionMap(kitEntry), new Rule.NestedMap<>(kitEntry)
            )
                .doc(
                    "Named, reusable starter kits (D6); every preset's chest kit either names one of "
                        + "these or defines its own inline. User entries merge over the shipped set."
                )
                .build(),
            Setting.<WorldzConfig>stringList("allowedBiomes", c -> c.allowedBiomes, (c, v) -> c.allowedBiomes = v)
                .rule(new Rule.BiomeIdList<>(
                    Rule.BiomeIdList.Mode.ALLOW_TAGS, "Ignoring invalid allowed biome or tag '{}'.", null, null, null
                ))
                .unit(Unit.BIOME_ID)
                .preset()
                .doc("Biome ids and biome-tag ids allowed in new Worldz worlds.")
                .build(),
            Setting.group("starter", starter)
                .render(starter::summary)
                .preset()
                .doc("Optional biome forced around the origin, and whether/how low terrain beneath it is reinforced.")
                .build(),
            Setting.group("naturalBiomes", naturalBiomes)
                .render(naturalBiomes::summary)
                .preset()
                .doc("Let vanilla's own river/ocean biomes generate naturally on the generic preset (GOALS 13/14).")
                .build(),
            Setting.<WorldzConfig, BorderConfig>section(
                    "overworldBorder", c -> c.overworldBorder, (c, v) -> c.overworldBorder = v, overworldBorder
                )
                .doc("Optional overworld border and End-portal reachability settings.")
                .build(),
            Setting.<WorldzConfig, BorderConfig>section(
                    "netherBorder", c -> c.netherBorder, (c, v) -> c.netherBorder = v, netherBorder
                )
                .doc("Optional Nether border and blaze-access settings.")
                .build(),
            Setting.<WorldzConfig, EndBorderConfig>section(
                    "endBorder", c -> c.endBorder, (c, v) -> c.endBorder = v, endBorder
                )
                .doc("Optional End border carried from the Overworld's final radius (GOALS 17).")
                .build(),
            Setting.<WorldzConfig, ExteriorConfig>section(
                    "overworldExterior", c -> c.overworldExterior, (c, v) -> c.overworldExterior = v, overworldExterior
                )
                .doc("Optional Overworld terrain outside a central square.")
                .build(),
            Setting.<WorldzConfig, ExteriorConfig>section(
                    "netherExterior", c -> c.netherExterior, (c, v) -> c.netherExterior = v, netherExterior
                )
                .doc("Optional Nether terrain outside a central square.")
                .build(),
            Setting.<WorldzConfig, LayoutConfig>section("layout", c -> c.layout, (c, v) -> c.layout = v, layout)
                .preset()
                .doc("Optional coordinated terrain-layout composition.")
                .build(),
            Setting.<WorldzConfig, SpawnConfig>section("spawn", c -> c.spawn, (c, v) -> c.spawn = v, spawn)
                .preset()
                .doc("Layout-origin and initial-spawn strategy.")
                .build(),
            Setting.<WorldzConfig, SingleBiomeConfig>section(
                    "singleBiome", c -> c.singleBiome, (c, v) -> c.singleBiome = v, singleBiome
                )
                .preset("single_biome")
                .doc("Defaults for the jlt_worldz:single_biome typed preset (DESIGN §20.2).")
                .build(),
            Setting.<WorldzConfig, ChaosBiomesConfig>section(
                    "chaosBiomes", c -> c.chaosBiomes, (c, v) -> c.chaosBiomes = v, chaosBiomes
                )
                .preset("chaos_biomes")
                .doc("Defaults for the jlt_worldz:chaos_biomes typed preset (DESIGN §20.11).")
                .build(),
            Setting.<WorldzConfig, StripWorldConfig>section(
                    "stripWorld", c -> c.stripWorld, (c, v) -> c.stripWorld = v, stripWorld
                )
                .preset("worldz", "strip_world")
                .doc("Generic strip opt-in and defaults for the jlt_worldz:strip_world typed preset (GOALS 32, DESIGN §45).")
                .build(),
            Setting.<WorldzConfig, OceanIslandConfig>section(
                    "oceanIsland", c -> c.oceanIsland, (c, v) -> c.oceanIsland = v, oceanIsland
                )
                .preset("ocean_island")
                .doc("Defaults for the jlt_worldz:ocean_island typed preset (GOALS 01, 04; DESIGN §24).")
                .build(),
            Setting.<WorldzConfig, SkyIslandConfig>section(
                    "skyIsland", c -> c.skyIsland, (c, v) -> c.skyIsland = v, skyIsland
                )
                .preset("sky_island")
                .doc("Defaults for the jlt_worldz:sky_island typed preset (GOALS 05; DESIGN §27).")
                .build(),
            Setting.<WorldzConfig, ChunkIslandConfig>section(
                    "chunkIsland", c -> c.chunkIsland, (c, v) -> c.chunkIsland = v, chunkIsland
                )
                .preset("sky_chunk")
                .doc("Defaults for the jlt_worldz:sky_chunk typed preset (GOALS 09/37; DESIGN §29).")
                .build(),
            Setting.<WorldzConfig, CaveConfig>section("cave", c -> c.cave, (c, v) -> c.cave = v, cave)
                .preset("cave")
                .doc("Defaults for the jlt_worldz:cave typed preset (GOALS 25-26; DESIGN §30).")
                .build(),
            Setting.<WorldzConfig, NetherStartConfig>section(
                    "netherStart", c -> c.netherStart, (c, v) -> c.netherStart = v, netherStart
                )
                .preset("nether_start")
                .doc("Defaults for the jlt_worldz:nether_start typed preset (GOALS 27; DESIGN §31).")
                .build(),
            Setting.<WorldzConfig, EndStartConfig>section(
                    "endStart", c -> c.endStart, (c, v) -> c.endStart = v, endStart
                )
                .preset("end_start")
                .doc("Defaults for the jlt_worldz:end_start typed preset (GOALS 34; DESIGN §32).")
                .build(),
            Setting.<WorldzConfig, FlatConfig>section("flat", c -> c.flat, (c, v) -> c.flat = v, flat)
                .preset("flat")
                .doc("Defaults for the jlt_worldz:flat typed preset (GOAL 15; DESIGN §33.2).")
                .build(),
            Setting.<WorldzConfig, DeepFlatConfig>section("deepFlat", c -> c.deepFlat, (c, v) -> c.deepFlat = v, deepFlat)
                .preset("deep_flat")
                .doc("Defaults for the jlt_worldz:deep_flat typed preset (GOAL 16; DESIGN §33.4).")
                .build(),
            Setting.<WorldzConfig, StackedConfig>section("stacked", c -> c.stacked, (c, v) -> c.stacked = v, stacked)
                .preset("stacked")
                .doc("Defaults for the jlt_worldz:stacked typed preset (GOAL 35; DESIGN §34.1).")
                .build(),
            Setting.<WorldzConfig, ForeverNightConfig>section(
                    "foreverNight", c -> c.foreverNight, (c, v) -> c.foreverNight = v, foreverNight
                )
                .live()
                .doc("World-hazard \"forever night\" module (GOAL 30; DESIGN §35.1) -- composes with any world type.")
                .build(),
            Setting.<WorldzConfig, RisingLavaConfig>section(
                    "risingLava", c -> c.risingLava, (c, v) -> c.risingLava = v, risingLava
                )
                .live()
                .doc("World-hazard \"rising lava floor\" module (GOAL 29; DESIGN §35.2) -- composes with any world type.")
                .build(),
            Setting.<WorldzConfig, StructureDistanceConfig>section(
                    "structureDistance", c -> c.structureDistance, (c, v) -> c.structureDistance = v, structureDistance
                )
                .live()
                .doc("\"Structures far from spawn\" module (GOAL 24; DESIGN §36) -- composes with any world type.")
                .build()
        );
    }

    /**
     * Overridden verbatim against today's {@code WorldzConfig.summary()} (class Javadoc, updated
     * TODO 25.6b): {@code starter=} and {@code naturalBiomes=} delegate to {@link #starter} and
     * {@link #naturalBiomes}'s own group summaries (the {@code land=} sub-segment inside {@code
     * starter=} still folds three fields into one, via {@link StarterLandSchema}'s own override,
     * matching pre-rename's {@code starterLand=} exactly except for the renamed labels), so the
     * whole line is hand-assembled here rather than derived from {@link #declare()}. The new
     * leading {@code kits=} segment (TODO 25.8b, DESIGN §44.7) renders the library's <em>name
     * list</em>, not each kit's contents -- matching {@link Rule.KitReference}'s own unknown-name
     * WARN, the other place a user checks what names exist.
     */
    @Override
    public String summary(WorldzConfig value) {
        return "kits=" + value.kits.keySet()
            + ", allowedBiomes=" + value.allowedBiomes
            + ", starter=" + starter.summary(value)
            + ", naturalBiomes=" + naturalBiomes.summary(value)
            + ", overworldBorder=" + overworldBorder.summary(value.overworldBorder)
            + ", netherBorder=" + netherBorder.summary(value.netherBorder)
            + ", endBorder=" + endBorder.summary(value.endBorder)
            + ", overworldExterior=" + overworldExterior.summary(value.overworldExterior)
            + ", netherExterior=" + netherExterior.summary(value.netherExterior)
            + ", layout=" + layout.summary(value.layout)
            + ", spawn=" + spawn.summary(value.spawn)
            + ", singleBiome=" + singleBiome.summary(value.singleBiome)
            + ", chaosBiomes=" + chaosBiomes.summary(value.chaosBiomes)
            + ", stripWorld=" + stripWorld.summary(value.stripWorld)
            + ", oceanIsland=" + oceanIsland.summary(value.oceanIsland)
            + ", skyIsland=" + skyIsland.summary(value.skyIsland)
            + ", chunkIsland=" + chunkIsland.summary(value.chunkIsland)
            + ", cave=" + cave.summary(value.cave)
            + ", netherStart=" + netherStart.summary(value.netherStart)
            + ", endStart=" + endStart.summary(value.endStart)
            + ", flat=" + flat.summary(value.flat)
            + ", deepFlat=" + deepFlat.summary(value.deepFlat)
            + ", stacked=" + stacked.summary(value.stacked)
            + ", foreverNight=" + foreverNight.summary(value.foreverNight)
            + ", risingLava=" + risingLava.summary(value.risingLava)
            + ", structureDistance=" + structureDistance.summary(value.structureDistance);
    }

    /**
     * The root's {@code starter.land} sub-group (TODO 25.6b): only the config root has {@code
     * ensureStarterLand}/{@code starterLandTransitionBlocks}/{@code
     * starterLandFoundationDepthBlocks} fields (DESIGN §42.1), so this is a private implementation
     * detail of {@link WorldzRootSchema} rather than a shared class like {@link StarterSchema}/
     * {@link NaturalBiomesSchema}.
     */
    private static final class StarterLandSchema extends SchemaSection<WorldzConfig> {
        StarterLandSchema(String path) {
            super(path, WorldzConfig::new);
        }

        @Override
        protected List<Setting<WorldzConfig, ?>> declare() {
            return List.of(
                Setting.<WorldzConfig>flag("enabled", c -> c.ensureStarterLand, (c, v) -> c.ensureStarterLand = v)
                    .doc("Whether low terrain beneath a starter biome is raised into usable land.")
                    .build(),
                Setting.<WorldzConfig>integer(
                        "transition", c -> c.starterLandTransitionBlocks, (c, v) -> c.starterLandTransitionBlocks = v
                    )
                    .range(0, WorldzConfig.MAX_STARTER_LAND_TRANSITION_BLOCKS)
                    .unit(Unit.BLOCKS)
                    .doc("Outward distance used to blend reinforced land into natural terrain.")
                    .build(),
                Setting.<WorldzConfig>integer(
                        "foundationDepth", c -> c.starterLandFoundationDepthBlocks, (c, v) -> c.starterLandFoundationDepthBlocks = v
                    )
                    .range(0, WorldzConfig.MAX_STARTER_LAND_FOUNDATION_DEPTH_BLOCKS)
                    .unit(Unit.BLOCKS)
                    .doc("Depth below the natural ocean floor repaired as solid foundation.")
                    .build()
            );
        }

        /**
         * Overridden: {@code enabled} gates the whole line and is itself excluded from the
         * surviving segments -- matching pre-rename {@code WorldzRootSchema.summary()}'s
         * {@code starterLand=} segment exactly (transition/foundation labels unchanged), not
         * mechanically derivable.
         */
        @Override
        public String summary(WorldzConfig value) {
            return value.ensureStarterLand
                ? "transition=" + value.starterLandTransitionBlocks + ", foundation=" + value.starterLandFoundationDepthBlocks
                : "<disabled>";
        }
    }
}
