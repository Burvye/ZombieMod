package burvy.systems

import burvy.api.utilities.ClaimChecker
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.math.abs

/**
 * Run zombie building/breaking every 5 seconds
 */
object ZombBlocks {
    private const val COOLDOWN_TICKS = 100 // 5 seconds
    private const val PILE_THRESHOLD = 5 // amount of zombies before turning into block
    private const val SOUND_VOLUME = 3.0f // 3 * 16 blocks

    fun tick(level: ServerLevel) {
        val tick = level.server.tickCount.toLong()
        if (tick % COOLDOWN_TICKS != 0L) return

        val seen = HashSet<UUID>()

        for (player in level.players()) {
            val range = AABB.ofSize(player.position(), 192.0, 128.0, 192.0)
            val zombies = level.getEntitiesOfClass(Zombie::class.java, range)

            for (zombie in zombies) {
                if (!seen.add(zombie.uuid)) continue
                val targetPos = zombie.target?.blockPosition() ?: continue

                val zombiePos = zombie.blockPosition()
                // always try to pile first
                if (pileAt(level, zombie, zombiePos)) continue

                when {
                    targetPos.y > zombiePos.y ->
                        breakAt(level, zombiePos, targetPos, BreakPattern.UP)
                    targetPos.y == zombiePos.y ->
                        breakAt(level, zombiePos, targetPos, BreakPattern.FORWARD)
                    else ->
                        breakAt(level, zombiePos, targetPos, BreakPattern.DOWN)
                }
            }
        }
    }

    private enum class BreakPattern { UP, FORWARD, DOWN }

    private fun cardinalBetween(
        from: BlockPos,
        to: BlockPos,
    ): Direction {
        val dx = to.x - from.x
        val dz = to.z - from.z
        return if (abs(dx) >= abs(dz)) {
            if (dx > 0) Direction.EAST else Direction.WEST
        } else {
            if (dz > 0) Direction.SOUTH else Direction.NORTH
        }
    }

    // break this block as a zombie
    private fun breakBlock(
        level: ServerLevel,
        pos: BlockPos,
    ): Boolean {
        if (level.getBlockState(pos).isAir) return false
        if (!ClaimChecker.isClaimed(level, pos)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)
        }
        return true
    }

    // break blocks between zombie and target depending on vertical diff
    private fun breakAt(
        level: ServerLevel,
        zombiePos: BlockPos,
        targetPos: BlockPos,
        pattern: BreakPattern,
    ): Boolean {
        val dir = cardinalBetween(zombiePos, targetPos)
        val eyes = zombiePos.above()

        // processing inline (functional programmingg)
        val hit =
            when (pattern) {
                BreakPattern.DOWN ->
                    breakBlock(level, zombiePos.relative(dir)) or
                        breakBlock(level, zombiePos.below()) or
                        breakBlock(level, zombiePos.relative(dir).below())
                BreakPattern.FORWARD ->
                    breakBlock(level, eyes.relative(dir)) or
                        breakBlock(level, zombiePos.relative(dir))
                BreakPattern.UP ->
                    breakBlock(level, eyes.relative(dir)) or
                        breakBlock(level, eyes.above()) or
                        breakBlock(level, eyes.relative(dir).above())
            }

        if (hit) {
            level.playSound(
                null,
                zombiePos,
                SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
                SoundSource.HOSTILE,
                SOUND_VOLUME,
                1.0f,
            )
        }

        return hit
    }

    // TODO: Hook into zombie deaths. If a zombie dies of fall damage, also pile at.
    private fun pileAt(
        level: ServerLevel,
        zombie: Zombie,
        zombiePos: BlockPos,
    ): Boolean {
        val center = Vec3.atLowerCornerOf(zombiePos)
        val box = AABB.ofSize(center, 3.0, 4.0, 3.0)
        val nearby = level.getEntitiesOfClass(Zombie::class.java, box)
        if (nearby.size < PILE_THRESHOLD) return false
        // pile non full blocks only
        if (level.getBlockState(zombiePos).isCollisionShapeFullBlock(level, zombiePos)) return false

        level.setBlockAndUpdate(zombiePos, Blocks.GRAVEL.defaultBlockState())

        level.playSound(
            null,
            zombiePos,
            SoundEvents.SLIME_BLOCK_PLACE,
            SoundSource.HOSTILE,
            SOUND_VOLUME,
            1.0f,
        )

        zombie.discard()
        return true
    }
}
