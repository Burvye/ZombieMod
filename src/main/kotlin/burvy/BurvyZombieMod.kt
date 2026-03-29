package burvy

import burvy.api.utilities.NoiseChecker
import burvy.api.utilities.ZombDrops
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.monster.zombie.Drowned
import net.minecraft.world.entity.monster.zombie.Zombie
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

        ServerPlayerEvents.AFTER_RESPAWN.register { _, newPlayer, _ ->
            NoiseChecker.clearPlayer(newPlayer.uuid)
        }

        ServerLivingEntityEvents.AFTER_DEATH.register { entity, _ ->
            val level = entity.level() as? ServerLevel ?: return@register
            if (entity is Zombie) {
                ZombDrops.onDeath(entity, level)
            }
        }

        // force drowned stats after conversion (convertTo copies old zombie attributes)
        ServerEntityEvents.ENTITY_LOAD.register { entity, _ ->
            if (entity is Drowned) {
                entity.getAttribute(Attributes.MOVEMENT_SPEED)?.baseValue = 4.0
                entity.getAttribute(Attributes.FOLLOW_RANGE)?.baseValue = 96.0
            }
        }
    }
}
