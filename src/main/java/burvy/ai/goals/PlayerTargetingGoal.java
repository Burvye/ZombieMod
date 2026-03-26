package burvy.ai.goals;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 Find the nearest targetable player
 */
public class PlayerTargetingGoal extends Goal {
    
    private final Zombie zombie;
    private LivingEntity lastTarget;
    
    public PlayerTargetingGoal(Zombie zombie) {
        this.zombie = zombie;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }
    
    @Override
    public boolean canUse() {
        return getReachableNearestPlayer() != null;
    }
    
    @Override
    public void tick() {
        LivingEntity target = getReachableNearestPlayer();
        if (target != null) {
            // CRITICAL: Set target for proper aggro behavior
            zombie.setTarget(target);
            zombie.getLookControl().setLookAt(target, 30.0f, 30.0f);
            zombie.getNavigation().moveTo(target, 1.0f);
            lastTarget = target;
        }
    }
    
    @Override
    public void stop() {
        lastTarget = null;
    }

    private LivingEntity getReachableNearestPlayer() {
        LivingEntity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;
        
        // iterate over all zombies
        for (LivingEntity entity : zombie.level().players()) {
            // skip non players
            if (!(entity instanceof net.minecraft.world.entity.player.Player)) {
                continue;
            }
            
            double d = entity.distanceToSqr(zombie);
            if (d > 4096) continue; // 64 blocks
            
            // give up on failed targets
            if (entity == lastTarget) {
                continue;
            }
            
            // TODO: Improve isReachable
            if (isReachable(entity) && d < nearestDistSq) {
                nearest = entity;
                nearestDistSq = d;
            }
        }
        
        // fallback to targetting the last unreachable target
        if (nearest == null && lastTarget != null) {
            if (isReachable(lastTarget)) {
                nearest = lastTarget;
            }
        }
        
        return nearest;
    }
    
    // function to check if a player is reachable
    // TODO: Rework isReachable to actually reflect if a player is reachable
    private boolean isReachable(LivingEntity player) {
        // just check if they are within range
        double distance = zombie.distanceTo(player);
        return distance <= 64.0;
    }
}