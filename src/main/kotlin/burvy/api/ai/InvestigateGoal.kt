package burvy.api.ai

import burvy.api.utilities.ZombInvestigate
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.goal.Goal
import java.util.EnumSet

/**
 * Paths a zombie toward a noise investigation position.
 * Only activates when the zombie has no attack target but has an investigate pos.
 * Clears the investigate pos on arrival.
 */
class InvestigateGoal(
    private val mob: Mob,
) : Goal() {
    private var targetPos: BlockPos? = null

    init {
        flags = EnumSet.of(Flag.MOVE, Flag.LOOK)
    }

    override fun canUse(): Boolean {
        if (mob.target != null) return false
        targetPos = ZombInvestigate.getTarget(mob.uuid)
        return targetPos != null
    }

    override fun start() {
        targetPos?.let {
            mob.navigation.moveTo(it.x + 0.5, it.y.toDouble(), it.z + 0.5, 1.0)
        }
    }

    override fun canContinueToUse(): Boolean {
        if (mob.target != null) return false
        if (!ZombInvestigate.hasTarget(mob.uuid)) return false
        return !mob.navigation.isDone
    }

    override fun tick() {
        // redirect if investigate target changed
        val current = ZombInvestigate.getTarget(mob.uuid)
        if (current != null && current != targetPos) {
            targetPos = current
            mob.navigation.moveTo(current.x + 0.5, current.y.toDouble(), current.z + 0.5, 1.0)
        }

        // arrived — clear and stop
        if (mob.navigation.isDone) {
            ZombInvestigate.clearTarget(mob.uuid)
        }
    }

    override fun stop() {
        ZombInvestigate.clearTarget(mob.uuid)
        mob.navigation.stop()
    }
}
