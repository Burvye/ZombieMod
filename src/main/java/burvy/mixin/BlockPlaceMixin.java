package burvy.mixin;

import burvy.api.utilities.NoiseChecker;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public class BlockPlaceMixin {

	@Inject(method = "useOn", at = @At("RETURN"))
	private void onPlace(UseOnContext ctx, CallbackInfoReturnable<InteractionResult> cir) {
		if (!(ctx.getPlayer() instanceof ServerPlayer player)) return;
		if (!(ctx.getLevel() instanceof ServerLevel level)) return;
		if (!cir.getReturnValue().consumesAction()) return;
		NoiseChecker.INSTANCE.makeNoise(player, level, player.blockPosition(), NoiseChecker.NoiseType.BLOCK_PLACE);
	}
}
