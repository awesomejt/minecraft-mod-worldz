package media.jlt.minecraft.mods.worldz.logic;

/**
 * Pure vertical/material classification for a sky island's slab (GOALS 05, DESIGN §27.2/27.3).
 * Because a sky island's chunk never runs the delegate's biome-aware surface builder (its whole
 * footprint is synthesized directly, exactly like {@code OCEAN}/{@code VOID} exteriors always
 * have been), this class supplies the block choice vanilla's surface pass would otherwise have
 * made. {@link #familyFor} is a deliberately non-exhaustive approximation -- a handful of
 * substring/id checks covering desert-, snowy-, mushroom-, and water-family biomes, not a
 * reimplementation of vanilla's real per-biome {@code SurfaceRules} -- good enough to make a
 * desert sky island read as sand rather than grass without chasing every one of vanilla's
 * overworld biomes.
 */
public final class SkyIslandProfile {
    /** Depth of the top+subsoil shell, in blocks, measured down from {@code surfaceY}. */
    private static final int SHELL_DEPTH_BLOCKS = 3;

    private SkyIslandProfile() {
    }

    /** One vertical layer of the slab. */
    public enum Layer {
        /** Above the surface or below the slab's bottom -- open air. */
        VOID,
        /** The single topmost solid block. */
        TOP,
        /** A thin subsoil shell just below the top block. */
        SUBSOIL,
        /** The slab's solid core. */
        CORE
    }

    /** A coarse biome-family classification driving the slab's block palette. */
    public enum BiomeFamily {
        /** Grass-block-over-dirt, the plains-like default. */
        DEFAULT,
        /** Sand-over-sandstone. */
        DESERT,
        /** Snow-block-over-dirt. */
        SNOWY,
        /** Mycelium-over-dirt. */
        MUSHROOM,
        /** Packed-ice-over-dirt (Jason, 2026-07-25): a floating island can't literally hold
         * standing water, so ocean/river/swamp real biomes (naturalBiome, DESIGN §28.4) get a
         * frozen-over top instead of silently reading as indistinguishable grass. */
        WATER
    }

    /**
     * Classifies one block's layer within the slab.
     *
     * @param y block Y
     * @param surfaceY the island's configured walkable surface Y (first air block above the slab)
     * @param thicknessBlocks how many blocks of solid ground extend below {@code surfaceY}
     * @return the layer at this Y
     */
    public static Layer layerAt(int y, int surfaceY, int thicknessBlocks) {
        if (y < surfaceY - thicknessBlocks || y >= surfaceY) {
            return Layer.VOID;
        }
        if (y == surfaceY - 1) {
            return Layer.TOP;
        }
        if (y >= surfaceY - SHELL_DEPTH_BLOCKS) {
            return Layer.SUBSOIL;
        }
        return Layer.CORE;
    }

    /**
     * Classifies a configured biome id into a coarse family for the slab's block palette.
     *
     * @param biomeId the island's configured biome id
     * @return the closest matching family, or {@link BiomeFamily#DEFAULT} otherwise
     */
    public static BiomeFamily familyFor(String biomeId) {
        String id = biomeId == null ? "" : biomeId;
        if (id.contains("mushroom")) {
            return BiomeFamily.MUSHROOM;
        }
        // Checked before the snowy family's own "frozen" substring below, so frozen_ocean/
        // frozen_river (real water biomes) land here instead -- only frozen_peaks (a mountain
        // biome, no "ocean"/"river"/"swamp" substring) falls through to snowy.
        if (id.contains("ocean") || id.contains("river") || id.contains("swamp")) {
            return BiomeFamily.WATER;
        }
        if (id.contains("desert") || id.contains("badlands") || id.contains("beach")) {
            return BiomeFamily.DESERT;
        }
        if (id.contains("snowy") || id.contains("ice_spikes") || id.contains("frozen") || id.contains("grove")) {
            return BiomeFamily.SNOWY;
        }
        return BiomeFamily.DEFAULT;
    }
}
