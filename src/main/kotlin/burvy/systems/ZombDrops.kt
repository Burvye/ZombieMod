package burvy.systems

import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.monster.zombie.Zombie
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

/**
 * Add more drops here
 */
object ZombDrops {
    private data class Drop(
        val item: Item,
        val count: Int,
        val chance: Float,
    )

    private val DROPS =
        listOf(
            Drop(Items.IRON_NUGGET, 1, 1.0f),
            Drop(Items.GOLD_NUGGET, 1, 1.0f),
            Drop(Items.GUNPOWDER, 2, 1.0f),
            Drop(Items.BREAD, 1, 0.25f),
            Drop(Items.SPRUCE_PLANKS, 16, 0.5f),
        )

    fun onDeath(
        zombie: Zombie,
        level: ServerLevel,
    ) {
        if (zombie.lastHurtByPlayer == null) return
        for (drop in DROPS) {
            if (drop.chance < 1.0f && level.random.nextFloat() >= drop.chance) continue
            zombie.spawnAtLocation(level, ItemStack(drop.item, drop.count))
        }
    }
}
