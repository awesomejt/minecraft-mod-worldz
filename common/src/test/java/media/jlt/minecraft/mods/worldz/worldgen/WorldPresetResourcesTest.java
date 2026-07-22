package media.jlt.minecraft.mods.worldz.worldgen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldPresetResourcesTest {
    @Test
    void overworldUsesEnvelopedLimitedBiomeSourceWithVanillaNoiseDelegate() throws IOException {
        JsonObject dimensions = resource("/data/jlt_worldz/worldgen/world_preset/worldz.json")
            .getAsJsonObject("dimensions");
        assertEquals(Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"), dimensions.keySet());
        JsonObject overworld = dimensions.getAsJsonObject("minecraft:overworld");
        JsonObject generator = overworld.getAsJsonObject("generator");
        JsonObject delegate = generator.getAsJsonObject("delegate");
        JsonObject biomeSource = delegate.getAsJsonObject("biome_source");

        assertEquals("minecraft:overworld", overworld.get("type").getAsString());
        assertEquals("jlt_worldz:enveloped", generator.get("type").getAsString());
        assertEquals("overworld", generator.get("dimension").getAsString());
        assertEquals("minecraft:noise", delegate.get("type").getAsString());
        assertEquals("minecraft:overworld", delegate.get("settings").getAsString());
        assertFalse(generator.has("exterior"));
        assertEquals(1, biomeSource.size());
        assertEquals("jlt_worldz:limited", biomeSource.get("type").getAsString());
    }

    @Test
    void netherIsEnvelopedWhileEndRemainsVanilla() throws IOException {
        JsonObject dimensions = resource("/data/jlt_worldz/worldgen/world_preset/worldz.json")
            .getAsJsonObject("dimensions");

        JsonObject endGenerator = dimensions.getAsJsonObject("minecraft:the_end").getAsJsonObject("generator");
        assertEquals("minecraft:end", endGenerator.get("settings").getAsString());
        assertEquals("minecraft:the_end", endGenerator.getAsJsonObject("biome_source").get("type").getAsString());

        JsonObject netherGenerator = dimensions.getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator");
        JsonObject delegate = netherGenerator.getAsJsonObject("delegate");
        assertEquals("jlt_worldz:enveloped", netherGenerator.get("type").getAsString());
        assertEquals("nether", netherGenerator.get("dimension").getAsString());
        assertEquals("minecraft:nether", delegate.get("settings").getAsString());
        assertEquals("minecraft:multi_noise", delegate.getAsJsonObject("biome_source").get("type").getAsString());
        assertEquals("minecraft:nether", delegate.getAsJsonObject("biome_source").get("preset").getAsString());
    }

    @Test
    void singleBiomePresetMirrorsWorldzButFlagsWorldType() throws IOException {
        JsonObject dimensions = resource("/data/jlt_worldz/worldgen/world_preset/single_biome.json")
            .getAsJsonObject("dimensions");
        assertEquals(Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"), dimensions.keySet());
        JsonObject biomeSource = dimensions.getAsJsonObject("minecraft:overworld").getAsJsonObject("generator")
            .getAsJsonObject("delegate").getAsJsonObject("biome_source");

        assertEquals(2, biomeSource.size());
        assertEquals("jlt_worldz:limited", biomeSource.get("type").getAsString());
        assertEquals("single_biome", biomeSource.get("world_type").getAsString());

        JsonObject netherGenerator = dimensions.getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator");
        assertEquals("jlt_worldz:enveloped", netherGenerator.get("type").getAsString());
        JsonObject endGenerator = dimensions.getAsJsonObject("minecraft:the_end").getAsJsonObject("generator");
        assertEquals("minecraft:the_end", endGenerator.getAsJsonObject("biome_source").get("type").getAsString());
    }

    @Test
    void normalPresetTagAppendsWorldz() throws IOException {
        JsonObject tag = resource("/data/minecraft/tags/worldgen/world_preset/normal.json");

        assertFalse(tag.get("replace").getAsBoolean());
        assertEquals(12, tag.getAsJsonArray("values").size());
        assertEquals("jlt_worldz:worldz", tag.getAsJsonArray("values").get(0).getAsString());
        assertEquals("jlt_worldz:single_biome", tag.getAsJsonArray("values").get(1).getAsString());
        assertEquals("jlt_worldz:chaos_biomes", tag.getAsJsonArray("values").get(2).getAsString());
        assertEquals("jlt_worldz:strip_world", tag.getAsJsonArray("values").get(3).getAsString());
        assertEquals("jlt_worldz:ocean_island", tag.getAsJsonArray("values").get(4).getAsString());
        assertEquals("jlt_worldz:sky_island", tag.getAsJsonArray("values").get(5).getAsString());
        assertEquals("jlt_worldz:sky_chunk", tag.getAsJsonArray("values").get(6).getAsString());
        assertEquals("jlt_worldz:cave", tag.getAsJsonArray("values").get(7).getAsString());
        assertEquals("jlt_worldz:nether_start", tag.getAsJsonArray("values").get(8).getAsString());
        assertEquals("jlt_worldz:end_start", tag.getAsJsonArray("values").get(9).getAsString());
        assertEquals("jlt_worldz:flat", tag.getAsJsonArray("values").get(10).getAsString());
        assertEquals("jlt_worldz:deep_flat", tag.getAsJsonArray("values").get(11).getAsString());
    }

    @Test
    void chaosBiomesPresetMirrorsWorldzButFlagsWorldType() throws IOException {
        JsonObject dimensions = resource("/data/jlt_worldz/worldgen/world_preset/chaos_biomes.json")
            .getAsJsonObject("dimensions");
        assertEquals(Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"), dimensions.keySet());
        JsonObject biomeSource = dimensions.getAsJsonObject("minecraft:overworld").getAsJsonObject("generator")
            .getAsJsonObject("delegate").getAsJsonObject("biome_source");

        assertEquals(2, biomeSource.size());
        assertEquals("jlt_worldz:limited", biomeSource.get("type").getAsString());
        assertEquals("chaos_biomes", biomeSource.get("world_type").getAsString());

        JsonObject netherGenerator = dimensions.getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator");
        assertEquals("jlt_worldz:enveloped", netherGenerator.get("type").getAsString());
        JsonObject endGenerator = dimensions.getAsJsonObject("minecraft:the_end").getAsJsonObject("generator");
        assertEquals("minecraft:the_end", endGenerator.getAsJsonObject("biome_source").get("type").getAsString());
    }

    @Test
    void stripWorldPresetMirrorsWorldzButFlagsWorldType() throws IOException {
        JsonObject dimensions = resource("/data/jlt_worldz/worldgen/world_preset/strip_world.json")
            .getAsJsonObject("dimensions");
        assertEquals(Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"), dimensions.keySet());
        JsonObject biomeSource = dimensions.getAsJsonObject("minecraft:overworld").getAsJsonObject("generator")
            .getAsJsonObject("delegate").getAsJsonObject("biome_source");

        assertEquals(2, biomeSource.size());
        assertEquals("jlt_worldz:limited", biomeSource.get("type").getAsString());
        assertEquals("strip_world", biomeSource.get("world_type").getAsString());

        JsonObject netherGenerator = dimensions.getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator");
        assertEquals("jlt_worldz:enveloped", netherGenerator.get("type").getAsString());
        JsonObject endGenerator = dimensions.getAsJsonObject("minecraft:the_end").getAsJsonObject("generator");
        assertEquals("minecraft:the_end", endGenerator.getAsJsonObject("biome_source").get("type").getAsString());
    }

    @Test
    void oceanIslandPresetMirrorsWorldzButFlagsWorldType() throws IOException {
        JsonObject dimensions = resource("/data/jlt_worldz/worldgen/world_preset/ocean_island.json")
            .getAsJsonObject("dimensions");
        assertEquals(Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"), dimensions.keySet());
        JsonObject biomeSource = dimensions.getAsJsonObject("minecraft:overworld").getAsJsonObject("generator")
            .getAsJsonObject("delegate").getAsJsonObject("biome_source");

        assertEquals(2, biomeSource.size());
        assertEquals("jlt_worldz:limited", biomeSource.get("type").getAsString());
        assertEquals("ocean_island", biomeSource.get("world_type").getAsString());

        JsonObject netherGenerator = dimensions.getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator");
        assertEquals("jlt_worldz:enveloped", netherGenerator.get("type").getAsString());
        JsonObject endGenerator = dimensions.getAsJsonObject("minecraft:the_end").getAsJsonObject("generator");
        assertEquals("minecraft:the_end", endGenerator.getAsJsonObject("biome_source").get("type").getAsString());
    }

    @Test
    void skyIslandPresetMirrorsWorldzButFlagsWorldType() throws IOException {
        JsonObject dimensions = resource("/data/jlt_worldz/worldgen/world_preset/sky_island.json")
            .getAsJsonObject("dimensions");
        assertEquals(Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"), dimensions.keySet());
        JsonObject biomeSource = dimensions.getAsJsonObject("minecraft:overworld").getAsJsonObject("generator")
            .getAsJsonObject("delegate").getAsJsonObject("biome_source");

        assertEquals(2, biomeSource.size());
        assertEquals("jlt_worldz:limited", biomeSource.get("type").getAsString());
        assertEquals("sky_island", biomeSource.get("world_type").getAsString());

        JsonObject netherGenerator = dimensions.getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator");
        assertEquals("jlt_worldz:enveloped", netherGenerator.get("type").getAsString());
        JsonObject endGenerator = dimensions.getAsJsonObject("minecraft:the_end").getAsJsonObject("generator");
        assertEquals("minecraft:the_end", endGenerator.getAsJsonObject("biome_source").get("type").getAsString());
    }

    @Test
    void skyChunkPresetMirrorsWorldzButFlagsWorldTypeAndWrapsTheEnd() throws IOException {
        JsonObject dimensions = resource("/data/jlt_worldz/worldgen/world_preset/sky_chunk.json")
            .getAsJsonObject("dimensions");
        assertEquals(Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"), dimensions.keySet());
        JsonObject biomeSource = dimensions.getAsJsonObject("minecraft:overworld").getAsJsonObject("generator")
            .getAsJsonObject("delegate").getAsJsonObject("biome_source");

        assertEquals(2, biomeSource.size());
        assertEquals("jlt_worldz:limited", biomeSource.get("type").getAsString());
        assertEquals("sky_chunk", biomeSource.get("world_type").getAsString());

        JsonObject overworldGenerator = dimensions.getAsJsonObject("minecraft:overworld").getAsJsonObject("generator");
        assertEquals("overworld", overworldGenerator.get("dimension").getAsString());
        JsonObject netherGenerator = dimensions.getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator");
        assertEquals("jlt_worldz:enveloped", netherGenerator.get("type").getAsString());
        assertEquals("nether", netherGenerator.get("dimension").getAsString());
        // Unlike every other typed preset (sky_island included), the End is also wrapped here --
        // DESIGN §29.5's finding that GOALS 09/37's chunk-island toggle needs it, unlike sky
        // island's own Phase 10.5 End-skip.
        JsonObject endGenerator = dimensions.getAsJsonObject("minecraft:the_end").getAsJsonObject("generator");
        assertEquals("jlt_worldz:enveloped", endGenerator.get("type").getAsString());
        assertEquals("end", endGenerator.get("dimension").getAsString());
        assertEquals(
            "minecraft:the_end",
            endGenerator.getAsJsonObject("delegate").getAsJsonObject("biome_source").get("type").getAsString()
        );
    }

    @Test
    void cavePresetMirrorsWorldzButFlagsWorldType() throws IOException {
        JsonObject dimensions = resource("/data/jlt_worldz/worldgen/world_preset/cave.json")
            .getAsJsonObject("dimensions");
        assertEquals(Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"), dimensions.keySet());
        JsonObject overworldGenerator = dimensions.getAsJsonObject("minecraft:overworld").getAsJsonObject("generator");
        JsonObject biomeSource = overworldGenerator.getAsJsonObject("delegate").getAsJsonObject("biome_source");

        assertEquals(2, biomeSource.size());
        assertEquals("jlt_worldz:limited", biomeSource.get("type").getAsString());
        assertEquals("cave", biomeSource.get("world_type").getAsString());
        // Unlike every other typed preset, cave's own CavePlan is never read from
        // LimitedBiomeSource (DESIGN §30.1) -- the outer enveloped generator carries its own
        // "world_type" hint instead (EnvelopedChunkGenerator.resolve's cave-plan fallback).
        assertEquals("cave", overworldGenerator.get("world_type").getAsString());

        JsonObject netherGenerator = dimensions.getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator");
        assertEquals("jlt_worldz:enveloped", netherGenerator.get("type").getAsString());
        JsonObject endGenerator = dimensions.getAsJsonObject("minecraft:the_end").getAsJsonObject("generator");
        assertEquals("minecraft:the_end", endGenerator.getAsJsonObject("biome_source").get("type").getAsString());
    }

    @Test
    void netherStartPresetFlagsWorldTypeOnTheNetherSideNotTheOverworld() throws IOException {
        JsonObject dimensions = resource("/data/jlt_worldz/worldgen/world_preset/nether_start.json")
            .getAsJsonObject("dimensions");
        assertEquals(Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"), dimensions.keySet());
        JsonObject overworldGenerator = dimensions.getAsJsonObject("minecraft:overworld").getAsJsonObject("generator");
        JsonObject biomeSource = overworldGenerator.getAsJsonObject("delegate").getAsJsonObject("biome_source");

        assertEquals(2, biomeSource.size());
        assertEquals("jlt_worldz:limited", biomeSource.get("type").getAsString());
        assertEquals("nether_start", biomeSource.get("world_type").getAsString());
        // Unlike cave (whose CavePlan attaches to the Overworld), Nether-start's own
        // NetherStartPlan attaches to the Nether generator (DESIGN §31.5) -- so unlike cave.json,
        // the Overworld's own enveloped generator carries no "world_type" hint of its own here.
        assertFalse(overworldGenerator.has("world_type"));

        JsonObject netherGenerator = dimensions.getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator");
        assertEquals("jlt_worldz:enveloped", netherGenerator.get("type").getAsString());
        assertEquals("nether_start", netherGenerator.get("world_type").getAsString());
        JsonObject endGenerator = dimensions.getAsJsonObject("minecraft:the_end").getAsJsonObject("generator");
        assertEquals("minecraft:the_end", endGenerator.getAsJsonObject("biome_source").get("type").getAsString());
    }

    @Test
    void endStartPresetFlagsWorldTypeOnTheEndSideNotTheOverworldOrNether() throws IOException {
        JsonObject dimensions = resource("/data/jlt_worldz/worldgen/world_preset/end_start.json")
            .getAsJsonObject("dimensions");
        assertEquals(Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"), dimensions.keySet());
        JsonObject overworldGenerator = dimensions.getAsJsonObject("minecraft:overworld").getAsJsonObject("generator");
        JsonObject biomeSource = overworldGenerator.getAsJsonObject("delegate").getAsJsonObject("biome_source");

        assertEquals(2, biomeSource.size());
        assertEquals("jlt_worldz:limited", biomeSource.get("type").getAsString());
        assertEquals("end_start", biomeSource.get("world_type").getAsString());
        // Unlike cave/nether_start, end_start's own EndStartPlan attaches to the End generator
        // (DESIGN §32.3) -- so neither the Overworld's nor the Nether's own enveloped generator
        // carries a "world_type" hint of its own here.
        assertFalse(overworldGenerator.has("world_type"));

        JsonObject netherGenerator = dimensions.getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator");
        assertEquals("jlt_worldz:enveloped", netherGenerator.get("type").getAsString());
        assertFalse(netherGenerator.has("world_type"));

        JsonObject endGenerator = dimensions.getAsJsonObject("minecraft:the_end").getAsJsonObject("generator");
        assertEquals("jlt_worldz:enveloped", endGenerator.get("type").getAsString());
        assertEquals("end", endGenerator.get("dimension").getAsString());
        assertEquals("end_start", endGenerator.get("world_type").getAsString());
        assertEquals("minecraft:the_end", endGenerator.getAsJsonObject("delegate").getAsJsonObject("biome_source").get("type").getAsString());
    }

    @Test
    void flatPresetFlagsWorldTypeOnTheOverworldGeneratorAndBiomeSource() throws IOException {
        JsonObject dimensions = resource("/data/jlt_worldz/worldgen/world_preset/flat.json")
            .getAsJsonObject("dimensions");
        assertEquals(Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"), dimensions.keySet());
        JsonObject overworldGenerator = dimensions.getAsJsonObject("minecraft:overworld").getAsJsonObject("generator");
        JsonObject biomeSource = overworldGenerator.getAsJsonObject("delegate").getAsJsonObject("biome_source");

        assertEquals(2, biomeSource.size());
        assertEquals("jlt_worldz:limited", biomeSource.get("type").getAsString());
        assertEquals("flat", biomeSource.get("world_type").getAsString());
        // Like cave (and unlike nether_start/end_start), FlatPlan attaches to the *Overworld's*
        // own enveloped generator (DESIGN §33.2) -- so the outer generator carries its own
        // "world_type" hint too, not just the biome_source.
        assertEquals("flat", overworldGenerator.get("world_type").getAsString());

        JsonObject netherGenerator = dimensions.getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator");
        assertEquals("jlt_worldz:enveloped", netherGenerator.get("type").getAsString());
        assertFalse(netherGenerator.has("world_type"));
        JsonObject endGenerator = dimensions.getAsJsonObject("minecraft:the_end").getAsJsonObject("generator");
        assertEquals("minecraft:the_end", endGenerator.getAsJsonObject("biome_source").get("type").getAsString());
    }

    @Test
    void deepFlatPresetFlagsWorldTypeOnTheOverworldGeneratorAndBiomeSource() throws IOException {
        JsonObject dimensions = resource("/data/jlt_worldz/worldgen/world_preset/deep_flat.json")
            .getAsJsonObject("dimensions");
        assertEquals(Set.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"), dimensions.keySet());
        JsonObject overworldGenerator = dimensions.getAsJsonObject("minecraft:overworld").getAsJsonObject("generator");
        JsonObject biomeSource = overworldGenerator.getAsJsonObject("delegate").getAsJsonObject("biome_source");

        assertEquals(2, biomeSource.size());
        assertEquals("jlt_worldz:limited", biomeSource.get("type").getAsString());
        assertEquals("deep_flat", biomeSource.get("world_type").getAsString());
        // Like cave/flat (and unlike nether_start/end_start), DeepFlatPlan attaches to the
        // *Overworld's* own enveloped generator (DESIGN §33.4).
        assertEquals("deep_flat", overworldGenerator.get("world_type").getAsString());

        JsonObject netherGenerator = dimensions.getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator");
        assertEquals("jlt_worldz:enveloped", netherGenerator.get("type").getAsString());
        assertFalse(netherGenerator.has("world_type"));
        JsonObject endGenerator = dimensions.getAsJsonObject("minecraft:the_end").getAsJsonObject("generator");
        assertEquals("minecraft:the_end", endGenerator.getAsJsonObject("biome_source").get("type").getAsString());
    }

    @Test
    void languageFileCoversPresetAndCustomizationScreens() throws IOException {
        JsonObject language = resource("/assets/jlt_worldz/lang/en_us.json");

        assertEquals("Worldz", language.get("generator.jlt_worldz.worldz").getAsString());
        assertEquals("Customize Worldz", language.get("jlt_worldz.customize.title").getAsString());
        assertTrue(language.has("jlt_worldz.customize.allowed_biomes"));
        assertTrue(language.has("jlt_worldz.customize.starter_land.title"));
        assertTrue(language.has("jlt_worldz.customize.starter_land.transition"));
        assertTrue(language.has("jlt_worldz.customize.starter_land.foundation"));
        assertTrue(language.has("jlt_worldz.customize.overworld_border.title"));
        assertTrue(language.has("jlt_worldz.customize.nether_border.title"));
        assertTrue(language.has("jlt_worldz.customize.ensure_end_portal"));
        assertTrue(language.has("jlt_worldz.customize.ensure_blaze_access"));
        assertTrue(language.has("jlt_worldz.customize.border.resize_rate_blocks"));
        assertTrue(language.has("jlt_worldz.customize.border.resize_rate_days"));
        assertTrue(language.has("jlt_worldz.customize.border.resize_delay_days"));
        assertTrue(language.has("jlt_worldz.customize.overworld_exterior.title"));
        assertTrue(language.has("jlt_worldz.customize.nether_exterior.title"));
        assertTrue(language.has("jlt_worldz.customize.exterior.mode.ocean"));
        assertTrue(language.has("jlt_worldz.customize.exterior.mode.void"));
        assertTrue(language.has("jlt_worldz.single_biome.allow_rivers"));
        assertTrue(language.has("jlt_worldz.single_biome.allow_oceans"));
        assertEquals("Worldz: Chaos Biomes", language.get("generator.jlt_worldz.chaos_biomes").getAsString());
        assertTrue(language.has("jlt_worldz.chaos_biomes.title"));
        assertTrue(language.has("jlt_worldz.chaos_biomes.biomes"));
        assertTrue(language.has("jlt_worldz.chaos_biomes.region_scale"));
        assertTrue(language.has("jlt_worldz.chaos_biomes.starter_biome"));
        assertTrue(language.has("jlt_worldz.chaos_biomes.starter_radius"));
        assertTrue(language.has("jlt_worldz.chaos_biomes.allow_rivers"));
        assertTrue(language.has("jlt_worldz.chaos_biomes.allow_oceans"));
        assertEquals("Worldz: Strip World", language.get("generator.jlt_worldz.strip_world").getAsString());
        assertTrue(language.has("jlt_worldz.strip_world.title"));
        assertTrue(language.has("jlt_worldz.strip_world.width_radius"));
        assertTrue(language.has("jlt_worldz.strip_world.width_mode"));
        assertTrue(language.has("jlt_worldz.strip_world.apply_to_nether"));
        assertEquals("Worldz: Ocean Island", language.get("generator.jlt_worldz.ocean_island").getAsString());
        assertTrue(language.has("jlt_worldz.ocean_island.title"));
        assertTrue(language.has("jlt_worldz.ocean_island.island_source"));
        assertTrue(language.has("jlt_worldz.ocean_island.island_source.artificial"));
        assertTrue(language.has("jlt_worldz.ocean_island.island_source.natural"));
        assertTrue(language.has("jlt_worldz.ocean_island.island_source.chest_boat"));
        assertTrue(language.has("jlt_worldz.ocean_island.fluid"));
        assertTrue(language.has("jlt_worldz.ocean_island.fluid.water"));
        assertTrue(language.has("jlt_worldz.ocean_island.fluid.lava"));
        assertTrue(language.has("jlt_worldz.ocean_island.fluid.none"));
        assertTrue(language.has("jlt_worldz.ocean_island.island_biome"));
        assertTrue(language.has("jlt_worldz.ocean_island.radius"));
        assertTrue(language.has("jlt_worldz.ocean_island.shape_amplitude"));
        assertTrue(language.has("jlt_worldz.ocean_island.shore_width"));
        assertTrue(language.has("jlt_worldz.ocean_island.exclusion_zone_enabled"));
        assertEquals("Worldz: Sky Island", language.get("generator.jlt_worldz.sky_island").getAsString());
        assertTrue(language.has("jlt_worldz.sky_island.title"));
        assertTrue(language.has("jlt_worldz.sky_island.island_biome"));
        assertTrue(language.has("jlt_worldz.sky_island.radius"));
        assertTrue(language.has("jlt_worldz.sky_island.shape_amplitude"));
        assertTrue(language.has("jlt_worldz.sky_island.surface_y"));
        assertTrue(language.has("jlt_worldz.sky_island.thickness"));
        assertTrue(language.has("jlt_worldz.sky_island.chest_tier"));
        assertTrue(language.has("jlt_worldz.sky_island.chest_tier.easy"));
        assertTrue(language.has("jlt_worldz.sky_island.chest_tier.medium"));
        assertTrue(language.has("jlt_worldz.sky_island.chest_tier.hard"));
        assertTrue(language.has("jlt_worldz.sky_island.apply_to_nether"));
        assertEquals("Worldz: Sky Chunk", language.get("generator.jlt_worldz.sky_chunk").getAsString());
        assertTrue(language.has("jlt_worldz.sky_chunk.title"));
        assertTrue(language.has("jlt_worldz.sky_chunk.spawn_chance"));
        assertTrue(language.has("jlt_worldz.sky_chunk.cell_size_chunks"));
        assertTrue(language.has("jlt_worldz.sky_chunk.top_only"));
        assertTrue(language.has("jlt_worldz.sky_chunk.top_only_depth"));
        assertTrue(language.has("jlt_worldz.sky_chunk.exclusion_zone_enabled"));
        assertTrue(language.has("jlt_worldz.sky_chunk.exclusion_zone_radius"));
        assertTrue(language.has("jlt_worldz.sky_chunk.scattered_top_only_chance"));
        assertTrue(language.has("jlt_worldz.sky_chunk.apply_to_nether"));
        assertTrue(language.has("jlt_worldz.sky_chunk.apply_to_end"));
    }

    @Test
    void chunkIslandShowcaseStructureTagListsExpectedStructures() throws IOException {
        JsonObject tag = resource("/data/jlt_worldz/tags/worldgen/structure/chunk_island_showcase.json");

        assertFalse(tag.get("replace").getAsBoolean());
        assertEquals(2, tag.getAsJsonArray("values").size());
        assertEquals("minecraft:ancient_city", tag.getAsJsonArray("values").get(0).getAsString());
        assertEquals("minecraft:trial_chambers", tag.getAsJsonArray("values").get(1).getAsString());
    }

    private static JsonObject resource(String path) throws IOException {
        try (InputStream stream = WorldPresetResourcesTest.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IOException("Missing classpath resource " + path);
            }
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }
}
