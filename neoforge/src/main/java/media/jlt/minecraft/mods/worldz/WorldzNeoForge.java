package media.jlt.minecraft.mods.worldz;

import com.mojang.serialization.MapCodec;
import media.jlt.minecraft.mods.worldz.worldgen.LimitedBiomeSource;
import media.jlt.minecraft.mods.worldz.worldgen.EnvelopedChunkGenerator;
import media.jlt.minecraft.mods.worldz.worldgen.SpawnOriginManager;
import media.jlt.minecraft.mods.worldz.worldgen.WorldHazardManager;
import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.storage.LevelData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** NeoForge loader entrypoint. */
@Mod(WorldzCommon.MOD_ID)
public final class WorldzNeoForge {
    private static final DeferredRegister<MapCodec<? extends BiomeSource>> BIOME_SOURCES =
        DeferredRegister.create(Registries.BIOME_SOURCE, WorldzCommon.MOD_ID);
    private static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
        DeferredRegister.create(Registries.CHUNK_GENERATOR, WorldzCommon.MOD_ID);

    /**
     * Initializes Worldz and attaches future registrations to the mod event bus.
     *
     * @param modBus this mod's event bus
     */
    public WorldzNeoForge(IEventBus modBus) {
        WorldzCommon.init(FMLPaths.CONFIGDIR.get());
        BIOME_SOURCES.register("limited", () -> LimitedBiomeSource.CODEC);
        BIOME_SOURCES.register(modBus);
        CHUNK_GENERATORS.register("enveloped", () -> EnvelopedChunkGenerator.CODEC);
        CHUNK_GENERATORS.register(modBus);
        if (FMLEnvironment.getDist() == Dist.CLIENT) {
            WorldzNeoForgeClient.register(modBus);
        }
        NeoForge.EVENT_BUS.addListener(WorldzNeoForge::onServerStarted);
        NeoForge.EVENT_BUS.addListener(WorldzNeoForge::onServerTick);
        NeoForge.EVENT_BUS.addListener(WorldzNeoForge::onLevelLoad);
        NeoForge.EVENT_BUS.addListener(WorldzNeoForge::onCreateSpawnPosition);
        NeoForge.EVENT_BUS.addListener(WorldzNeoForge::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(WorldzNeoForge::onChunkUnload);
    }

    private static void onServerStarted(ServerStartedEvent event) {
        WorldLimitManager.onServerStarted(event.getServer());
        WorldHazardManager.onServerStarted(event.getServer());
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        WorldLimitManager.onServerTick(event.getServer());
        WorldHazardManager.onServerTick(event.getServer());
    }

    private static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level) {
            WorldHazardManager.onChunkLoad(level, event.getChunk());
        }
    }

    private static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel() instanceof ServerLevel level) {
            WorldHazardManager.onChunkUnload(level, event.getChunk());
        }
    }

    private static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel() instanceof ServerLevel level && level.dimension() == Level.OVERWORLD) {
            SpawnOriginManager.reapplyPersistedOrigin(level);
        }
    }

    private static void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        SpawnOriginManager.resolveFreshOrigin(level).ifPresent(pos -> {
            event.getSettings().setSpawn(LevelData.RespawnData.of(level.dimension(), pos, 0.0F, 0.0F));
            event.setCanceled(true);
        });
    }
}
