package media.jlt.minecraft.mods.worldz;

import media.jlt.minecraft.mods.worldz.client.CavePresetEditor;
import media.jlt.minecraft.mods.worldz.client.ChaosBiomesPresetEditor;
import media.jlt.minecraft.mods.worldz.client.DeepFlatPresetEditor;
import media.jlt.minecraft.mods.worldz.client.EndStartPresetEditor;
import media.jlt.minecraft.mods.worldz.client.FlatPresetEditor;
import media.jlt.minecraft.mods.worldz.client.NetherStartPresetEditor;
import media.jlt.minecraft.mods.worldz.client.OceanIslandPresetEditor;
import media.jlt.minecraft.mods.worldz.client.SingleBiomePresetEditor;
import media.jlt.minecraft.mods.worldz.client.SkyChunkPresetEditor;
import media.jlt.minecraft.mods.worldz.client.SkyIslandPresetEditor;
import media.jlt.minecraft.mods.worldz.client.StackedPresetEditor;
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
        event.register(SkyChunkPresetEditor.SKY_CHUNK_PRESET, SkyChunkPresetEditor.INSTANCE);
        event.register(CavePresetEditor.CAVE_PRESET, CavePresetEditor.INSTANCE);
        event.register(NetherStartPresetEditor.NETHER_START_PRESET, NetherStartPresetEditor.INSTANCE);
        event.register(EndStartPresetEditor.END_START_PRESET, EndStartPresetEditor.INSTANCE);
        event.register(FlatPresetEditor.FLAT_PRESET, FlatPresetEditor.INSTANCE);
        event.register(DeepFlatPresetEditor.DEEP_FLAT_PRESET, DeepFlatPresetEditor.INSTANCE);
        event.register(StackedPresetEditor.STACKED_PRESET, StackedPresetEditor.INSTANCE);
    }
}
