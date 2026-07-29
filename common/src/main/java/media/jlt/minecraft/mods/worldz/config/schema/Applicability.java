package media.jlt.minecraft.mods.worldz.config.schema;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Where a {@link Setting} takes effect (DESIGN §41.2, CONFIG-RESTRUCTURE.md F3). Distinguishes
 * live global runtime rules (re-read every tick/day) from world-creation defaults baked once into
 * a save, and which typed presets a preset-scoped setting actually applies to.
 *
 * @param scope live-vs-baked classification
 * @param presets preset ids the setting applies to (only meaningful for {@link Scope#PRESET});
 *     empty means "the generic {@code worldz} preset only"
 * @param customizeExposed whether the in-game Customize screen exposes this setting
 * @param customizeDescendantsExposed whether every descendant of this container is exposed
 */
public record Applicability(
    Scope scope,
    Set<String> presets,
    boolean customizeExposed,
    boolean customizeDescendantsExposed
) {
    /** Inherits scope and preset ids from the containing setting. */
    public static Applicability inherit() {
        return new Applicability(Scope.INHERIT, Set.of(), false, false);
    }

    /** Live global runtime rule, re-read from config on every relevant tick/day (F3's first row). */
    public static Applicability live() {
        return new Applicability(Scope.LIVE, Set.of(), false, false);
    }

    /** World-creation default shared by every typed preset (F3's second row). */
    public static Applicability worldDefault() {
        return new Applicability(Scope.WORLD_DEFAULT, Set.of(), false, false);
    }

    /** World-creation default scoped to specific typed preset(s) (F3's third row). */
    public static Applicability preset(String... presetIds) {
        return new Applicability(
            Scope.PRESET,
            Collections.unmodifiableSet(new LinkedHashSet<>(List.of(presetIds))),
            false,
            false
        );
    }

    /** Returns a copy with {@link #customizeExposed} set. */
    public Applicability exposedInCustomize() {
        return new Applicability(scope, presets, true, customizeDescendantsExposed);
    }

    /** Returns a copy marking every child setting as exposed in Customize. */
    public Applicability descendantsExposedInCustomize() {
        return new Applicability(scope, presets, customizeExposed, true);
    }

    /** Live-vs-baked classification (CONFIG-RESTRUCTURE.md F3). */
    public enum Scope {
        /** Inherit the containing setting's effective scope; illegal on a root setting. */
        INHERIT,
        /** Re-read from config every time; editing changes existing worlds. */
        LIVE,
        /** Read once at world creation for every typed preset; editing does nothing afterward. */
        WORLD_DEFAULT,
        /** Read once at world creation, but only inert/active for specific typed preset(s). */
        PRESET
    }
}
