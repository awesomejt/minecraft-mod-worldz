package media.jlt.minecraft.mods.worldz;

import net.fabricmc.api.ModInitializer;

public final class WorldzFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        WorldzCommon.init();
    }
}
