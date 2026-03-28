package burvy.api.utilities

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.UUID
import kotlin.math.abs

/**
 * Zombie world modification: breaking blocks and piling gravel.
 * Runs every tick, processes up to 5 zombies per tick.
 * Each zombie has a 5-second personal cooldown.
 */
object ZombBlocks {
    private const val COOLDOWN_TICKS = 100
    private const val MAX_PER_TICK = 5
    private const val PILE_THRESHOLD = 5
    private const val SOUND_VOLUME = 2.0f // (volume * 16 blocks)

    private val cooldowns = HashMap<UUID, Long>()

    private val SAFE_BLOCKS =
        setOf(
            Blocks.IRON_DOOR,
            Blocks.IRON_BLOCK,
            Blocks.DIAMOND_BLOCK,
            Blocks.NETHERITE_BLOCK,
            Blocks.GOLD_BLOCK,
            Blocks.WHITE_CONCRETE,
            Blocks.ORANGE_CONCRETE,
            Blocks.MAGENTA_CONCRETE,
            Blocks.LIGHT_BLUE_CONCRETE,
            Blocks.YELLOW_CONCRETE,
            Blocks.LIME_CONCRETE,
            Blocks.PINK_CONCRETE,
            Blocks.GRAY_CONCRETE,
            Blocks.LIGHT_GRAY_CONCRETE,
            Blocks.CYAN_CONCRETE,
            Blocks.PURPLE_CONCRETE,
            Blocks.BLUE_CONCRETE,
            Blocks.BROWN_CONCRETE,
            Blocks.GREEN_CONCRETE,
            Blocks.RED_CONCRETE,
            Blocks.BLACK_CONCRETE,
        )

    fun tick(level: ServerLevel) {
        val tick = level.server.tickCount.toLong()
        var processed = 0
        val seen = HashSet<UUID>()

        // cleanup stale ticks
        if (tick % 200 == 0L) {
            cooldowns.entries.removeIf { tick - it.value > COOLDOWN_TICKS * 2 }
        }

        // processing only zombies around players
        for (player in level.players()) {
            if (processed >= MAX_PER_TICK) break

            // i mean zombies in range
            val range = AABB.ofSize(player.position(), 192.0, 128.0, 192.0)
            val zombies = level.getEntitiesOfClass(Zombie::class.java, range)

            for (zombie in zombies) {
                // this second check prevents wasted iterations as the zombies
                if (processed >= MAX_PER_TICK) break
                if (!seen.add(zombie.uuid)) continue

                val target = zombie.target ?: continue
                val tickPrev = cooldowns[zombie.uuid] ?: 0L
                if (tick - tickPrev < COOLDOWN_TICKS) continue

                val zombiePos = zombie.blockPosition()
                val targetPos = target.blockPosition()
                val skyExposed = level.canSeeSky(zombiePos)

                val acted =
                    when {
                        // player above + sky exposed -> pile
                        targetPos.y > zombiePos.y && skyExposed ->
                            pileAt(level, zombie, zombiePos)
                        // player above + sky covered -> break upward
                        targetPos.y > zombiePos.y ->
                            breakAt(level, zombiePos, targetPos, BreakPattern.UP)
                        // player level -> break forward
                        targetPos.y == zombiePos.y ->
                            breakAt(level, zombiePos, targetPos, BreakPattern.FORWARD)
                        // player below -> break downward
                        else ->
                            breakAt(level, zombiePos, targetPos, BreakPattern.DOWN)
                    }

                if (acted) {
                    cooldowns[zombie.uuid] = tick
                    processed++
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
        val feet = zombiePos
        val eyes = feet.above()

        val targets =
            when (pattern) {
                BreakPattern.DOWN ->
                    listOf(
                        feet.relative(dir),
                        feet.below(),
                        feet.relative(dir).below(),
                    )
                BreakPattern.FORWARD ->
                    listOf(
                        eyes.relative(dir),
                        feet.relative(dir),
                    )
                BreakPattern.UP ->
                    listOf(
                        eyes.relative(dir),
                        eyes.above(),
                        eyes.relative(dir).above(),
                    )
            }

        var broken = false
        for (pos in targets) {
            val state = level.getBlockState(pos)
            if (state.isAir) continue
            if (state.block in SAFE_BLOCKS) continue
            level.removeBlock(pos, false)
            broken = true
        }

        if (broken) {
            level.playSound(
                null,
                zombiePos,
                SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
                SoundSource.HOSTILE,
                SOUND_VOLUME,
                1.0f,
            )
        }

        return broken
    }

    private fun pileAt(
        level: ServerLevel,
        zombie: Zombie,
        zombiePos: BlockPos,
    ): Boolean {
        // 3x4x3 box around zombie (at block origin pos)
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
        cooldowns.remove(zombie.uuid)

        return true
    }
}
