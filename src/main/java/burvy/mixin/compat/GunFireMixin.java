package burvy.mixin.compat;

import burvy.api.utilities.NoiseChecker;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "burvy.gunmod.gun.animation.PlayerGunState")
public class GunFireMixin {

	// gun mod hook, change when updating gun mod
	// TODO: Change signature when gun mod is rewritten to Kotlin
	@Inject(method = "fireShot", at = @At("HEAD"), require = 0)
	private void onGunfire(ServerPlayer player, CallbackInfo ci) {
		NoiseChecker.INSTANCE.gunshot(player);
	}
}
