package burvy.mixin;

import burvy.api.utilities.NoiseChecker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Detect damage
 */
@Mixin(LivingEntity.class)
public class DamageNoiseMixin {

	@Inject(method = "hurtServer", at = @At("RETURN"))
	private void hurt(ServerLevel level, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
		if (!cir.getReturnValue()) return;

		// player took damage
		if ((Object) this instanceof ServerPlayer victim) {
			NoiseChecker.INSTANCE.makeNoise(victim, level, victim.blockPosition(), NoiseChecker.NoiseType.HURT);
		}

		// player dealt damage (melee, projectile, gun, etc.)
		if (source.getEntity() instanceof ServerPlayer attacker && attacker.level() instanceof ServerLevel attackerLevel) {
			NoiseChecker.INSTANCE.makeNoise(attacker, attackerLevel, attacker.blockPosition(), NoiseChecker.NoiseType.ATTACK);
		}
	}
}
