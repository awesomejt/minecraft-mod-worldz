package media.jlt.minecraft.mods.worldz;

import media.jlt.minecraft.mods.worldz.client.ChaosBiomesPresetEditor;
import media.jlt.minecraft.mods.worldz.client.OceanIslandPresetEditor;
import media.jlt.minecraft.mods.worldz.client.SingleBiomePresetEditor;
import media.jlt.minecraft.mods.worldz.client.SkyIslandPresetEditor;
import media.jlt.minecraft.mods.worldz.client.StripWorldPresetEditor;
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
        event.register(SingleBiomePresetEditor.SINGLE_BIOME_PRESET, SingleBiomePresetEditor.INSTANCE);
        event.register(ChaosBiomesPresetEditor.CHAOS_BIOMES_PRESET, ChaosBiomesPresetEditor.INSTANCE);
        event.register(StripWorldPresetEditor.STRIP_WORLD_PRESET, StripWorldPresetEditor.INSTANCE);
        event.register(OceanIslandPresetEditor.OCEAN_ISLAND_PRESET, OceanIslandPresetEditor.INSTANCE);
        event.register(SkyIslandPresetEditor.SKY_ISLAND_PRESET, SkyIslandPresetEditor.INSTANCE);
    }
}
