package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.LayoutConfig;
import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.logic.BiomeListSpec;
import media.jlt.minecraft.mods.worldz.logic.BiomeRole;
import media.jlt.minecraft.mods.worldz.logic.BiomeRoles;
import media.jlt.minecraft.mods.worldz.logic.LayoutMode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Schema for {@link LayoutConfig} (DESIGN §17; TODO 25.2d) -- the coordinated world-layout terrain
 * composition. The one section in this sub-step whose sanitize order genuinely diverges from its
 * {@code layoutMap}/read order (DESIGN §41.1's ordering invariant does not universally hold):
 * {@code sanitizeLayout} validates {@code roleOverrides} <em>third</em> (right after {@code
 * biomes}), but {@code layoutMap} emits it <em>last</em>. Reusing per-setting rules for {@code
 * regionScale}/{@code singleBiome}/{@code roleOverrides} in declaration order would silently
 * reorder their WARN lines relative to the original for any input that trips more than one of
 * them, so all three -- plus the final mode-vs-roles cross-check -- are pushed into
 * {@link #postValidate}, executed by hand in {@code sanitizeLayout}'s exact original sequence.
 * Only {@code mode} (a plain null-fallback, position 1 in both orders) and {@code biomes} (weighted
 * list, position 2 in both orders) stay ordinary per-setting rules.
 *
 * <p>Summary is overridden (DESIGN §41.6's table): gated on {@code mode == LEGACY} with a
 * conditional trailing segment, and {@code roleOverrides} never appears in it at all -- not
 * mechanically derivable.
 */
public final class LayoutSchema extends SchemaSection<LayoutConfig> {
    public LayoutSchema(String path) {
        super(path, LayoutConfig::new);
    }

    @Override
    protected List<Setting<LayoutConfig, ?>> declare() {
        return List.of(
            Setting.<LayoutConfig, LayoutMode>enumeration(
                    "mode", c -> c.mode, (c, v) -> c.mode = v,
                    LayoutMode::parse, LayoutMode::serializedName, LayoutMode.LEGACY
                )
                .doc("Layout mode; legacy preserves pre-Phase-15 climate-filter-only behavior.")
                .build(),
            Setting.<LayoutConfig>stringList("biomes", c -> c.biomes, (c, v) -> c.biomes = v)
                .rule(new Rule.WeightedBiomeIdList<>("Ignoring invalid " + path() + " biome '{}'.", null, null))
                .unit(Unit.BIOME_ID)
                .doc("Weighted candidate biome ids, id or id@weight; tags are not accepted.")
                .build(),
            Setting.<LayoutConfig>integer("regionScale", c -> c.regionScaleBlocks, (c, v) -> c.regionScaleBlocks = v)
                .unit(Unit.BLOCKS)
                .rangeText(WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS + ".." + WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS)
                .doc("Grid-cell edge length in blocks.")
                .build(),
            Setting.<LayoutConfig>text("singleBiome", c -> c.singleBiome, (c, v) -> c.singleBiome = v)
                .unit(Unit.BIOME_ID)
                .doc("SINGLE_BIOME mode only: the one biome id filling the world.")
                .build(),
            Setting.<LayoutConfig>stringMap("roleOverrides", c -> c.roleOverrides, (c, v) -> c.roleOverrides = v)
                .doc("Explicit biome id to role (land/ocean/beach) overrides.")
                .build()
        );
    }

    /**
     * Hand-ordered to match {@code sanitizeLayout}'s exact tail (DESIGN R9-style divergence between
     * sanitize order and map order, class Javadoc): {@code roleOverrides} validation, then the
     * {@code regionScale} clamp, then {@code singleBiome}'s id-or-empty check, then the
     * mode-vs-roles fallback -- in that order, so any input tripping more than one produces the
     * same WARN sequence as today.
     */
    @Override
    protected void postValidate(LayoutConfig value, SanitizeContext ctx) {
        Map<String, String> validOverrides = new LinkedHashMap<>();
        if (value.roleOverrides != null) {
            value.roleOverrides.forEach((rawId, rawRole) -> {
                BiomeListSpec idSpec = BiomeListSpec.parse(List.of(rawId == null ? "" : rawId));
                if (idSpec.entries().size() != 1 || idSpec.entries().getFirst().tag()) {
                    ctx.logger().warn("Ignoring {} roleOverrides entry with an invalid biome id '{}'.", path(), rawId);
                    return;
                }
                try {
                    BiomeRole role = BiomeRole.parse(rawRole);
                    validOverrides.put(idSpec.entries().getFirst().id(), role.serializedName());
                } catch (IllegalArgumentException exception) {
                    ctx.logger().warn("Ignoring {} roleOverrides entry for '{}' with an invalid role '{}'.", path(), rawId, rawRole);
                }
            });
        }
        value.roleOverrides = validOverrides;

        int clampedScale = Math.clamp(
            value.regionScaleBlocks, WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS, WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS
        );
        if (clampedScale != value.regionScaleBlocks) {
            ctx.logger().warn("Clamped {}.regionScale from {} to {}.", path(), value.regionScaleBlocks, clampedScale);
        }
        value.regionScaleBlocks = clampedScale;

        value.singleBiome = value.singleBiome == null ? "" : value.singleBiome.trim();
        if (!value.singleBiome.isEmpty()) {
            BiomeListSpec singleSpec = BiomeListSpec.parse(List.of(value.singleBiome));
            if (singleSpec.entries().size() != 1 || singleSpec.entries().getFirst().tag()) {
                ctx.logger().warn("Ignoring invalid {} singleBiome '{}'.", path(), value.singleBiome);
                value.singleBiome = "";
            } else {
                value.singleBiome = singleSpec.entries().getFirst().id();
            }
        }

        Map<String, BiomeRole> overrides = new LinkedHashMap<>();
        value.roleOverrides.forEach((id, role) -> overrides.put(id, BiomeRole.parse(role)));
        boolean hasOcean = value.biomes.stream()
            .anyMatch(entry -> BiomeRoles.resolve(stripWeight(entry), overrides) == BiomeRole.OCEAN);
        boolean hasLand = value.biomes.stream()
            .anyMatch(entry -> BiomeRoles.resolve(stripWeight(entry), overrides) == BiomeRole.LAND);
        boolean unsupported = switch (value.mode) {
            case OCEAN -> !hasOcean;
            case SINGLE_BIOME -> value.singleBiome.isEmpty();
            case CHAOS -> !hasLand;
            case VOID, LEGACY -> false;
            // STRIP_BANDS is strip_world-only (GOALS 36): the generic preset's layout: section has
            // no field for an ordered band sequence, so it can never resolve here.
            case STRIP_BANDS -> true;
        };
        if (unsupported) {
            ctx.logger().warn(
                "Layout mode '{}' has no usable biomes for its required role(s); using legacy mode instead.",
                value.mode.serializedName()
            );
            value.mode = LayoutMode.LEGACY;
        }
    }

    private static String stripWeight(String configValue) {
        int at = configValue.lastIndexOf('@');
        return at < 0 ? configValue : configValue.substring(0, at);
    }

    /**
     * Overridden: gated on {@code mode == LEGACY} ({@code "<legacy>"}), {@code roleOverrides} never
     * appears, and {@code singleBiome} is a conditional trailing segment -- not mechanically
     * derivable.
     */
    @Override
    public String summary(LayoutConfig value) {
        if (value.mode == LayoutMode.LEGACY) {
            return "<legacy>";
        }
        return value.mode.serializedName()
            + ", biomes=" + value.biomes
            + ", regionScale=" + value.regionScaleBlocks
            + (value.singleBiome.isEmpty() ? "" : ", singleBiome=" + value.singleBiome);
    }
}
