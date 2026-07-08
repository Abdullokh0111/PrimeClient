package com.prime.client.modules;

import com.prime.client.api.Module;
import com.prime.client.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;

public class Fullbright implements Module {
    @Override
    public String getName() {
        return "Fullbright";
    }

    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            
            if (ConfigManager.getConfig().enableFullbright) {
                if (!client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                    client.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false, false));
                } else {
                    // Refresh if duration is running low
                    StatusEffectInstance effect = client.player.getStatusEffect(StatusEffects.NIGHT_VISION);
                    if (effect != null && effect.getDuration() < 100 && !effect.shouldShowParticles()) {
                        client.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 400, 0, false, false, false));
                    }
                }
            } else {
                if (client.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
                    StatusEffectInstance effect = client.player.getStatusEffect(StatusEffects.NIGHT_VISION);
                    // Only remove if it's our ambient effect (no particles)
                    if (effect != null && !effect.shouldShowParticles()) {
                        client.player.removeStatusEffect(StatusEffects.NIGHT_VISION);
                    }
                }
            }
        });
    }
}
