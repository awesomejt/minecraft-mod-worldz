package media.jlt.minecraft.mods.worldz.config.schema;

import java.util.List;
import java.util.function.Supplier;

/**
 * Shared group section for the synthetic underground-biome-band pair (GOAL 42, DESIGN §37.3;
 * DESIGN §42.1's cross-class table row {@code underground}; TODO 25.6g): {@code biome}/{@code
 * belowSurface}. Appears inside {@code flat} and {@code skyIsland}. Each owner keeps its own flat
 * {@code undergroundBiome}/{@code undergroundBelowSurfaceBlocks} fields directly -- never a shared
 * nested POJO -- so this is a {@link Setting#group} over the owner's own type {@code S} (DESIGN
 * §42.2), not a {@link Setting#section}, the same shape {@link ExclusionZoneSchema}/{@link
 * ChestSchema} (TODO 25.6d) already use. Parameterized only on {@code S} -- unlike those two, both
 * owners here share one identical shape with no structural or per-site bound difference, so there
 * is nothing else to parameterize.
 *
 * <p><strong>Wiring this group up is a behavior change, not a pure rename</strong> (DESIGN
 * §42.1/§42.7, TODO 25.6g): {@code undergroundBiome}/{@code undergroundBelowSurfaceBlocks} exist on
 * both {@code FlatConfig} and {@code SkyIslandConfig}, and are read by {@code FlatPlan#fromConfig}/
 * {@code SkyIslandPlan#fromConfig}, but neither field was ever declared as a real {@link Setting} --
 * {@code config/tests/97}/{@code 98}'s values were silently ignored, parsed as the untouched
 * constructor defaults ({@code ""}, {@code 10}) instead of the fixture's own configured values.
 * This class is what wires them up for real.
 *
 * <p>{@code biome} reuses the same "blank means disabled" shape {@link StarterSchema}'s own {@code
 * biome} leaf already uses -- {@link Rule.BiomeId} with {@code allowEmpty = true} -- rather than
 * {@link Rule.BiomeIdOrDefault} (which always resolves to a concrete fallback): an empty {@code
 * undergroundBiome} is the documented, legal "band disabled" state (DESIGN §37.3), not an error.
 * {@code belowSurface} floors at {@code 0} -- matching {@code FlatPlan}/{@code SkyIslandPlan}'s own
 * compact-constructor rejection of a negative value, the one real invariant this field has -- with
 * no upper bound: neither {@code Plan} record nor any Customize-screen control (the band is
 * config-only for now, per both classes' own Javadoc) imposes one today, so clamping to an invented
 * ceiling here would be new behavior with no justification.
 */
public final class UndergroundSchema<S> extends SchemaSection<S> {
    private final Accessor<S, String> biome;
    private final Accessor<S, Integer> belowSurface;

    public UndergroundSchema(String path, Supplier<S> factory, Accessor<S, String> biome, Accessor<S, Integer> belowSurface) {
        super(path, factory);
        this.biome = biome;
        this.belowSurface = belowSurface;
    }

    @Override
    protected List<Setting<S, ?>> declare() {
        return List.of(
            Setting.<S>text("biome", biome.getter(), biome.setter())
                .rule(new Rule.BiomeId<>(true, () -> "", "Ignoring invalid " + path() + ".biome '{}'."))
                .unit(Unit.BIOME_ID)
                .render(UndergroundSchema::renderBiome)
                .doc("Biome reported below belowSurface blocks under the surface; blank disables the underground band entirely.")
                .build(),
            Setting.<S>integer("belowSurface", belowSurface.getter(), belowSurface.setter())
                .range(0, Integer.MAX_VALUE)
                .unit(Unit.BLOCKS)
                .doc("How many blocks below the surface the underground band starts; ignored (band never applies) at 0 even with a biome configured.")
                .build()
        );
    }

    private static String renderBiome(String value) {
        return value.isEmpty() ? "<none>" : value;
    }
}
