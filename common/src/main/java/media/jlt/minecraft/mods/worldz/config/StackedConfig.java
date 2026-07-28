package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.ExteriorMode;

import java.util.ArrayList;
import java.util.List;

/**
 * Defaults for the {@code jlt_worldz:stacked} typed preset (GOAL 35, DESIGN §34.1), consulted
 * only when that preset resolves without explicit Customize-screen values. Each layer entry is
 * {@code "<biome>;<blocks>;<air gap>"} -- see {@code StackedLayerSpec}.
 */
public final class StackedConfig {
    /** Ordered bottom-to-top layer list: eight bands, deep taiga through surface plains
     * (DESIGN §34.7). */
    public List<String> layers = defaultLayers();
    /** Whether the configured layer order is shuffled, seeded off the real world seed. */
    public boolean seedRandomizedOrder = false;
    /** Overworld exterior half-width in chunks (DESIGN §34.10, TODO 25.5): whenever this is
     * nonzero *and* the user hasn't already configured their own {@code overworldBorder}/
     * {@code overworldExterior}, it supersedes the shared {@code overworldExterior} section for a
     * stacked world with a void wall at this radius. Before TODO 25.5 the second half of that
     * condition didn't exist -- the code could not tell "left at default" apart from "explicitly
     * configured the same shape stacked would have picked anyway", so *any* nonzero value
     * (including the untouched default, 4) always won, even over a config that explicitly set its
     * own {@code overworldBorder}/{@code overworldExterior} -- config 72's own never-customized
     * default (relying on exactly this coincidence for its own bounded-by-default acceptance test)
     * had to keep working, which rules out gating on this field's *own* presence; gating on the
     * sibling sections' presence instead (via {@link
     * media.jlt.minecraft.mods.worldz.config.WorldzConfig#present}) preserves config 72/99/101
     * unchanged while letting configs 73-76 drop their {@code worldSizeChunks: 0} opt-out
     * boilerplate, since their own explicit {@code overworldBorder}/{@code overworldExterior}
     * sections now speak for themselves. Zero remains the deliberate, full opt-out back to the
     * shared section regardless of presence (restoring this preset's pre-§34.7 unlimited-by-default
     * behavior) -- a literal zero radius would never be a valid void-wall boundary anyway ({@code
     * ExteriorPlan.DimensionEnvelope}'s own compact constructor rejects a non-normal mode paired
     * with a zero boundary), so honoring it any other way would only trade a silent override bug
     * for a crash. Deliberately does *not* also force the Overworld border on (§34.7's original
     * coupling, reverted by §34.10 at Jason's request): the platform's own void wall is
     * accessibility enough on its own -- a real border is opt-in via the shared {@code
     * overworldBorder} section, exactly like every other typed preset. Every resolution path
     * (Customize screen, direct config-driven world creation, and the codec's own
     * never-customized-preset fallback) must consult {@link #effectiveOverworldExterior} rather
     * than reading {@code overworldExterior} directly -- see DESIGN §34.7's own note on this being
     * fixed twice, once per bypassed path. */
    public int worldSizeChunks = 4;
    /** Maximum per-column height bump applied to each layer's own surface, traded out of that
     * layer's own air gap so biome-band boundaries never move (DESIGN §34.7). Zero restores the
     * pre-§34.7 perfectly flat layers. */
    public int reliefBlocks = 4;
    /** Whether a real vanilla village is always force-generated near spawn on the top layer's own
     * surface, provided its biome is village-compatible (DESIGN §34.9). Always forced, never a
     * natural-search-first attempt -- mirrors sky island's own guaranteed-village posture (GOALS
     * 07), since natural random-spread placement isn't reliable in a small, bounded stacked world. */
    public boolean forceTopVillage = false;

    /** Creates a config populated with defaults. */
    public StackedConfig() {
    }

    /**
     * Resolves the Overworld border this stacked world should actually use: always the passed-in
     * shared config, unchanged (DESIGN §34.10 -- {@link #worldSizeChunks} no longer forces a
     * border on; it only shapes {@link #effectiveOverworldExterior}). Kept as an explicit
     * pass-through, rather than having call sites read {@code WorldzConfig.overworldBorder}
     * directly, so a future preset-specific border default has one obvious place to land.
     *
     * @param configuredOverworldBorder the shared, preset-agnostic Overworld border config
     * @return the border to actually apply for this stacked world
     */
    public BorderConfig effectiveOverworldBorder(BorderConfig configuredOverworldBorder) {
        return configuredOverworldBorder;
    }

    /**
     * Resolves the Overworld exterior this stacked world should actually use: a {@code VOID} wall
     * at {@link #worldSizeChunks}'s own boundary when nonzero and the shared sections are
     * untouched (mirrors {@code StripConfig.widthMode}'s own "wall off with void" default
     * precedent), or the passed-in shared config unchanged otherwise (DESIGN §34.7, TODO 25.5). The
     * boundary is set explicitly here rather than left to derive from an enabled border (DESIGN
     * §34.10): since §34.7 no longer forces the Overworld border on, the void wall must carry its
     * own radius so a never-bordered stacked world still floats in void at the platform's own edge,
     * like a sky chunk. Every stacked-aware resolution path must call this instead of reading
     * {@code WorldzConfig.overworldExterior} directly.
     *
     * @param configuredOverworldExterior the shared, preset-agnostic Overworld exterior config
     * @param sharedOverworldConfigured whether the user has explicitly written their own {@code
     *     overworldBorder} or {@code overworldExterior} section (TODO 25.5, {@code
     *     WorldzConfig#present}) -- when true, that explicit shape always wins over this preset's
     *     own derived void wall, even when {@link #worldSizeChunks} is left at its (nonzero)
     *     default
     * @return the exterior to actually apply for this stacked world
     */
    public ExteriorConfig effectiveOverworldExterior(ExteriorConfig configuredOverworldExterior, boolean sharedOverworldConfigured) {
        if (this.worldSizeChunks <= 0 || sharedOverworldConfigured) {
            return configuredOverworldExterior;
        }
        ExteriorConfig derived = new ExteriorConfig();
        derived.mode = ExteriorMode.VOID;
        derived.boundaryRadiusBlocks = this.worldSizeChunks * 16;
        return derived;
    }

    private static List<String> defaultLayers() {
        List<String> list = new ArrayList<>();
        // Bottom layer stays full shorthand: real bedrock + deep ore-bearing stone (DESIGN
        // §34.7's own thickened bottom layer) isn't a biome's *standard* composition, only this
        // one layer's own deliberate choice -- StackedBiomeDefaults never bakes stack position
        // (bottom/top) into a biome's simplified defaults.
        list.add("minecraft:taiga;minecraft:bedrock:1,minecraft:stone:43;30");
        // The six middle layers use DESIGN §34.8's simplified bare-biome shorthand -- each
        // expands to that biome's own StackedBiomeDefaults composition (10 blocks) plus this
        // preset's own 30-block default air gap, reproducing exactly what was hand-written here
        // before §34.8 (dogfeeds the new shorthand in the shipped default).
        list.add("minecraft:desert");
        list.add("minecraft:badlands");
        list.add("minecraft:swamp");
        list.add("minecraft:jungle");
        list.add("minecraft:savanna");
        list.add("minecraft:snowy_taiga");
        // Top layer stays full shorthand: air gap 0 (open sky above the surface, no headroom
        // needed), unlike the simplified shorthand's own 30-block default.
        list.add("minecraft:plains;minecraft:stone:6,minecraft:dirt:3,minecraft:grass_block:1;0");
        return list;
    }
}
