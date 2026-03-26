package burvy

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object BurvyZombieMod : ModInitializer {
    private val logger = LoggerFactory.getLogger("burvy-zombie-mod")

    override fun onInitialize() {
        logger.info("Initializing Burvy Zombie Mod")
    }
}
