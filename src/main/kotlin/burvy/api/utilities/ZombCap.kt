package burvy.api.utilities

import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.phys.AABB

/**
 * Per player zombie cap
 */
object ZombCap {
    private const val MAX = 128
    private const val RANGE = 192.0 // 96 block radius

    fun remaining(
        level: ServerLevel,
        player: ServerPlayer,
    ): Int {
        val box = AABB.ofSize(player.position(), RANGE, 128.0, RANGE)
        val count = level.getEntitiesOfClass(Zombie::class.java, box).size
        return (MAX - count).coerceAtLeast(0)
    }
}
