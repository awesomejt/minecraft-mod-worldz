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
        assertEquals(6, tag.getAsJsonArray("values").size());
        assertEquals("jlt_worldz:worldz", tag.getAsJsonArray("values").get(0).getAsString());
        assertEquals("jlt_worldz:single_biome", tag.getAsJsonArray("values").get(1).getAsString());
        assertEquals("jlt_worldz:chaos_biomes", tag.getAsJsonArray("values").get(2).getAsString());
        assertEquals("jlt_worldz:strip_world", tag.getAsJsonArray("values").get(3).getAsString());
        assertEquals("jlt_worldz:ocean_island", tag.getAsJsonArray("values").get(4).getAsString());
        assertEquals("jlt_worldz:sky_island", tag.getAsJsonArray("values").get(5).getAsString());
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
