package media.jlt.minecraft.mods.worldz;

import media.jlt.minecraft.mods.worldz.worldgen.LimitedBiomeSource;
import media.jlt.minecraft.mods.worldz.worldgen.EnvelopedChunkGenerator;
import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;

/** Fabric loader entrypoint. */
public final class WorldzFabric implements ModInitializer {
    /** Creates the Fabric entrypoint. */
    public WorldzFabric() {
    }

    @Override
    public void onInitialize() {
        WorldzCommon.init(FabricLoader.getInstance().getConfigDir());
        Registry.register(
            BuiltInRegistries.BIOME_SOURCE,
            Identifier.fromNamespaceAndPath(WorldzCommon.MOD_ID, "limited"),
            LimitedBiomeSource.CODEC
        );
        Registry.register(
            BuiltInRegistries.CHUNK_GENERATOR,
            Identifier.fromNamespaceAndPath(WorldzCommon.MOD_ID, "enveloped"),
            EnvelopedChunkGenerator.CODEC
        );
        ServerLifecycleEvents.SERVER_STARTED.register(WorldLimitManager::onServerStarted);
        ServerTickEvents.END_SERVER_TICK.register(WorldLimitManager::onServerTick);
    }
}
