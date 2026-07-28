package media.jlt.minecraft.mods.worldz.config.schema;

import media.jlt.minecraft.mods.worldz.config.WorldzConfig;
import org.slf4j.Logger;

/**
 * Threaded through {@link SchemaSection#sanitize} and {@link Rule#apply}. {@code root} is a
 * deliberate, documented coupling with exactly one user: {@code ExteriorSchema} (TODO 25.2c),
 * whose validation reads the sibling border config -- it cannot be deferred to a root-level
 * post-pass because the exterior's own clamps and cross-section checks interleave (DESIGN
 * §41.5, R2). Sections that don't need it may receive {@code null}.
 *
 * @param logger destination for sanitize-time warnings
 * @param root the config instance being sanitized, or {@code null} when the caller has none
 *     available (e.g. a nested legacy sanitize helper with no access to the root instance)
 */
public record SanitizeContext(Logger logger, WorldzConfig root) {
}
