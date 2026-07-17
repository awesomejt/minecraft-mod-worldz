package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitPlan;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renders a commented, human-readable YAML record of one newly created world's resolved
 * settings (DESIGN §20.3). Write-only: this file is never parsed back by the mod, so unlike
 * {@code WorldzConfig} there is no matching {@code parse}/{@code sanitize} half.
 */
public final class WorldSnapshotWriter {
    private static final String HEADER = """
        # jlt_worldz per-world settings snapshot -- a reference record of what this
        # world was created with, for your own notes and reproducibility. The mod
        # NEVER reads this file back; editing it has no effect on the world.
        # Authoritative settings are baked into the world's generator codec at
        # creation time, exactly as before this file existed.
        #
        # Mod version: %s
        # Created: %s
        """;

    private WorldSnapshotWriter() {
    }

    /**
     * Renders one world's resolved settings as commented YAML text.
     *
     * @param snapshot resolved values to record
     * @return complete file contents, header comment included
     */
    public static String render(WorldSnapshot snapshot) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("allowedBiomes", snapshot.allowedBiomes());
        values.put("starterBiome", snapshot.starterBiome().isEmpty() ? "" : snapshot.starterBiome());
        values.put("starterRadiusBlocks", snapshot.starterRadiusBlocks());
        values.put("starterLand", starterLandMap(snapshot.starterLandPlan()));
        values.put("overworldBorder", borderMap(snapshot.worldLimits().overworld()));
        values.put("netherBorder", borderMap(snapshot.worldLimits().nether()));
        values.put("overworldExterior", exteriorMap(snapshot.exteriorPlan().overworld()));
        values.put("netherExterior", exteriorMap(snapshot.exteriorPlan().nether()));
        values.put("layout", layoutMap(snapshot.worldLayoutPlan()));
        values.put("spawn", spawnMap(snapshot));

        String header = HEADER.formatted(snapshot.modVersion(), snapshot.createdAtIso());
        return header + createYaml().dump(values);
    }

    private static Map<String, Object> starterLandMap(StarterLandPlan plan) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", plan.enabled());
        values.put("transitionWidthBlocks", plan.transitionWidthBlocks());
        values.put("foundationDepthBlocks", plan.foundationDepthBlocks());
        return values;
    }

    private static Map<String, Object> borderMap(WorldLimitPlan.DimensionLimit limit) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("enabled", limit.enabled());
        values.put("initialRadiusBlocks", limit.initialRadiusBlocks());
        values.put("finalRadiusBlocks", limit.finalRadiusBlocks());
        values.put("resizeDays", limit.resizeDays());
        values.put("resizeDelayDays", limit.resizeDelayDays());
        values.put("resizeRateBlocks", limit.resizeRateBlocks());
        values.put("resizeRateDays", limit.resizeRateDays());
        values.put("ensureObjective", limit.ensureObjective());
        return values;
    }

    private static Map<String, Object> exteriorMap(ExteriorPlan.DimensionEnvelope envelope) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("mode", envelope.mode().serializedName());
        values.put("boundaryRadiusBlocks", envelope.boundaryRadiusBlocks());
        values.put("oceanTransitionWidthBlocks", envelope.oceanTransitionWidthBlocks());
        return values;
    }

    private static Map<String, Object> layoutMap(WorldLayoutPlan plan) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("mode", plan.mode().serializedName());
        values.put("singleBiome", plan.singleBiome().orElse(""));
        values.put("regionScaleBlocks", plan.regionScaleBlocks());
        return values;
    }

    private static Map<String, Object> spawnMap(WorldSnapshot snapshot) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("strategy", snapshot.spawnStrategy().serializedName());
        values.put("layoutOriginBlockX", snapshot.originBlockX());
        values.put("layoutOriginBlockZ", snapshot.originBlockZ());
        return values;
    }

    private static Yaml createYaml() {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        dumperOptions.setIndent(2);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setWidth(120);
        return new Yaml(new Representer(dumperOptions), dumperOptions);
    }

    /**
     * Resolved values recorded for one newly created world.
     *
     * @param modVersion the mod version that created this world
     * @param createdAtIso creation timestamp, ISO-8601
     * @param allowedBiomes resolved allowed-biome registry names
     * @param starterBiome resolved starter biome registry name, or empty when disabled
     * @param starterRadiusBlocks starter-zone radius
     * @param starterLandPlan resolved starter-land guarantee
     * @param worldLimits resolved border plan
     * @param exteriorPlan resolved exterior-terrain plan
     * @param worldLayoutPlan resolved coordinated-layout plan
     * @param spawnStrategy resolved layout-origin and spawn strategy
     * @param originBlockX resolved layout origin X (0 unless recentered)
     * @param originBlockZ resolved layout origin Z (0 unless recentered)
     */
    public record WorldSnapshot(
        String modVersion,
        String createdAtIso,
        List<String> allowedBiomes,
        String starterBiome,
        int starterRadiusBlocks,
        StarterLandPlan starterLandPlan,
        WorldLimitPlan worldLimits,
        ExteriorPlan exteriorPlan,
        WorldLayoutPlan worldLayoutPlan,
        SpawnStrategy spawnStrategy,
        int originBlockX,
        int originBlockZ
    ) {
    }
}
