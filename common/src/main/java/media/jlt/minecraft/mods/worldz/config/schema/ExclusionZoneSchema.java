package media.jlt.minecraft.mods.worldz.config.schema;

import java.util.List;
import java.util.function.Supplier;

/**
 * Shared group section for the "void/shaping buffer around the starter zone" pair (DESIGN §42.1's
 * cross-class table, corrected: 3 live sites, not 4; TODO 25.6d): {@code enabled}/{@code radius}.
 * Appears inside {@code oceanIsland}, {@code skyIsland} (its own buffer, newly wired -- see below),
 * {@code skyIsland.floatingIslands} and {@code chunkIsland}. Each owner keeps its own flat {@code
 * exclusionZoneEnabled}/{@code exclusionZoneRadiusBlocks} fields directly -- never a shared nested
 * POJO -- so this is a {@link Setting#group} over the owner's own type {@code S} (DESIGN §42.2), not
 * a {@link Setting#section}. Parameterized on {@code S} because the four owners are four different
 * flat POJOs, and on the radius bound because the four call sites clamp differently today: {@code
 * oceanIsland}/{@code floatingIslands} floor at {@code 1} with an unbounded ceiling, {@code
 * chunkIsland} allows {@code 0} up to {@code WorldzConfig.MAX_BORDER_RADIUS_BLOCKS}.
 *
 * <p><strong>{@code skyIsland}'s own instance is a behavior change, not a pure rename</strong>
 * (DESIGN §42.1/§42.7, TODO 25.6d): {@code SkyIslandConfig.exclusionZoneEnabled}/{@code
 * exclusionZoneRadiusBlocks} existed on the POJO and were read by world-gen logic, but were never
 * wired into the config read/sanitize path at all -- {@code config/tests/57}'s values were silently
 * ignored. This class is what wires them up for real; every other call site is a pure rename.
 * {@code summary()} is overridden (DESIGN §41.6's table) rather than left to the mechanical
 * per-setting derivation, since {@code enabled}/{@code radius} collapse into one segment (matching
 * {@link FloatingIslandsSchema}'s own pre-existing {@code exclusionZone=} rendering) rather than two.
 */
public final class ExclusionZoneSchema<S> extends SchemaSection<S> {
    private final Accessor<S, Boolean> enabled;
    private final Accessor<S, Integer> radius;
    private final int minRadius;
    private final int maxRadius;

    public ExclusionZoneSchema(
        String path, Supplier<S> factory, Accessor<S, Boolean> enabled, Accessor<S, Integer> radius, int minRadius, int maxRadius
    ) {
        super(path, factory);
        this.enabled = enabled;
        this.radius = radius;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
    }

    @Override
    protected List<Setting<S, ?>> declare() {
        return List.of(
            Setting.<S>flag("enabled", enabled.getter(), enabled.setter())
                .doc("Whether shaping/generation releases to the seed's own terrain beyond radius.")
                .build(),
            Setting.<S>integer("radius", radius.getter(), radius.setter())
                .range(minRadius, maxRadius)
                .unit(Unit.BLOCKS)
                .doc("Radius beyond which the buffer ends, when enabled.")
                .build()
        );
    }

    /**
     * Overridden: collapses {@code enabled}/{@code radius} into one segment (DESIGN §41.6's table),
     * matching {@link FloatingIslandsSchema}'s pre-existing {@code exclusionZone=} rendering and
     * {@code oceanIsland}'s pre-25.6 complementary-{@code includeInSummaryWhen} trick -- not
     * mechanically derivable from two independent per-setting segments.
     */
    @Override
    public String summary(S value) {
        return enabled.get(value) ? "radius=" + radius.get(value) : "<disabled>";
    }
}
