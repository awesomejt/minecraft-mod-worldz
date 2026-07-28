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
import media.jlt.minecraft.mods.worldz.config.StripConfig;
import media.jlt.minecraft.mods.worldz.config.StripWorldConfig;
import media.jlt.minecraft.mods.worldz.config.StructureDistanceConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

import java.util.List;

/**
 * Root schema for {@link WorldzConfig} itself (DESIGN §41.7; TODO 25.2g). Declares the eight
 * top-level scalars, then a {@code Setting.section(...)} entry for each of the 25
 * already-converted sections, in today's exact {@code toYaml()} emit order (DESIGN §41.1's
 * ordering invariant) -- the same order {@code WorldzConfig.parse} checked {@code containsKey} in
 * and {@code WorldzConfig.sanitize} processed fields in (re-verified against the current code, not
 * DESIGN's line numbers, which predate 25.2a-f).
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
 * warning/exception text for the eight scalars (e.g. {@code "Clamped starterRadiusBlocks from ..
 * .to .."}) matches today's exactly; every other (non-root) section already had a non-empty path,
 * so that change is behavior-identical for them.
 *
 * <p>{@code summary()} is overridden rather than derived: {@code starterLand=} folds three separate
 * fields ({@code ensureStarterLand}, {@code starterLandTransitionBlocks}, {@code
 * starterLandFoundationDepthBlocks}) into one segment, which no single {@link Setting}'s renderer
 * can express -- the same "not mechanically derivable" shape as {@code BorderSchema}, {@code
 * ExteriorSchema} and {@code SkyIslandSchema} (DESIGN §41.6's table).
 */
