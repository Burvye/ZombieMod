package burvy.systems

import burvy.api.utilities.TickChecker
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.phys.AABB

/**
 * Despawn zombies to reduce lag
 */
object ZombCuller {
    private const val CULL_AMOUNT = 2

    fun tick(level: ServerLevel) {
        if (!TickChecker.isBadlyLagging()) return

        // TODO: just delete arbritrary zombies (wouldn't this delete n * CULL_AMOUNT amount of zombies each run?)
        for (player in level.players()) {
            val box = AABB.ofSize(player.position(), 192.0, 128.0, 192.0)
            level
                .getEntitiesOfClass(Zombie::class.java, box)
                .asSequence()
                .take(CULL_AMOUNT)
                .forEach { it.discard() }
        }
    }
}
