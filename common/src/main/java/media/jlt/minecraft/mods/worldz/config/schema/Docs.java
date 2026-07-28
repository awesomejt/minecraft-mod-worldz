package media.jlt.minecraft.mods.worldz.config.schema;

/**
 * Human-facing documentation attached to a {@link Setting} (DESIGN §41.2). Populated during
 * TODO 25.2 so the 25.4 reference generator and the 25.10 README tables are a pure walk over the
 * same objects that drive parsing -- they cannot drift from what the code actually does.
 *
 * @param doc one-line description of what the setting does
 * @param unit the physical/conceptual unit the value is measured in
 * @param defaultText the rendered default value, for the reference generator
 * @param rangeText the rendered valid range, or {@code ""} when the setting has none
 */
public record Docs(String doc, Unit unit, String defaultText, String rangeText) {
    /** A blank placeholder for settings not yet documented; every real setting must override this. */
    public static final Docs NONE = new Docs("", Unit.NONE, "", "");
}
