package media.jlt.minecraft.mods.worldz;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;

/** NeoForge loader entrypoint. */
@Mod(WorldzCommon.MOD_ID)
public final class WorldzNeoForge {
    /**
     * Initializes Worldz and attaches future registrations to the mod event bus.
     *
     * @param modBus this mod's event bus
     */
    public WorldzNeoForge(IEventBus modBus) {
        WorldzCommon.init(FMLPaths.CONFIGDIR.get());
    }
}
