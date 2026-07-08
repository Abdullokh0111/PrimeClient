package com.prime.client.modules;

import com.prime.client.api.Module;
import com.prime.client.config.ConfigManager;
import com.prime.client.config.ModConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ArmorHUD implements Module {
    private static final Logger LOGGER = LoggerFactory.getLogger("prime-armorhud");
    private int warningCooldown = 0;

    @Override
    public String getName() {
        return "ArmorHUD";
    }

    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                warningCooldown = 0;
                return;
            }

            ModConfig config = ConfigManager.getConfig();
            if (!config.enableArmorWarning) {
                return;
            }

            if (warningCooldown > 0) {
                warningCooldown--;
            }

            boolean lowDurabilityDetected = false;
            PlayerEntity player = client.player;

            for (ItemStack stack : player.getInventory().armor) {
                if (stack.isEmpty() || !stack.isDamageable()) {
                    continue;
                }

                int remaining = stack.getMaxDamage() - stack.getDamage();
                if (remaining <= config.armorWarningThreshold) {
                    lowDurabilityDetected = true;
                    break;
                }
            }

            if (lowDurabilityDetected && warningCooldown == 0) {
                LOGGER.info("Low durability alert triggered!");
                player.playSound(
                        SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(),
                        SoundCategory.PLAYERS,
                        1.0F,
                        1.8F
                );
                warningCooldown = 80; // 4 seconds cooldown to prevent spam
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden) {
                return;
            }

            ModConfig config = ConfigManager.getConfig();
            if (!config.enableArmorHUD) {
                return;
            }

            renderArmorHUD(drawContext, client);
        });
    }

    private void renderArmorHUD(DrawContext context, MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        List<ItemStack> items = new ArrayList<>();
        ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack legs = player.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        ItemStack mainHand = player.getEquippedStack(EquipmentSlot.MAINHAND);
        ItemStack offHand = player.getEquippedStack(EquipmentSlot.OFFHAND);

        if (!helmet.isEmpty()) items.add(helmet);
        if (!chest.isEmpty()) items.add(chest);
        if (!legs.isEmpty()) items.add(legs);
        if (!boots.isEmpty()) items.add(boots);
        if (!mainHand.isEmpty()) items.add(mainHand);
        if (!offHand.isEmpty()) items.add(offHand);

        if (items.isEmpty()) {
            return;
        }

        int itemSize = 16;
        int spacer = 4;
        int totalWidth = (items.size() * itemSize) + ((items.size() - 1) * spacer);
        int startX = (screenWidth / 2) - (totalWidth / 2);
        int y = screenHeight - 60;

        TextRenderer textRenderer = client.textRenderer;

        for (int i = 0; i < items.size(); i++) {
            ItemStack stack = items.get(i);
            int currentX = startX + (i * (itemSize + spacer));

            context.drawItem(stack, currentX, y);
            context.drawItemInSlot(textRenderer, stack, currentX, y);

            if (stack.isDamageable()) {
                int maxDamage = stack.getMaxDamage();
                int currentDamage = stack.getDamage();
                int remaining = maxDamage - currentDamage;
                float pct = (float) remaining / maxDamage;

                int color = 0xFF55FF55;
                if (pct < 0.15F) {
                    color = 0xFFFF5555;
                } else if (pct < 0.40F) {
                    color = 0xFFFFAA00;
                } else if (pct < 0.70F) {
                    color = 0xFFFFFF55;
                }

                String text = String.valueOf(remaining);
                float textW = textRenderer.getWidth(text);

                context.getMatrices().push();
                context.getMatrices().translate(currentX + 8.0F, y - 6.0F, 0.0F);
                context.getMatrices().scale(0.5F, 0.5F, 1.0F);

                context.drawTextWithShadow(
                        textRenderer,
                        text,
                        (int) (-textW / 2.0F),
                        0,
                        color
                );
                context.getMatrices().pop();
            }
        }
    }
}
