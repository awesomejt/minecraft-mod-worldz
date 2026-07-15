package media.jlt.minecraft.mods.worldz;

import media.jlt.minecraft.mods.worldz.client.WorldzPresetEditor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterPresetEditorsEvent;

/** Client-only NeoForge event registration. */
final class WorldzNeoForgeClient {
    private WorldzNeoForgeClient() {
    }

    static void register(IEventBus modBus) {
        modBus.addListener(WorldzNeoForgeClient::registerPresetEditor);
    }

    private static void registerPresetEditor(RegisterPresetEditorsEvent event) {
        event.register(WorldzPresetEditor.WORLDZ_PRESET, WorldzPresetEditor.INSTANCE);
    }
}
