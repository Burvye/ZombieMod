package burvy.systems

import burvy.api.utilities.TickChecker
import burvy.api.utilities.ZombCap
import burvy.api.utilities.ZombSpawner
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer

/**
 * Higher level wave spawning utilities
 */
object WaveSpawner {
    private const val WAVE_SIZE = 12
    private const val DEADLY_WAVE_SIZE = 23 // 24 is mob cramping threshold
    private const val DARKNESS_THRESHOLD = 7

    /**
     * Spawns a wave of zombies around the player.
     * Wave size increases if spawning in a dark area above the player. (in buildings)
     */
    fun spawnWave(
        level: ServerLevel,
        player: ServerPlayer,
    ) {
        if (TickChecker.isLagging()) return
        val playerPos = player.blockPosition()

        val spawnPos = ZombSpawner.posAround(level, playerPos) ?: return

        val isDarkAbove =
            level.getMaxLocalRawBrightness(spawnPos) < DARKNESS_THRESHOLD &&
                spawnPos.y > playerPos.y
        val desired = if (isDarkAbove) DEADLY_WAVE_SIZE else WAVE_SIZE
        val waveSize = desired.coerceAtMost(ZombCap.remaining(level, player))
        if (waveSize <= 0) return

        repeat(waveSize) {
            ZombSpawner.zombAt(level, spawnPos, player)
        }
    }
}
