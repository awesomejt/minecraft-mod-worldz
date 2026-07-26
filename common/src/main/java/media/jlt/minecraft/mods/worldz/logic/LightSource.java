package media.jlt.minecraft.mods.worldz.logic;

import java.util.Locale;

/**
 * The light-emitting block a guaranteed starter capsule/base is lit with (GOALS 41.2, DESIGN
 * §31.9) -- kept as a pure logic-layer enum, like {@link SealedSurfaceBlock}, so this stays
 * JUnit-testable without booting Minecraft registries; the deployment layer maps each value to
 * its actual block(s) and placement rule.
 */
public enum LightSource {
    /** Wall-mounted, spaced every {@code lightSpacingBlocks} around the room's perimeter. */
    TORCH,
    /** Hung from the ceiling in a grid spaced by {@code lightSpacingBlocks} (Jason, 2026-07-25). */
    LANTERN,
    /** Same ceiling-hung placement as {@link #LANTERN}, just the Nether-flavored variant. */
    SOUL_LANTERN,
    /** Wall-embedded, spaced like {@link #TORCH} -- the Nether-thematic default. */
    GLOWSTONE,
    /** Wall-embedded, spaced like {@link #TORCH} -- another Nether-native light block. */
    SHROOMLIGHT,
    /** Coats the entire interior surface (walls, floor, ceiling) instead of spaced points. */
    GLOW_LICHEN;

    /**
     * Parses the stable lowercase configuration name.
     *
     * @param value configuration value
     * @return matching light source
     */
    public static LightSource parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("light source must be a string");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown light source '" + value + "'", exception);
        }
    }

    /**
     * Returns the stable YAML and codec representation.
     *
     * @return lowercase light source name
     */
    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
