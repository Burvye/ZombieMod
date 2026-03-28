package burvy.api.utilities

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.levelgen.Heightmap
import kotlin.math.cos
import kotlin.math.sin

/**
 * Low level wave spawning utilities
 */
object ZombSpawner {
    const val MIN_DISTANCE = 24
    const val MAX_DISTANCE = 64

    // spawn zombies around a player's location
    fun zombAround(
        level: ServerLevel,
        player: ServerPlayer,
        count: Int,
    ) {
        val playerPos = player.blockPosition()
        (0 until count).forEach { _ ->
            val pos = posAround(level, playerPos) ?: return@forEach
            zombAt(level, pos, player)
        }
    }

    // spawn a zombie to target the given player
    fun zombAt(
        level: ServerLevel,
        pos: BlockPos,
        target: ServerPlayer,
    ) {
        val zombie = EntityType.ZOMBIE.create(level, EntitySpawnReason.NATURAL) ?: return
        zombie.setPos(pos.x + 0.5, pos.y.toDouble(), pos.z + 0.5)
        zombie.yRot = level.random.nextFloat() * 360
        if (NoiseChecker.isNoisy(target)) {
            zombie.target = target
        }
        level.addFreshEntity(zombie)
    }

    // find a good position around a location for spawning
    fun posAround(
        level: ServerLevel,
        center: BlockPos,
    ): BlockPos? {
        (0 until 10).forEach { _ ->
            val rngAngle = level.random.nextDouble() * Math.PI * 2
            val rngDist = MIN_DISTANCE + level.random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE)

            val x = center.x + (cos(rngAngle) * rngDist).toInt()
            val z = center.z + (sin(rngAngle) * rngDist).toInt()
            val surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z)
            val pos = BlockPos(x, surfaceY, z)

            if (!level.canSeeSky(pos)) return@forEach
            if (!level.getBlockState(pos).isAir) return@forEach
            if (!level.getBlockState(pos.above()).isAir) return@forEach
            if (!level.getBlockState(pos).fluidState.isEmpty) return@forEach

            return pos
        }
        return null
    }
}
