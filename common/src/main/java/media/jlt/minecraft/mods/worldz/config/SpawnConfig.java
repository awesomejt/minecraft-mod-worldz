package media.jlt.minecraft.mods.worldz.config;

import media.jlt.minecraft.mods.worldz.logic.SpawnStrategy;

/** Configuration for how a newly created world chooses its layout origin and spawn (DESIGN §18). */
public final class SpawnConfig {
    /** How the layout origin and initial spawn are chosen. */
    public SpawnStrategy strategy = SpawnStrategy.STARTER_AT_ORIGIN;

    /** Creates the backward-compatible default. */
    public SpawnConfig() {
    }
}
