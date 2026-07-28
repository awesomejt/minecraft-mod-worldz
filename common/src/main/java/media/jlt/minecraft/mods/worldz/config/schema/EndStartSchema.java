package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.EndStartConfig;
import media.jlt.minecraft.mods.worldz.config.StarterCapsuleConfig;
import media.jlt.minecraft.mods.worldz.config.StarterKitConfig;
import media.jlt.minecraft.mods.worldz.logic.EndStartPlan;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;

import java.util.List;

/**
 * Schema for {@link EndStartConfig} (GOALS 34, 41; DESIGN §32; TODO 25.2f). Same {@code
 * chestTier} + three-kit + nested capsule shape as {@link NetherStartSchema}, but simpler: unlike
 * Nether-start, {@link EndStartConfig} has no {@code spawnY} (the End's safe-site search has no
 * equivalent Y target) and no {@code forceCapsule} (the guaranteed platform is End-start's only
 * mode -- there is no natural safe-site search to prefer over it). The nested {@link
 * StarterCapsuleSchema} is parameterized with End-start's own bounds ({@link EndStartPlan}'s
 * {@code MIN}/{@code MAX_CAPSULE_*} constants), exactly the parameterization {@code
 * sanitizeEndStart} passes to {@code sanitizeStarterCapsule} today.
 *
 * <p>{@code readEndStartConfig}/{@code sanitizeEndStart}/{@code endStartMap}/{@code
 * endStartSummary} all process {@code chestTier}, the three kits and {@code capsule} in the same
 * order, so no {@link #postValidate} override is needed and the summary is fully derivable.
 */
public final class EndStartSchema extends SchemaSection<EndStartConfig> {
    private final StarterKitSchema easyKitSchema;
    private final StarterKitSchema mediumKitSchema;
    private final StarterKitSchema hardKitSchema;
    private final StarterCapsuleSchema capsuleSchema;

    public EndStartSchema(String path) {
        super(path, EndStartConfig::new);
        this.easyKitSchema = new StarterKitSchema(path() + ".easyKit");
        this.mediumKitSchema = new StarterKitSchema(path() + ".mediumKit");
        this.hardKitSchema = new StarterKitSchema(path() + ".hardKit");
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
            Setting.<EndStartConfig, StarterKitTier>enumeration(
                    "chestTier", c -> c.chestTier, (c, v) -> c.chestTier = v,
                    StarterKitTier::parse, StarterKitTier::serializedName, StarterKitTier.MEDIUM
                )
                .doc("Which of easyKit/mediumKit/hardKit the starter chest uses; every tier guarantees a pickaxe.")
                .build(),
            Setting.<EndStartConfig, StarterKitConfig>section(
                    "easyKit", c -> c.easyKit, (c, v) -> c.easyKit = v, easyKitSchema
                )
                .render(easyKitSchema::summary)
                .doc("Generous starter-chest contents: rockets, blocks, food, combat gear, and a copper pickaxe.")
                .build(),
            Setting.<EndStartConfig, StarterKitConfig>section(
                    "mediumKit", c -> c.mediumKit, (c, v) -> c.mediumKit = v, mediumKitSchema
                )
                .render(mediumKitSchema::summary)
                .doc("Middle-ground starter-chest contents: fewer rockets, lighter gear, a stone pickaxe.")
                .build(),
            Setting.<EndStartConfig, StarterKitConfig>section(
                    "hardKit", c -> c.hardKit, (c, v) -> c.hardKit = v, hardKitSchema
                )
                .render(hardKitSchema::summary)
                .doc("Bare-essentials starter-chest contents: no guaranteed rockets or weapon, just a wooden pickaxe.")
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
