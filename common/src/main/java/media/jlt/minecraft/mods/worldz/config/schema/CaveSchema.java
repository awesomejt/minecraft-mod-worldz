package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.CaveConfig;
import media.jlt.minecraft.mods.worldz.logic.CavePlan;
import media.jlt.minecraft.mods.worldz.logic.SealedSurfaceBlock;

import java.util.List;

/**
 * Schema for {@link CaveConfig} (GOALS 25-26, DESIGN §30, §41.6's worked example; TODO 25.2f,
 * 25.6e). Exercises every mechanism at once -- plain ints, two booleans, two enums with different
 * null-fallbacks, the one conditional clamp in the codebase, three nested groups/sections -- and
 * today's {@code readCaveConfig}/{@code sanitizeCave}/{@code caveMap}/{@code caveSummary} process
 * every field in the same order (re-verified against the current code, not just DESIGN §41.6's
 * line numbers, which predate 25.2a-e), so no {@link #postValidate} override is needed and the
 * summary is fully derivable.
 *
 * <p>{@code sealedSurface.y} clamps only a lower bound ({@code sealedSurface.thickness} has a
 * separate, ordinary two-sided range) and only when {@code sealedSurface.enabled} is set (DESIGN
 * R5) -- the one conditional clamp in the codebase, via {@link Setting.IntBuilder#when}, surviving
 * the {@code sealedSurface} nest verbatim since the predicate reads a POJO field that stays flat
 * on {@link CaveConfig} itself (DESIGN §42.2).
 *
 * <p>{@code chest} is the {@link ChestSchema} full constructor -- the {@code enabled}-leaf variant,
 * proven here for the first time (TODO 25.6d's Javadoc); every other {@link ChestSchema} owner
 * ({@code skyIsland}, {@code netherStart}, {@code endStart}) uses the no-{@code enabled} variant
 * instead, since their chest is unconditional.
 */
public final class CaveSchema extends SchemaSection<CaveConfig> {
    private final SealedSurfaceSchema sealedSurface;
    private final CavernSchema cavern;
    private final ChestSchema<CaveConfig> chest;

    public CaveSchema(String path) {
        super(path, CaveConfig::new);
        this.sealedSurface = new SealedSurfaceSchema(path() + ".sealedSurface");
        this.cavern = new CavernSchema(path() + ".cavern");
        this.chest = new ChestSchema<>(
            path() + ".chest", CaveConfig::new,
            new Accessor<>(c -> c.chestEnabled, (c, v) -> c.chestEnabled = v),
            new Accessor<>(c -> c.chestTier, (c, v) -> c.chestTier = v),
            new Accessor<>(c -> c.easyKit, (c, v) -> c.easyKit = v),
            new Accessor<>(c -> c.mediumKit, (c, v) -> c.mediumKit = v),
            new Accessor<>(c -> c.hardKit, (c, v) -> c.hardKit = v),
            "cave-easy", "cave-medium", "cave-hard"
        );
    }

    @Override
    protected List<Setting<CaveConfig, ?>> declare() {
        return List.of(
            Setting.<CaveConfig>integer("spawnY", c -> c.spawnDepthY, (c, v) -> c.spawnDepthY = v)
                .unit(Unit.Y_LEVEL)
                .customizeExposed()
                .doc("Target Y for the underground spawn-cavity search.")
                .build(),
            Setting.group("sealedSurface", sealedSurface)
                .render(sealedSurface::summary)
                .doc("Whether/how a solid roof seals off sky access everywhere.")
                .build(),
            Setting.group("cavern", cavern)
                .render(cavern::summary)
                .customizeChildrenExposed()
                .doc("Whether/how a large cavern is carved around spawn.")
                .build(),
            Setting.group("chest", chest)
                .render(chest::summary)
                .doc("Whether/how a starter chest is placed at spawn.")
                .build()
        );
    }

