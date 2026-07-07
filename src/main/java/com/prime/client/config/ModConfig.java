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

    public ModConfig() {
        // Initialize default A/B slots
        swaps.add(new SwapSlot("Slot 1 (G)", "щит", "тотем"));
        swaps.add(new SwapSlot("Slot 2 (V)", "сфера", "талисман"));
        swaps.add(new SwapSlot("Slot 3 (B)", "факел", ""));
        swaps.add(new SwapSlot("Slot 4 (H)", "гелиос", "афина"));
        swaps.add(new SwapSlot("Slot 5 (J)", "золото", "железо"));
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
