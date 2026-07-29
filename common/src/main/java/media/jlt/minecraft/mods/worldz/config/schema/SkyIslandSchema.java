package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.FloatingIslandsConfig;
import media.jlt.minecraft.mods.worldz.config.SkyIslandConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.logic.IslandShapeProfile;
import media.jlt.minecraft.mods.worldz.logic.SkyIslandPlan;

import java.util.List;

/**
 * Schema for {@link SkyIslandConfig} (GOALS 05, DESIGN §27; TODO 25.2e, 25.6d). {@code biome}
 * reuses {@link Rule.BiomeIdOrDefault}, exactly like {@link OceanIslandSchema} ({@code
 * sanitizeSkyIsland} :487-492 mirrors {@code sanitizeOceanIsland} :437-442 verbatim). Every other
 * sanitized field processes in the same order it reads/emits, so no {@link #postValidate} is
 * needed at all. Unlike {@code oceanIsland}, there is no {@code island:} wrapper -- the section
 * name {@code skyIsland} already supplies the prefix (DESIGN §42.3's noted asymmetry) -- and
 * {@code applyToNether} stays a bare key rather than a one-member {@code applyTo:} group, matching
 * {@code strip.applyToNether}'s own precedent.
 *
 * <p><strong>{@code underground} is a real, working group as of TODO 25.6g</strong>, the same shared
 * {@link UndergroundSchema} {@link FlatSchema} now also uses: {@code undergroundBiome}/{@code
 * undergroundBelowSurfaceBlocks} (GOAL 42, DESIGN §37.3) previously existed on {@link
 * SkyIslandConfig} but were entirely unread, unmapped and unsummarized by this schema -- {@code
 * config/tests/98}'s values were silently ignored, read directly by {@code SkyIslandCustomization}
 * from the Customize screen instead (which always disables the band). Now wired for real.
 *
 * <p><strong>{@code exclusionZone} is a real, working group as of TODO 25.6d</strong> (DESIGN
 * §42.1/§42.7's answered open question), not the pre-25.6d dead pair: {@code
 * exclusionZoneEnabled}/{@code exclusionZoneRadiusBlocks} were real {@link SkyIslandConfig} fields,
 * consumed by world-gen logic, but were never read/sanitized through this schema at all --
 * {@code config/tests/57}'s values were silently ignored, and the pre-25.6d {@link #summary}
 * override had to render their untouched constructor defaults by hand for exactly that reason. Now
 * that {@code exclusionZone} is a genuine {@link ExclusionZoneSchema} instance (floored at {@code 1},
 * matching {@code oceanIsland}'s own bound -- the closest conceptual precedent), every field this
 * section declares is a real, derivable {@link Setting}, so the hand-written {@link #summary}
 * override this class used to need is gone: the inherited {@link SchemaSection#summary} now
 * produces the exact same segments mechanically.
 */
public final class SkyIslandSchema extends SchemaSection<SkyIslandConfig> {
    private final ChestSchema<SkyIslandConfig> chest;
    private final ExclusionZoneSchema<SkyIslandConfig> exclusionZone;
    private final UndergroundSchema<SkyIslandConfig> underground;
    private final FloatingIslandsSchema floatingIslandsSchema;

    public SkyIslandSchema(String path) {
        super(path, SkyIslandConfig::new);
        this.chest = new ChestSchema<>(
            path() + ".chest", SkyIslandConfig::new,
            new Accessor<>(c -> c.chestTier, (c, v) -> c.chestTier = v),
            new Accessor<>(c -> c.easyKit, (c, v) -> c.easyKit = v),
            new Accessor<>(c -> c.mediumKit, (c, v) -> c.mediumKit = v),
            new Accessor<>(c -> c.hardKit, (c, v) -> c.hardKit = v),
            "sky-island-easy", "sky-island-medium", "sky-island-hard"
        );
        this.exclusionZone = new ExclusionZoneSchema<>(
            path() + ".exclusionZone", SkyIslandConfig::new,
            new Accessor<>(c -> c.exclusionZoneEnabled, (c, v) -> c.exclusionZoneEnabled = v),
            new Accessor<>(c -> c.exclusionZoneRadiusBlocks, (c, v) -> c.exclusionZoneRadiusBlocks = v),
            1, Integer.MAX_VALUE
        );
        this.underground = new UndergroundSchema<>(
            path() + ".underground", SkyIslandConfig::new,
            new Accessor<>(c -> c.undergroundBiome, (c, v) -> c.undergroundBiome = v),
            new Accessor<>(c -> c.undergroundBelowSurfaceBlocks, (c, v) -> c.undergroundBelowSurfaceBlocks = v)
        );
        this.floatingIslandsSchema = new FloatingIslandsSchema(path() + ".floatingIslands");
    }

