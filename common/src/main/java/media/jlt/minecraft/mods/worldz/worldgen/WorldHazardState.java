package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.WorldzCommon;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.OptionalLong;

/**
 * Persistent world-hazard state (GOALS 29-30, DESIGN §35): whether forever night has already
 * locked, its pending delayed-lock tick, and (DESIGN §35.2) rising lava's own progress. Mirrors
 * {@link WorldLimitState}'s exact shape/idiom -- a small saved-data marker, not a full schedule.
 */
public final class WorldHazardState extends SavedData {
    private static final long NO_PENDING_LOCK = -1L;

    /** Saved-data descriptor stored in the overworld data directory. */
    public static final SavedDataType<WorldHazardState> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(WorldzCommon.MOD_ID, "world_hazards"),
        () -> new WorldHazardState(false, NO_PENDING_LOCK),
        RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("night_locked").forGetter(state -> state.nightLocked),
            Codec.LONG.optionalFieldOf("pending_lock_tick", NO_PENDING_LOCK).forGetter(state -> state.pendingLockTick)
        ).apply(instance, WorldHazardState::new)),
        DataFixTypes.SAVED_DATA_WORLD_BORDER
    );

    private boolean nightLocked;
    private long pendingLockTick;

    /**
     * Creates persisted forever-night state.
     *
     * @param nightLocked whether night has already been locked
     * @param pendingLockTick pending delayed-lock game tick, or {@code -1}
     */
    public WorldHazardState(boolean nightLocked, long pendingLockTick) {
        if (pendingLockTick < NO_PENDING_LOCK) {
            throw new IllegalArgumentException("pending lock tick must be -1 or nonnegative");
        }
        this.nightLocked = nightLocked;
        this.pendingLockTick = pendingLockTick;
    }

    /**
     * Returns whether night has already been locked.
     *
     * @return locked state
     */
    public boolean nightLocked() {
        return this.nightLocked;
    }

    /**
     * Records night as locked and clears any pending delayed lock.
     */
    public void markNightLocked() {
        this.nightLocked = true;
        this.pendingLockTick = NO_PENDING_LOCK;
        setDirty();
    }

    /**
     * Returns the pending delayed-lock game tick, if one is scheduled.
     *
     * @return pending game tick, or empty when no lock is pending
     */
    public OptionalLong pendingLockTick() {
        return this.pendingLockTick == NO_PENDING_LOCK ? OptionalLong.empty() : OptionalLong.of(this.pendingLockTick);
    }

    /**
     * Records a delayed lock as newly scheduled.
     *
     * @param tick the game tick night should lock at
     */
    public void schedulePendingLock(long tick) {
        this.pendingLockTick = tick;
        setDirty();
    }
}
