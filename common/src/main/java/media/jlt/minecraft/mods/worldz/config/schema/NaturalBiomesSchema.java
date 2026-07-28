package media.jlt.minecraft.mods.worldz.config.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Shared group section for the "let vanilla's own natural biomes generate" trio (DESIGN §42.1's
 * cross-class table; TODO 25.6b): {@code rivers}/{@code oceans}(/{@code beaches}), appearing at
 * the config root and inside {@code singleBiome}, {@code chaosBiomes} and {@code
 * stripWorld.bands}. Each owner keeps its own flat {@code allowRivers}/{@code allowOceans}(/
 * {@code allowBeaches}) fields directly -- never a shared nested POJO -- so this is a {@link
 * Setting#group} over the owner's own type {@code S} (DESIGN §42.2), not a {@link
 * Setting#section}. Parameterized on {@code S} because the four owners are four different flat
 * POJOs, and on whether a {@code beaches} leaf exists at all: the config root has no {@code
 * allowBeaches} field (DESIGN §42.1), so {@code beaches} is nullable and simply omitted from
 * {@link #declare()} when absent -- the same "parameterized instance" mechanism {@link
 * BorderSchema} uses for {@code objectiveKey} (DESIGN R1).
 *
 * <p>One canonical doc string per leaf, reused at every instantiation site regardless of the
 * owner's own context -- the same choice {@link SpawnSchema} already makes for its one shared
 * {@code strategy} setting across four different owners.
 */
public final class NaturalBiomesSchema<S> extends SchemaSection<S> {
    private final Accessor<S, Boolean> rivers;
    private final Accessor<S, Boolean> oceans;
    private final Accessor<S, Boolean> beaches;

    /** Full constructor, with a {@code beaches} leaf ({@code singleBiome}, {@code chaosBiomes},
     * {@code stripWorld.bands}). */
    public NaturalBiomesSchema(
        String path, Supplier<S> factory, Accessor<S, Boolean> rivers, Accessor<S, Boolean> oceans, Accessor<S, Boolean> beaches
    ) {
        super(path, factory);
        this.rivers = rivers;
        this.oceans = oceans;
        this.beaches = beaches;
    }

    /** Constructor without a {@code beaches} leaf (the config root only). */
    public NaturalBiomesSchema(String path, Supplier<S> factory, Accessor<S, Boolean> rivers, Accessor<S, Boolean> oceans) {
        this(path, factory, rivers, oceans, null);
    }

    @Override
    protected List<Setting<S, ?>> declare() {
        List<Setting<S, ?>> settings = new ArrayList<>();
        settings.add(
            Setting.<S>flag("rivers", rivers.getter(), rivers.setter())
                .doc("Let vanilla's own river biomes generate where vanilla would place one.")
                .build()
        );
        settings.add(
            Setting.<S>flag("oceans", oceans.getter(), oceans.setter())
                .doc("Let vanilla's own river/ocean-family biomes generate naturally, additive over rivers.")
                .build()
        );
        if (beaches != null) {
            settings.add(
                Setting.<S>flag("beaches", beaches.getter(), beaches.setter())
                    .doc("Let vanilla's own beach/stony-shore biomes generate where vanilla would place one.")
                    .build()
            );
        }
        return settings;
    }
}
