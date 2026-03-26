package burvy.mixin;

import burvy.ai.goals.BlockDestructionGoal;
import burvy.ai.goals.BuildingGoal;
import burvy.ai.goals.PlayerTargetingGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Completely replace zombie behavior
 */
@Mixin(Zombie.class)
public class ZombieEntityMixin {
    
    @Shadow
    private GoalSelector goalSelector;
    
    @Shadow
    private GoalSelector targetSelector;
    
    @Inject(
        method = "registerGoals",
        at = @At("HEAD"),
        cancellable = true
    )
    private void replaceZombieAI(CallbackInfo ci) {
        // delete all past goals
        this.goalSelector.removeAllGoals(goal -> true);
        this.targetSelector.removeAllGoals(goal -> true);
        
        // add our goals
        // 0: target players
        // 1: attempt building
        // 2: attempt breaking
        this.goalSelector.addGoal(0, new PlayerTargetingGoal((Zombie)(Object)this));
        this.goalSelector.addGoal(1, new BuildingGoal((Zombie)(Object)this));
        this.goalSelector.addGoal(2, new BlockDestructionGoal((Zombie)(Object)this));
        
        // cancel all other logic
        ci.cancel();
    }
}