package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.logic.BorderSchedule;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.border.WorldBorder;

/** Applies the border plan baked into a new Worldz world's biome source. */
public final class WorldLimitManager {
    private WorldLimitManager() {
    }

    /**
     * Initializes both dimension borders once, after the server has loaded.
     * Vanilla persists and advances each resulting border transition.
     *
     * @param server newly started logical server
     */
    public static void onServerStarted(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        BiomeSource source = overworld.getChunkSource().getGenerator().getBiomeSource();
        if (!(source instanceof LimitedBiomeSource limitedSource)) {
            return;
        }

        WorldLimitPlan plan = limitedSource.worldLimits();
        if (!plan.enabled()) {
            return;
        }

        WorldLimitState existing = overworld.getDataStorage().get(WorldLimitState.TYPE);
        if (existing != null && existing.initialized()) {
            return;
        }

        apply(overworld, plan.overworld(), "overworld");
        ProgressionGuarantees.ensureEndPortal(overworld, plan.overworld());
        ServerLevel nether = server.getLevel(Level.NETHER);
        if (nether != null) {
            apply(nether, plan.nether(), "Nether");
            ProgressionGuarantees.ensureBlazeAccess(nether, plan.nether());
        }
        overworld.getDataStorage().set(WorldLimitState.TYPE, new WorldLimitState(true));
    }

    private static void apply(ServerLevel level, WorldLimitPlan.DimensionLimit limit, String dimensionName) {
        if (!limit.enabled()) {
            return;
        }

        BorderSchedule schedule = limit.schedule();
        WorldBorder border = level.getWorldBorder();
        border.setCenter(0.0, 0.0);
        if (schedule.durationTicks() == 0L) {
            border.setSize(schedule.finalDiameterBlocks());
        } else {
            border.setSize(schedule.initialDiameterBlocks());
            border.lerpSizeBetween(
                schedule.initialDiameterBlocks(),
                schedule.finalDiameterBlocks(),
                schedule.durationTicks(),
                level.getGameTime()
            );
        }
        WorldzCommon.LOGGER.info(
            "Applied {} border: initial radius {}, final radius {}, resize days {}.",
            dimensionName, limit.initialRadiusBlocks(), limit.finalRadiusBlocks(), limit.resizeDays()
        );
    }
}
