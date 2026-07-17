package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitPlan;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldSnapshotWriterTest {
    private static final Yaml YAML = new Yaml(new SafeConstructor(new LoaderOptions()));

    @Test
    void headerDocumentsThatTheFileIsNeverReadBack() {
        String rendered = WorldSnapshotWriter.render(sample());

        assertTrue(rendered.startsWith("# jlt_worldz per-world settings snapshot"));
        assertTrue(rendered.contains("NEVER reads this file back"));
        assertTrue(rendered.contains("Mod version: 0.2.1"));
        assertTrue(rendered.contains("Created: 2026-07-16T00:00:00Z"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void rendersEveryResolvedFieldAsParseableYaml() {
        String rendered = WorldSnapshotWriter.render(sample());
        String yamlBody = rendered.substring(rendered.indexOf("allowedBiomes:"));

        Map<String, Object> parsed = (Map<String, Object>) YAML.load(yamlBody);

        assertEquals(List.of("minecraft:desert"), parsed.get("allowedBiomes"));
        assertEquals("minecraft:plains", parsed.get("starterBiome"));
        assertEquals(256, parsed.get("starterRadiusBlocks"));

        Map<String, Object> starterLand = (Map<String, Object>) parsed.get("starterLand");
        assertEquals(true, starterLand.get("enabled"));
        assertEquals(128, starterLand.get("transitionWidthBlocks"));
        assertEquals(48, starterLand.get("foundationDepthBlocks"));

        Map<String, Object> overworldBorder = (Map<String, Object>) parsed.get("overworldBorder");
        assertEquals(false, overworldBorder.get("enabled"));

        Map<String, Object> layout = (Map<String, Object>) parsed.get("layout");
        assertEquals("single_biome", layout.get("mode"));
        assertEquals("minecraft:desert", layout.get("singleBiome"));

        Map<String, Object> spawn = (Map<String, Object>) parsed.get("spawn");
        assertEquals("starter_at_origin", spawn.get("strategy"));
        assertEquals(64, spawn.get("layoutOriginBlockX"));
        assertEquals(-64, spawn.get("layoutOriginBlockZ"));
    }

    private static WorldSnapshotWriter.WorldSnapshot sample() {
        return new WorldSnapshotWriter.WorldSnapshot(
            "0.2.1",
            "2026-07-16T00:00:00Z",
            List.of("minecraft:desert"),
            "minecraft:plains",
            256,
            new StarterLandPlan(true, 128, 48),
            WorldLimitPlan.disabled(),
            ExteriorPlan.normal(),
            new WorldLayoutPlan(
                LayoutMode.SINGLE_BIOME, 1L, 512, List.of(), List.of(), List.of(),
                java.util.Optional.of("minecraft:desert"), Map.of(), 0, 0, WorldLayoutPlan.CURRENT_REVISION
            ),
            SpawnStrategy.STARTER_AT_ORIGIN,
            64,
            -64
        );
    }
}
