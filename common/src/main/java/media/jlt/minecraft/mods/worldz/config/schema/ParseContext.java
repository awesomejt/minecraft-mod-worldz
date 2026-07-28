package media.jlt.minecraft.mods.worldz.config.schema;

import org.slf4j.Logger;

import java.util.function.Consumer;

/**
 * Threaded through {@link SchemaSection#read} and {@link ValueCodec#read}: carries the logger
 * codecs use for parse-time warnings (e.g. {@code readStringList}'s "ignoring non-string entry"),
 * the current dotted path for error messages, and a presence-tracking hook.
 *
 * <p>The {@link #markPresent(String)} hook is a deliberate no-op in TODO 25.2a -- presence
 * tracking is 25.3's job. 25.3 swaps in a recording sink stored on {@code WorldzConfig}; nothing
 * about the schema itself needs to change, since the dotted paths are already built here and
 * already correct (DESIGN §41.9).
 */
public final class ParseContext {
    private final Logger logger;
    private final String path;
    private final Consumer<String> presenceSink;

    /**
     * Creates a root parse context with a discarding presence sink.
     *
     * @param logger destination for parse-time warnings
     */
    public ParseContext(Logger logger) {
        this(logger, "", dottedPath -> { });
    }

    private ParseContext(Logger logger, String path, Consumer<String> presenceSink) {
        this.logger = logger;
        this.path = path;
        this.presenceSink = presenceSink;
    }

    /**
     * Returns a context scoped one level deeper, for a nested key.
     *
     * @param key leaf key being descended into
     * @return child context whose {@link #path()} is {@code thisPath + "." + key} (or just
     *     {@code key} at the root)
     */
    public ParseContext child(String key) {
        String childPath = path.isEmpty() ? key : path + "." + key;
        return new ParseContext(logger, childPath, presenceSink);
    }

    /**
     * Returns a context carrying the same logger and presence sink, but an explicit absolute
     * path rather than one appended to the current path. Used by {@link SchemaSection}, which
     * always knows its own and its settings' full dotted paths already, so it never needs to
     * depend on the caller having pre-scoped the incoming context correctly.
     *
     * @param path the explicit absolute dotted path
     * @return the rescoped context
     */
    ParseContext rescopedTo(String path) {
        return new ParseContext(logger, path, presenceSink);
    }

    /** Returns the destination for parse-time warnings. */
    public Logger logger() {
        return logger;
    }

    /** Returns the current dotted path, for error/warning messages. */
    public String path() {
        return path;
    }

    /**
     * Records that the user explicitly wrote a key. A no-op in 25.2a; see the class Javadoc.
     *
     * @param dottedPath the full dotted path of the key that was present
     */
    public void markPresent(String dottedPath) {
        presenceSink.accept(dottedPath);
    }
}
