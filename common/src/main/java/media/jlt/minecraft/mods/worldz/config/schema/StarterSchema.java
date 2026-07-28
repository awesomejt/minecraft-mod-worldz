package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Shared group section for the "starter zone" pair (DESIGN §42.1's three-shapes-F1-missed table;
 * TODO 25.6b): {@code biome}/{@code radius}, with an optional {@code land} sub-group. Appears at
 * the config root (with {@code land}) and, without {@code land}, inside {@code singleBiome} and
 * {@code chaosBiomes}. Each owner keeps its own flat {@code starterBiome}/{@code
 * starterRadiusBlocks} fields directly -- never a shared nested POJO -- so this is a {@link
 * Setting#group} over the owner's own type {@code S} (DESIGN §42.2), not a {@link
 * Setting#section}. Parameterized on {@code S} because the three owners are three different flat
 * POJOs, and on whether a {@code land} sub-group exists at all: only the config root has {@code
 * ensureStarterLand}/{@code starterLandTransitionBlocks}/{@code starterLandFoundationDepthBlocks}
 * fields (DESIGN §42.1) -- the same "parameterized instance" mechanism {@link BorderSchema} uses
 * for {@code objectiveKey} (DESIGN R1). {@code land} is itself a {@code SchemaSection<S>} (never a
 * separate type parameter) so it composes as a nested group exactly like {@code
 * overworldBorder.resize.rate} does (DESIGN §42.2's "groups nest").
 *
 * <p>One canonical doc string per leaf, reused at every instantiation site regardless of the
 * owner's own context -- the same choice {@link SpawnSchema} already makes for its one shared
 * {@code strategy} setting across four different owners.
 */
public final class StarterSchema<S> extends SchemaSection<S> {
    private final Accessor<S, String> biome;
    private final Accessor<S, Integer> radius;
    private final SchemaSection<S> land;

    /** Full constructor, with a {@code land} sub-group (the config root only). */
    public StarterSchema(
        String path, Supplier<S> factory, Accessor<S, String> biome, Accessor<S, Integer> radius, SchemaSection<S> land
    ) {
        super(path, factory);
        this.biome = biome;
        this.radius = radius;
        this.land = land;
    }

    /** Constructor without a {@code land} sub-group ({@code singleBiome}, {@code chaosBiomes}). */
    public StarterSchema(String path, Supplier<S> factory, Accessor<S, String> biome, Accessor<S, Integer> radius) {
        this(path, factory, biome, radius, null);
    }

    @Override
    protected List<Setting<S, ?>> declare() {
        List<Setting<S, ?>> settings = new ArrayList<>();
        settings.add(
            Setting.<S>text("biome", biome.getter(), biome.setter())
                .rule(new Rule.BiomeId<>(true, () -> "", "Ignoring invalid " + path() + ".biome '{}'."))
                .unit(Unit.BIOME_ID)
                .render(StarterSchema::renderBiome)
                .doc("Optional biome id forced in a circular zone around the starter origin; empty disables the starter zone.")
                .build()
        );
        settings.add(
            Setting.<S>integer("radius", radius.getter(), radius.setter())
                .range(WorldzConfig.MIN_STARTER_RADIUS_BLOCKS, WorldzConfig.MAX_STARTER_RADIUS_BLOCKS)
                .unit(Unit.BLOCKS)
                .doc("Starter-zone radius, only meaningful when biome is set.")
                .build()
        );
        if (land != null) {
            settings.add(
                Setting.group("land", land)
                    .render(land::summary)
                    .doc("Whether/how low terrain beneath the starter biome is reinforced into usable land.")
                    .build()
            );
        }
        return settings;
    }

    private static String renderBiome(String value) {
        return value.isEmpty() ? "<none>" : value;
    }
}
