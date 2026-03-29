package burvy.api.utilities

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.math.abs

/**
 * Zombie world modification. Runs every 5 seconds, processes all zombies.
 */
object ZombBlocks {
    private const val COOLDOWN_TICKS = 100 // 5 seconds
    private const val PILE_THRESHOLD = 5 // amount of zombies before turning into block
    private const val SOUND_VOLUME = 2.0f // 32 blocks

    fun tick(level: ServerLevel) {
        val tick = level.server.tickCount.toLong()
        if (tick % COOLDOWN_TICKS != 0L) return

        val seen = HashSet<UUID>()

        for (player in level.players()) {
            val range = AABB.ofSize(player.position(), 192.0, 128.0, 192.0)
            val zombies = level.getEntitiesOfClass(Zombie::class.java, range)

            for (zombie in zombies) {
                if (!seen.add(zombie.uuid)) continue

                val zombiePos = zombie.blockPosition()
                val targetPos =
                    zombie.target?.blockPosition()
                        ?: ZombInvestigate.getTarget(zombie.uuid)
                        ?: continue
                val canPile =
                    level.dimension() != Level.OVERWORLD || level.canSeeSky(zombiePos)

                when {
                    targetPos.y > zombiePos.y && canPile ->
                        pileAt(level, zombie, zombiePos)
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

    private fun breakAt(
        level: ServerLevel,
        zombiePos: BlockPos,
        targetPos: BlockPos,
        pattern: BreakPattern,
    ): Boolean {
        val dir = cardinalBetween(zombiePos, targetPos)
        val eyes = zombiePos.above()

        val targets =
            when (pattern) {
                BreakPattern.DOWN ->
                    listOf(
                        zombiePos.relative(dir),
                        zombiePos.below(),
                        zombiePos.relative(dir).below(),
                    )
                BreakPattern.FORWARD ->
                    listOf(
                        eyes.relative(dir),
                        zombiePos.relative(dir),
                    )
                BreakPattern.UP ->
                    listOf(
                        eyes.relative(dir),
                        eyes.above(),
                        eyes.relative(dir).above(),
                    )
            }

        var hit = false
        for (pos in targets) {
            val state = level.getBlockState(pos)
            if (state.isAir) continue
            hit = true
            // skip actual breaking in claimed chunks (factions compat)
            if (ClaimChecker.isClaimed(level, pos)) continue
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS)
        }

        // play sound even if blocks were protected just to scare people a bit
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

    private fun pileAt(
        level: ServerLevel,
        zombie: Zombie,
        zombiePos: BlockPos,
    ): Boolean {
        val center = Vec3(zombiePos.x.toDouble(), zombiePos.y.toDouble(), zombiePos.z.toDouble())
        val box = AABB.ofSize(center, 3.0, 4.0, 3.0)
        val nearby = level.getEntitiesOfClass(Zombie::class.java, box)
        if (nearby.size < PILE_THRESHOLD) return false

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
