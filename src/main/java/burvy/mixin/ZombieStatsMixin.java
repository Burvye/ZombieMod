package burvy.mixin;

import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Edit zombie attributes
 */
@Mixin(Zombie.class)
public class ZombieStatsMixin {

	@Inject(method = "createAttributes", at = @At("HEAD"), cancellable = true)
	private static void customAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> cir) {
		cir.setReturnValue(Monster.createMonsterAttributes()
				.add(Attributes.FOLLOW_RANGE, 4.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3F)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.ARMOR, 2.0)
				.add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0));
	}

	// removed sun sensitive override
}
