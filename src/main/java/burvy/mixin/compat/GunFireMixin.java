package burvy.mixin.compat;

import burvy.systems.NoiseChecker;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "burvy.gunmod.gun.animation.GunAnimationController")
public class GunFireMixin {

	// TODO LATER: Change when gun mod is rewritten
	@Inject(method = "startFiring", at = @At("HEAD"), require = 0)
	private void onGunfire(ServerPlayer player, CallbackInfo ci) {
		NoiseChecker.INSTANCE.gunshot(player);
	}
}
