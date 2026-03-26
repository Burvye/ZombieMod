package burvy.ai.goals;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.EnumSet;

/**
 Building blocks
 1. Runs when no reachable players are found
 2. Player is above me
 3. I am exposed to the sky
 4. Build in 30 seconds
 5. Reset my data
 */

public class BuildingGoal extends Goal {
    
    private final Zombie zombie;
    private long conditionsMetSince;
    private boolean hasBuilt;
    private static final long BUILD_DELAY = 600L; // 30 secs
    
    public BuildingGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        resetConditions();
    }
    
    @Override
    public boolean canUse() {
        LivingEntity target = zombie.getTarget();
        if (target == null) {
            resetConditions();
            return false;
        }
        
        // cant find a reachable player
        if (isPathfindingSuccessful()) {
            resetConditions();
            return false;
        }
        
        // above my Y level + 1 (i can go up a block by jumping)
        if (target.blockPosition().getY() <= zombie.blockPosition().getY() + 1) {
            resetConditions();
            return false;
        }
        
        // i am exposed to the sky
        if (!isExposedToSky()) {
            resetConditions();
            return false;
        }
        
        // check if we last had these conditions
        if (conditionsMetSince == 0L) {
            conditionsMetSince = zombie.level().getGameTime();
        }
        
        // that time was 30 seconds ago
        long elapsed = zombie.level().getGameTime() - conditionsMetSince;
        return elapsed >= BUILD_DELAY && !hasBuilt;
    }
    
    @Override
    public void start() {
        hasBuilt = true;
    }
    
    @Override
    public void stop() {
        resetConditions();
    }
    
    @Override
    public void tick() {
        // place one block under us
        net.minecraft.world.level.block.state.BlockState blockState = Blocks.GRAVEL.defaultBlockState();
        BlockPos targetPos = zombie.blockPosition().above();
        // TODO: Set block, but also remove the zombie
        zombie.level().setBlock(targetPos, blockState, 1);
    }
    
    // check if i am exposed to the sky starting from head level
    private boolean isExposedToSky() {
        return zombie.level().canSeeSky(zombie.blockPosition().above(2));
    }
    
    // check if i found a path to the player
    private boolean isPathfindingSuccessful() {
        net.minecraft.world.entity.ai.navigation.PathNavigation navigation = zombie.getNavigation();
        return navigation.isDone() && 
               navigation.getPath() != null &&
               !navigation.getPath().isDone();
    }
    
    // clean up memory
    private void resetConditions() {
        conditionsMetSince = 0L;
        hasBuilt = false;
    }
}