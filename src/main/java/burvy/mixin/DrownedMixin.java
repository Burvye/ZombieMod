package burvy.mixin;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Drowned;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Drowned overrides
 */
@Mixin(Drowned.class)
public class DrownedMixin {

	@Inject(method = "createAttributes", at = @At("HEAD"), cancellable = true)
	private static void attributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
		cir.setReturnValue(Monster.createMonsterAttributes()
				.add(Attributes.FOLLOW_RANGE, 96.0)
				.add(Attributes.MOVEMENT_SPEED, 4.0)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.ARMOR, 2.0)
				.add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0)
				.add(Attributes.STEP_HEIGHT, 1.0));
	}

	@Inject(method = "travelInWater", at = @At("HEAD"), cancellable = true)
	private void swim(Vec3 vec3, double d, boolean bl, double e, CallbackInfo ci) {
		Drowned drowned = (Drowned) (Object) this;
		if (drowned.isUnderWater()) {
			drowned.moveRelative(0.1F, vec3);
			drowned.setDeltaMovement(drowned.getDeltaMovement().scale(0.9));
			ci.cancel();
		}
	}
}
