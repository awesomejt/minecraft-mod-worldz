package media.jlt.minecraft.mods.worldz.config.schema;

import java.util.Map;

/**
 * The four operations every config section supports (DESIGN §41.8): parse, sanitize, emit and
 * summarize. Both {@link SchemaSection} (the declarative implementation) and the legacy adapters
 * wrapping today's hand-written {@code readXConfig}/{@code sanitizeX}/{@code xMap}/{@code
 * xSummary} methods implement this, so production code can be indifferent to which one it's
 * calling -- that indifference is what makes the differential harness possible.
 */
public interface SectionCodec<S> {
    /**
     * Parses a raw YAML-loaded value into {@code S}, replacing {@code readXConfig}.
     *
     * @param raw the raw value from SnakeYAML's loaded tree
     * @param ctx parse context (logger, dotted path, presence hook)
     * @return the parsed value
     * @throws IllegalArgumentException on a bad shape/type, with today's exact message
     */
    S read(Object raw, ParseContext ctx);

    /**
     * Validates and clamps a value, replacing {@code sanitizeX}.
     *
     * @param value the value to sanitize, or {@code null}
     * @param ctx sanitize context (logger, root)
     * @return the sanitized value (a fresh instance when {@code value} was {@code null})
     */
    S sanitize(S value, SanitizeContext ctx);

    /**
     * Converts a value into the {@code LinkedHashMap} SnakeYAML dumps, replacing {@code xMap}.
     *
     * @param value the value to emit
     * @return the map to write into the parent's own emitted map
     */
    Map<String, Object> toMap(S value);

    /**
     * Renders a compact summary segment, replacing {@code xSummary}.
     *
     * @param value the value to summarize
     * @return the summary text (not including the leading {@code "name="} the caller prepends)
     */
    String summary(S value);
}
