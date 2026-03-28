package burvy.api.utilities

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

/**
 * Higher level wave spawning utilities
 */
object WaveSpawner {
    private const val WAVE_SIZE = 16
    private const val DEADLY_WAVE_SIZE = 32
    private const val DARKNESS_THRESHOLD = 7

    /**
     * Spawns a wave of zombies around the player.
     * Wave size increases if spawning in a dark area above the player. (in buildings)
     */
    fun spawnWave(
        level: ServerLevel,
        player: ServerPlayer,
    ) {
        val playerPos = player.blockPosition()

        val spawnPos = ZombSpawner.posAround(level, playerPos) ?: return

        val isDarkAbove =
            level.getMaxLocalRawBrightness(spawnPos) < DARKNESS_THRESHOLD &&
                spawnPos.y > playerPos.y
        val waveSize = if (isDarkAbove) DEADLY_WAVE_SIZE else WAVE_SIZE

        ZombSpawner.zombAround(level, player, waveSize)
    }
}
