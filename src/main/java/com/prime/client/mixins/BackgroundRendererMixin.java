package com.prime.client.mixins;

import com.prime.client.config.ConfigManager;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Pushes fog start/end distances far beyond render distance when NoFog is
 * enabled, effectively making fog invisible. Works for terrain fog, water
 * fog, lava fog, and powder snow fog.
 */
@Mixin(BackgroundRenderer.class)
public abstract class BackgroundRendererMixin {

    @Inject(method = "applyFog", at = @At("TAIL"))
    private static void onApplyFog(Camera camera, BackgroundRenderer.FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo ci) {
        if (ConfigManager.getConfig().enableNoFog) {
            RenderSystem.setShaderFogStart(viewDistance * 4.0f);
            RenderSystem.setShaderFogEnd(viewDistance * 8.0f);
        }
    }
}
