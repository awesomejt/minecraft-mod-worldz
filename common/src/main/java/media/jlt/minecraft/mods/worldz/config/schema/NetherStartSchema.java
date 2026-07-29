package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.NetherStartConfig;
import media.jlt.minecraft.mods.worldz.config.StarterCapsuleConfig;
import media.jlt.minecraft.mods.worldz.logic.NetherStartPlan;

import java.util.List;

/**
 * Schema for {@link NetherStartConfig} (GOALS 27, 41.1; DESIGN §31.9; TODO 25.2f, 25.6e). Proves
 * the shared {@code chest} tier + three-kit shape (also in {@link CaveSchema}, {@link
 * media.jlt.minecraft.mods.worldz.config.SkyIslandConfig}, via the same no-{@code enabled} {@link
 * ChestSchema} constructor {@code skyIsland} uses -- the chest is unconditional here) alongside a
 * nested {@link StarterCapsuleSchema} parameterized with Nether-start's own bounds ({@link
 * NetherStartPlan}'s {@code MIN}/{@code MAX_CAPSULE_*} constants), exactly the parameterization
 * {@code sanitizeNetherStart} passes to {@code sanitizeStarterCapsule} today.
 *
 * <p>{@code readNetherStartConfig}/{@code sanitizeNetherStart}/{@code netherStartMap}/{@code
 * netherStartSummary} all process {@code spawnY}, {@code chestTier}, the three kits, {@code
 * forceCapsule} and {@code capsule} in the same order, so no {@link #postValidate} override is
 * needed and the summary is fully derivable.
 */
public final class NetherStartSchema extends SchemaSection<NetherStartConfig> {
    private final ChestSchema<NetherStartConfig> chest;
    private final StarterCapsuleSchema capsuleSchema;

    public NetherStartSchema(String path) {
        super(path, NetherStartConfig::new);
        this.chest = new ChestSchema<>(
            path() + ".chest", NetherStartConfig::new,
            new Accessor<>(c -> c.chestTier, (c, v) -> c.chestTier = v),
            new Accessor<>(c -> c.easyKit, (c, v) -> c.easyKit = v),
            new Accessor<>(c -> c.mediumKit, (c, v) -> c.mediumKit = v),
            new Accessor<>(c -> c.hardKit, (c, v) -> c.hardKit = v),
            "nether-start-easy", "nether-start-medium", "nether-start-hard"
        );
        this.capsuleSchema = new StarterCapsuleSchema(
            path() + ".capsule",
            NetherStartPlan.MIN_CAPSULE_SIZE_BLOCKS, NetherStartPlan.MAX_CAPSULE_SIZE_BLOCKS,
            NetherStartPlan.MIN_CAPSULE_HEIGHT_BLOCKS, NetherStartPlan.MAX_CAPSULE_HEIGHT_BLOCKS,
            NetherStartPlan.MIN_CAPSULE_LIGHT_SPACING_BLOCKS, NetherStartPlan.MAX_CAPSULE_LIGHT_SPACING_BLOCKS
        );
    }

    @Override
    protected List<Setting<NetherStartConfig, ?>> declare() {
        return List.of(
            Setting.<NetherStartConfig>integer("spawnY", c -> c.spawnY, (c, v) -> c.spawnY = v)
                .range(NetherStartPlan.MIN_SPAWN_Y, NetherStartPlan.MAX_SPAWN_Y)
                .unit(Unit.Y_LEVEL)
                .customizeExposed()
                .doc("Target Y for the safe-site search.")
                .build(),
            Setting.group("chest", chest)
                .render(chest::summary)
                .doc("The starter chest's difficulty tier and easy/medium/hard kit contents.")
                .build(),
            Setting.<NetherStartConfig>flag("forceCapsule", c -> c.forceCapsule, (c, v) -> c.forceCapsule = v)
                .doc("Whether to always build the guaranteed capsule instead of only falling back to it.")
                .build(),
            Setting.<NetherStartConfig, StarterCapsuleConfig>section(
                    "capsule", c -> c.capsule, (c, v) -> c.capsule = v, capsuleSchema
                )
                .render(capsuleSchema::summary)
                .doc("The capsule's shape and lighting, Nether-appropriate defaults.")
                .build()
        );
    }
}
