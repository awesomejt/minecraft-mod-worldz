package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.StarterKitConfig;
import media.jlt.minecraft.mods.worldz.logic.StarterKitTier;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Shared group section for the "difficulty-tiered starter chest" shape (DESIGN §42.1's cross-class
 * table; TODO 25.6d): {@code tier}/{@code kits: {easy, medium, hard}}, with an optional {@code
 * enabled} leaf. Appears inside {@code skyIsland}, {@code netherStart}, {@code endStart} (none of
 * which have {@code enabled} -- the chest is unconditional there) and, from TODO 25.6e onward,
 * {@code cave} (which additionally carries {@code chestEnabled}). Each owner keeps its own flat
 * {@code chestTier}/{@code easyKit}/{@code mediumKit}/{@code hardKit}(/{@code chestEnabled}) fields
 * directly -- never a shared nested POJO -- so this is a {@link Setting#group} over the owner's own
 * type {@code S} (DESIGN §42.2), not a {@link Setting#section}. Parameterized on {@code S} because
 * the four owners are four different flat POJOs, and on whether an {@code enabled} leaf exists at
 * all -- the same "parameterized instance" mechanism {@link BorderSchema} uses for {@code
 * objectiveKey} (DESIGN R1), designed now (25.6d) so 25.6e only has to instantiate it, not extend it.
 *
 * <p>{@code kits} is itself a private {@link SchemaSection}{@code <S>} nested inside this group
 * (DESIGN §42.2's "groups nest", the same composition {@code overworldBorder.resize.rate} already
 * proves): each kit is a real {@link StarterKitConfig} field on the parent POJO, so {@code kits}'
 * own settings are ordinary {@link Setting#section} bindings (not further groups), delegating to a
 * {@link StarterKitSchema} instance per kit exactly as every pre-25.6d call site already did.
 *
 * <p>One canonical doc string per leaf, reused at every instantiation site regardless of the
 * owner's own context -- the same choice {@link StarterKitSchema} itself already makes.
 */
public final class ChestSchema<S> extends SchemaSection<S> {
    private final Accessor<S, Boolean> enabled;
    private final Accessor<S, StarterKitTier> tier;
    private final KitsSchema<S> kits;

    /** Full constructor, with an {@code enabled} leaf (from TODO 25.6e, {@code cave} only). */
    public ChestSchema(
        String path, Supplier<S> factory,
        Accessor<S, Boolean> enabled, Accessor<S, StarterKitTier> tier,
        Accessor<S, StarterKitConfig> easyKit, Accessor<S, StarterKitConfig> mediumKit, Accessor<S, StarterKitConfig> hardKit
    ) {
        super(path, factory);
        this.enabled = enabled;
        this.tier = tier;
        this.kits = new KitsSchema<>(path + ".kits", factory, easyKit, mediumKit, hardKit);
    }

    /** Constructor without an {@code enabled} leaf ({@code skyIsland}, {@code netherStart},
     * {@code endStart}: the chest is unconditional). */
    public ChestSchema(
        String path, Supplier<S> factory,
        Accessor<S, StarterKitTier> tier,
        Accessor<S, StarterKitConfig> easyKit, Accessor<S, StarterKitConfig> mediumKit, Accessor<S, StarterKitConfig> hardKit
    ) {
        this(path, factory, null, tier, easyKit, mediumKit, hardKit);
    }

    @Override
    protected List<Setting<S, ?>> declare() {
        List<Setting<S, ?>> settings = new ArrayList<>();
        if (enabled != null) {
            settings.add(
                Setting.<S>flag("enabled", enabled.getter(), enabled.setter())
                    .doc("Whether the starter chest generates at all.")
                    .build()
            );
        }
        settings.add(
            Setting.<S, StarterKitTier>enumeration(
                    "tier", tier.getter(), tier.setter(),
                    StarterKitTier::parse, StarterKitTier::serializedName, StarterKitTier.MEDIUM
                )
                .doc("Which of the easy/medium/hard kits the starter chest uses.")
                .build()
        );
        settings.add(
            Setting.group("kits", kits)
                .render(kits::summary)
                .doc("The easy/medium/hard kit contents themselves.")
                .build()
        );
        return settings;
    }

    /**
     * {@code chest.kits}: {@code easy}/{@code medium}/{@code hard}, each a real {@link
     * StarterKitConfig} field delegated to its own {@link StarterKitSchema} instance -- a group
     * inside {@link ChestSchema}'s own group (DESIGN §42.2's "groups nest"), private exactly like
     * {@code BorderSchema.ResizeSchema}/{@code RateSchema}.
     */
    private static final class KitsSchema<S> extends SchemaSection<S> {
        private final Accessor<S, StarterKitConfig> easy;
        private final Accessor<S, StarterKitConfig> medium;
        private final Accessor<S, StarterKitConfig> hard;
        private final StarterKitSchema easySchema;
        private final StarterKitSchema mediumSchema;
        private final StarterKitSchema hardSchema;

        KitsSchema(
            String path, Supplier<S> factory,
            Accessor<S, StarterKitConfig> easy, Accessor<S, StarterKitConfig> medium, Accessor<S, StarterKitConfig> hard
        ) {
            super(path, factory);
            this.easy = easy;
            this.medium = medium;
            this.hard = hard;
            this.easySchema = new StarterKitSchema(path() + ".easy");
            this.mediumSchema = new StarterKitSchema(path() + ".medium");
            this.hardSchema = new StarterKitSchema(path() + ".hard");
        }

        @Override
        protected List<Setting<S, ?>> declare() {
            return List.of(
                Setting.<S, StarterKitConfig>section("easy", easy.getter(), easy.setter(), easySchema)
                    .render(easySchema::summary)
                    .doc("Generous starter-chest contents.")
                    .build(),
                Setting.<S, StarterKitConfig>section("medium", medium.getter(), medium.setter(), mediumSchema)
                    .render(mediumSchema::summary)
                    .doc("Middle-ground starter-chest contents.")
                    .build(),
                Setting.<S, StarterKitConfig>section("hard", hard.getter(), hard.setter(), hardSchema)
                    .render(hardSchema::summary)
                    .doc("Bare-essentials starter-chest contents.")
                    .build()
            );
        }
    }
}
