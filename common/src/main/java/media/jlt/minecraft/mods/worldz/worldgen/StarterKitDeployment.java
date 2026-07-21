package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.config.SkyIslandConfig;
import media.jlt.minecraft.mods.worldz.config.StarterKitConfig;
import media.jlt.minecraft.mods.worldz.logic.SkyIslandPlan;
import media.jlt.minecraft.mods.worldz.logic.SkyIslandProfile;
import media.jlt.minecraft.mods.worldz.logic.StarterKitPlan;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.vehicle.boat.AbstractChestBoat;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Spawns the GOALS 03 chest boat (DESIGN §25.3) for a land-free {@code ocean_island} world, and
 * the GOALS 05 necessities chest (DESIGN §27.8) for a {@code sky_island} world.
 */
final class StarterKitDeployment {
    private StarterKitDeployment() {
    }

    /**
     * Places a filled chest on top of the sky island at the world origin (GOALS 05, DESIGN
     * §27.8): the selected difficulty tier's essentials/extras, plus one guaranteed water-source
     * item chosen from the island's biome -- a water bucket for a dry (desert-family) biome, since
     * no rain will ever fill a cauldron there, or a cauldron otherwise (rain naturally fills it
     * over time). Called once, only for a new sky island world.
     *
     * @param overworld the Overworld server level
     * @param originX world spawn origin block X
     * @param originZ world spawn origin block Z
     * @param skyIsland the world's resolved sky island plan
     */
    static void spawnStarterChest(ServerLevel overworld, int originX, int originZ, SkyIslandPlan skyIsland) {
        overworld.getChunk(originX >> 4, originZ >> 4);
        BlockPos pos = new BlockPos(originX, skyIsland.surfaceY(), originZ);
        overworld.setBlock(pos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);

        StarterKitPlan plan = resolvePlan(tierConfig(WorldzCommon.config().skyIsland, skyIsland.chestTier()));
        List<StarterKitPlan.ItemAmount> essentials = new ArrayList<>(plan.essentials());
        essentials.add(waterSourceItem(skyIsland.islandBiome()));
        StarterKitPlan withWaterSource = new StarterKitPlan(essentials, plan.extras(), plan.extrasCount());
        List<StarterKitPlan.ItemAmount> resolved = withWaterSource.resolve(overworld.getSeed());

        BlockEntity blockEntity = overworld.getBlockEntity(pos);
        if (!(blockEntity instanceof ChestBlockEntity chest)) {
            WorldzCommon.LOGGER.warn("Could not create the GOALS 05 starter chest at {}.", pos);
            return;
        }
        int slot = 0;
        for (StarterKitPlan.ItemAmount amount : resolved) {
            if (slot >= chest.getContainerSize()) {
                break;
            }
            Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(amount.itemId()));
            chest.setItem(slot, new ItemStack(item, amount.count()));
            slot++;
        }
        WorldzCommon.LOGGER.info("Spawned the GOALS 05 starter chest at {}.", pos);
    }

    private static StarterKitConfig tierConfig(SkyIslandConfig config, StarterKitTier tier) {
        return switch (tier) {
            case EASY -> config.easyKit;
            case MEDIUM -> config.mediumKit;
            case HARD -> config.hardKit;
        };
    }

    /**
     * A dry (desert-family) biome never gets rain, so a cauldron there would never fill -- it
     * gets a ready-to-use water bucket instead, the only water this island's life will ever see.
     * Every other biome family gets a cauldron, since rain will fill it naturally over time.
     */
    private static StarterKitPlan.ItemAmount waterSourceItem(String islandBiome) {
        return SkyIslandProfile.familyFor(islandBiome) == SkyIslandProfile.BiomeFamily.DESERT
            ? new StarterKitPlan.ItemAmount("minecraft:water_bucket", 1)
            : new StarterKitPlan.ItemAmount("minecraft:cauldron", 1);
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

    /** Package-visible so {@link EnvelopedChunkGenerator}'s own scattered-island loot chests (GOALS 08, DESIGN §28.2) can reuse it. */
    static StarterKitPlan resolvePlan(StarterKitConfig config) {
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
