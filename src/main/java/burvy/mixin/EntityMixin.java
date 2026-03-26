package burvy.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Make zombies not burn
 */
@Mixin(Entity.class)
public class EntityMixin {
    @Inject(method = "fireImmune", at = @At("HEAD"), cancellable = true)
    private void makeImmuneToFire(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Zombie) {
            cir.setReturnValue(true);
        }
    }
}