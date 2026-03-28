package burvy.mixin;

import burvy.api.utilities.WaveSpawner;
import burvy.api.utilities.ZombBlocks;
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
    private void onServerTick(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        MinecraftServer server = (MinecraftServer) (Object) this;

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
}
