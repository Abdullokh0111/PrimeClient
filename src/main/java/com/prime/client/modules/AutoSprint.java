package com.prime.client.modules;

import com.prime.client.api.Module;
import com.prime.client.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class AutoSprint implements Module {
    @Override
    public String getName() {
        return "AutoSprint";
    }

    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            if (ConfigManager.getConfig().enableAutoSprint) {
                if (client.options.forwardKey.isPressed() && !client.player.isSneaking() && !client.player.isUsingItem() && client.player.getHungerManager().getFoodLevel() > 6) {
                    client.player.setSprinting(true);
                }
            }
        });
    }
}
