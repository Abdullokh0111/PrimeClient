package com.prime.client.mixins;

import com.prime.client.modules.Freecam;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks the player from sending movement packets to the server while
 * Freecam is active. This keeps the real player body stationary so the
 * server doesn't see impossible movement and kick for flying/speed hacks.
 */
@Mixin(ClientPlayerEntity.class)
public abstract class ClientPlayerEntityMixin {

    @Inject(method = "sendMovementPackets", at = @At("HEAD"), cancellable = true)
    private void onSendMovementPackets(CallbackInfo ci) {
        if (Freecam.isActive()) {
            ci.cancel();
        }
    }
}
