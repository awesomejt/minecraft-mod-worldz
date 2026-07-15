package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.WorldzCommon;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Persistent marker preventing a saved border schedule from being restarted. */
public final class WorldLimitState extends SavedData {
    /** Saved-data descriptor stored in the overworld data directory. */
    public static final SavedDataType<WorldLimitState> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(WorldzCommon.MOD_ID, "world_limits"),
        () -> new WorldLimitState(false),
        RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("initialized").forGetter(WorldLimitState::initialized)
        ).apply(instance, WorldLimitState::new)),
        DataFixTypes.SAVED_DATA_WORLD_BORDER
    );

    private final boolean initialized;

    /**
     * Creates a persisted initialization marker.
     *
     * @param initialized whether border schedules have been started
     */
    public WorldLimitState(boolean initialized) {
        this.initialized = initialized;
    }

    /**
     * Returns whether the schedule has already been applied.
     *
     * @return initialization state
     */
    public boolean initialized() {
        return initialized;
    }
}
