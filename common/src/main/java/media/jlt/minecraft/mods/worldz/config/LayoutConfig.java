package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.LayoutMode;
import media.jlt.minecraft.mods.worldz.logic.WorldLayoutPlan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Configuration for the coordinated world-layout terrain composition (DESIGN §17). */
public final class LayoutConfig {
    /** Layout mode; {@code legacy} preserves pre-Phase-15 climate-filter-only behavior. */
    public LayoutMode mode = LayoutMode.LEGACY;
    /**
     * Weighted candidate biome ids, {@code id} or {@code id@weight}. Tags are not
     * accepted here: each entry's role is resolved per concrete id (see
     * {@code roleOverrides} and the maintained default mapping).
     */
    public List<String> biomes = new ArrayList<>();
    /** Grid-cell edge length in blocks. */
    public int regionScaleBlocks = WorldLayoutPlan.DEFAULT_REGION_SCALE_BLOCKS;
    /** {@code SINGLE_BIOME} only: the one biome id filling the world. */
    public String singleBiome = "";
    /** Explicit biome id to role ({@code land}/{@code ocean}/{@code beach}) overrides. */
    public Map<String, String> roleOverrides = new LinkedHashMap<>();

    /** Creates the disabled, backward-compatible legacy default. */
    public LayoutConfig() {
    }
}
