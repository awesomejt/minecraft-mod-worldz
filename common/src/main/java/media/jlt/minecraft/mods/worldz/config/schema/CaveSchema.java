package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.CaveConfig;
import media.jlt.minecraft.mods.worldz.config.StarterKitConfig;
import media.jlt.minecraft.mods.worldz.logic.CavePlan;
import media.jlt.minecraft.mods.worldz.logic.SealedSurfaceBlock;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;

import java.util.List;

/**
 * Schema for {@link CaveConfig} (GOALS 25-26, DESIGN §30, §41.6's worked example; TODO 25.2f).
 * Exercises every mechanism at once -- plain ints, two booleans, two enums with different
 * null-fallbacks, the one conditional clamp in the codebase, three nested sections -- and today's
 * {@code readCaveConfig}/{@code sanitizeCave}/{@code caveMap}/{@code caveSummary} process every
 * field in the same order (re-verified against the current code, not just DESIGN §41.6's line
 * numbers, which predate 25.2a-e), so no {@link #postValidate} override is needed and the summary
 * is fully derivable.
 *
 * <p>{@code sealedSurfaceY} clamps only a lower bound ({@code sealedSurfaceThicknessBlocks} has a
 * separate, ordinary two-sided range) and only when {@code sealedSurface} is set (DESIGN R5) --
 * the one conditional clamp in the codebase, via {@link Setting.IntBuilder#when}.
 */
public final class CaveSchema extends SchemaSection<CaveConfig> {
    private final StarterKitSchema easyKitSchema;
    private final StarterKitSchema mediumKitSchema;
    private final StarterKitSchema hardKitSchema;

    public CaveSchema(String path) {
        super(path, CaveConfig::new);
        this.easyKitSchema = new StarterKitSchema(path() + ".easyKit");
        this.mediumKitSchema = new StarterKitSchema(path() + ".mediumKit");
        this.hardKitSchema = new StarterKitSchema(path() + ".hardKit");
    }

    @Override
    protected List<Setting<CaveConfig, ?>> declare() {
        return List.of(
            Setting.<CaveConfig>integer("spawnDepthY", c -> c.spawnDepthY, (c, v) -> c.spawnDepthY = v)
                .unit(Unit.Y_LEVEL)
                .doc("Target Y for the underground spawn-cavity search.")
                .build(),
            Setting.<CaveConfig>flag("sealedSurface", c -> c.sealedSurface, (c, v) -> c.sealedSurface = v)
                .doc("Whether a solid roof seals off sky access everywhere.")
                .build(),
            Setting.<CaveConfig>integer("sealedSurfaceY", c -> c.sealedSurfaceY, (c, v) -> c.sealedSurfaceY = v)
                .min(CaveConfig.MIN_SEALED_SURFACE_Y).when(c -> c.sealedSurface)
                .unit(Unit.Y_LEVEL)
                .doc("The roof's Y; only meaningful when sealedSurface is set.")
                .build(),
            Setting.<CaveConfig, SealedSurfaceBlock>enumeration(
                    "sealedSurfaceBlock", c -> c.sealedSurfaceBlock, (c, v) -> c.sealedSurfaceBlock = v,
                    SealedSurfaceBlock::parse, SealedSurfaceBlock::serializedName,
                    CavePlan.DEFAULT_SEALED_SURFACE_BLOCK
                )
                .doc("The roof's block: stone, deepslate or bedrock.")
                .build(),
            Setting.<CaveConfig>integer(
                    "sealedSurfaceThicknessBlocks", c -> c.sealedSurfaceThicknessBlocks, (c, v) -> c.sealedSurfaceThicknessBlocks = v
                )
                .range(CavePlan.MIN_SEALED_SURFACE_THICKNESS_BLOCKS, CavePlan.MAX_SEALED_SURFACE_THICKNESS_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("The roof's thickness.")
                .build(),
            Setting.<CaveConfig>flag("cavernEnabled", c -> c.cavernEnabled, (c, v) -> c.cavernEnabled = v)
                .doc("Whether the mega-cavern is carved around spawn.")
                .build(),
            Setting.<CaveConfig>integer("cavernRadiusBlocks", c -> c.cavernRadiusBlocks, (c, v) -> c.cavernRadiusBlocks = v)
                .range(CavePlan.MIN_CAVERN_BLOCKS, CavePlan.MAX_CAVERN_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("The mega-cavern's horizontal half-width.")
                .build(),
            Setting.<CaveConfig>integer("cavernHeightBlocks", c -> c.cavernHeightBlocks, (c, v) -> c.cavernHeightBlocks = v)
                .range(CavePlan.MIN_CAVERN_BLOCKS, CavePlan.MAX_CAVERN_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("The mega-cavern's vertical half-height.")
                .build(),
            Setting.<CaveConfig>flag("chestEnabled", c -> c.chestEnabled, (c, v) -> c.chestEnabled = v)
                .doc("Whether a starter chest is placed at spawn.")
                .build(),
            Setting.<CaveConfig, StarterKitTier>enumeration(
                    "chestTier", c -> c.chestTier, (c, v) -> c.chestTier = v,
                    StarterKitTier::parse, StarterKitTier::serializedName, StarterKitTier.MEDIUM
                )
                .doc("Which of easyKit/mediumKit/hardKit the starter chest uses.")
                .build(),
            Setting.<CaveConfig, StarterKitConfig>section(
                    "easyKit", c -> c.easyKit, (c, v) -> c.easyKit = v, easyKitSchema
                )
                .render(easyKitSchema::summary)
                .doc("Generous starter-chest contents.")
                .build(),
            Setting.<CaveConfig, StarterKitConfig>section(
                    "mediumKit", c -> c.mediumKit, (c, v) -> c.mediumKit = v, mediumKitSchema
                )
                .render(mediumKitSchema::summary)
                .doc("Middle-ground starter-chest contents.")
                .build(),
            Setting.<CaveConfig, StarterKitConfig>section(
                    "hardKit", c -> c.hardKit, (c, v) -> c.hardKit = v, hardKitSchema
                )
                .render(hardKitSchema::summary)
                .doc("Bare-essentials starter-chest contents.")
                .build()
        );
    }
}