public final class WorldzRootSchema extends SchemaSection<WorldzConfig> {
    private final SectionCodec<BorderConfig> overworldBorder =
        SchemaSections.border("overworldBorder", "ensureEndPortal", "endPortal");
    private final SectionCodec<BorderConfig> netherBorder =
        SchemaSections.border("netherBorder", "ensureBlazeAccess", "blazeAccess");
    private final SectionCodec<EndBorderConfig> endBorder = SchemaSections.endBorder();
    private final SectionCodec<ExteriorConfig> overworldExterior =
        SchemaSections.exterior("overworldExterior", c -> c.overworldBorder, true);
    private final SectionCodec<ExteriorConfig> netherExterior =
        SchemaSections.exterior("netherExterior", c -> c.netherBorder, false);
    private final SectionCodec<StripConfig> strip = SchemaSections.strip();
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
            Setting.<WorldzConfig>stringList("allowedBiomes", c -> c.allowedBiomes, (c, v) -> c.allowedBiomes = v)
                .rule(new Rule.BiomeIdList<>(
                    Rule.BiomeIdList.Mode.ALLOW_TAGS, "Ignoring invalid allowed biome or tag '{}'.", null, null, null
                ))
                .unit(Unit.BIOME_ID)
                .doc("Biome ids and biome-tag ids allowed in new Worldz worlds.")
                .build(),
            Setting.<WorldzConfig>text("starterBiome", c -> c.starterBiome, (c, v) -> c.starterBiome = v)
                .rule(new Rule.BiomeId<>(true, () -> "", "Ignoring invalid starter biome '{}'."))
                .unit(Unit.BIOME_ID)
                .doc("Optional biome id forced around the origin.")
                .build(),
            Setting.<WorldzConfig>integer("starterRadiusBlocks", c -> c.starterRadiusBlocks, (c, v) -> c.starterRadiusBlocks = v)
                .range(WorldzConfig.MIN_STARTER_RADIUS_BLOCKS, WorldzConfig.MAX_STARTER_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Starter-zone radius measured in blocks.")
                .build(),
            Setting.<WorldzConfig>flag("ensureStarterLand", c -> c.ensureStarterLand, (c, v) -> c.ensureStarterLand = v)
                .doc("Whether low terrain beneath a starter biome is raised into usable land.")
                .build(),
            Setting.<WorldzConfig>integer(
                    "starterLandTransitionBlocks", c -> c.starterLandTransitionBlocks, (c, v) -> c.starterLandTransitionBlocks = v
                )
                .range(0, WorldzConfig.MAX_STARTER_LAND_TRANSITION_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Outward distance used to blend reinforced land into natural terrain.")
                .build(),
            Setting.<WorldzConfig>integer(
                    "starterLandFoundationDepthBlocks",
                    c -> c.starterLandFoundationDepthBlocks, (c, v) -> c.starterLandFoundationDepthBlocks = v
                )
                .range(0, WorldzConfig.MAX_STARTER_LAND_FOUNDATION_DEPTH_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Depth below the natural ocean floor repaired as solid foundation.")
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
            Setting.<WorldzConfig, StripConfig>section("strip", c -> c.strip, (c, v) -> c.strip = v, strip)
                .doc("Optional narrow strip-world corridor (GOALS 32).")
                .build(),
            Setting.<WorldzConfig, LayoutConfig>section("layout", c -> c.layout, (c, v) -> c.layout = v, layout)
                .doc("Optional coordinated terrain-layout composition.")
                .build(),
            Setting.<WorldzConfig, SpawnConfig>section("spawn", c -> c.spawn, (c, v) -> c.spawn = v, spawn)
                .doc("Layout-origin and initial-spawn strategy.")
                .build(),
            Setting.<WorldzConfig, SingleBiomeConfig>section(
                    "singleBiome", c -> c.singleBiome, (c, v) -> c.singleBiome = v, singleBiome
                )
                .doc("Defaults for the jlt_worldz:single_biome typed preset (DESIGN §20.2).")
                .build(),
            Setting.<WorldzConfig, ChaosBiomesConfig>section(
                    "chaosBiomes", c -> c.chaosBiomes, (c, v) -> c.chaosBiomes = v, chaosBiomes
                )
                .doc("Defaults for the jlt_worldz:chaos_biomes typed preset (DESIGN §20.11).")
                .build(),
            Setting.<WorldzConfig, StripWorldConfig>section(
                    "stripWorld", c -> c.stripWorld, (c, v) -> c.stripWorld = v, stripWorld
                )
                .doc("Defaults for the jlt_worldz:strip_world typed preset (GOALS 32, DESIGN §23).")
                .build(),
            Setting.<WorldzConfig, OceanIslandConfig>section(
                    "oceanIsland", c -> c.oceanIsland, (c, v) -> c.oceanIsland = v, oceanIsland
                )
                .doc("Defaults for the jlt_worldz:ocean_island typed preset (GOALS 01, 04; DESIGN §24).")
                .build(),
            Setting.<WorldzConfig, SkyIslandConfig>section(
                    "skyIsland", c -> c.skyIsland, (c, v) -> c.skyIsland = v, skyIsland
                )
                .doc("Defaults for the jlt_worldz:sky_island typed preset (GOALS 05; DESIGN §27).")
                .build(),
            Setting.<WorldzConfig, ChunkIslandConfig>section(
                    "chunkIsland", c -> c.chunkIsland, (c, v) -> c.chunkIsland = v, chunkIsland
                )
                .doc("Defaults for the jlt_worldz:sky_chunk typed preset (GOALS 09/37; DESIGN §29).")
                .build(),
            Setting.<WorldzConfig, CaveConfig>section("cave", c -> c.cave, (c, v) -> c.cave = v, cave)
                .doc("Defaults for the jlt_worldz:cave typed preset (GOALS 25-26; DESIGN §30).")
                .build(),
            Setting.<WorldzConfig, NetherStartConfig>section(
                    "netherStart", c -> c.netherStart, (c, v) -> c.netherStart = v, netherStart
                )
                .doc("Defaults for the jlt_worldz:nether_start typed preset (GOALS 27; DESIGN §31).")
                .build(),
            Setting.<WorldzConfig, EndStartConfig>section(
                    "endStart", c -> c.endStart, (c, v) -> c.endStart = v, endStart
                )
                .doc("Defaults for the jlt_worldz:end_start typed preset (GOALS 34; DESIGN §32).")
                .build(),
            Setting.<WorldzConfig, FlatConfig>section("flat", c -> c.flat, (c, v) -> c.flat = v, flat)
                .doc("Defaults for the jlt_worldz:flat typed preset (GOAL 15; DESIGN §33.2).")
                .build(),
            Setting.<WorldzConfig, DeepFlatConfig>section("deepFlat", c -> c.deepFlat, (c, v) -> c.deepFlat = v, deepFlat)
                .doc("Defaults for the jlt_worldz:deep_flat typed preset (GOAL 16; DESIGN §33.4).")
                .build(),
            Setting.<WorldzConfig, StackedConfig>section("stacked", c -> c.stacked, (c, v) -> c.stacked = v, stacked)
                .doc("Defaults for the jlt_worldz:stacked typed preset (GOAL 35; DESIGN §34.1).")
                .build(),
            Setting.<WorldzConfig, ForeverNightConfig>section(
                    "foreverNight", c -> c.foreverNight, (c, v) -> c.foreverNight = v, foreverNight
                )
                .doc("World-hazard \"forever night\" module (GOAL 30; DESIGN §35.1) -- composes with any world type.")
                .build(),
            Setting.<WorldzConfig, RisingLavaConfig>section(
                    "risingLava", c -> c.risingLava, (c, v) -> c.risingLava = v, risingLava
                )
                .doc("World-hazard \"rising lava floor\" module (GOAL 29; DESIGN §35.2) -- composes with any world type.")
                .build(),
            Setting.<WorldzConfig, StructureDistanceConfig>section(
                    "structureDistance", c -> c.structureDistance, (c, v) -> c.structureDistance = v, structureDistance
                )
                .doc("\"Structures far from spawn\" module (GOAL 24; DESIGN §36) -- composes with any world type.")
                .build(),
            Setting.<WorldzConfig>flag("allowRivers", c -> c.allowRivers, (c, v) -> c.allowRivers = v)
                .doc("Let vanilla's own river biomes generate naturally on the generic preset (GOALS 13).")
                .build(),
            Setting.<WorldzConfig>flag("allowOceans", c -> c.allowOceans, (c, v) -> c.allowOceans = v)
                .doc("Let vanilla's own river/ocean-family biomes generate naturally on the generic preset (GOALS 14).")
                .build()
        );
    }

    /**
     * Overridden verbatim against today's {@code WorldzConfig.summary()} (class Javadoc): {@code
     * starterLand=} folds three fields into one segment, which no single {@link Setting}'s renderer
     * can express, so the whole line is hand-assembled here rather than derived from {@link
     * #declare()}.
     */
    @Override
    public String summary(WorldzConfig value) {
        return "allowedBiomes=" + value.allowedBiomes
            + ", starterBiome=" + (value.starterBiome.isEmpty() ? "<none>" : value.starterBiome)
            + ", starterRadiusBlocks=" + value.starterRadiusBlocks
            + ", starterLand=" + (value.ensureStarterLand
                ? "transition=" + value.starterLandTransitionBlocks + ", foundation=" + value.starterLandFoundationDepthBlocks
                : "<disabled>")
            + ", overworldBorder=" + overworldBorder.summary(value.overworldBorder)
            + ", netherBorder=" + netherBorder.summary(value.netherBorder)
            + ", endBorder=" + endBorder.summary(value.endBorder)
            + ", overworldExterior=" + overworldExterior.summary(value.overworldExterior)
            + ", netherExterior=" + netherExterior.summary(value.netherExterior)
            + ", strip=" + strip.summary(value.strip)
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
            + ", structureDistance=" + structureDistance.summary(value.structureDistance)
            + ", allowRivers=" + value.allowRivers
            + ", allowOceans=" + value.allowOceans;
    }
}
