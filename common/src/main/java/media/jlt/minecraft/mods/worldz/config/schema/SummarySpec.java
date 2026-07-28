package media.jlt.minecraft.mods.worldz.config.schema;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * How a {@link Setting} contributes to its section's {@code summary()} log line (DESIGN §41.2,
 * §41.5). {@link SchemaSection#summary(Object)} joins {@code label + "=" + render(value)} for
 * every setting whose {@link #include} predicate is true and whose {@link #shown} flag is set,
 * in declaration order.
 *
 * @param label the segment label; defaults to the setting's own key
 * @param render how to render the value; defaults to {@link String#valueOf(Object)}
 * @param include whether this setting contributes to the summary for a given owner instance
 *     (used by the one conditional segment, {@code chunkIsland.topOnlyDepthBlocks})
 * @param shown whether this setting is ever part of the derived summary at all
 */
public record SummarySpec<S, T>(String label, Function<T, String> render, Predicate<S> include, boolean shown) {
    static <S, T> SummarySpec<S, T> defaultFor(String key) {
        return new SummarySpec<>(key, String::valueOf, owner -> true, true);
    }

    static <S, T> SummarySpec<S, T> hidden(String key) {
        return new SummarySpec<>(key, String::valueOf, owner -> true, false);
    }
}
