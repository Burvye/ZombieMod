package burvy

import burvy.api.utilities.NoiseChecker
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.event.player.AttackEntityCallback
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionResult
import org.slf4j.LoggerFactory

object BurvyZombieMod : ModInitializer {
    private val logger = LoggerFactory.getLogger("burvy-zombie-mod")

    override fun onInitialize() {
        logger.info("Initializing Burvy Zombie Mod")

        PlayerBlockBreakEvents.AFTER.register { level, player, _, _, _ ->
            if (player is ServerPlayer && level is ServerLevel) {
                NoiseChecker.makeNoise(player, level, player.blockPosition(), NoiseChecker.NoiseType.BLOCK_BREAK)
            }
        }

        AttackEntityCallback.EVENT.register { player, level, _, _, _ ->
            if (player is ServerPlayer && level is ServerLevel) {
                NoiseChecker.makeNoise(player, level, player.blockPosition(), NoiseChecker.NoiseType.ATTACK)
            }
            InteractionResult.PASS
        }

        ServerLivingEntityEvents.AFTER_DEATH.register { entity, _ ->
            if (entity is ServerPlayer) {
                NoiseChecker.clearPlayer(entity.uuid)
            }
        }
    }
}
