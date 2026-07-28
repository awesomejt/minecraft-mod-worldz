package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.StarterCapsuleConfig;
import media.jlt.minecraft.mods.worldz.logic.LightSource;

import java.util.List;

/**
 * Schema for {@link StarterCapsuleConfig} (GOALS 41; DESIGN §41.10 R3). Parameterized per parent
 * since {@code nether_start} and {@code end_start} accept different size/height/light-spacing
 * bounds ({@code NetherStartPlan} vs {@code EndStartPlan} constants) -- one of the reasons
 * sections are instances, not static singletons (DESIGN §41.5).
 *
 * <p>{@code sizeBlocks} composes odd-rounding <em>before</em> the range clamp
 * ({@link Setting.IntBuilder#oddRounding()} always applies before {@link Setting.IntBuilder#range}
 * regardless of call order), matching {@code sanitizeStarterCapsule}'s own order (round first,
 * then clamp) exactly.
 */
public final class StarterCapsuleSchema extends SchemaSection<StarterCapsuleConfig> {
    private final int minSizeBlocks;
    private final int maxSizeBlocks;
    private final int minHeightBlocks;
    private final int maxHeightBlocks;
    private final int minLightSpacingBlocks;
    private final int maxLightSpacingBlocks;

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
        this.minLightSpacingBlocks = minLightSpacingBlocks;
        this.maxLightSpacingBlocks = maxLightSpacingBlocks;
    }

    @Override
    protected List<Setting<StarterCapsuleConfig, ?>> declare() {
        return List.of(
            Setting.<StarterCapsuleConfig>integer("sizeBlocks", c -> c.sizeBlocks, (c, v) -> c.sizeBlocks = v)
                .oddRounding().range(minSizeBlocks, maxSizeBlocks)
                .unit(Unit.BLOCKS)
                .doc("Total exterior footprint width/depth, walls included; must stay odd.")
                .build(),
            Setting.<StarterCapsuleConfig>integer("heightBlocks", c -> c.heightBlocks, (c, v) -> c.heightBlocks = v)
                .range(minHeightBlocks, maxHeightBlocks)
                .unit(Unit.BLOCKS)
                .doc("Interior height.")
                .build(),
            Setting.<StarterCapsuleConfig, LightSource>enumeration(
                    "lightSource", c -> c.lightSource, (c, v) -> c.lightSource = v,
                    LightSource::parse, LightSource::serializedName, LightSource.TORCH
                )
                .doc("Which block lights the capsule.")
                .build(),
            Setting.<StarterCapsuleConfig>integer(
                    "lightSpacingBlocks", c -> c.lightSpacingBlocks, (c, v) -> c.lightSpacingBlocks = v
                )
                .range(minLightSpacingBlocks, maxLightSpacingBlocks)
                .unit(Unit.BLOCKS)
                .doc("Spacing between embedded/hung light sources.")
                .build()
        );
    }
}
