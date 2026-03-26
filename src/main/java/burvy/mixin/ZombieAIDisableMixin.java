package burvy.mixin;

import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Zombies only have these goals
 */
@Mixin(Zombie.class)
public class ZombieAIDisableMixin {

	@Inject(method = "registerGoals", at = @At("HEAD"), cancellable = true)
	private void replaceGoals(CallbackInfo ci) {
		Zombie zombie = (Zombie) (Object) this;
		MobAccessor accessor = (MobAccessor) zombie;

		// clear existing goals
		accessor.getGoalSelector().removeAllGoals(goal -> true);
		accessor.getTargetSelector().removeAllGoals(goal -> true);

		// find the nearest player
		accessor.getTargetSelector().addGoal(1, new NearestAttackableTargetGoal<>(
			zombie,
			Player.class,
			false // instant detect
		));

		// melee attack
		accessor.getGoalSelector().addGoal(2, new MeleeAttackGoal(zombie, 1.0, false));

		// wander
		accessor.getGoalSelector().addGoal(3, new WaterAvoidingRandomStrollGoal(zombie, 0.8));

		// clear other goals
		ci.cancel();
	}
}