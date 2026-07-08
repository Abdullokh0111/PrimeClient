package com.prime.client;

import com.prime.client.api.ModuleManager;
import com.prime.client.config.ConfigManager;
import com.prime.client.gui.ConfigScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PrimeClient implements ClientModInitializer {
    public static final String MOD_ID = "prime";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static KeyBinding openConfigKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Prime Client...");

        // Load configuration first - every module reads from it, so this must
        // not depend on which module happens to init first.
        ConfigManager.load();

        // Adding a new module now means registering it in ModuleManager,
        // not hand-editing this method.
        ModuleManager.initAll();

        // Register configuration screen keybinding (RSHIFT)
        openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.prime.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT, // Default: Right Shift
                "category.prime.general"
        ));

        // Tick listener to open the configuration GUI screen
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) {
                return;
            }
            while (openConfigKey.wasPressed()) {
                LOGGER.info("Opening Prime Config GUI...");
                client.setScreen(new ConfigScreen(client.currentScreen));
            }
        });
    }
}
