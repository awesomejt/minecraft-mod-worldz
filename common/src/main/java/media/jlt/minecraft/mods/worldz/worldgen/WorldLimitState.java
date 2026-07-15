package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.WorldzCommon;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.OptionalLong;

/** Persistent initialization marker and delayed transition start ticks. */
public final class WorldLimitState extends SavedData {
    private static final long NO_PENDING_START = -1L;
    /** Saved-data descriptor stored in the overworld data directory. */
    public static final SavedDataType<WorldLimitState> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(WorldzCommon.MOD_ID, "world_limits"),
        () -> new WorldLimitState(false),
        RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("initialized").forGetter(WorldLimitState::initialized),
            Codec.LONG.optionalFieldOf("overworld_start_tick", NO_PENDING_START)
                .forGetter(state -> state.overworldStartTick),
            Codec.LONG.optionalFieldOf("nether_start_tick", NO_PENDING_START)
                .forGetter(state -> state.netherStartTick)
        ).apply(instance, WorldLimitState::new)),
        DataFixTypes.SAVED_DATA_WORLD_BORDER
    );

    private final boolean initialized;
    private long overworldStartTick;
    private long netherStartTick;

    /**
     * Creates a persisted initialization marker.
     *
     * @param initialized whether border schedules have been started
     */
    public WorldLimitState(boolean initialized) {
        this(initialized, NO_PENDING_START, NO_PENDING_START);
    }

    /**
     * Creates persisted initialization and pending-delay state.
     *
     * @param initialized whether initial borders and objectives were applied
     * @param overworldStartTick pending Overworld start game tick, or {@code -1}
     * @param netherStartTick pending Nether start game tick, or {@code -1}
     */
    public WorldLimitState(boolean initialized, long overworldStartTick, long netherStartTick) {
        if (overworldStartTick < NO_PENDING_START || netherStartTick < NO_PENDING_START) {
            throw new IllegalArgumentException("pending border start ticks must be -1 or nonnegative");
        }
        this.initialized = initialized;
        this.overworldStartTick = overworldStartTick;
        this.netherStartTick = netherStartTick;
    }

    /**
     * Returns whether the schedule has already been applied.
     *
     * @return initialization state
     */
    public boolean initialized() {
        return initialized;
    }

    /**
     * Returns a dimension's pending transition start tick.
     *
     * @param overworld whether to query the Overworld rather than Nether
     * @return pending game tick, or empty after the transition starts
     */
    public OptionalLong pendingStartTick(boolean overworld) {
        long tick = overworld ? this.overworldStartTick : this.netherStartTick;
        return tick == NO_PENDING_START ? OptionalLong.empty() : OptionalLong.of(tick);
    }

    /**
     * Returns whether either dimension is still waiting to resize.
     *
     * @return whether a server-tick check is needed
     */
    public boolean hasPendingStarts() {
        return this.overworldStartTick != NO_PENDING_START || this.netherStartTick != NO_PENDING_START;
    }

    /**
     * Clears a pending start after its transition begins.
     *
     * @param overworld whether to clear the Overworld rather than Nether
     */
    public void clearPendingStart(boolean overworld) {
        if (overworld) {
            this.overworldStartTick = NO_PENDING_START;
        } else {
            this.netherStartTick = NO_PENDING_START;
        }
        setDirty();
    }
}
