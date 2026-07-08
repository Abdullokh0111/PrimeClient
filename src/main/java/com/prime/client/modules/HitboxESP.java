package com.prime.client.modules;

import com.prime.client.api.Module;
import com.prime.client.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

/**
 * Hitbox ESP: programmatically toggles the built-in entity hitbox rendering
 * (the same as F3+B) via our settings menu, so the player doesn't need to
 * fumble with debug key combos.
 */
public class HitboxESP implements Module {
    @Override
    public String getName() {
        return "HitboxESP";
    }

    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;

            boolean shouldShow = ConfigManager.getConfig().enableHitboxESP;
            // EntityRenderDispatcher stores the hitbox toggle
            client.getEntityRenderDispatcher().setRenderHitboxes(shouldShow);
        });
    }
}
