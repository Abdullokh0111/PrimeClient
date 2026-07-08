package com.prime.client.modules;

import com.prime.client.api.Module;
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

public class AutoSwap implements Module {
    private static final Logger LOGGER = LoggerFactory.getLogger("prime-autoswap");
    // Static, not instance-level: there's only ever one AutoSwap (see ModuleManager),
    // and ConfigScreen needs to read the live KeyBinding objects (to detect conflicts
    // with keys the user rebound in vanilla Controls) without holding a reference to
    // the module instance itself.
    private static final List<KeyBinding> swapKeys = new ArrayList<>();

    // Minecraft keybindings can only be registered once, at mod init time -
    // they can't be created dynamically when the user adds a config slot later.
    // So we pre-register a fixed pool of slots up front; ConfigScreen is only
    // allowed to use up to this many.
    public static final int MAX_SLOTS = 8;

    private static final int[] DEFAULT_KEYS = new int[] {
            GLFW.GLFW_KEY_G, // Slot 1
            GLFW.GLFW_KEY_V, // Slot 2
            GLFW.GLFW_KEY_B, // Slot 3
            GLFW.GLFW_KEY_H, // Slot 4
            GLFW.GLFW_KEY_J, // Slot 5
            GLFW.GLFW_KEY_N, // Slot 6
            GLFW.GLFW_KEY_K, // Slot 7
            GLFW.GLFW_KEY_L  // Slot 8
    };

    private static final String[] KEY_NAMES = new String[] {
            "G", "V", "B", "H", "J", "N", "K", "L"
    };

    @Override
    public String getName() {
        return "AutoSwap";
    }

    @Override
    public void init() {
        for (int i = 0; i < MAX_SLOTS; i++) {
            KeyBinding key = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                    "key.prime.swap_slot_" + (i + 1),
                    InputUtil.Type.KEYSYM,
                    DEFAULT_KEYS[i],
                    "category.prime.general"
            ));
            swapKeys.add(key);
        }

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

    /** Display name (e.g. "G") of the key bound to the given slot index, for GUI labels. */
    public static String getKeyName(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= KEY_NAMES.length) {
            return "?";
        }
        return KEY_NAMES[slotIndex];
    }

    /**
     * Checks whether the key currently bound to the given slot (which may have been
     * rebound by the user in vanilla Controls, so this reads the live KeyBinding, not
     * the default) collides with any other registered keybinding - vanilla, another
     * mod, or one of our own other slots. Meant to be called on-demand (e.g. when
     * ConfigScreen opens), since other mods' keybindings aren't all registered yet
     * at our own init() time.
     *
     * @return the display name of the conflicting action (e.g. "Swap Offhand"), or
     *         null if there's no conflict.
     */
    public static String getConflict(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= swapKeys.size()) {
            return null;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return null;
        }

        KeyBinding ours = swapKeys.get(slotIndex);
        InputUtil.Key oursKey = InputUtil.fromTranslationKey(ours.getBoundKeyTranslationKey());
        if (oursKey.getCode() == InputUtil.UNKNOWN_KEY.getCode()) {
            return null; // unbound, nothing to conflict with
        }

        for (KeyBinding other : client.options.allKeys) {
            if (other == ours) {
                continue;
            }
            InputUtil.Key otherKey = InputUtil.fromTranslationKey(other.getBoundKeyTranslationKey());
            if (otherKey.getCode() == oursKey.getCode() && otherKey.getCategory() == oursKey.getCategory()) {
                return net.minecraft.client.resource.language.I18n.translate(other.getTranslationKey());
            }
        }
        return null;
    }

    private void trySwapSlot(MinecraftClient client, int slotIndex) {
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

        ItemStack offhandStack = player.getOffHandStack();
        String offhandName = offhandStack.isEmpty() ? "" : offhandStack.getName().getString().toLowerCase();

        boolean hasA = !qA.isEmpty() && offhandName.contains(qA);
        boolean hasB = !qB.isEmpty() && offhandName.contains(qB);

        String targetQuery = "";
        if (hasA) {
            if (!qB.isEmpty()) {
                targetQuery = qB;
            } else {
                LOGGER.info("Item A is already equipped and Item B is not specified. Skipping.");
                return;
            }
        } else if (hasB) {
            if (!qA.isEmpty()) {
                targetQuery = qA;
            } else {
                LOGGER.info("Item B is already equipped and Item A is not specified. Skipping.");
                return;
            }
        } else {
            if (!qA.isEmpty()) {
                targetQuery = qA;
            } else {
                targetQuery = qB;
            }
        }

        if (targetQuery.isEmpty()) {
            return;
        }

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
