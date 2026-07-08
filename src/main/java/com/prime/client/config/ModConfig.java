package com.prime.client.config;

import java.util.ArrayList;
import java.util.List;

public class ModConfig {
    public List<SwapSlot> swaps = new ArrayList<>();

    // HUD Toggles
    public boolean enableArmorHUD = true;
    public boolean enableEffectsHUD = true;
    public boolean enableArmorWarning = true;
    public int armorWarningThreshold = 50; // Triggers sound if durability drops below 50

    public boolean enableDeathLocation = true;

    public boolean enableTabListHealth = true;

    public boolean enableItemNotifier = true;
    public String itemWatchList = "алмаз, незерит, тотем, элитры";

    // New features
    public boolean enableAutoSprint = false;
    public boolean enableFullbright = false;
    public boolean enableNoFog = false;
    public boolean enableFreecam = true;
    public boolean enableHitboxESP = false;

    public ModConfig() {
        // Initialize default A/B slots. Key labels are no longer baked into the
        // name string - ConfigScreen resolves the actual bound key from
        // AutoSwap.getKeyName(index), so this can't go out of sync or crash
        // the GUI if a slot is renamed.
        swaps.add(new SwapSlot("Slot 1", "щит", "тотем"));
        swaps.add(new SwapSlot("Slot 2", "сфера", "талисман"));
        swaps.add(new SwapSlot("Slot 3", "факел", ""));
        swaps.add(new SwapSlot("Slot 4", "гелиос", "афина"));
        swaps.add(new SwapSlot("Slot 5", "золото", "железо"));
    }

    public static class SwapSlot {
        public String name;
        public String queryA;
        public String queryB;

        public SwapSlot() {}

        public SwapSlot(String name, String queryA, String queryB) {
            this.name = name;
            this.queryA = queryA;
            this.queryB = queryB;
        }
    }
}
