package media.jlt.minecraft.mods.worldz.config.schema;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A declarative config section: an ordered list of {@link Setting}s that together replace one
 * of today's hand-written {@code readXConfig}/{@code sanitizeX}/{@code xMap}/{@code xSummary}
 * quads (DESIGN §41.5). Sections are instances, not static singletons, because three real shapes
 * force parameterization: one POJO field with two YAML key names (border's {@code objectiveKey}),
 * per-parent bounds (the starter capsule's Nether-start vs End-start limits), and per-parent
 * legality (exterior's ocean mode).
 *
 * <p><strong>The declaration order returned by {@link #declare()} must equal today's {@code
 * xMap} emit order</strong> -- that ordering invariant (DESIGN §41.1) is what lets one list drive
 * both parse and emit with byte-identical output, and the differential harness
 * ({@code ConfigSchemaDifferentialTest}) is what enforces it.
 */
public abstract class SchemaSection<S> implements SectionCodec<S> {
    private final String path;
    private final Supplier<S> factory;
    private List<Setting<S, ?>> settings;

    /**
     * @param path this section's own dotted path (e.g. {@code "cave.easyKit"}), used to build
     *     every leaf setting's full dotted name and every warning/exception message
     * @param factory produces a fresh default instance; also backs {@link #defaults()}
     */
    protected SchemaSection(String path, Supplier<S> factory) {
        this.path = path;
        this.factory = factory;
    }

    /**
     * Declares this section's settings, in the exact order today's {@code xMap} emits them.
     *
     * @return the ordered settings
     */
    protected abstract List<Setting<S, ?>> declare();

    /** Returns this section's own dotted path. */
    public final String path() {
        return path;
    }

    /** Returns the declared settings (cached after the first call). */
    public final List<Setting<S, ?>> settings() {
        if (settings == null) {
            settings = declare();
        }
        return settings;
    }

    /** Returns a fresh default instance, for the 25.4 reference generator. */
    public final S defaults() {
        return factory.get();
    }

    @Override
    public final S read(Object raw, ParseContext ctx) {
        if (!(raw instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException(path + " must be a mapping");
        }
        ParseContext scoped = ctx.rescopedTo(path);
        S target = factory.get();
        for (Setting<S, ?> setting : settings()) {
            if (map.containsKey(setting.key())) {
                readOne(target, setting, map.get(setting.key()), scoped);
            }
        }
        return target;
    }

    private <T> void readOne(S target, Setting<S, T> setting, Object raw, ParseContext scoped) {
        String fullPath = childPath(setting.key());
        ParseContext leaf = scoped.rescopedTo(fullPath);
        T value = setting.codec().read(raw, leaf);
        setting.accessor().set(target, value);
        leaf.markPresent(fullPath);
    }

    /** Builds a leaf setting's full dotted path, omitting the leading {@code "."} at the config
     * root, whose own {@link #path()} is {@code ""} (TODO 25.2g's {@code WorldzRootSchema}). Every
     * non-root section has a non-empty {@link #path()}, so this is behavior-identical to the
     * unconditional {@code path + "." + key} it replaces for all 25 sections converted before now. */
    private String childPath(String key) {
        return path.isEmpty() ? key : path + "." + key;
    }

    @Override
    public S sanitize(S value, SanitizeContext ctx) {
        S sanitized = value == null ? factory.get() : value;
        for (Setting<S, ?> setting : settings()) {
            sanitizeOne(sanitized, setting, ctx);
        }
        postValidate(sanitized, ctx);
        return sanitized;
    }

    private <T> void sanitizeOne(S owner, Setting<S, T> setting, SanitizeContext ctx) {
        String fullPath = childPath(setting.key());
        T current = setting.accessor().get(owner);
        T sanitizedValue = setting.rule().apply(owner, current, fullPath, ctx);
        setting.accessor().set(owner, sanitizedValue);
    }

    /**
     * Cross-field validation hook, run after every setting's own rule (DESIGN §41.5). Used by
     * sections whose validation needs to look at two of the section's own fields together (e.g.
     * {@code StarterKitConfig.extrasCount} clamped to {@code 0} when {@code extras} is empty).
     *
     * @param value the sanitized value
     * @param ctx sanitize context (logger, root)
     */
    protected void postValidate(S value, SanitizeContext ctx) {
    }

    @Override
    public final Map<String, Object> toMap(S value) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (Setting<S, ?> setting : settings()) {
            toMapOne(values, setting, value);
        }
        return values;
    }

    private <T> void toMapOne(Map<String, Object> values, Setting<S, T> setting, S value) {
        values.put(setting.key(), setting.codec().write(setting.accessor().get(value)));
    }

    /**
     * Renders {@code label=render(value)} for every setting whose summary is shown and included,
     * joined by {@code ", "}, in declaration order. Overridden by the handful of sections whose
     * log line isn't mechanically derivable (DESIGN §41.6's table) -- summary() is a log line, not
     * a contract, so those overrides stay hand-written rather than risk byte-identity.
     */
    @Override
    public String summary(S value) {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (Setting<S, ?> setting : settings()) {
            first = summarizeOne(builder, setting, value, first);
        }
        return builder.toString();
    }

    private <T> boolean summarizeOne(StringBuilder builder, Setting<S, T> setting, S value, boolean first) {
        SummarySpec<S, T> spec = setting.summary();
        if (!spec.shown() || !spec.include().test(value)) {
            return first;
        }
        if (!first) {
            builder.append(", ");
        }
        builder.append(spec.label()).append('=').append(spec.render().apply(setting.accessor().get(value)));
        return false;
    }
}
