package com.prime.client.modules;

import com.prime.client.config.ConfigManager;
import com.prime.client.config.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.SlotActionType;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class AutoSwap {
    private static final Logger LOGGER = LoggerFactory.getLogger("prime-autoswap");
    private static final List<KeyBinding> swapKeys = new ArrayList<>();

    public static void init() {
        LOGGER.info("Initializing AutoSwap module...");
        ConfigManager.load();

        int[] defaultKeys = new int[] {
                GLFW.GLFW_KEY_G, // Slot 1
                GLFW.GLFW_KEY_V, // Slot 2
                GLFW.GLFW_KEY_B, // Slot 3
                GLFW.GLFW_KEY_H, // Slot 4
                GLFW.GLFW_KEY_J  // Slot 5
        };

        for (int i = 0; i < 5; i++) {
            KeyBinding key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.prime.swap_slot_" + (i + 1),
                    InputUtil.Type.KEYSYM,
                    defaultKeys[i],
                    "category.prime.general"
            ));
            swapKeys.add(key);
        }

        // Register client tick listener
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.interactionManager == null) {
                return;
            }

            for (int i = 0; i < swapKeys.size(); i++) {
                while (swapKeys.get(i).wasPressed()) {
                    LOGGER.info("Swap Key {} pressed.", i + 1);
                    trySwapSlot(client, i);
                }
            }
        });
    }

    private static void trySwapSlot(MinecraftClient client, int slotIndex) {
        ClientPlayerEntity player = client.player;
        ClientPlayerInteractionManager interactionManager = client.interactionManager;

        if (player == null || interactionManager == null) {
            return;
        }

        ModConfig config = ConfigManager.getConfig();
        if (slotIndex >= config.swaps.size()) {
            return;
        }

        ModConfig.SwapSlot swapSlot = config.swaps.get(slotIndex);
        String qA = swapSlot.queryA.trim().toLowerCase();
        String qB = swapSlot.queryB.trim().toLowerCase();

        if (qA.isEmpty() && qB.isEmpty()) {
            LOGGER.info("Both swap queries for Slot {} are empty. Skipping.", slotIndex + 1);
            return;
        }

        // Check current item in offhand
        ItemStack offhandStack = player.getOffHandStack();
        String offhandName = offhandStack.isEmpty() ? "" : offhandStack.getName().getString().toLowerCase();

        boolean hasA = !qA.isEmpty() && offhandName.contains(qA);
        boolean hasB = !qB.isEmpty() && offhandName.contains(qB);

        String targetQuery = "";
        if (hasA) {
            // Offhand has Item A. Swap to Item B if specified.
            if (!qB.isEmpty()) {
                targetQuery = qB;
            } else {
                LOGGER.info("Item A is already equipped and Item B is not specified. Skipping.");
                return;
            }
        } else if (hasB) {
            // Offhand has Item B. Swap to Item A if specified.
            if (!qA.isEmpty()) {
                targetQuery = qA;
            } else {
                LOGGER.info("Item B is already equipped and Item A is not specified. Skipping.");
                return;
            }
        } else {
            // Offhand has neither. Equip Item A if specified, otherwise Item B.
            if (!qA.isEmpty()) {
                targetQuery = qA;
            } else {
                targetQuery = qB;
            }
        }

        if (targetQuery.isEmpty()) {
            return;
        }

        // Scan inventory (slots 9 to 44) for targetQuery
        int targetSlot = -1;
        for (int i = 9; i <= 44; i++) {
            ItemStack stack = player.playerScreenHandler.getSlot(i).getStack();
            if (stack.isEmpty()) {
                continue;
            }

            String displayName = stack.getName().getString().toLowerCase();
            if (displayName.contains(targetQuery)) {
                targetSlot = i;
                break;
            }
        }

        if (targetSlot != -1) {
            LOGGER.info("Found item matching '{}' at slot {}. Swapping to offhand...", targetQuery, targetSlot);
            int syncId = player.playerScreenHandler.syncId;
            interactionManager.clickSlot(syncId, targetSlot, 40, SlotActionType.SWAP, player);
        } else {
            LOGGER.info("No item matching '{}' found in inventory to swap.", targetQuery);
        }
    }
}
