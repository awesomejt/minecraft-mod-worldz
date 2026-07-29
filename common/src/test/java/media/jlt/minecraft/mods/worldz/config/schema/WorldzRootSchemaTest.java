package media.jlt.minecraft.mods.worldz.config.schema;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Structural invariants of {@link WorldzRootSchema#declare()} itself, distinct from {@link
 * ConfigSchemaMetadataTest}'s per-setting metadata gate.
 */
class WorldzRootSchemaTest {
    private static final WorldzRootSchema ROOT = new WorldzRootSchema();

    /**
     * Pins DESIGN §44.4.3's load-bearing declaration order (TODO 25.8b): {@code
     * SchemaSection.sanitize} runs settings in declaration order, so the {@code kits} library must
     * be fully sanitized before any preset's own {@code Rule.KitReference} reads it through {@code
     * ctx.root().kits}. A later reorder of {@link WorldzRootSchema#declare()} would break this
     * silently without this assertion.
     */
    @Test
    void kitsIsDeclaredFirst() {
        assertEquals("kits", ROOT.settings().getFirst().key());
    }
}
