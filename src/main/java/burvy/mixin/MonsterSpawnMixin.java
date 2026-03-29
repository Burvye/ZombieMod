package burvy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.StructureManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Spawn only zombies around players
 */
@Mixin(NaturalSpawner.class)
public class MonsterSpawnMixin {

    @Inject(method = "getRandomSpawnMobAt", at = @At("RETURN"), cancellable = true)
    private static void zombieFilter(
            ServerLevel dimension,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            MobCategory mobCategory,
            net.minecraft.util.RandomSource randomSource,
            BlockPos blockPos,
            CallbackInfoReturnable<Optional<MobSpawnSettings.SpawnerData>> cir
    ) {
        if (mobCategory != MobCategory.MONSTER) return;

        Optional<MobSpawnSettings.SpawnerData> result = cir.getReturnValue();
        if (result.isEmpty()) return;
        EntityType<?> type = result.get().type();
        if (type != EntityType.ZOMBIE
                && type != EntityType.HUSK
                && type != EntityType.DROWNED
                && type != EntityType.ZOMBIE_VILLAGER) {
            cir.setReturnValue(Optional.empty());
        }
    }

    @Inject(method = "isRightDistanceToPlayerAndSpawnPoint", at = @At("RETURN"), cancellable = true)
    private static void spawnRules(
            ServerLevel dimension,
            ChunkAccess chunkAccess,
            BlockPos.MutableBlockPos pos,
            double distanceToPlayer,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!cir.getReturnValue()) return;

        // XZ distance to nearest player
        double playerDistXZSq = Double.MAX_VALUE;
        for (ServerPlayer player : dimension.players()) {
            double dx = player.getX() - pos.getX();
            double dz = player.getZ() - pos.getZ();
            playerDistXZSq = Math.min(playerDistXZSq, dx * dx + dz * dz);
        }

        // outside 24 < x < 64, cant spawn
        if (playerDistXZSq < 576.0 || playerDistXZSq > 4096.0) {
            cir.setReturnValue(false);
            return;
        }

        // overworld only: cant see sky, cant spawn
        if (dimension.dimension() == Level.OVERWORLD && !dimension.canSeeSky(pos)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "canSpawnMobAt", at = @At("RETURN"), cancellable = true)
    private static void permissiveLight(
            ServerLevel dimension,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            MobCategory mobCategory,
            MobSpawnSettings.SpawnerData spawnerData,
            BlockPos blockPos,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (mobCategory != MobCategory.MONSTER) return;

        if (!cir.getReturnValue()) {
            cir.setReturnValue(true);
        }
    }
}
