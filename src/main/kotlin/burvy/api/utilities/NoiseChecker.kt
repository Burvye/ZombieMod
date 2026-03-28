package burvy.api.utilities

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.phys.AABB
import java.util.UUID

/**
 * Tick stealth
 */
object NoiseChecker {
    private const val STEALTH_COOLDOWN = 1200L // 60s
    private const val LOS_CHECK_INTERVAL = 20
    private const val LOS_RANGE_SQ = 16.0 // 4 blocks squared

    private val noisyPlayers = HashMap<UUID, Long>()

    enum class NoiseType(
        val radius: Int,
    ) {
        SPRINT(32),
        BLOCK_BREAK(48),
        BLOCK_PLACE(48),
        ATTACK(96),
        GUNFIRE(96),
    }

    fun isNoisy(player: ServerPlayer): Boolean {
        val lastNoise = noisyPlayers[player.uuid] ?: return false
        val level = player.level()
        return level.server.tickCount - lastNoise < STEALTH_COOLDOWN
    }

    fun makeNoise(
        player: ServerPlayer,
        level: ServerLevel,
        pos: BlockPos,
        type: NoiseType,
    ) {
        noisyPlayers[player.uuid] = level.server.tickCount.toLong()
        ZombInvestigate.zombAlert(level, pos, type.radius)
    }

    // gunshot API
    fun gunshot(player: ServerPlayer) {
        val level = player.level()
        makeNoise(player, level, player.blockPosition(), NoiseType.GUNFIRE)
    }

    // detection per tick
    fun tick(level: ServerLevel) {
        if (TickChecker.isLagging()) return
        val tick = level.server.tickCount

        // sprint detection every tick
        for (player in level.players()) {
            if (player.isSprinting) {
                makeNoise(player, level, player.blockPosition(), NoiseType.SPRINT)
            }
        }

        if (tick % LOS_CHECK_INTERVAL != 0) return

        for (player in level.players()) {
            if (isNoisy(player)) continue

            // LOS detection at 4 blocks
            val nearBox = AABB.ofSize(player.position(), 8.0, 8.0, 8.0)
            val nearZombies = level.getEntitiesOfClass(Zombie::class.java, nearBox)
            var detected = false

            for (zombie in nearZombies) {
                if (zombie.distanceToSqr(player) > LOS_RANGE_SQ) continue
                if (zombie.sensing.hasLineOfSight(player)) {
                    makeNoise(player, level, player.blockPosition(), NoiseType.ATTACK)
                    detected = true
                    break
                }
            }

            if (detected) continue

            // dont track this stealthy player
            val wideBox = AABB.ofSize(player.position(), 192.0, 128.0, 192.0)
            val allZombies = level.getEntitiesOfClass(Zombie::class.java, wideBox)
            for (zombie in allZombies) {
                if (zombie.target == player && zombie.distanceToSqr(player) > LOS_RANGE_SQ) {
                    zombie.target = null
                }
            }
        }

        // cleanup stale entries
        if (tick % 200 == 0) {
            noisyPlayers.entries.removeIf { tick - it.value > STEALTH_COOLDOWN * 2 }
        }
    }
}
