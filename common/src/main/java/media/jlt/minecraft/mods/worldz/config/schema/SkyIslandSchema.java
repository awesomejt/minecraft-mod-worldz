package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.FloatingIslandsConfig;
import media.jlt.minecraft.mods.worldz.config.SkyIslandConfig;
import media.jlt.minecraft.mods.worldz.config.StarterKitConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.logic.IslandShapeProfile;
import media.jlt.minecraft.mods.worldz.logic.SkyIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;

import java.util.List;

/**
 * Schema for {@link SkyIslandConfig} (GOALS 05, DESIGN §27; TODO 25.2e). {@code islandBiome}
 * reuses {@link Rule.BiomeIdOrDefault}, exactly like {@link OceanIslandSchema} ({@code
 * sanitizeSkyIsland} :487-492 mirrors {@code sanitizeOceanIsland} :437-442 verbatim). Every other
 * sanitized field processes in the same order it reads/emits, so no {@link #postValidate} is
 * needed at all.
 *
 * <p>Two fields exist on {@link SkyIslandConfig} but are not wired into today's {@code
 * readSkyIslandConfig}/{@code sanitizeSkyIsland}/{@code skyIslandMap} -- pre-existing gaps carried
 * over unchanged (nothing moves), the same shape {@link FlatSchema} documents for {@code
 * FlatConfig}:
 * <ul>
 *   <li>{@code undergroundBiome}/{@code undergroundBelowSurfaceBlocks} (GOAL 42, DESIGN §37.3) are
 *   entirely unread, unmapped and unsummarized -- read directly by {@code SkyIslandCustomization}
 *   from the Customize screen instead, never through this config path.</li>
 *   <li>{@code exclusionZoneEnabled}/{@code exclusionZoneRadiusBlocks} are unread and unmapped, but
 *   {@code skyIslandSummary} (:2172-2184) references them anyway -- so the summary line always
 *   renders their untouched constructor defaults ({@code true}, {@code 128}), never a value a user
 *   actually configured. Reproduced exactly via the {@link #summary} override below rather than
 *   declaring them as real {@link Setting}s, since a {@code Setting} is inherently read+mapped.</li>
 * </ul>
 *
 * <p>Because of that second gap, the summary cannot be derived (unlike {@link OceanIslandSchema}'s
 * genuinely-paired {@code exclusionZone=} settings): it is overridden wholesale here, verbatim
 * against {@code skyIslandSummary}.
 */
public final class SkyIslandSchema extends SchemaSection<SkyIslandConfig> {
    private final StarterKitSchema easyKitSchema;
    private final StarterKitSchema mediumKitSchema;
    private final StarterKitSchema hardKitSchema;
    private final FloatingIslandsSchema floatingIslandsSchema;

    public SkyIslandSchema(String path) {
        super(path, SkyIslandConfig::new);
        this.easyKitSchema = new StarterKitSchema(path() + ".easyKit");
        this.mediumKitSchema = new StarterKitSchema(path() + ".mediumKit");
        this.hardKitSchema = new StarterKitSchema(path() + ".hardKit");
        this.floatingIslandsSchema = new FloatingIslandsSchema(path() + ".floatingIslands");
    }

