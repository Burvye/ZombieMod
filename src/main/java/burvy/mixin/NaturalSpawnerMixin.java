package burvy.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.VisibleForDebug;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NaturalSpawner.AfterSpawnCallback;
import net.minecraft.world.level.NaturalSpawner.SpawnPredicate;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(NaturalSpawner.class)
public class NaturalSpawnerMixin {

    /**
     * @author Burvy
     * @reason Completely replace vanilla mob spawning
     */
    @Overwrite
    public static void spawnCategoryForChunk(MobCategory category, ServerLevel level, LevelChunk chunk, SpawnPredicate predicate, AfterSpawnCallback callback) {
        // stuff here
    }

    /**
     * @author Burvy
     * @reason Completely replace vanilla mob spawning
     */
    @VisibleForDebug
    @Overwrite
    public static void spawnCategoryForPosition(MobCategory category, ServerLevel level, BlockPos pos) {
        // stuff here
    }
}