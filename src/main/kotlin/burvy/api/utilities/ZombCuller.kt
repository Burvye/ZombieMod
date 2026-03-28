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

        var culled = 0
        for (player in level.players()) {
            if (culled >= CULL_AMOUNT) break

            val box = AABB.ofSize(player.position(), 192.0, 128.0, 192.0)
            val zombies = level.getEntitiesOfClass(Zombie::class.java, box)

            for (zombie in zombies) {
                if (culled >= CULL_AMOUNT) break
                zombie.discard()
                culled++
            }
        }
    }
}