    @Override
    protected List<Setting<SkyIslandConfig, ?>> declare() {
        return List.of(
            Setting.<SkyIslandConfig>text("islandBiome", c -> c.islandBiome, (c, v) -> c.islandBiome = v)
                .rule(new Rule.BiomeIdOrDefault<>(
                    "Ignoring invalid " + path() + ".islandBiome '{}'.",
                    "Invalid " + path() + ".islandBiome '{}'; using the default instead.",
                    () -> new SkyIslandConfig().islandBiome
                ))
                .unit(Unit.BIOME_ID)
                .doc("The one biome that fills the island's interior.")
                .build(),
            Setting.<SkyIslandConfig>integer("radiusBlocks", c -> c.radiusBlocks, (c, v) -> c.radiusBlocks = v)
                .range(WorldzConfig.MIN_ISLAND_RADIUS_BLOCKS, WorldzConfig.MAX_ISLAND_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Configured (unperturbed) island radius -- small by default, matching Skyblock's scale.")
                .build(),
            Setting.<SkyIslandConfig>decimal("shapeAmplitude", c -> c.shapeAmplitude, (c, v) -> c.shapeAmplitude = v)
                .range(0.0, IslandShapeProfile.MAX_AMPLITUDE)
                .doc("Coastline perturbation strength.")
                .build(),
            Setting.<SkyIslandConfig>integer("surfaceY", c -> c.surfaceY, (c, v) -> c.surfaceY = v)
                .unit(Unit.Y_LEVEL)
                .doc("The island's walkable surface Y.")
                .build(),
            Setting.<SkyIslandConfig>integer("thicknessBlocks", c -> c.thicknessBlocks, (c, v) -> c.thicknessBlocks = v)
                .range(SkyIslandPlan.MIN_THICKNESS_BLOCKS, SkyIslandPlan.MAX_THICKNESS_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("How many blocks of solid ground extend below surfaceY.")
                .build(),
            Setting.<SkyIslandConfig, StarterKitTier>enumeration(
                    "chestTier", c -> c.chestTier, (c, v) -> c.chestTier = v,
                    StarterKitTier::parse, StarterKitTier::serializedName, StarterKitTier.MEDIUM
                )
                .doc("Which of easyKit/mediumKit/hardKit the starter chest uses.")
                .build(),
            Setting.<SkyIslandConfig, StarterKitConfig>section(
                    "easyKit", c -> c.easyKit, (c, v) -> c.easyKit = v, easyKitSchema
                )
                .doc("Generous starter-chest contents.")
                .build(),
            Setting.<SkyIslandConfig, StarterKitConfig>section(
                    "mediumKit", c -> c.mediumKit, (c, v) -> c.mediumKit = v, mediumKitSchema
                )
                .doc("Middle-ground starter-chest contents.")
                .build(),
            Setting.<SkyIslandConfig, StarterKitConfig>section(
                    "hardKit", c -> c.hardKit, (c, v) -> c.hardKit = v, hardKitSchema
                )
                .doc("Bare-essentials starter-chest contents.")
                .build(),
            Setting.<SkyIslandConfig>flag("applyToNether", c -> c.applyToNether, (c, v) -> c.applyToNether = v)
                .doc("Whether the Nether is also a sky island, reusing this same shape.")
                .build(),
            Setting.<SkyIslandConfig, FloatingIslandsConfig>section(
                    "floatingIslands", c -> c.floatingIslands, (c, v) -> c.floatingIslands = v, floatingIslandsSchema
                )
                .doc("Scattered small floating islands beyond the starter island's own footprint.")
                .build()
        );
    }

    /**
     * Overridden: unlike {@link OceanIslandSchema}'s genuinely-paired {@code exclusionZone=}
     * settings, {@code exclusionZoneEnabled}/{@code exclusionZoneRadiusBlocks} are not real {@link
     * Setting}s here at all (class Javadoc's second gap) -- their untouched constructor defaults
     * are read directly, matching {@code skyIslandSummary} exactly. Not mechanically derivable.
     */
    @Override
    public String summary(SkyIslandConfig value) {
        return "islandBiome=" + value.islandBiome
            + ", radiusBlocks=" + value.radiusBlocks
            + ", shapeAmplitude=" + value.shapeAmplitude
            + ", surfaceY=" + value.surfaceY
            + ", thicknessBlocks=" + value.thicknessBlocks
            + ", chestTier=" + value.chestTier.serializedName()
            + ", easyKit=" + easyKitSchema.summary(value.easyKit)
            + ", mediumKit=" + mediumKitSchema.summary(value.mediumKit)
            + ", hardKit=" + hardKitSchema.summary(value.hardKit)
            + ", applyToNether=" + value.applyToNether
            + ", exclusionZone=" + (value.exclusionZoneEnabled ? "radius=" + value.exclusionZoneRadiusBlocks : "<disabled>")
            + ", floatingIslands=" + floatingIslandsSchema.summary(value.floatingIslands);
    }
}
