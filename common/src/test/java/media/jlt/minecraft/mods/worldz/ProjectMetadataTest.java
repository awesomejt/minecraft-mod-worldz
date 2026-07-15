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
    }

    @Test
    void projectIdentityMatchesDesignContract() throws IOException {
        Properties properties = projectProperties();
        String settings = Files.readString(ROOT.resolve("settings.gradle"));

        assertTrue(settings.contains("rootProject.name = 'mod-worldz'"));
        assertEquals("media.jlt.minecraft.mods", properties.getProperty("group"));
        assertEquals("0.1.0", properties.getProperty("version"));
        assertEquals("jlt_worldz", properties.getProperty("mod_id"));
        assertEquals("JLT Worldz", properties.getProperty("mod_name"));
        assertEquals("25", properties.getProperty("java_version"));
        assertEquals("26.2", properties.getProperty("minecraft_version"));
    }

    private static Properties projectProperties() throws IOException {
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(ROOT.resolve("gradle.properties"))) {
            properties.load(reader);
        }
        return properties;
    }
}
