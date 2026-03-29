package burvy.api.utilities

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import java.util.UUID

/**
 * Track zombie investigation targets
 */
object ZombInvestigate {
    private val targets = HashMap<UUID, BlockPos>()

    // alert all zombies in this radius unless it is lagging
    fun zombAlert(
        level: ServerLevel,
        noisePos: BlockPos,
        radius: Int,
    ) {
        if (TickChecker.isLagging()) return

        val r = radius.toDouble()
        val center = Vec3.atCenterOf(noisePos)
        val radiusSq = r * r
        val box = AABB.ofSize(center, r * 2, r * 2, r * 2)
        val zombies = level.getEntitiesOfClass(Zombie::class.java, box)

        for (zombie in zombies) {
            if (zombie.distanceToSqr(center) <= radiusSq) {
                targets[zombie.uuid] = noisePos
            }
        }
    }

    fun setTarget(
        zombieId: UUID,
        pos: BlockPos,
    ) {
        targets[zombieId] = pos
    }

    fun getTarget(zombieId: UUID): BlockPos? = targets[zombieId]

    fun hasTarget(zombieId: UUID): Boolean = targets.containsKey(zombieId)

    fun clearTarget(zombieId: UUID) {
        targets.remove(zombieId)
    }

    fun cleanup(tick: Long) {
        if (tick % 600 == 0L && targets.size > 10000) {
            targets.clear()
        }
    }
}
