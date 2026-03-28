package burvy.mixin;

import burvy.api.utilities.TickChecker;
import burvy.api.utilities.WaveSpawner;
import burvy.api.utilities.ZombBlocks;
import burvy.api.utilities.ZombCuller;
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
    private static final int WAVE_TIME = 200; // in ticks

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

        // stop everything else if we are lagging
        if (TickChecker.INSTANCE.isLagging()) {
            timer++;
            return;
        }

        // zombie world modification per tick across all dimensions
        for (ServerLevel level : server.getAllLevels()) {
            ZombBlocks.INSTANCE.tick(level);
        }

        // wave spawner per wave time around all players
        timer++;
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
