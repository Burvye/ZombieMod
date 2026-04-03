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

/**
 * Noise instantly aggros zombies
 */
object NoiseChecker {
    private const val SUDDEN_WAVES = 4

    private val ALERT_SOUND: Holder<SoundEvent> =
        Holder.direct(SoundEvent.createVariableRangeEvent(Identifier.parse("gbg:alert")))

    enum class NoiseType(
        val radius: Int,
    ) {
        BLOCK_BREAK(64),
        ATTACK(128),
        HURT(96),
        GUNFIRE(128),
    }

    /**
     * call this whenever you make noise
     */
    fun makeNoise(
        player: ServerPlayer,
        level: ServerLevel,
        type: NoiseType,
    ) {
        // aggro zombies that aren't already aggroed and skip if lagging
        if (!TickChecker.isLagging()) {
            val r = type.radius.toDouble()
            val box = AABB.ofSize(player.position(), r * 2, 128.0, r * 2)
            for (zombie in level.getEntitiesOfClass(Zombie::class.java, box)) {
                val current = zombie.target
                if (current == null || !current.isAlive) {
                    zombie.target = player
                    // sending a sound for each zombie at the zombie's location
                    player.connection.send(
                        ClientboundSoundPacket(
                            ALERT_SOUND,
                            SoundSource.HOSTILE,
                            zombie.x,
                            zombie.y,
                            zombie.z,
                            4.0f,
                            0.5f,
                            level.random.nextLong(),
                        ),
                    )
                }
            }
        }

        repeat(SUDDEN_WAVES) { WaveSpawner.spawnWave(level, player) }
    }

    // gunshot API
    fun gunshot(player: ServerPlayer) {
        @Suppress("USELESS_CAST")
        val level = player.level() as ServerLevel
        makeNoise(player, level, NoiseType.GUNFIRE)
    }

    // TODO: Detect sprint as an event instead of polling in tick()
}