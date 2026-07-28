package media.jlt.minecraft.mods.worldz.config.schema;

/**
 * YAML {@code Object} &lt;-&gt; typed value, both directions (DESIGN §41.3). The only place YAML
 * types are touched -- parse and emit both go through the same codec, which is what makes
 * byte-identical output achievable at all (DESIGN §41.1's ordering invariant).
 */
public interface ValueCodec<T> {
    /**
     * Converts a raw YAML-loaded value into {@code T}.
     *
     * @param raw the raw value from SnakeYAML's loaded tree
     * @param ctx parse context (logger, dotted path for messages)
     * @return the typed value
     * @throws IllegalArgumentException using today's exact message text, on a bad shape/type
     */
    T read(Object raw, ParseContext ctx);

    /**
     * Converts a typed value back into the exact {@code Object} SnakeYAML dumps today. Must
     * return today's exact Java type (no widening) -- an {@code Integer} must stay an
     * {@code Integer}, not become a {@code Long}, or SnakeYAML's rendering changes and
     * byte-identity breaks (DESIGN §41.3, R8).
     *
     * @param value the typed value
     * @return the object to put into the emitted {@code LinkedHashMap}
     */
    Object write(T value);
}
