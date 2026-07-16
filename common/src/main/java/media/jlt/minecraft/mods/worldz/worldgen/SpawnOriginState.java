package media.jlt.minecraft.mods.worldz.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import media.jlt.minecraft.mods.worldz.WorldzCommon;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Persisted layout-origin resolution marker for the {@code PREFERRED_NATURAL_BIOME}
 * spawn strategy. The origin itself is re-applied to the live
 * {@code LimitedBiomeSource} on every load (see {@code SpawnOriginManager}) rather
 * than stored on that codec-decoded, otherwise-immutable object.
 */
public final class SpawnOriginState extends SavedData {
    /** Saved-data descriptor stored in the overworld data directory. */
    public static final SavedDataType<SpawnOriginState> TYPE = new SavedDataType<>(
        Identifier.fromNamespaceAndPath(WorldzCommon.MOD_ID, "spawn_origin"),
        () -> new SpawnOriginState(false, 0, 0),
        RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("initialized").forGetter(SpawnOriginState::initialized),
            Codec.INT.fieldOf("origin_x").forGetter(SpawnOriginState::originBlockX),
            Codec.INT.fieldOf("origin_z").forGetter(SpawnOriginState::originBlockZ)
        ).apply(instance, SpawnOriginState::new)),
        DataFixTypes.SAVED_DATA_WORLD_BORDER
    );

    private final boolean initialized;
    private final int originBlockX;
    private final int originBlockZ;

    /**
     * Creates a persisted origin-resolution marker.
     *
     * @param initialized whether the origin has already been resolved
     * @param originBlockX resolved origin X, meaningful only when initialized
     * @param originBlockZ resolved origin Z, meaningful only when initialized
     */
    public SpawnOriginState(boolean initialized, int originBlockX, int originBlockZ) {
        this.initialized = initialized;
        this.originBlockX = originBlockX;
        this.originBlockZ = originBlockZ;
    }

    /**
     * Returns whether the origin has already been resolved for this world.
     *
     * @return initialization state
     */
    public boolean initialized() {
        return this.initialized;
    }

    /**
     * Returns the resolved origin's X coordinate.
     *
     * @return origin block X
     */
    public int originBlockX() {
        return this.originBlockX;
    }

    /**
     * Returns the resolved origin's Z coordinate.
     *
     * @return origin block Z
     */
    public int originBlockZ() {
        return this.originBlockZ;
    }
}
