package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.StripBandsConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

import java.util.List;

/**
 * Schema for {@link StripBandsConfig} (GOALS 36) -- the ordered biome-band sequence nested inside
 * {@code stripWorld.bands} (TODO 25.2d). Only reachable through {@link StripWorldSchema} in
 * production, but also registered on its own in both {@code LegacySections}/{@code SchemaSections}
 * (like {@code spawn}/{@code starterKit}, DESIGN §41.8) so the differential harness can exercise it
 * directly.
 *
 * <p>{@code biomes} rejects tags outright ({@code Rule.BiomeIdList.Mode#REJECT_TAGS}): a band
 * sequence needs concrete, walkable biome ids, not a tag's whole pool. Unlike every other
 * biome-list warning in this file, {@code sanitizeStripBands} takes no {@code name} parameter at
 * all -- its wording hardcodes {@code "stripWorld.bands"} literally, since the method has exactly
 * one call site. That happens to equal this section's own {@link #path()} (never the leaf-key
 * {@code fullPath} the framework would otherwise interpolate), so the warning templates below are
 * built from {@code path()} rather than relying on {@link Rule}'s generic {@code name} argument.
 */
public final class StripBandsSchema extends SchemaSection<StripBandsConfig> {
    public StripBandsSchema(String path) {
        super(path, StripBandsConfig::new);
    }

    @Override
    protected List<Setting<StripBandsConfig, ?>> declare() {
        return List.of(
            Setting.<StripBandsConfig>flag("enabled", c -> c.enabled, (c, v) -> c.enabled = v)
                .doc("Whether the strip passes through biome bands instead of ordinary vanilla terrain.")
                .build(),
            Setting.<StripBandsConfig>stringList("biomes", c -> c.biomes, (c, v) -> c.biomes = v)
                .rule(new Rule.BiomeIdList<>(
                    Rule.BiomeIdList.Mode.REJECT_TAGS,
                    "Ignoring invalid " + path() + " biome '{}'.",
                    "Ignoring " + path() + " biome tag '#{}'; bands require concrete biome ids.",
                    null, null
                ))
                .unit(Unit.BIOME_ID)
                .doc("Ordered land biome ids walked along the strip's length; repeats once exhausted.")
                .build(),
            Setting.<StripBandsConfig>integer("widthBlocks", c -> c.widthBlocks, (c, v) -> c.widthBlocks = v)
                .range(WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS, WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Band width in blocks along the strip's length axis.")
                .build(),
            Setting.<StripBandsConfig>flag("seedRandomOrder", c -> c.seedRandomOrder, (c, v) -> c.seedRandomOrder = v)
                .doc("Shuffle the sequence once (a fixed permutation) instead of using it as given.")
                .build(),
            Setting.<StripBandsConfig>flag("allowRivers", c -> c.allowRivers, (c, v) -> c.allowRivers = v)
                .doc("Let vanilla's own river biomes generate where vanilla would place one.")
                .build(),
            Setting.<StripBandsConfig>flag("allowOceans", c -> c.allowOceans, (c, v) -> c.allowOceans = v)
                .doc("Let vanilla's own river/ocean-family biomes generate naturally, additive over allowRivers.")
                .build(),
            Setting.<StripBandsConfig>flag("allowBeaches", c -> c.allowBeaches, (c, v) -> c.allowBeaches = v)
                .doc("Let vanilla's own beach/stony-shore biomes generate where vanilla would place one.")
                .build()
        );
    }

    /**
     * Cross-field check: an enabled band sequence with no usable biomes left after {@code biomes}'
     * own per-setting rule ran disables itself, matching {@code sanitizeStripBands}'s tail exactly.
     */
    @Override
    protected void postValidate(StripBandsConfig value, SanitizeContext ctx) {
        if (value.enabled && value.biomes.isEmpty()) {
            ctx.logger().warn("{}.enabled is set but has no usable biomes; disabling biome bands.", path());
            value.enabled = false;
        }
    }

    /**
     * Overridden: {@code enabled} gates the whole line and is itself excluded from the surviving
     * segments -- not mechanically derivable (no section-level "disabled" gate in the framework).
     */
    @Override
    public String summary(StripBandsConfig value) {
        if (!value.enabled) {
            return "<disabled>";
        }
        return "biomes=" + value.biomes
            + ", widthBlocks=" + value.widthBlocks
            + ", seedRandomOrder=" + value.seedRandomOrder
            + ", allowRivers=" + value.allowRivers
            + ", allowOceans=" + value.allowOceans
            + ", allowBeaches=" + value.allowBeaches;
    }
}
