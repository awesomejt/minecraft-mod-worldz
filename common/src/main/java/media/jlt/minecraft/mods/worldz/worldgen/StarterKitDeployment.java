package media.jlt.minecraft.mods.worldz.worldgen;

import media.jlt.minecraft.mods.worldz.WorldzCommon;
import media.jlt.minecraft.mods.worldz.config.CaveConfig;
import media.jlt.minecraft.mods.worldz.config.NetherStartConfig;
import media.jlt.minecraft.mods.worldz.config.SkyIslandConfig;
import media.jlt.minecraft.mods.worldz.config.StarterKitConfig;
import media.jlt.minecraft.mods.worldz.logic.CavePlan;
import media.jlt.minecraft.mods.worldz.logic.NetherStartPlan;
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
     * §27.8): the selected difficulty tier's essentials/extras, plus guaranteed biome-driven
     * items chosen from the island's biome and tier (see {@link #biomeEssentialItems}). Called
     * once, only for a new sky island world.
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
        essentials.addAll(biomeEssentialItems(skyIsland.islandBiome(), skyIsland.chestTier()));
        StarterKitPlan withBiomeItems = new StarterKitPlan(essentials, plan.extras(), plan.extrasCount());
        List<StarterKitPlan.ItemAmount> resolved = withBiomeItems.resolve(overworld.getSeed());

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
     * Places a filled chest set into the floor directly beneath the resolved underground spawn
     * position (GOALS 25's "optionally with a starter chest", DESIGN §30.3) -- the same "replace
     * solid ground within the already-validated safe area" placement {@link #spawnStarterChest}
     * uses, rather than an adjacent column, since {@code searchCaveCavity}'s floor/headroom check
     * only validates the spawn column itself, not its neighbors. Filled from the selected
     * difficulty tier's essentials/extras. Unlike {@link #spawnStarterChest}, there is no
     * biome-driven water-source item -- cave has no biome concept of its own to key off (DESIGN
     * §30.1: full vanilla biome variety, not a restricted island biome). Called once, only for a
     * new cave world with the chest option enabled.
     *
     * @param overworld the Overworld server level
     * @param spawnPos the actual resolved underground spawn position
     * @param cave the world's resolved cave plan
     */
    static void spawnCaveStarterChest(ServerLevel overworld, BlockPos spawnPos, CavePlan cave) {
        BlockPos pos = spawnPos.below();
        overworld.setBlock(pos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);

        StarterKitPlan plan = resolvePlan(tierConfig(WorldzCommon.config().cave, cave.chestTier()));
        List<StarterKitPlan.ItemAmount> resolved = plan.resolve(overworld.getSeed());

        BlockEntity blockEntity = overworld.getBlockEntity(pos);
        if (!(blockEntity instanceof ChestBlockEntity chest)) {
            WorldzCommon.LOGGER.warn("Could not create the GOALS 25 starter chest at {}.", pos);
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
        WorldzCommon.LOGGER.info("Spawned the GOALS 25 cave starter chest at {}.", pos);
    }

    private static StarterKitConfig tierConfig(CaveConfig config, StarterKitTier tier) {
        return switch (tier) {
            case EASY -> config.easyKit;
            case MEDIUM -> config.mediumKit;
            case HARD -> config.hardKit;
        };
    }

    /**
     * Places a filled chest set into the floor directly beneath the resolved safe Nether-start
     * site (GOALS 27, DESIGN §31.6) -- the same "replace solid ground within the already-
     * validated safe area" placement {@link #spawnCaveStarterChest} uses. Filled from the selected
     * difficulty tier's essentials/extras; no biome-driven item the way {@link #spawnStarterChest}
     * has -- Nether-start has no biome concept of its own to key off (single fixed dimension).
     * Called once, only for a new {@code nether_start} world.
     *
     * @param nether the Nether server level
     * @param site the resolved safe spawn site (DESIGN §31.4)
     * @param netherStart the world's resolved Nether-start plan
     */
    static void spawnNetherStartChest(ServerLevel nether, BlockPos site, NetherStartPlan netherStart) {
        BlockPos pos = site.below();
        nether.setBlock(pos, Blocks.CHEST.defaultBlockState(), Block.UPDATE_ALL);

        StarterKitPlan plan = resolvePlan(tierConfig(WorldzCommon.config().netherStart, netherStart.chestTier()));
        List<StarterKitPlan.ItemAmount> resolved = plan.resolve(nether.getSeed());

        BlockEntity blockEntity = nether.getBlockEntity(pos);
        if (!(blockEntity instanceof ChestBlockEntity chest)) {
            WorldzCommon.LOGGER.warn("Could not create the GOALS 27 starter chest at {}.", pos);
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
        WorldzCommon.LOGGER.info("Spawned the GOALS 27 Nether-start starter chest at {}.", pos);
    }

    private static StarterKitConfig tierConfig(NetherStartConfig config, StarterKitTier tier) {
        return switch (tier) {
            case EASY -> config.easyKit;
            case MEDIUM -> config.mediumKit;
            case HARD -> config.hardKit;
        };
    }

    /**
     * Biome- and tier-driven essentials on top of the configured kit (DESIGN §27.8, beatability
     * follow-up from real in-game testing of config 41). Every non-desert-family biome already
     * has natural dirt in its own slab (grass/snow/mycelium-over-dirt, §27.3) and eventual rain,
     * so it only needs a cauldron. A desert-family biome has neither -- sand-over-sandstone all
     * the way down, and no rain ever -- so on top of the water item it also needs a guaranteed
     * dirt allotment (otherwise the kit's own saplings have nowhere plantable, since sand cannot
     * hold a sapling) and, below hard tier, wheat seeds (there is no tall grass to break for them
     * either, since sky islands suppress vegetation entirely, §27.2). Hard tier deliberately omits
     * seeds and gets only a single-use water bucket like medium -- it is meant to lean on mob
     * drops (zombies/husks can drop carrots and potatoes) and, once GOALS 08's floating islands
     * are enabled, bridging to a rainy biome for a cauldron, same "harder but still beatable"
     * posture as every other hard-tier kit in this project. Easy tier gets 2 ice blocks instead of
     * a single bucket -- melted (desert biomes are warm enough) into two adjacent water sources,
     * a real infinite supply rather than one disposable bucket's worth.
     */
    private static List<StarterKitPlan.ItemAmount> biomeEssentialItems(String islandBiome, StarterKitTier tier) {
        if (SkyIslandProfile.familyFor(islandBiome) != SkyIslandProfile.BiomeFamily.DESERT) {
            return List.of(new StarterKitPlan.ItemAmount("minecraft:cauldron", 1));
        }
        List<StarterKitPlan.ItemAmount> items = new ArrayList<>();
        items.add(tier == StarterKitTier.EASY
            ? new StarterKitPlan.ItemAmount("minecraft:ice", 2)
            : new StarterKitPlan.ItemAmount("minecraft:water_bucket", 1));
        items.add(new StarterKitPlan.ItemAmount("minecraft:dirt", dirtCountFor(tier)));
        if (tier != StarterKitTier.HARD) {
            items.add(new StarterKitPlan.ItemAmount("minecraft:wheat_seeds", tier == StarterKitTier.EASY ? 4 : 2));
        }
        return items;
    }

    private static int dirtCountFor(StarterKitTier tier) {
        return switch (tier) {
            case EASY -> 6;
            case MEDIUM -> 4;
            case HARD -> 2;
        };
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
