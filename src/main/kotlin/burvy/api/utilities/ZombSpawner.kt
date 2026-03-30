package burvy.api.utilities

import burvy.systems.NoiseChecker
import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType
import net.minecraft.world.level.Level
import net.minecraft.world.level.levelgen.Heightmap
import kotlin.math.cos
import kotlin.math.sin

/**
 * Low level wave spawning utilities
 */
object ZombSpawner {
    const val MIN_DISTANCE = 24
    const val MAX_DISTANCE = 64
    private const val MIN_Y = 70

    // spawn a zombie targeting the given player if noisy
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
        val overworld = level.dimension() == Level.OVERWORLD
        (0 until 10).forEach { _ ->
            val rngAngle = level.random.nextDouble() * Math.PI * 2
            val rngDist = MIN_DISTANCE + level.random.nextDouble() * (MAX_DISTANCE - MIN_DISTANCE)

            val x = center.x + (cos(rngAngle) * rngDist).toInt()
            val z = center.z + (sin(rngAngle) * rngDist).toInt()

            // overworld below Y 70: heightmap surface
            // overworld above Y 70: any valid spot near player Y
            // other dimensions: any valid spot near player Y
            val pos =
                if (overworld && center.y < MIN_Y) {
                    val surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z)
                    BlockPos(x, surfaceY, z)
                } else {
                    suitableNearest(level, x, z, center.y) ?: return@forEach
                }

            if (!level.getBlockState(pos).isAir) return@forEach
            if (!level.getBlockState(pos.above()).isAir) return@forEach
            if (!level.getBlockState(pos).fluidState.isEmpty) return@forEach

            return pos
        }
        return null
    }

    // nearest valid spawn spot
    private fun suitableNearest(
        level: ServerLevel,
        x: Int,
        z: Int,
        startY: Int,
    ): BlockPos? {
        val maxY = level.maxY - 1
        val minY = level.minY + 1
        for (offset in 0..(maxY - minY)) {
            for (y in listOf(startY - offset, startY + offset)) {
                if (y !in minY..maxY) continue
                if (level.getBlockState(BlockPos(x, y - 1, z)).isAir) continue
                if (!level.getBlockState(BlockPos(x, y, z)).isAir) continue
                if (!level.getBlockState(BlockPos(x, y + 1, z)).isAir) continue
                return BlockPos(x, y, z)
            }
        }
        return null
    }
}
