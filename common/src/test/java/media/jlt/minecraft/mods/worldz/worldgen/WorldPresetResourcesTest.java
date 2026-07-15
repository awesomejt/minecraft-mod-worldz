package media.jlt.minecraft.mods.worldz.worldgen;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WorldPresetResourcesTest {
    @Test
    void overworldUsesBareLimitedBiomeSourceWithVanillaNoiseSettings() throws IOException {
        JsonObject dimensions = resource("/data/jlt_worldz/worldgen/world_preset/worldz.json")
            .getAsJsonObject("dimensions");
        JsonObject overworld = dimensions.getAsJsonObject("minecraft:overworld");
        JsonObject generator = overworld.getAsJsonObject("generator");
        JsonObject biomeSource = generator.getAsJsonObject("biome_source");

        assertEquals("minecraft:overworld", overworld.get("type").getAsString());
        assertEquals("minecraft:noise", generator.get("type").getAsString());
        assertEquals("minecraft:overworld", generator.get("settings").getAsString());
        assertEquals(1, biomeSource.size());
        assertEquals("jlt_worldz:limited", biomeSource.get("type").getAsString());
    }

    @Test
    void netherAndEndRemainVanilla() throws IOException {
        JsonObject dimensions = resource("/data/jlt_worldz/worldgen/world_preset/worldz.json")
            .getAsJsonObject("dimensions");

        JsonObject endGenerator = dimensions.getAsJsonObject("minecraft:the_end").getAsJsonObject("generator");
        assertEquals("minecraft:end", endGenerator.get("settings").getAsString());
        assertEquals("minecraft:the_end", endGenerator.getAsJsonObject("biome_source").get("type").getAsString());

        JsonObject netherGenerator = dimensions.getAsJsonObject("minecraft:the_nether").getAsJsonObject("generator");
        assertEquals("minecraft:nether", netherGenerator.get("settings").getAsString());
        assertEquals("minecraft:multi_noise", netherGenerator.getAsJsonObject("biome_source").get("type").getAsString());
        assertEquals("minecraft:nether", netherGenerator.getAsJsonObject("biome_source").get("preset").getAsString());
    }

    @Test
    void normalPresetTagAppendsWorldz() throws IOException {
        JsonObject tag = resource("/data/minecraft/tags/worldgen/world_preset/normal.json");

        assertFalse(tag.get("replace").getAsBoolean());
        assertEquals("jlt_worldz:worldz", tag.getAsJsonArray("values").get(0).getAsString());
    }

    @Test
    void languageFileUsesVerifiedWorldPresetKey() throws IOException {
        JsonObject language = resource("/assets/jlt_worldz/lang/en_us.json");

        assertEquals("Worldz", language.get("generator.jlt_worldz.worldz").getAsString());
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
