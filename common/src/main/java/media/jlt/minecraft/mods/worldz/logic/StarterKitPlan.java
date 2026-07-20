package media.jlt.minecraft.mods.worldz.logic;

import java.util.ArrayList;
import java.util.List;

/**
 * A deterministic starter-item kit (GOALS 03; DESIGN §25.3): a fixed "essentials" list, always
 * included, plus a configurable "extras" pool resolved by picking {@link #extrasCount} items
 * from it (with replacement) using the world's real seed, so a given world always hands out the
 * same kit. Kept general enough for a later phase (10 sky island, 25 cave, 27 Nether, 34 End) to
 * layer a tier concept on top of multiple instances of this record without retrofitting it --
 * no tier concept is built here, since GOALS 03 itself never asks for one.
 *
 * @param essentials always-included item/count pairs
 * @param extras candidate item/count pairs the random picks draw from
 * @param extrasCount how many extras to pick, with replacement
 */
public record StarterKitPlan(List<ItemAmount> essentials, List<ItemAmount> extras, int extrasCount) {
    /** Validates and canonicalizes kit values. */
    public StarterKitPlan {
        essentials = List.copyOf(essentials);
        extras = List.copyOf(extras);
        if (extrasCount < 0) {
            throw new IllegalArgumentException("Extras count must not be negative.");
        }
        if (extrasCount > 0 && extras.isEmpty()) {
            throw new IllegalArgumentException("Extras count is positive but the extras pool is empty.");
        }
    }

    /**
     * Resolves the deterministic contents of this kit for one world.
     *
     * @param seed the world's real seed
     * @return essentials followed by {@link #extrasCount} seed-picked extras
     */
    public List<ItemAmount> resolve(long seed) {
        List<ItemAmount> resolved = new ArrayList<>(essentials);
        for (int index = 0; index < extrasCount; index++) {
            int pick = (int) Math.floorMod(hashIndex(seed, index), (long) extras.size());
            resolved.add(extras.get(pick));
        }
        return List.copyOf(resolved);
    }

    private static long hashIndex(long seed, long salt) {
        long h = splitmix64(seed);
        h = splitmix64(h ^ salt);
        return h;
    }

    private static long splitmix64(long x) {
        x += 0x9E3779B97F4A7C15L;
        x = (x ^ (x >>> 30)) * 0xBF58476D1CE4E5B9L;
        x = (x ^ (x >>> 27)) * 0x94D049BB133111EBL;
        return x ^ (x >>> 31);
    }

    /**
     * One item-stack entry.
     *
     * @param itemId a vanilla item id
     * @param count a positive stack count
     */
    public record ItemAmount(String itemId, int count) {
        /** Validates the entry. */
        public ItemAmount {
            if (itemId == null || itemId.isBlank()) {
                throw new IllegalArgumentException("Item id is required.");
            }
            if (count <= 0) {
                throw new IllegalArgumentException("Item count must be positive.");
            }
        }

        /**
         * Parses the config-file shorthand: {@code "<item id>"} (count 1) or
         * {@code "<item id>:<count>"}. Only the trailing colon-separated segment is treated as a
         * count (and only when every character in it is a digit), since item ids themselves
         * already contain a {@code namespace:path} colon.
         *
         * @param raw shorthand text
         * @return parsed item/count pair
         */
        public static ItemAmount parse(String raw) {
            String trimmed = raw == null ? "" : raw.trim();
            int separator = trimmed.lastIndexOf(':');
            if (separator > 0 && separator < trimmed.length() - 1 && isDigits(trimmed.substring(separator + 1))) {
                return new ItemAmount(trimmed.substring(0, separator), Integer.parseInt(trimmed.substring(separator + 1)));
            }
            return new ItemAmount(trimmed, 1);
        }

        private static boolean isDigits(String value) {
            return value.chars().allMatch(Character::isDigit);
        }
    }
}
