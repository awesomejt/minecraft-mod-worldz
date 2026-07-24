package media.jlt.minecraft.mods.worldz.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Defaults for the "structures far from spawn" module (GOAL 24, DESIGN §36) -- a shared runtime
 * rule, not a typed preset, composable with any world type.
 */
public final class StructureDistanceConfig {
    /** Whether vanilla structure sets are held back from spawn at all. */
    public boolean enabled = false;
    /** Minimum block distance (Chebyshev, matching border/exclusion-zone convention) from spawn. */
    public int minDistanceBlocks = 2000;
    /**
     * Structure set ids (e.g. {@code minecraft:strongholds}) exempted from the restriction --
     * always allowed at their normal vanilla distance regardless of {@link #minDistanceBlocks}.
     */
    public List<String> exemptStructureSets = new ArrayList<>();

    /** Creates a config populated with defaults. */
    public StructureDistanceConfig() {
    }
}
