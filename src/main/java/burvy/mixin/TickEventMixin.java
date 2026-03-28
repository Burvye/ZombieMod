package burvy.mixin;

import burvy.api.utilities.WaveSpawner;
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
    private static final int WAVE_TIME = 200;

    @Unique
    private int tickCounter = 0;

    @Inject(method = "tickServer", at = @At("HEAD"))
    private void onServerTick(BooleanSupplier booleanSupplier, CallbackInfo ci) {
        tickCounter++;
        if (tickCounter % WAVE_TIME != 0) return;

        // try spawning waves around all players
        MinecraftServer server = (MinecraftServer) (Object) this;
        for (ServerLevel level : server.getAllLevels()) {
            for (ServerPlayer player : level.players()) {
                WaveSpawner.INSTANCE.spawnWave(level, player);
            }
        }
    }
}
