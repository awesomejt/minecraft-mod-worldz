package media.jlt.minecraft.mods.worldz;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

/** Fabric loader entrypoint. */
public final class WorldzFabric implements ModInitializer {
    /** Creates the Fabric entrypoint. */
    public WorldzFabric() {
    }

    @Override
    public void onInitialize() {
        WorldzCommon.init(FabricLoader.getInstance().getConfigDir());
    }
}
