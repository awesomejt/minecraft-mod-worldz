package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.StarterCapsuleConfig;
import media.jlt.minecraft.mods.worldz.logic.LightSource;

import java.util.List;

/**
 * Schema for {@link StarterCapsuleConfig} (GOALS 41; DESIGN §41.10 R3). Parameterized per parent
 * since {@code nether_start} and {@code end_start} accept different size/height/light-spacing
 * bounds ({@code NetherStartPlan} vs {@code EndStartPlan} constants) -- one of the reasons
 * sections are instances, not static singletons (DESIGN §41.5). TODO 25.6e renames {@code
 * sizeBlocks}/{@code heightBlocks} to {@code size}/{@code height} and nests {@code lightSource}/
 * {@code lightSpacingBlocks} into a {@code light: {source, spacing}} group (DESIGN §42.3); both
 * parameterized instances ({@link media.jlt.minecraft.mods.worldz.config.schema.NetherStartSchema}
 * and {@link media.jlt.minecraft.mods.worldz.config.schema.EndStartSchema}) are renamed together
 * since they share this one class (DESIGN R3).
 *
 * <p>{@code size} composes odd-rounding <em>before</em> the range clamp
 * ({@link Setting.IntBuilder#oddRounding()} always applies before {@link Setting.IntBuilder#range}
 * regardless of call order), matching {@code sanitizeStarterCapsule}'s own order (round first,
 * then clamp) exactly.
 */
public final class StarterCapsuleSchema extends SchemaSection<StarterCapsuleConfig> {
    private final int minSizeBlocks;
    private final int maxSizeBlocks;
    private final int minHeightBlocks;
    private final int maxHeightBlocks;
    private final LightSchema light;

    public StarterCapsuleSchema(
        String path,
        int minSizeBlocks, int maxSizeBlocks,
        int minHeightBlocks, int maxHeightBlocks,
        int minLightSpacingBlocks, int maxLightSpacingBlocks
    ) {
        super(path, StarterCapsuleConfig::new);
        this.minSizeBlocks = minSizeBlocks;
        this.maxSizeBlocks = maxSizeBlocks;
        this.minHeightBlocks = minHeightBlocks;
        this.maxHeightBlocks = maxHeightBlocks;
        this.light = new LightSchema(path + ".light", minLightSpacingBlocks, maxLightSpacingBlocks);
    }

    @Override
    protected List<Setting<StarterCapsuleConfig, ?>> declare() {
        return List.of(
            Setting.<StarterCapsuleConfig>integer("size", c -> c.sizeBlocks, (c, v) -> c.sizeBlocks = v)
                .oddRounding().range(minSizeBlocks, maxSizeBlocks)
                .unit(Unit.BLOCKS)
                .doc("Total exterior footprint width/depth, walls included; must stay odd.")
                .build(),
            Setting.<StarterCapsuleConfig>integer("height", c -> c.heightBlocks, (c, v) -> c.heightBlocks = v)
                .range(minHeightBlocks, maxHeightBlocks)
                .unit(Unit.BLOCKS)
                .doc("Interior height.")
                .build(),
            Setting.group("light", light)
                .render(light::summary)
                .doc("Which block lights the capsule, and how far apart.")
                .build()
        );
    }

    /**
     * {@code capsule.light}: {@code source}/{@code spacing} (TODO 25.6e), a group over {@link
     * StarterCapsuleSchema}'s own {@link StarterCapsuleConfig} type -- there is only ever one owner
     * shape here (unlike {@link ChestSchema}/{@link StarterSchema}), but the light-spacing bound is
     * still per-parent, so it is threaded through the constructor exactly like the outer class's
     * own size/height bounds.
     */
    private static final class LightSchema extends SchemaSection<StarterCapsuleConfig> {
        private final int minSpacingBlocks;
        private final int maxSpacingBlocks;

        LightSchema(String path, int minSpacingBlocks, int maxSpacingBlocks) {
            super(path, StarterCapsuleConfig::new);
            this.minSpacingBlocks = minSpacingBlocks;
            this.maxSpacingBlocks = maxSpacingBlocks;
        }

        @Override
        protected List<Setting<StarterCapsuleConfig, ?>> declare() {
            return List.of(
                Setting.<StarterCapsuleConfig, LightSource>enumeration(
                        "source", c -> c.lightSource, (c, v) -> c.lightSource = v,
                        LightSource::parse, LightSource::serializedName, LightSource.TORCH
                    )
                    .doc("Which block lights the capsule.")
                    .build(),
                Setting.<StarterCapsuleConfig>integer(
                        "spacing", c -> c.lightSpacingBlocks, (c, v) -> c.lightSpacingBlocks = v
                    )
                    .range(minSpacingBlocks, maxSpacingBlocks)
                    .unit(Unit.BLOCKS)
                    .doc("Spacing between embedded/hung light sources.")
                    .build()
            );
        }
    }
}
