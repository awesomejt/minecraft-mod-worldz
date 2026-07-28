package media.jlt.minecraft.mods.worldz.config.schema;

/**
 * The physical/conceptual unit a {@link Setting}'s value is measured in (DESIGN §41.2). Lets the
 * 25.4 reference generator render "Unit: blocks" mechanically instead of per-setting prose, and
 * makes D7's "documentation carries the unit" checkable rather than aspirational.
 */
public enum Unit {
    /** A distance or width measured in blocks. */
    BLOCKS,
    /** A distance measured in chunks (16-block columns). */
    CHUNKS,
    /** A duration measured in in-game days. */
    DAYS,
    /** An absolute vertical coordinate. */
    Y_LEVEL,
    /** A probability in {@code [0.0, 1.0]}. */
    CHANCE,
    /** A plain count with no other unit. */
    COUNT,
    /** A dimensionless multiplier or shape-strength coefficient (not a 0..1 probability). */
    FACTOR,
    /** A biome id or biome-tag id string. */
    BIOME_ID,
    /** A list of item ids (optionally with counts). */
    ITEM_LIST,
    /** No unit applies (flags, enums, free text). */
    NONE
}
