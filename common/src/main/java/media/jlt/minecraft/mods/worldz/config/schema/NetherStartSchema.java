package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.NetherStartConfig;
import media.jlt.minecraft.mods.worldz.config.StarterCapsuleConfig;
import media.jlt.minecraft.mods.worldz.config.StarterKitConfig;
import media.jlt.minecraft.mods.worldz.logic.NetherStartPlan;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;

import java.util.List;

/**
 * Schema for {@link NetherStartConfig} (GOALS 27, 41.1; DESIGN §31.9; TODO 25.2f). Proves the
 * shared {@code chestTier} + three-kit shape (also in {@link CaveSchema}, {@link
 * media.jlt.minecraft.mods.worldz.config.SkyIslandConfig}) alongside a nested {@link
 * StarterCapsuleSchema} parameterized with Nether-start's own bounds ({@link NetherStartPlan}'s
 * {@code MIN}/{@code MAX_CAPSULE_*} constants), exactly the parameterization {@code
 * sanitizeNetherStart} passes to {@code sanitizeStarterCapsule} today.
 *
 * <p>{@code readNetherStartConfig}/{@code sanitizeNetherStart}/{@code netherStartMap}/{@code
 * netherStartSummary} all process {@code spawnY}, {@code chestTier}, the three kits, {@code
 * forceCapsule} and {@code capsule} in the same order, so no {@link #postValidate} override is
 * needed and the summary is fully derivable.
 */
public final class NetherStartSchema extends SchemaSection<NetherStartConfig> {
    private final StarterKitSchema easyKitSchema;
    private final StarterKitSchema mediumKitSchema;
    private final StarterKitSchema hardKitSchema;
    private final StarterCapsuleSchema capsuleSchema;

    public NetherStartSchema(String path) {
        super(path, NetherStartConfig::new);
        this.easyKitSchema = new StarterKitSchema(path() + ".easyKit");
        this.mediumKitSchema = new StarterKitSchema(path() + ".mediumKit");
        this.hardKitSchema = new StarterKitSchema(path() + ".hardKit");
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
                .doc("Target Y for the safe-site search.")
                .build(),
            Setting.<NetherStartConfig, StarterKitTier>enumeration(
                    "chestTier", c -> c.chestTier, (c, v) -> c.chestTier = v,
                    StarterKitTier::parse, StarterKitTier::serializedName, StarterKitTier.MEDIUM
                )
                .doc("Which of easyKit/mediumKit/hardKit the starter chest uses.")
                .build(),
            Setting.<NetherStartConfig, StarterKitConfig>section(
                    "easyKit", c -> c.easyKit, (c, v) -> c.easyKit = v, easyKitSchema
                )
                .render(easyKitSchema::summary)
                .doc("Generous starter-chest contents: a full portal, ready to use.")
                .build(),
            Setting.<NetherStartConfig, StarterKitConfig>section(
                    "mediumKit", c -> c.mediumKit, (c, v) -> c.mediumKit = v, mediumKitSchema
                )
                .render(mediumKitSchema::summary)
                .doc("Middle-ground starter-chest contents: a full frame's worth of obsidian, no ignition.")
                .build(),
            Setting.<NetherStartConfig, StarterKitConfig>section(
                    "hardKit", c -> c.hardKit, (c, v) -> c.hardKit = v, hardKitSchema
                )
                .render(hardKitSchema::summary)
                .doc("Bare-essentials starter-chest contents: no guaranteed obsidian at all.")
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
