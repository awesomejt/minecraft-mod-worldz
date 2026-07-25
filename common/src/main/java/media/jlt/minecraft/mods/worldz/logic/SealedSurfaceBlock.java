package media.jlt.minecraft.mods.worldz.logic;

import java.util.Locale;

/**
 * The block used to seal off sky access for the {@code cave} preset's optional roof (GOALS 25,
 * DESIGN §30.4). Kept as a pure logic-layer enum -- like {@link StarterKitTier} -- rather than
 * holding a real {@code BlockState}, so this stays JUnit-testable without booting Minecraft
 * registries; {@code EnvelopedChunkGenerator} maps each value to its actual block.
 */
public enum SealedSurfaceBlock {
    /** Ordinary stone -- mineable with any pickaxe, the original fixed behavior. */
    STONE,
    /** Deepslate -- mineable, but slower to break than stone. */
    DEEPSLATE,
    /** Bedrock -- unbreakable in survival, the strongest option (Jason, 2026-07-25). */
    BEDROCK;

    /**
     * Parses the stable lowercase configuration name.
     *
     * @param value configuration value
     * @return matching block choice
     */
    public static SealedSurfaceBlock parse(String value) {
        if (value == null) {
            throw new IllegalArgumentException("sealed surface block must be a string");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("unknown sealed surface block '" + value + "'", exception);
        }
    }

    /**
     * Returns the stable YAML and codec representation.
     *
     * @return lowercase block name
     */
    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }
}
