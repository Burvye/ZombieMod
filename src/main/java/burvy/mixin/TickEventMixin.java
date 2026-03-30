package burvy.mixin;

import burvy.api.utilities.TickChecker;
import burvy.systems.NoiseChecker;
import burvy.systems.WaveSpawner;
import burvy.systems.ZombBlocks;
import burvy.systems.ZombCuller;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/**
 * Place to put per-tick events
 */
@Mixin(MinecraftServer.class)
public class TickEventMixin {

    @Unique
    private static final int WAVE_TIME = 200; // 10 seconds

    @Unique
    private int timer = 0;

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void tick(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;
        TickChecker.INSTANCE.tickStart();

        // cull zombies if lagging
        for (ServerLevel level : server.getAllLevels()) {
            ZombCuller.INSTANCE.tick(level);
        }

        // zombie world modification and noise processing
        for (ServerLevel level : server.getAllLevels()) {
            ZombBlocks.INSTANCE.tick(level);
            NoiseChecker.INSTANCE.tick(level);
        }

        // skip spawning when lagging
        timer++;
        if (TickChecker.INSTANCE.isLagging()) return;
        if (timer % WAVE_TIME != 0) return;
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                WaveSpawner.INSTANCE.spawnWave(level, player);
            }
        }
    }

    @Inject(method = "tickServer", at = @At("TAIL"))
    private void tickEnd(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        TickChecker.INSTANCE.tickEnd();
    }
}
