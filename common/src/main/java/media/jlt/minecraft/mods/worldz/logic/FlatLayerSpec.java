package media.jlt.minecraft.mods.worldz.logic;

/**
 * One block-and-thickness entry in a flat layer stack (GOALS 15/16, DESIGN §33.2/§33.5), bottom
 * to top -- shared by {@code jlt_worldz:flat}'s full-height layer list and (Phase 16.2b)
 * {@code jlt_worldz:deep_flat}'s cap-layer list.
 *
 * @param blockId the layer's block id
 * @param heightBlocks the layer's thickness in blocks
 */
public record FlatLayerSpec(String blockId, int heightBlocks) {
    /** Validates the entry. */
    public FlatLayerSpec {
        if (blockId == null || blockId.isBlank()) {
            throw new IllegalArgumentException("Layer block id is required.");
        }
        if (heightBlocks <= 0) {
            throw new IllegalArgumentException("Layer height must be positive.");
        }
    }

    /**
     * Parses the config-file/text-editor shorthand: {@code "<block id>"} (height 1) or
     * {@code "<block id>:<height>"} -- mirrors {@code StarterKitPlan.ItemAmount.parse}'s exact
     * convention (only a trailing all-digit colon segment is treated as the height, since block
     * ids already contain a {@code namespace:path} colon).
     *
     * @param raw shorthand text
     * @return parsed block/height pair
     */
    public static FlatLayerSpec parse(String raw) {
        String trimmed = raw == null ? "" : raw.trim();
        int separator = trimmed.lastIndexOf(':');
        if (separator > 0 && separator < trimmed.length() - 1 && isDigits(trimmed.substring(separator + 1))) {
            return new FlatLayerSpec(trimmed.substring(0, separator), Integer.parseInt(trimmed.substring(separator + 1)));
        }
        return new FlatLayerSpec(trimmed, 1);
    }

    private static boolean isDigits(String value) {
        return !value.isEmpty() && value.chars().allMatch(Character::isDigit);
    }

    /**
     * Renders the config-file/text-editor shorthand.
     *
     * @return {@code "<block id>:<height>"}
     */
    public String format() {
        return this.blockId + ":" + this.heightBlocks;
    }
}
