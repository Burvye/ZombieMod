package burvy.api.utilities

import net.minecraft.core.BlockPos
import net.minecraft.core.Holder
import net.minecraft.network.protocol.game.ClientboundSoundPacket
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.sounds.SoundEvent
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.phys.AABB
import java.util.UUID

/**
 * Alert spawns waves
 * Detected aggroes all zombies
 */
object NoiseChecker {
    private const val ALERT_TIME = 600L // 30s
    private const val DETECTED_TIME = 600L // 30s
    private const val SUDDEN_WAVES = 4
    private const val CHECK_INTERVAL = 20
    private const val DETECT_RANGE_SQ = 64.0 // 8 blocks sq
    private const val AGGRO_SPREAD = 192.0 // 96 block radius

    private val ALERT_SOUND: Holder<SoundEvent> =
        Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse("gbg:alert")))

    private val alerted = HashMap<UUID, Long>()
    private val detected = HashMap<UUID, Long>()

    enum class NoiseType(
        val radius: Int,
    ) {
        SPRINT(16),
        BLOCK_BREAK(16),
        ATTACK(96),
        HURT(64),
        GUNFIRE(96),
    }

    fun isDetected(player: ServerPlayer): Boolean {
        val stamp = detected[player.uuid] ?: return false
        return player.level().server.tickCount - stamp < DETECTED_TIME
    }

    fun isAlerted(player: ServerPlayer): Boolean {
        val stamp = alerted[player.uuid] ?: return false
        return player.level().server.tickCount - stamp < ALERT_TIME
    }

    fun isNoisy(player: ServerPlayer): Boolean = isDetected(player)

    fun makeNoise(
        player: ServerPlayer,
        level: ServerLevel,
        pos: BlockPos,
        type: NoiseType,
    ) {
        val wasQuiet = !isAlerted(player)
        alerted[player.uuid] = level.server.tickCount.toLong()
        ZombInvestigate.zombAlert(level, pos, type.radius)

        if (wasQuiet) {
            player.connection.send(
                ClientboundSoundPacket(
                    ALERT_SOUND,
                    SoundSource.HOSTILE,
                    player.x,
                    player.y,
                    player.z,
                    1.0f,
                    1.0f,
                    level.random.nextLong(),
                ),
            )
            repeat(SUDDEN_WAVES) { WaveSpawner.spawnWave(level, player) }
        }
    }

    fun clearPlayer(playerId: UUID) {
        alerted.remove(playerId)
        detected.remove(playerId)
    }

    // gunshot API
    fun gunshot(player: ServerPlayer) {
        val level = player.level()
        makeNoise(player, level, player.blockPosition(), NoiseType.GUNFIRE)
    }

    fun tick(level: ServerLevel) {
        if (TickChecker.isLagging()) return
        val tick = level.server.tickCount

        // sprint detection every tick
        for (player in level.players()) {
            if (player.isSprinting) {
                makeNoise(player, level, player.blockPosition(), NoiseType.SPRINT)
            }
        }

        if (tick % CHECK_INTERVAL != 0) return

        for (player in level.players()) {
            if (isDetected(player)) continue

            if (isAlerted(player)) {
                val nearBox = AABB.ofSize(player.position(), 16.0, 16.0, 16.0)
                val nearZombies = level.getEntitiesOfClass(Zombie::class.java, nearBox)
                for (zombie in nearZombies) {
                    if (zombie.target != player) continue
                    detected[player.uuid] = tick.toLong()
                    val aggroBox = AABB.ofSize(zombie.position(), AGGRO_SPREAD, 128.0, AGGRO_SPREAD)
                    for (z in level.getEntitiesOfClass(Zombie::class.java, aggroBox)) {
                        z.target = player
                    }
                    break
                }
            } else {
                val box = AABB.ofSize(player.position(), 192.0, 128.0, 192.0)
                for (zombie in level.getEntitiesOfClass(Zombie::class.java, box)) {
                    if (zombie.target == player && zombie.distanceToSqr(player) > DETECT_RANGE_SQ) {
                        zombie.target = null
                    }
                }
            }
        }
        if (tick % 200 == 0) {
            alerted.entries.removeIf { tick - it.value > ALERT_TIME * 2 }
            detected.entries.removeIf { tick - it.value > DETECTED_TIME * 2 }
        }
    }
}
