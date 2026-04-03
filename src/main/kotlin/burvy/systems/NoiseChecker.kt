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
        BLOCK_BREAK(32),
        ATTACK(64),
        HURT(96),
        GUNFIRE(96),
        SPRINT(48),
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

            // predicate filters during the iteration itself which is more efficient
            val zombies =
                level.getEntities(player, box) { entity ->
                    entity is Zombie && entity.isAlive
                }

            for (entity in zombies) {
                if (entity is Zombie) {
                    val distanceToNoiseMaker = entity.distanceToSqr(player)

                    // aggro towards noise maker since im not targeting anyone
                    if (entity.target == null) {
                        entity.target = player
                        player.connection.send(
                            ClientboundSoundPacket(
                                ALERT_SOUND,
                                SoundSource.HOSTILE,
                                entity.x,
                                entity.y,
                                entity.z,
                                4.0f,
                                0.5f,
                                level.random.nextLong(),
                            ),
                        )
                    } else if (distanceToNoiseMaker < entity.distanceToSqr(entity.target!!)) {
                        entity.target = player
                        player.connection.send(
                            ClientboundSoundPacket(
                                ALERT_SOUND,
                                SoundSource.HOSTILE,
                                entity.x,
                                entity.y,
                                entity.z,
                                4.0f,
                                0.5f,
                                level.random.nextLong(),
                            ),
                        )
                    }
                }
            }
        }

        // TODO: Spawn zombies until mob cap is filled more directly instead of relying on spawnWave
        repeat(SUDDEN_WAVES) { WaveSpawner.spawnWave(level, player) }
    }

    // gunshot API
    fun gunshot(player: ServerPlayer) {
        @Suppress("USELESS_CAST")
        val level = player.level() as ServerLevel
        makeNoise(player, level, NoiseType.GUNFIRE)
    }

    /**
     * tick check for sprinting players
     */
    fun tick(level: ServerLevel) {
        if (TickChecker.isLagging()) return

        for (player in level.players()) {
            if (player.isSprinting) {
                makeNoise(player, level, NoiseType.SPRINT)
            }
        }
    }
}
