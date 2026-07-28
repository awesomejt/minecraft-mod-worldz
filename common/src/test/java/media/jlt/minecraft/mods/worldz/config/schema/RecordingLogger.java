package media.jlt.minecraft.mods.worldz.config.schema;

import org.slf4j.Marker;
import org.slf4j.event.Level;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;

import java.util.ArrayList;
import java.util.List;

/**
 * A test-only {@link org.slf4j.Logger} that captures every {@code WARN} line, in order, instead
 * of printing it (DESIGN §41.8). Used by {@code ConfigSchemaDifferentialTest} to compare the
 * ordered list of warnings the legacy path and the schema path each produce for the same input --
 * the strictest of the three differential checks, since an interleaving change or a lost advisory
 * changes the order or count without necessarily changing the final YAML or summary.
 *
 * <p>Extends {@link LegacyAbstractLogger} rather than implementing {@link org.slf4j.Logger}
 * directly: slf4j-api 2.0.17 (this project's test-classpath version) routes every legacy {@code
 * warn(...)} overload through {@link #handleNormalizedLoggingCall}, so a single method capture
 * point is enough regardless of which overload the production code happens to call.
 */
public final class RecordingLogger extends LegacyAbstractLogger {
    private final List<String> warnings = new ArrayList<>();

    @Override
    protected String getFullyQualifiedCallerName() {
        return RecordingLogger.class.getName();
    }

    @Override
    protected void handleNormalizedLoggingCall(Level level, Marker marker, String messagePattern, Object[] arguments, Throwable throwable) {
        if (level != Level.WARN) {
            return;
        }
        warnings.add(MessageFormatter.arrayFormat(messagePattern, arguments).getMessage());
    }

    /**
     * Returns the captured {@code WARN} messages, in the order they were logged.
     *
     * @return an unmodifiable-in-spirit (but not wrapped) list; callers should not mutate it
     */
    public List<String> warnings() {
        return warnings;
    }

    @Override
    public boolean isTraceEnabled() {
        return false;
    }

    @Override
    public boolean isDebugEnabled() {
        return false;
    }

    @Override
    public boolean isInfoEnabled() {
        return false;
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }

    @Override
    public boolean isErrorEnabled() {
        return false;
    }
}
