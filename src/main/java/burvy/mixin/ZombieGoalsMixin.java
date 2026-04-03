package burvy.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.monster.zombie.Drowned;
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

        // target players within 96 blocks
        TargetingConditions.Selector nearbyPlayers = (target, level) -> {
            if (!(target instanceof ServerPlayer sp)) return false;
            return zombie.distanceToSqr(sp) < 9216.0; // 96^2
        };

        // scan attackable every 20 ticks
        accessor.getTargetSelector().addGoal(1, new NearestAttackableTargetGoal<>(
                zombie, Player.class, 20, false, false, nearbyPlayers
        ));

        // TODO: Somehow make Minecraft A* pathfinding not block up the server thread or stagger pathfinding (intensive!)
        // TODO 2: Somehow make pathfinding chunked? Just pathfinding closer to a player rather than to them directly?
        accessor.getGoalSelector().addGoal(2, new ZombieAttackGoal(zombie, 1.0, false) {
            private int repathTimer = 0;

            @Override
            public void tick() {
                if (++repathTimer % 10 == 0) {
                    super.tick();
                }
            }
        });
        // drowneds should stay in water, other zombies avoid it
        if (zombie instanceof Drowned) {
            accessor.getGoalSelector().addGoal(3, new RandomSwimmingGoal(zombie, 1.0, 40));
        } else {
            accessor.getGoalSelector().addGoal(3, new WaterAvoidingRandomStrollGoal(zombie, 0.8));
        }

        ci.cancel();
    }
}
