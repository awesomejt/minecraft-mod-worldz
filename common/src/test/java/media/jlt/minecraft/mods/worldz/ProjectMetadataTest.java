package media.jlt.minecraft.mods.worldz;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectMetadataTest {
    private static final Path ROOT = Path.of("..");

    @Test
    void declaredAndRepositoryLicenseIdentityIsConsistentlyMit() throws IOException {
        Properties properties = projectProperties();
        JsonObject fabricMetadata = JsonParser.parseString(Files.readString(
            ROOT.resolve("fabric/src/main/resources/fabric.mod.json")
        )).getAsJsonObject();
        String neoForgeMetadata = Files.readString(
            ROOT.resolve("neoforge/src/main/resources/META-INF/neoforge.mods.toml")
        );
        String license = Files.readString(ROOT.resolve("LICENSE"));

        assertEquals("MIT", properties.getProperty("license"));
        assertEquals("MIT", fabricMetadata.get("license").getAsString());
        assertTrue(neoForgeMetadata.contains("license = \"${license}\""));
        assertTrue(license.startsWith("MIT License\n\nCopyright (c) 2026 Jason Taylor"));
        assertFalse(license.contains("CC0 1.0"));
        assertTrue(Files.readString(
            ROOT.resolve("common/src/main/resources/META-INF/LICENSES/Apache-2.0.txt")
        ).contains("Apache License\n                           Version 2.0"));
        assertTrue(Files.readString(
            ROOT.resolve("common/src/main/resources/META-INF/NOTICE")
        ).contains("SnakeYAML 2.6"));
    }

    @Test
    void projectIdentityMatchesDesignContract() throws IOException {
        Properties properties = projectProperties();
        String settings = Files.readString(ROOT.resolve("settings.gradle"));

        assertTrue(settings.contains("rootProject.name = 'mod-worldz'"));
        assertEquals("media.jlt.minecraft.mods", properties.getProperty("group"));
        assertEquals("0.1.1", properties.getProperty("version"));
        assertEquals("jlt_worldz", properties.getProperty("mod_id"));
        assertEquals("JLT Worldz", properties.getProperty("mod_name"));
        assertEquals("25", properties.getProperty("java_version"));
        assertEquals("26.2", properties.getProperty("minecraft_version"));
    }

    @Test
    void canonicalConfigurationExampleIsYaml() throws IOException {
        String readme = Files.readString(ROOT.resolve("README.md"));
        String design = Files.readString(ROOT.resolve("DESIGN.md"));

        assertTrue(Files.isRegularFile(ROOT.resolve("config/jlt_worldz.example.yaml")));
        assertFalse(Files.exists(ROOT.resolve("config/jlt_worldz.example.json")));
        assertTrue(readme.contains("config/jlt_worldz.yaml"));
        assertFalse(readme.contains("config/jlt_worldz.example.json"));
        assertTrue(design.contains("Config `config/jlt_worldz.yaml`"));
    }

    @Test
    void clientPresetEditorIsDeclaredForFabricAndImplementedForNeoForge() throws IOException {
        JsonObject fabricMetadata = JsonParser.parseString(Files.readString(
            ROOT.resolve("fabric/src/main/resources/fabric.mod.json")
        )).getAsJsonObject();
        JsonObject mixinConfig = JsonParser.parseString(Files.readString(
            ROOT.resolve("fabric/src/main/resources/jlt_worldz.mixins.json")
        )).getAsJsonObject();

        assertEquals("jlt_worldz.mixins.json", fabricMetadata.getAsJsonArray("mixins").get(0).getAsString());
        assertEquals(
            "client.WorldCreationUiStateMixin",
            mixinConfig.getAsJsonArray("client").get(0).getAsString()
        );
        assertTrue(Files.readString(
            ROOT.resolve("neoforge/src/main/java/media/jlt/minecraft/mods/worldz/WorldzNeoForgeClient.java")
        ).contains("RegisterPresetEditorsEvent"));
    }

    private static Properties projectProperties() throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(ROOT.resolve("gradle.properties"))) {
            properties.load(reader);
        }
        return properties;
    }
}
