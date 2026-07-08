package com.prime.client.modules;

import com.prime.client.api.Module;
import com.prime.client.gui.HudPanel;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public class TargetHUD implements Module {
    private PlayerEntity targetPlayer = null;
    private int fadeTicks = 0;
    private static final int MAX_FADE_TICKS = 30; // 1.5 seconds (30 client ticks)

    @Override
    public String getName() {
        return "TargetHUD";
    }

    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                targetPlayer = null;
                fadeTicks = 0;
                return;
            }

            if (client.targetedEntity instanceof PlayerEntity) {
                targetPlayer = (PlayerEntity) client.targetedEntity;
                fadeTicks = MAX_FADE_TICKS;
            } else {
                if (fadeTicks > 0) {
                    fadeTicks--;
                    if (fadeTicks == 0) {
                        targetPlayer = null;
                    }
                }
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            if (targetPlayer == null || targetPlayer.isRemoved()) {
                return;
            }

            MinecraftClient client = MinecraftClient.getInstance();
            if (client.options.hudHidden) {
                return;
            }

            renderTargetHUD(drawContext, client);
        });
    }

    private void renderTargetHUD(DrawContext context, MinecraftClient client) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int width = 160;
        int height = 45;
        int x = (screenWidth / 2) - (width / 2);
        int y = screenHeight - 110;

        float alpha = (float) fadeTicks / MAX_FADE_TICKS;

        int bgAlpha = (int) (0xCC * alpha) << 24;
        int borderAlpha = (int) (0x33 * alpha) << 24;
        int accentAlpha = (int) (0xFF * alpha) << 24;

        int bgColor = bgAlpha | 0x101010;
        int borderColor = borderAlpha | 0xFFFFFF;
        int accentColor = accentAlpha | 0x00BFFF; // DeepSkyBlue

        HudPanel.drawWithTopAccent(context, x, y, width, height, bgColor, borderColor, accentColor);

        if (targetPlayer instanceof AbstractClientPlayerEntity) {
            Identifier skinTexture = ((AbstractClientPlayerEntity) targetPlayer).getSkinTexture();
            int headX = x + 6;
            int headY = y + 6;
            int headSize = 32;

            context.drawTexture(skinTexture, headX, headY, headSize, headSize, 8, 8, 8, 8, 64, 64);
            context.drawTexture(skinTexture, headX, headY, headSize, headSize, 40, 8, 8, 8, 64, 64);
        }

        String name = targetPlayer.getName().getString();
        int maxNameWidth = 105;
        if (client.textRenderer.getWidth(name) > maxNameWidth) {
            name = client.textRenderer.trimToWidth(name, maxNameWidth - 8) + "...";
        }
        int textColor = (int) (0xFF * alpha) << 24 | 0xFFFFFF;
        context.drawTextWithShadow(client.textRenderer, name, x + 44, y + 6, textColor);

        int ping = 0;
        if (client.getNetworkHandler() != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(targetPlayer.getUuid());
            if (entry != null) {
                ping = entry.getLatency();
            }
        }
        String pingText = ping + "ms";
        int pingColor = (int) (0xFF * alpha) << 24 | 0x888888;
        context.drawTextWithShadow(client.textRenderer, pingText, x + 44, y + 17, pingColor);

        float health = targetPlayer.getHealth();
        float maxHealth = targetPlayer.getMaxHealth();
        float healthPercent = Math.min(1.0f, Math.max(0.0f, health / maxHealth));

        int barX = x + 44;
        int barY = y + 29;
        int barW = 108;
        int barH = 9;

        int barBgColor = (int) (0x33 * alpha) << 24 | 0x555555;
        context.fill(barX, barY, barX + barW, barY + barH, barBgColor);

        int r = (int) (255 * (1.0f - healthPercent));
        int g = (int) (255 * healthPercent);
        int barColor = (int) (0xFF * alpha) << 24 | (r << 16) | (g << 8);
        context.fill(barX, barY, barX + (int) (barW * healthPercent), barY + barH, barColor);

        String healthText = String.format("%.1f HP", health);
        int textW = client.textRenderer.getWidth(healthText);
        int healthTextColor = (int) (0xFF * alpha) << 24 | 0xFFFFFF;
        context.drawTextWithShadow(
                client.textRenderer,
                healthText,
                barX + (barW / 2) - (textW / 2),
                barY + 1,
                healthTextColor
        );
    }
}
