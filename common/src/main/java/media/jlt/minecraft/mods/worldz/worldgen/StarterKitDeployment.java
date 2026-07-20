package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.config.StarterKitConfig;
import media.jlt.minecraft.mods.worldz.logic.StarterKitPlan;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/** Spawns the GOALS 03 chest boat (DESIGN §25.3) for a land-free {@code ocean_island} world. */
final class StarterKitDeployment {
    private StarterKitDeployment() {
    }

    /**
     * Spawns an oak chest boat at the world origin, water surface, with a seed-resolved starter
     * kit. Called once, only for a land-free island (GOALS 03, {@code IslandSource.CHEST_BOAT}).
     *
     * @param overworld the Overworld server level
     * @param originX world spawn origin block X
     * @param originZ world spawn origin block Z
     */
    static void spawnChestBoat(ServerLevel overworld, int originX, int originZ) {
        overworld.getChunk(originX >> 4, originZ >> 4);
        int surfaceY = overworld.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, originX, originZ);

        AbstractChestBoat boat = EntityTypes.OAK_CHEST_BOAT.create(overworld, EntitySpawnReason.STRUCTURE);
        if (boat == null) {
            WorldzCommon.LOGGER.warn("Could not create the GOALS 03 starter chest boat at ({}, {}).", originX, originZ);
            return;
        }
        boat.setPos(originX + 0.5, surfaceY, originZ + 0.5);

        StarterKitPlan plan = resolvePlan(WorldzCommon.config().oceanIsland.starterKit);
        List<StarterKitPlan.ItemAmount> resolved = plan.resolve(overworld.getSeed());
        int slot = 0;
        for (StarterKitPlan.ItemAmount amount : resolved) {
            if (slot >= boat.getContainerSize()) {
                break;
            }
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(amount.itemId()));
            boat.setItem(slot, new ItemStack(item, amount.count()));
            slot++;
        }

        overworld.addFreshEntity(boat);
        WorldzCommon.LOGGER.info("Spawned the GOALS 03 starter chest boat at ({}, {}, {}).", originX, surfaceY, originZ);
    }

    private static StarterKitPlan resolvePlan(StarterKitConfig config) {
        List<StarterKitPlan.ItemAmount> essentials = new ArrayList<>();
        for (String raw : config.essentials) {
            essentials.add(StarterKitPlan.ItemAmount.parse(raw));
        }
        List<StarterKitPlan.ItemAmount> extras = new ArrayList<>();
        for (String raw : config.extras) {
            extras.add(StarterKitPlan.ItemAmount.parse(raw));
        }
        return new StarterKitPlan(essentials, extras, config.extrasCount);
    }
}