    @Override
    protected List<Setting<SkyIslandConfig, ?>> declare() {
        return List.of(
            Setting.<SkyIslandConfig>text("biome", c -> c.islandBiome, (c, v) -> c.islandBiome = v)
                .rule(new Rule.BiomeIdOrDefault<>(
                    "Ignoring invalid " + path() + ".biome '{}'.",
                    "Invalid " + path() + ".biome '{}'; using the default instead.",
                    () -> new SkyIslandConfig().islandBiome
                ))
                .unit(Unit.BIOME_ID)
                .customizeExposed()
                .doc("The one biome that fills the island's interior.")
                .build(),
            Setting.<SkyIslandConfig>integer("radius", c -> c.radiusBlocks, (c, v) -> c.radiusBlocks = v)
                .range(WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS, WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS)
                .customizeExposed()
                .doc("Configured (unperturbed) island radius -- small by default, matching Skyblock's scale.")
                .build(),
            Setting.<SkyIslandConfig>decimal("shapeAmplitude", c -> c.shapeAmplitude, (c, v) -> c.shapeAmplitude = v)
                .range(0.0, IslandShapeProfile.MAX_AMPLITUDE)
                .unit(Unit.FACTOR)
                .customizeExposed()
                .doc("Coastline perturbation strength.")
                .build(),
            Setting.<SkyIslandConfig>integer("surfaceY", c -> c.surfaceY, (c, v) -> c.surfaceY = v)
                .unit(Unit.Y_LEVEL)
                .customizeExposed()
                .doc("The island's walkable surface Y.")
                .build(),
            Setting.<SkyIslandConfig>integer("thickness", c -> c.thicknessBlocks, (c, v) -> c.thicknessBlocks = v)
                .range(SkyIslandPlan.MIN_THICKNESS_BLOCKS, SkyIslandPlan.MAX_THICKNESS_BLOCKS)
                .unit(Unit.BLOCKS)
                .customizeExposed()
                .doc("How many blocks of solid ground extend below surfaceY.")
                .build(),
            Setting.group("chest", chest)
                .render(chest::summary)
                .doc("The starter chest's difficulty tier and easy/medium/hard kit contents.")
                .build(),
            Setting.<SkyIslandConfig>flag("applyToNether", c -> c.applyToNether, (c, v) -> c.applyToNether = v)
                .customizeExposed()
                .doc("Whether the Nether is also a sky island, reusing this same shape.")
                .build(),
            Setting.group("exclusionZone", exclusionZone)
                .render(exclusionZone::summary)
                .customizeChildrenExposed()
                .doc("Whether/where the starter island's own biome-pinning buffer releases to the seed's own biomes.")
                .build(),
            Setting.group("underground", underground)
                .render(underground::summary)
                .doc("Synthetic underground-biome band a configured number of blocks below surfaceY; config-only for now.")
                .build(),
            Setting.<SkyIslandConfig, FloatingIslandsConfig>section(
                    "floatingIslands", c -> c.floatingIslands, (c, v) -> c.floatingIslands = v, floatingIslandsSchema
                )
                .render(floatingIslandsSchema::summary)
                .doc("Scattered small floating islands beyond the starter island's own footprint.")
                .build()
        );
    }
}
