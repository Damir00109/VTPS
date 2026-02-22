package com.damir00109.mixin;

import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.damir00109.VTPS;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {

    @Inject(method = "tickServer", at = @At("TAIL")) // tick -> tickServer
    private void onServerTick(CallbackInfo ci) {
        VTPS.onServerTick((MinecraftServer) (Object) this);
    }
}