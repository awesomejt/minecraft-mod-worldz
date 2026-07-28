package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.DeepFlatConfig;
import media.jlt.minecraft.mods.worldz.logic.DeepFlatPlan;

import java.util.List;

/**
 * Schema for {@link DeepFlatConfig} (GOAL 16, DESIGN §33.4) -- defaults for the {@code
 * jlt_worldz:deep_flat} typed preset, consulted only when it resolves without explicit
 * Customize-screen values. Summary is fully derivable: no gate, no relabeling.
 *
 * <p>{@code rivers: {enabled, exclusionRadius}} (TODO 25.6f, DESIGN §42.1's "three shapes F1
 * missed") is a group over this section's own {@link DeepFlatConfig} type -- there is only ever one
 * owner shape here, unlike {@link ExclusionZoneSchema}/{@link ChestSchema}, so it is private,
 * exactly like {@code CaveSchema.SealedSurfaceSchema}/{@code CavernSchema}.
 */
public final class DeepFlatSchema extends SchemaSection<DeepFlatConfig> {
    private final RiversSchema rivers;

    public DeepFlatSchema(String path) {
        super(path, DeepFlatConfig::new);
        this.rivers = new RiversSchema(path() + ".rivers");
    }

    @Override
    protected List<Setting<DeepFlatConfig, ?>> declare() {
        return List.of(
            Setting.<DeepFlatConfig>integer("surfaceY", c -> c.surfaceY, (c, v) -> c.surfaceY = v)
                .range(DeepFlatPlan.MIN_SURFACE_Y, DeepFlatPlan.MAX_SURFACE_Y)
                .unit(Unit.Y_LEVEL).preset("deep_flat").customizeExposed()
                .doc("The flat cap height -- everything above clears to air, real terrain below stays.")
                .build(),
            Setting.<DeepFlatConfig>stringList("capLayers", c -> c.capLayers, (c, v) -> c.capLayers = v)
                .rule(new Rule.EmptyListFallback<>(
                    () -> new DeepFlatConfig().capLayers, "deepFlat.capLayers was empty; using the default cap layer stack."
                ))
                .preset("deep_flat").customizeExposed()
                .doc("Land-cap layer stack, painted immediately below surfaceY.")
                .build(),
            Setting.group("rivers", rivers)
                .render(rivers::summary)
                .doc("Whether/how river and ocean columns get a water-surface cap instead of the land cap.")
                .build()
        );
    }

    /**
     * {@code deepFlat.rivers}: {@code enabled}/{@code exclusionRadius} (TODO 25.6f), a group over
     * {@link DeepFlatSchema}'s own {@link DeepFlatConfig} type.
     */
    private static final class RiversSchema extends SchemaSection<DeepFlatConfig> {
        RiversSchema(String path) {
            super(path, DeepFlatConfig::new);
        }

        @Override
        protected List<Setting<DeepFlatConfig, ?>> declare() {
            return List.of(
                Setting.<DeepFlatConfig>flag("enabled", c -> c.riversEnabled, (c, v) -> c.riversEnabled = v)
                    .preset("deep_flat").customizeExposed()
                    .doc("Whether a river/ocean biome column gets a water-surface cap instead of the land cap.")
                    .build(),
                Setting.<DeepFlatConfig>integer(
                        "exclusionRadius", c -> c.riverExclusionRadiusBlocks, (c, v) -> c.riverExclusionRadiusBlocks = v
                    )
                    .min(0)
                    .unit(Unit.BLOCKS).preset("deep_flat").customizeExposed()
                    .doc("Radius around the origin within which river/ocean columns always get the land cap.")
                    .build()
            );
        }
    }
}
