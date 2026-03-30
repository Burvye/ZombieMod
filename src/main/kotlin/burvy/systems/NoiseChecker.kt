package burvy.systems

import burvy.api.utilities.TickChecker
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
 * Noise instantly aggros zombies
 */
object NoiseChecker {
    private const val NOISY_TIME = 600L // 30s
    private const val SUDDEN_WAVES = 4

    private val ALERT_SOUND: Holder<SoundEvent> =
        Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse("gbg:alert")))

    // timestamp of last noise per player
    private val noisyStamps = HashMap<UUID, Long>()

    // a list of noisy players
    private val noisies = HashSet<UUID>()

    enum class NoiseType(
        val radius: Int,
    ) {
        SPRINT(48),
        BLOCK_BREAK(64),
        ATTACK(128),
        HURT(96),
        GUNFIRE(128),
    }

    // fast check because its used by all zombies
    fun isNoisy(player: ServerPlayer): Boolean = player.uuid in noisies

    /**
     * call this whenever you make noise
     */
    fun makeNoise(
        player: ServerPlayer,
        level: ServerLevel,
        type: NoiseType,
    ) {
        val wasQuiet = player.uuid !in noisies
        noisyStamps[player.uuid] = level.server.tickCount.toLong()
        noisies.add(player.uuid)

        if (!wasQuiet) return

        // the first noise aggos all zombies
        val r = type.radius.toDouble()
        val box = AABB.ofSize(player.position(), r * 2, 128.0, r * 2)
        for (zombie in level.getEntitiesOfClass(Zombie::class.java, box)) {
            val current = zombie.target
            if (current == null || !current.isAlive) {
                zombie.target = player
            }
        }

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

    fun clearPlayer(playerId: UUID) {
        noisyStamps.remove(playerId)
        noisies.remove(playerId)
    }

    // gunshot API
    fun gunshot(player: ServerPlayer) {
        @Suppress("USELESS_CAST")
        val level = player.level() as ServerLevel
        makeNoise(player, level, NoiseType.GUNFIRE)
    }

    fun tick(level: ServerLevel) {
        if (TickChecker.isLagging()) return
        val tick = level.server.tickCount

        // rebuild active set from timestamps
        noisies.clear()
        for ((uuid, stamp) in noisyStamps) {
            if (tick - stamp < NOISY_TIME) noisies.add(uuid)
        }

        // sprint makes you noisy for longer
        for (player in level.players()) {
            if (player.isSprinting) {
                makeNoise(player, level, NoiseType.SPRINT)
            }
        }

        // clean up old timers
        if (tick % 200 == 0) {
            noisyStamps.entries.removeIf { tick - it.value > NOISY_TIME * 2 }
        }
    }
}
