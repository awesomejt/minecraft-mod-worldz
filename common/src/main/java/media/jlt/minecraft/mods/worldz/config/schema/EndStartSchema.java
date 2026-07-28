package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.EndStartConfig;
import media.jlt.minecraft.mods.worldz.config.StarterCapsuleConfig;
import media.jlt.minecraft.mods.worldz.logic.EndStartPlan;

import java.util.List;

/**
 * Schema for {@link EndStartConfig} (GOALS 34, 41; DESIGN §32; TODO 25.2f, 25.6e). Same {@code
 * chest} tier + three-kit + nested capsule shape as {@link NetherStartSchema} (the same no-{@code
 * enabled} {@link ChestSchema} constructor -- the chest is unconditional here too), but simpler:
 * unlike Nether-start, {@link EndStartConfig} has no {@code spawnY} (the End's safe-site search
 * has no equivalent Y target) and no {@code forceCapsule} (the guaranteed platform is End-start's
 * only mode -- there is no natural safe-site search to prefer over it). The nested {@link
 * StarterCapsuleSchema} is parameterized with End-start's own bounds ({@link EndStartPlan}'s
 * {@code MIN}/{@code MAX_CAPSULE_*} constants), exactly the parameterization {@code
 * sanitizeEndStart} passes to {@code sanitizeStarterCapsule} today.
 *
 * <p>{@code readEndStartConfig}/{@code sanitizeEndStart}/{@code endStartMap}/{@code
 * endStartSummary} all process {@code chestTier}, the three kits and {@code capsule} in the same
 * order, so no {@link #postValidate} override is needed and the summary is fully derivable.
 */
public final class EndStartSchema extends SchemaSection<EndStartConfig> {
    private final ChestSchema<EndStartConfig> chest;
    private final StarterCapsuleSchema capsuleSchema;

    public EndStartSchema(String path) {
        super(path, EndStartConfig::new);
        this.chest = new ChestSchema<>(
            path() + ".chest", EndStartConfig::new,
            new Accessor<>(c -> c.chestTier, (c, v) -> c.chestTier = v),
            new Accessor<>(c -> c.easyKit, (c, v) -> c.easyKit = v),
            new Accessor<>(c -> c.mediumKit, (c, v) -> c.mediumKit = v),
            new Accessor<>(c -> c.hardKit, (c, v) -> c.hardKit = v)
        );
        this.capsuleSchema = new StarterCapsuleSchema(
            path() + ".capsule",
            EndStartPlan.MIN_CAPSULE_SIZE_BLOCKS, EndStartPlan.MAX_CAPSULE_SIZE_BLOCKS,
            EndStartPlan.MIN_CAPSULE_HEIGHT_BLOCKS, EndStartPlan.MAX_CAPSULE_HEIGHT_BLOCKS,
            EndStartPlan.MIN_CAPSULE_LIGHT_SPACING_BLOCKS, EndStartPlan.MAX_CAPSULE_LIGHT_SPACING_BLOCKS
        );
    }

    @Override
    protected List<Setting<EndStartConfig, ?>> declare() {
        return List.of(
            Setting.group("chest", chest)
                .render(chest::summary)
                .doc("The starter chest's difficulty tier and easy/medium/hard kit contents; every tier guarantees a pickaxe.")
                .build(),
            Setting.<EndStartConfig, StarterCapsuleConfig>section(
                    "capsule", c -> c.capsule, (c, v) -> c.capsule = v, capsuleSchema
                )
                .render(capsuleSchema::summary)
                .doc("The guaranteed platform's shape and lighting.")
                .build()
        );
    }
}
