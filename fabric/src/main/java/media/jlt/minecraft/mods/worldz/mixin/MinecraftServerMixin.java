package media.jlt.minecraft.mods.worldz.mixin;

import media.jlt.minecraft.mods.worldz.worldgen.SpawnOriginManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.progress.LevelLoadListener;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.storage.ServerLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/**
 * Supplies Fabric's missing equivalent of NeoForge's cancellable
 * {@code LevelEvent.CreateSpawnPosition}: a hook before vanilla's own spawn
 * search that still has the real, finalized seed bound (DESIGN §18).
 */
@Mixin(MinecraftServer.class)
abstract class MinecraftServerMixin {
    @Inject(method = "setInitialSpawn", at = @At("HEAD"), cancellable = true)
    private static void jltWorldz$setInitialSpawn(
        ServerLevel level,
        ServerLevelData levelData,
        boolean spawnBonusChest,
        boolean isDebug,
        LevelLoadListener levelLoadListener,
        CallbackInfo callback
    ) {
        Optional<BlockPos> resolved = SpawnOriginManager.resolveFreshOrigin(level);
        if (resolved.isPresent()) {
            levelData.setSpawn(LevelData.RespawnData.of(level.dimension(), resolved.get(), 0.0F, 0.0F));
            callback.cancel();
        }
    }
}
