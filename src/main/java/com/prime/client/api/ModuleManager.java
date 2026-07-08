package com.prime.client.api;

import com.prime.client.modules.ArmorHUD;
import com.prime.client.modules.AutoSwap;
import com.prime.client.modules.DeathLocationHUD;
import com.prime.client.modules.EffectsHUD;
import com.prime.client.modules.ItemNotifier;
import com.prime.client.modules.NickChanger;
import com.prime.client.modules.TargetHUD;
import com.prime.client.modules.AutoSprint;
import com.prime.client.modules.Fullbright;
import com.prime.client.modules.Freecam;
import com.prime.client.modules.HitboxESP;
import com.prime.client.modules.NoFog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Central registry of every module in the mod. Adding a new module means
 * adding one line here instead of hand-editing PrimeClient's init order.
 */
public final class ModuleManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("prime-modules");

    private static final List<Module> MODULES = List.of(
            new AutoSwap(),
            new TargetHUD(),
            new ArmorHUD(),
            new EffectsHUD(),
            new DeathLocationHUD(),
            new ItemNotifier(),
            new NickChanger(),
            new AutoSprint(),
            new Fullbright(),
            new Freecam(),
            new HitboxESP(),
            new NoFog()
    );

    private ModuleManager() {
    }

    /** Initializes every registered module, in order. Config must already be loaded. */
    public static void initAll() {
        for (Module module : MODULES) {
            LOGGER.info("Initializing module: {}", module.getName());
            module.init();
        }
    }
}