    /**
     * {@code cave.sealedSurface}: {@code enabled}/{@code y}/{@code block}/{@code thickness} (TODO
     * 25.6e), a group over {@link CaveSchema}'s own {@link CaveConfig} type -- there is only ever
     * one owner shape here, unlike {@link ChestSchema}/{@link ExclusionZoneSchema}. {@code y}'s
     * {@code .when(c -> c.sealedSurface)} predicate is DESIGN R5's one conditional clamp, surviving
     * the nest verbatim since it reads {@link CaveConfig#sealedSurface} directly.
     */
    private static final class SealedSurfaceSchema extends SchemaSection<CaveConfig> {
        SealedSurfaceSchema(String path) {
            super(path, CaveConfig::new);
        }

        @Override
        protected List<Setting<CaveConfig, ?>> declare() {
            return List.of(
                Setting.<CaveConfig>flag("enabled", c -> c.sealedSurface, (c, v) -> c.sealedSurface = v)
                    .customizeExposed()
                    .doc("Whether a solid roof seals off sky access everywhere.")
                    .build(),
                Setting.<CaveConfig>integer("y", c -> c.sealedSurfaceY, (c, v) -> c.sealedSurfaceY = v)
                    .min(CaveConfig.MIN_SEALED_SURFACE_Y).when(c -> c.sealedSurface)
                    .unit(Unit.Y_LEVEL)
                    .customizeExposed()
                    .doc("The roof's Y; only meaningful when enabled is set.")
                    .build(),
                Setting.<CaveConfig, SealedSurfaceBlock>enumeration(
                        "block", c -> c.sealedSurfaceBlock, (c, v) -> c.sealedSurfaceBlock = v,
                        SealedSurfaceBlock::parse, SealedSurfaceBlock::serializedName,
                        CavePlan.DEFAULT_SEALED_SURFACE_BLOCK
                    )
                    .doc("The roof's block: stone, deepslate or bedrock.")
                    .build(),
                Setting.<CaveConfig>integer(
                        "thickness", c -> c.sealedSurfaceThicknessBlocks, (c, v) -> c.sealedSurfaceThicknessBlocks = v
                    )
                    .range(CavePlan.MIN_SEALED_SURFACE_THICKNESS_BLOCKS, CavePlan.MAX_SEALED_SURFACE_THICKNESS_BLOCKS)
                    .unit(Unit.BLOCKS)
                    .doc("The roof's thickness.")
                    .build()
            );
        }
    }

    /**
     * {@code cave.cavern}: {@code enabled}/{@code radius}/{@code height} (TODO 25.6e), a group over
     * {@link CaveSchema}'s own {@link CaveConfig} type, exactly like {@link SealedSurfaceSchema}.
     */
    private static final class CavernSchema extends SchemaSection<CaveConfig> {
        CavernSchema(String path) {
            super(path, CaveConfig::new);
        }

        @Override
        protected List<Setting<CaveConfig, ?>> declare() {
            return List.of(
                Setting.<CaveConfig>flag("enabled", c -> c.cavernEnabled, (c, v) -> c.cavernEnabled = v)
                    .doc("Whether the mega-cavern is carved around spawn.")
                    .build(),
                Setting.<CaveConfig>integer("radius", c -> c.cavernRadiusBlocks, (c, v) -> c.cavernRadiusBlocks = v)
                    .range(CavePlan.MIN_CAVERN_BLOCKS, CavePlan.MAX_CAVERN_BLOCKS)
                    .unit(Unit.BLOCKS)
                    .doc("The mega-cavern's horizontal half-width.")
                    .build(),
                Setting.<CaveConfig>integer("height", c -> c.cavernHeightBlocks, (c, v) -> c.cavernHeightBlocks = v)
                    .range(CavePlan.MIN_CAVERN_BLOCKS, CavePlan.MAX_CAVERN_BLOCKS)
                    .unit(Unit.BLOCKS)
                    .doc("The mega-cavern's vertical half-height.")
                    .build()
            );
        }
    }
}
