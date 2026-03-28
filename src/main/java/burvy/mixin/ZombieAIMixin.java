package burvy.mixin;

import burvy.api.ai.InvestigateGoal;
import burvy.api.utilities.NoiseChecker;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Strip zombie AI
 */
@Mixin(Zombie.class)
public class ZombieAIMixin {

	@Inject(method = "registerGoals", at = @At("HEAD"), cancellable = true)
	private void simpleGoals(CallbackInfo ci) {
		Zombie zombie = (Zombie) (Object) this;
		MobAccessor accessor = (MobAccessor) zombie;

		accessor.getGoalSelector().removeAllGoals(goal -> true);
		accessor.getTargetSelector().removeAllGoals(goal -> true);

		TargetingConditions.Selector noisyOnly =
				(target, level) -> target instanceof ServerPlayer sp && NoiseChecker.INSTANCE.isNoisy(sp);
		accessor.getTargetSelector().addGoal(1, new NearestAttackableTargetGoal<>(
				zombie, Player.class, false, noisyOnly
		));
		accessor.getGoalSelector().addGoal(2, new ZombieAttackGoal(zombie, 1.0, false));
		accessor.getGoalSelector().addGoal(3, new InvestigateGoal(zombie));
		accessor.getGoalSelector().addGoal(4, new WaterAvoidingRandomStrollGoal(zombie, 0.8));

		ci.cancel();
	}

	@Inject(method = "createAttributes", at = @At("HEAD"), cancellable = true)
	private static void customAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
		cir.setReturnValue(Monster.createMonsterAttributes()
				.add(Attributes.FOLLOW_RANGE, 96.0)
				.add(Attributes.MOVEMENT_SPEED, 0.4F)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.ARMOR, 2.0)
				.add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0));
	}

	@Inject(method = "isSunSensitive", at = @At("HEAD"), cancellable = true)
	private void sunLiberation(CallbackInfoReturnable<Boolean> cir) {
		cir.setReturnValue(false);
	}
}
