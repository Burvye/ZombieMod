package burvy.mixin;

import burvy.api.ai.InvestigateGoal;
import burvy.api.utilities.NoiseChecker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replace AI for all zombie types
 */
@Mixin(Zombie.class)
public class ZombieGoalsMixin {

	@Inject(method = "registerGoals", at = @At("HEAD"), cancellable = true)
	private void simpleGoals(CallbackInfo ci) {
		Zombie zombie = (Zombie) (Object) this;
		MobAccessor accessor = (MobAccessor) zombie;

		accessor.getGoalSelector().removeAllGoals(goal -> true);
		accessor.getTargetSelector().removeAllGoals(goal -> true);

		// noisy players will be heard everywhere, quiet players are heard in a range of 8 - the default
		TargetingConditions.Selector noisies = (target, level) -> {
			if (!(target instanceof ServerPlayer sp)) return false;
			if (NoiseChecker.INSTANCE.isNoisy(sp)) return true;
			return zombie.distanceToSqr(target) <= 64.0;
		};
		accessor.getTargetSelector().addGoal(1, new NearestAttackableTargetGoal<>(
				zombie, Player.class, false, noisies
		));
		accessor.getGoalSelector().addGoal(2, new ZombieAttackGoal(zombie, 1.0, false));
		accessor.getGoalSelector().addGoal(3, new InvestigateGoal(zombie));
		accessor.getGoalSelector().addGoal(4, new WaterAvoidingRandomStrollGoal(zombie, 0.8));

		ci.cancel();
	}
}
