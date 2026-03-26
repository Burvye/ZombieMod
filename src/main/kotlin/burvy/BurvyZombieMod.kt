package burvy

import net.fabricmc.api.ModInitializer
import org.slf4j.LoggerFactory

object BurvyZombieMod : ModInitializer {
    private val logger = LoggerFactory.getLogger("burvy-zombie-mod")

    override fun onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.
        logger.info("Hello Fabric world!")
        for (i in 1..5) {
            logger.info("Hello Test {}", i)
        }
        // Zombies already auto aggro and are spawned near players in the factions mod
        // this mod should optimize zombie logic

        /* TODO: #1 Overwrite Zombie Logic completely using a mixin.
            Zombies should have only this logic:
                1. Look for the player with the shortest squared distance to me, and aggro at them until I cannot pathfind to them.
                2. If I cannot pathfind to the player I want, look for another nearest player.
                3. If there are no other nearest players, begin breaking or building to get to them.
         */

        // Block destruction logic

        /* TODO: #2 Add Zombie Goal to damage blocks if these conditions are met:
            a. I cannot pathfind to my desired player
            b. I am not exposed to the sky if the player is above me.
            c. I am always allowed to dig down if the player is below me.
            Block destruction must follow this pattern:
                1. There are 4 blocks to choose from. Z represents the zombie:
                                1 -
                                Z 2
                                Z 3
                                4 -
                2. If any of the blocks are defined SAFE_BLOCKS, do not attempt to attack blocks.
                3. If the player has a higher Y level than the player at all, wait 5 seconds and attack 1 and 2 unless blocks 3 is an air block.
                4. If the player has a lower Y level than the player at all, wait 5 seconds and attack 3 and 4.
                5. If the player has an equal Y level, wait 5 seconds and attack 2 and 3.
         */

        // Building logic

        /* TODO: #3 Add Zombie Goal to build upwards if these conditions are met:
                a. Players have a HIGHER Y level than my Y level + 1
                b. I am exposed to the sky
                c. I was in these conditions 30 seconds ago (Make sure to save only once, NO MEMORY LEAKS)
         */
    }
}
