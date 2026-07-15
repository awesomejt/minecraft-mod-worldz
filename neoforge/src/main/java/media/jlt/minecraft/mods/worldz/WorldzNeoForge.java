package media.jlt.minecraft.mods.worldz;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(WorldzCommon.MOD_ID)
public final class WorldzNeoForge {
    public WorldzNeoForge(IEventBus modBus) {
        WorldzCommon.init();
    }
}
