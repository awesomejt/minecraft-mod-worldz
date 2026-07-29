package media.jlt.minecraft.mods.worldz.logic;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import media.jlt.minecraft.mods.worldz.worldgen.WorldLimitPlan;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Immutable, loader-neutral values selected for one new {@code jlt_worldz:strip_world}
 * world (GOALS 32, DESIGN §23): the small per-type field set, not the full generic
 * Customize screen's. The corridor's length uses the ordinary border/exterior machinery
 * (below) unmodified; only the width is strip-specific.
 *
 * @param width absolute corridor width in blocks
 * @param widthMode terrain generated beyond the width -- void or ocean, never normal
 * @param applyToNether whether the same corridor width also applies to the Nether
 * @param bandsEnabled whether the strip passes through an ordered biome-band sequence (GOALS 36)
 * @param bandBiomes ordered, unweighted band biome ids walked along the strip's length
 * @param bandWidthBlocks band width in blocks, only meaningful when {@link #bandsEnabled} is set
 * @param bandSeedRandomOrder shuffle {@link #bandBiomes} once instead of using it as given
 * @param bandAllowRivers let vanilla's own river biomes pass through the band sequence
 * @param bandAllowOceans let vanilla's own river/ocean-family biomes pass through the band sequence
 * @param bandAllowBeaches let vanilla's own beach/stony-shore biomes pass through the band sequence
 * @param spawnStrategy layout-origin and initial-spawn strategy
 * @param overworldBorder overworld border selection -- the corridor's length (GOALS 17-20)
 * @param netherBorder Nether border selection
 * @param endBorder End border selection (GOALS 17's Overworld-to-End carry-over)
 * @param overworldExterior Overworld exterior-terrain selection beyond the length
 * @param netherExterior Nether exterior-terrain selection beyond the length
 */
public record StripWorldCustomization(
    int width,
    ExteriorMode widthMode,
    boolean applyToNether,
    boolean bandsEnabled,
    List<String> bandBiomes,
    int bandWidthBlocks,
    boolean bandSeedRandomOrder,
    boolean bandAllowRivers,
    boolean bandAllowOceans,
    boolean bandAllowBeaches,
    SpawnStrategy spawnStrategy,
    WorldzCustomization.BorderSettings overworldBorder,
    WorldzCustomization.BorderSettings netherBorder,
    WorldzCustomization.EndBorderSettings endBorder,
    WorldzCustomization.ExteriorSettings overworldExterior,
    WorldzCustomization.ExteriorSettings netherExterior
) {
    /** Validates and canonicalizes customization values. */
    public StripWorldCustomization {
        if (width <= 0) {
            throw new IllegalArgumentException("Strip width must be positive.");
        }
        if (widthMode == null || widthMode == ExteriorMode.NORMAL) {
            throw new IllegalArgumentException("Strip width mode must be void or ocean.");
        }
        if (spawnStrategy == null || overworldBorder == null || netherBorder == null || endBorder == null
            || overworldExterior == null || netherExterior == null) {
            throw new IllegalArgumentException("Spawn strategy, border, and exterior settings are required.");
        }
        if (netherExterior.mode() == ExteriorMode.OCEAN) {
            throw new IllegalArgumentException("Ocean exterior is only supported in the Overworld.");
        }
        WorldzCustomization.validateAutomaticBoundary(overworldExterior, overworldBorder, "Overworld");
        WorldzCustomization.validateAutomaticBoundary(netherExterior, netherBorder, "Nether");

        if (bandsEnabled) {
            BiomeListSpec spec = BiomeListSpec.parse(bandBiomes);
            if (!spec.invalidEntries().isEmpty()) {
                throw new IllegalArgumentException("Invalid band biome: " + spec.invalidEntries().getFirst());
            }
            if (spec.entries().isEmpty()) {
                throw new IllegalArgumentException("At least one band biome is required when biome bands are enabled.");
            }
            if (spec.entries().stream().anyMatch(BiomeListSpec.Entry::tag)) {
                throw new IllegalArgumentException("Band biomes must be concrete biome ids, not tags.");
            }
            bandBiomes = spec.entries().stream().map(BiomeListSpec.Entry::id).toList();
            if (bandWidthBlocks < WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS
                || bandWidthBlocks > WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS) {
                throw new IllegalArgumentException(
                    "Band width must be between " + WorldzConfig.MIN_LAYOUT_REGION_SCALE_BLOCKS
                        + " and " + WorldzConfig.MAX_LAYOUT_REGION_SCALE_BLOCKS + "."
                );
            }
        } else {
            bandBiomes = List.of();
        }
    }

    /**
     * Creates values from the sanitized YAML configuration.
     *
     * @param config sanitized startup configuration
     * @return an immutable customization snapshot
     */
    public static StripWorldCustomization fromConfig(WorldzConfig config) {
        return new StripWorldCustomization(
            config.stripWorld.width,
            config.stripWorld.widthMode,
            config.stripWorld.applyToNether,
            config.stripWorld.bands.enabled,
            config.stripWorld.bands.biomes,
            config.stripWorld.bands.widthBlocks,
            config.stripWorld.bands.seedRandomOrder,
            config.stripWorld.bands.allowRivers,
            config.stripWorld.bands.allowOceans,
            config.stripWorld.bands.allowBeaches,
            config.stripWorld.spawn.strategy,
            WorldzCustomization.BorderSettings.fromConfig(config.overworldBorder),
            WorldzCustomization.BorderSettings.fromConfig(config.netherBorder),
            WorldzCustomization.EndBorderSettings.fromConfig(config.endBorder),
            WorldzCustomization.ExteriorSettings.fromConfig(config.overworldExterior),
            WorldzCustomization.ExteriorSettings.fromConfig(config.netherExterior)
        );
    }

    /**
     * Parses client text/toggle fields into validated customization values.
     *
     * @param width decimal absolute width
     * @param widthMode void or ocean
     * @param applyToNether whether the same corridor width also applies to the Nether
     * @param bandsEnabled whether the strip passes through an ordered biome-band sequence
     * @param bandBiomesText newline- or comma-separated band biome ids
     * @param bandWidthBlocks decimal band width
     * @param bandSeedRandomOrder shuffle the band sequence once instead of using it as given
     * @param bandAllowRivers let vanilla's own river biomes pass through the band sequence
     * @param bandAllowOceans let vanilla's own river/ocean-family biomes pass through the band sequence
     * @param bandAllowBeaches let vanilla's own beach/stony-shore biomes pass through the band sequence
     * @param spawnStrategy layout-origin and spawn strategy
     * @param overworldBorder validated overworld border values
     * @param netherBorder validated Nether border values
     * @param endBorder validated End border values
     * @param overworldExterior validated Overworld exterior values
     * @param netherExterior validated Nether exterior values
     * @return canonical immutable customization values
     */
    public static StripWorldCustomization fromText(
        String width,
        String widthMode,
        boolean applyToNether,
        boolean bandsEnabled,
        String bandBiomesText,
        String bandWidthBlocks,
        boolean bandSeedRandomOrder,
        boolean bandAllowRivers,
        boolean bandAllowOceans,
        boolean bandAllowBeaches,
        SpawnStrategy spawnStrategy,
        WorldzCustomization.BorderSettings overworldBorder,
        WorldzCustomization.BorderSettings netherBorder,
        WorldzCustomization.EndBorderSettings endBorder,
        WorldzCustomization.ExteriorSettings overworldExterior,
        WorldzCustomization.ExteriorSettings netherExterior
    ) {
        List<String> bandBiomes = Arrays.stream(bandBiomesText.split("[,\\r\\n]+"))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .toList();
        return new StripWorldCustomization(
            parseInteger(width, "Strip width"),
            ExteriorMode.parse(widthMode),
            applyToNether,
            bandsEnabled,
            bandBiomes,
            parseInteger(bandWidthBlocks, "Band width"),
            bandSeedRandomOrder,
            bandAllowRivers,
            bandAllowOceans,
            bandAllowBeaches,
            spawnStrategy,
            overworldBorder,
            netherBorder,
            endBorder,
            overworldExterior,
            netherExterior
        );
    }

    /**
     * Resolves this world's strip plan for one dimension.
     *
     * @param overworld whether to resolve the Overworld rather than the Nether
     * @return resolved plan, disabled for the Nether unless {@link #applyToNether} is set
     */
    public StripPlan stripPlan(boolean overworld) {
        return overworld || applyToNether ? new StripPlan(true, width, widthMode) : StripPlan.disabled();
    }

    /**
     * Renders the configured band biome ids one per line for the multi-line editor.
     *
     * @return newline-separated canonical values
     */
    public String bandBiomesText() {
        return String.join("\n", bandBiomes);
    }

    /**
     * Resolves this world's coordinated-layout plan: the ordered biome-band sequence
     * (GOALS 36) when {@link #bandsEnabled} is set, otherwise the legacy no-op plan that
     * leaves the corridor's terrain and biomes entirely to ordinary vanilla generation.
     *
     * @param seed sampling seed, consulted only for the optional one-time band shuffle
     * @return immutable resolved layout plan
     */
    public WorldLayoutPlan layoutPlan(long seed) {
        return bandsEnabled
            ? WorldLayoutPlan.resolveBands(bandBiomes, bandWidthBlocks, bandSeedRandomOrder, Map.of(), seed)
            : WorldLayoutPlan.legacy();
    }

    /**
     * Converts all three border selections to the world-persisted plan.
     *
     * @return immutable codec-backed world-limit plan
     */
    public WorldLimitPlan worldLimitPlan() {
        return new WorldLimitPlan(overworldBorder.toPlan(), netherBorder.toPlan(), endBorder.toPlan());
    }

    /**
     * Converts both exterior selections to resolved, persisted envelopes.
     *
     * @return immutable resolved exterior plan
     */
    public ExteriorPlan exteriorPlan() {
        return new ExteriorPlan(overworldExterior.toPlan(overworldBorder), netherExterior.toPlan(netherBorder));
    }

    private static int parseInteger(String value, String name) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NullPointerException | NumberFormatException exception) {
            throw new IllegalArgumentException(name + " must be a whole number.", exception);
        }
    }
}
