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
    /** Overworld border/exterior half-width in chunks (DESIGN §34.7): whenever this is nonzero,
     * it always supersedes the shared {@code overworldBorder}/{@code overworldExterior} sections
     * for a stacked world -- plain config fields carry no "was this explicitly set" flag, so
     * there is no way to tell "left at default" apart from "explicitly configured the same shape
     * stacked would have picked anyway". Zero is the deliberate, full opt-out back to the shared
     * sections, restoring this preset's pre-§34.7 unlimited-by-default behavior. Every resolution
     * path (Customize screen, direct config-driven world creation, and the codec's own
     * never-customized-preset fallback) must consult {@link #effectiveOverworldBorder}/
     * {@link #effectiveOverworldExterior} rather than reading {@code overworldBorder}/
     * {@code overworldExterior} directly -- see DESIGN §34.7's own note on this being fixed twice,
     * once per bypassed path. */
    public int worldSizeChunks = 4;
    /** Maximum per-column height bump applied to each layer's own surface, traded out of that
     * layer's own air gap so biome-band boundaries never move (DESIGN §34.7). Zero restores the
     * pre-§34.7 perfectly flat layers. */
    public int reliefBlocks = 4;

    /** Creates a config populated with defaults. */
    public StackedConfig() {
    }

    /**
     * Resolves the Overworld border this stacked world should actually use: derived from {@link
     * #worldSizeChunks} when nonzero, or the passed-in shared config unchanged otherwise (DESIGN
     * §34.7). Every stacked-aware resolution path must call this instead of reading
     * {@code WorldzConfig.overworldBorder} directly.
     *
     * @param configuredOverworldBorder the shared, preset-agnostic Overworld border config
     * @return the border to actually apply for this stacked world
     */
    public BorderConfig effectiveOverworldBorder(BorderConfig configuredOverworldBorder) {
        if (this.worldSizeChunks <= 0) {
            return configuredOverworldBorder;
        }
        BorderConfig derived = new BorderConfig();
        derived.enabled = true;
        derived.initialRadiusBlocks = this.worldSizeChunks * 16;
        derived.finalRadiusBlocks = derived.initialRadiusBlocks;
        derived.ensureObjective = true;
        return derived;
    }

    /**
     * Resolves the Overworld exterior this stacked world should actually use: a {@code VOID} wall
     * at {@link #worldSizeChunks}'s own boundary when nonzero (mirrors {@code StripConfig
     * .widthMode}'s own "wall off with void" default precedent), or the passed-in shared config
     * unchanged otherwise (DESIGN §34.7). Every stacked-aware resolution path must call this
     * instead of reading {@code WorldzConfig.overworldExterior} directly.
     *
     * @param configuredOverworldExterior the shared, preset-agnostic Overworld exterior config
     * @return the exterior to actually apply for this stacked world
     */
    public ExteriorConfig effectiveOverworldExterior(ExteriorConfig configuredOverworldExterior) {
        if (this.worldSizeChunks <= 0) {
            return configuredOverworldExterior;
        }
        ExteriorConfig derived = new ExteriorConfig();
        derived.mode = ExteriorMode.VOID;
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
