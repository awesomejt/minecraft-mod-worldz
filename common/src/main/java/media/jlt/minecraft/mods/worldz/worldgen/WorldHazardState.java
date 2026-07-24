package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.WorldzCommon;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Persistent world-hazard state (GOALS 29-30, DESIGN §35): whether forever night has already
 * locked, its pending delayed-lock tick, and rising lava's own schedule origin and progress
 * (DESIGN §35.2). Mirrors {@link WorldLimitState}'s exact shape/idiom -- a small saved-data
 * marker, not a full schedule.
 */
public final class WorldHazardState extends SavedData {
    private static final long NO_PENDING_LOCK = -1L;
    private static final long NO_LAVA_ORIGIN = -1L;
    private static final int NO_LAVA_PROGRESS = Integer.MIN_VALUE;

    /** Saved-data descriptor stored in the overworld data directory. */
    public static final SavedDataType<WorldHazardState> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(WorldzCommon.MOD_ID, "world_hazards"),
        WorldHazardState::new,
        RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("night_locked").forGetter(state -> state.nightLocked),
            Codec.LONG.optionalFieldOf("pending_lock_tick", NO_PENDING_LOCK).forGetter(state -> state.pendingLockTick),
            Codec.LONG.optionalFieldOf("lava_origin_tick", NO_LAVA_ORIGIN).forGetter(state -> state.lavaOriginTick),
            Codec.INT.optionalFieldOf("last_applied_lava_y", NO_LAVA_PROGRESS).forGetter(state -> state.lastAppliedLavaY)
        ).apply(instance, WorldHazardState::new)),
        DataFixTypes.SAVED_DATA_WORLD_BORDER
    );

    private boolean nightLocked;
    private long pendingLockTick;
    private long lavaOriginTick;
    private int lastAppliedLavaY;

    /**
     * Creates fresh, uninitialized world-hazard state.
     */
    public WorldHazardState() {
        this(false, NO_PENDING_LOCK, NO_LAVA_ORIGIN, NO_LAVA_PROGRESS);
    }

    /**
     * Creates persisted world-hazard state.
     *
     * @param nightLocked whether night has already been locked
     * @param pendingLockTick pending delayed-lock game tick, or {@code -1}
     * @param lavaOriginTick the game tick rising lava's own schedule began counting from, or
     *     {@code -1} if not yet started
     * @param lastAppliedLavaY the highest Y already converted to lava, or {@link
     *     Integer#MIN_VALUE} if none yet
     */
    public WorldHazardState(boolean nightLocked, long pendingLockTick, long lavaOriginTick, int lastAppliedLavaY) {
        if (pendingLockTick < NO_PENDING_LOCK) {
            throw new IllegalArgumentException("pending lock tick must be -1 or nonnegative");
        }
        if (lavaOriginTick < NO_LAVA_ORIGIN) {
            throw new IllegalArgumentException("lava origin tick must be -1 or nonnegative");
        }
        this.nightLocked = nightLocked;
        this.pendingLockTick = pendingLockTick;
        this.lavaOriginTick = lavaOriginTick;
        this.lastAppliedLavaY = lastAppliedLavaY;
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

    /**
     * Returns the game tick rising lava's own schedule began counting from.
     *
     * @return origin game tick, or empty when the schedule hasn't started yet
     */
    public OptionalLong lavaOriginTick() {
        return this.lavaOriginTick == NO_LAVA_ORIGIN ? OptionalLong.empty() : OptionalLong.of(this.lavaOriginTick);
    }

    /**
     * Records the schedule's origin tick, once, the first time rising lava becomes active.
     *
     * @param tick the game tick to measure elapsed schedule time from
     */
    public void recordLavaOrigin(long tick) {
        this.lavaOriginTick = tick;
        setDirty();
    }

    /**
     * Returns the highest Y already converted to lava.
     *
     * @return applied Y, or empty when nothing has been converted yet
     */
    public OptionalInt lastAppliedLavaY() {
        return this.lastAppliedLavaY == NO_LAVA_PROGRESS ? OptionalInt.empty() : OptionalInt.of(this.lastAppliedLavaY);
    }

    /**
     * Records progress after converting every loaded column up to a new level.
     *
     * @param y the highest Y just converted
     */
    public void recordLastAppliedLavaY(int y) {
        this.lastAppliedLavaY = y;
        setDirty();
    }
}
