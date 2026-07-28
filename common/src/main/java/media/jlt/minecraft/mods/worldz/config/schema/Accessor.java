package media.jlt.minecraft.mods.worldz.config.schema;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * A getter/setter pair over a single field of the owning POJO {@code S} (DESIGN §41.2). Always a
 * plain lambda over a public field -- no reflection anywhere, so a field rename is a compile
 * error rather than a silent runtime miss.
 *
 * @param getter reads the field
 * @param setter writes the field
 */
public record Accessor<S, T>(Function<S, T> getter, BiConsumer<S, T> setter) {
    public T get(S owner) {
        return getter.apply(owner);
    }

    public void set(S owner, T value) {
        setter.accept(owner, value);
    }
}
