package burvy.api.utilities

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.phys.AABB

/**
 * Despawn zombies to reduce lag
 */
object ZombCuller {
    private const val COOLDOWN_TICKS = 100 // 5 seconds
    private const val CULL_AMOUNT = 16

    private var timer = 0

    fun tick(level: ServerLevel) {
        timer++
        if (timer % COOLDOWN_TICKS != 0) return
        if (!TickChecker.isLagging()) return

        // flatten all player-nearby zombies into one stream, cull first zombies
        level
            .players()
            .asSequence()
            .flatMap { player ->
                level.getEntitiesOfClass(
                    Zombie::class.java,
                    AABB.ofSize(player.position(), 192.0, 128.0, 192.0),
                )
            }.take(CULL_AMOUNT)
            .forEach { it.discard() }
    }
}
