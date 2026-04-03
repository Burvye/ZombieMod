package burvy.api.utilities

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel

/**
 * Faction mod integration to check if chunks are claimed
 *
 * Current factions mod is io.icker.factions
 * We use "Claim.get(chunkX, chunkZ, dimensionString)" from "io.icker.factions.faction.api.Claim"
 *
 * TODO LATER:  Update when the factions mod is reworked.
 */
object ClaimChecker {
    private val getMethod =
        try {
            val claimClass = Class.forName("io.icker.factions.faction.api.Claim")
            claimClass.getMethod("get", Int::class.java, Int::class.java, String::class.java)
        } catch (_: Exception) {
            null
        }

    // this is your black box boolean
    fun isClaimed(
        level: ServerLevel,
        pos: BlockPos,
    ): Boolean {
        val method = getMethod ?: return false
        return try {
            val cx = pos.x shr 4
            val cz = pos.z shr 4
            val dimension = level.dimension().identifier().toString()
            method.invoke(null, cx, cz, dimension) != null
        } catch (_: Exception) {
            false
        }
    }
}
