package media.jlt.minecraft.mods.worldz;

import media.jlt.minecraft.mods.worldz.worldgen.LimitedBiomeSource;
import net.fabricmc.api.ModInitializer;
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
    }
}
