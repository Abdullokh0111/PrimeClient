package com.prime.client.modules;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;

public class TargetHUD {
    private static PlayerEntity targetPlayer = null;
    private static int fadeTicks = 0;
    private static final int MAX_FADE_TICKS = 30; // 1.5 seconds (30 client ticks)

    public static void init() {
        // Register tick event to track targeted player
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

        // Register HUD renderer
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

    private static void renderTargetHUD(DrawContext context, MinecraftClient client) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int width = 160;
        int height = 45;
        int x = (screenWidth / 2) - (width / 2);
        int y = screenHeight - 110;

        float alpha = (float) fadeTicks / MAX_FADE_TICKS;
        
        // Setup colors with transparency
        int bgAlpha = (int) (0xCC * alpha) << 24;
        int borderAlpha = (int) (0x33 * alpha) << 24;
        int accentAlpha = (int) (0xFF * alpha) << 24;

        int bgColor = bgAlpha | 0x101010;
        int borderColor = borderAlpha | 0xFFFFFF;
        int accentColor = accentAlpha | 0x00BFFF; // DeepSkyBlue

        // 1. Draw Background
        context.fill(x, y, x + width, y + height, bgColor);

        // 2. Draw Borders
        context.fill(x, y, x + width, y + 1, borderColor); // Top
        context.fill(x, y + height - 1, x + width, y + height, borderColor); // Bottom
        context.fill(x, y, x + 1, y + height, borderColor); // Left
        context.fill(x + width - 1, y, x + width, y + height, borderColor); // Right

        // 3. Draw Accent Line (Top)
        context.fill(x, y, x + width, y + 2, accentColor);

        // 4. Draw Player Head
        if (targetPlayer instanceof AbstractClientPlayerEntity) {
            Identifier skinTexture = ((AbstractClientPlayerEntity) targetPlayer).getSkinTexture();
            int headX = x + 6;
            int headY = y + 6;
            int headSize = 32;

            // Draw inner face layer
            context.drawTexture(skinTexture, headX, headY, headSize, headSize, 8, 8, 8, 8, 64, 64);
            // Draw outer hat layer
            context.drawTexture(skinTexture, headX, headY, headSize, headSize, 40, 8, 8, 8, 64, 64);
        }

        // 5. Draw Nickname
        String name = targetPlayer.getName().getString();
        int maxNameWidth = 105;
        if (client.textRenderer.getWidth(name) > maxNameWidth) {
            name = client.textRenderer.trimToWidth(name, maxNameWidth - 8) + "...";
        }
        int textColor = (int) (0xFF * alpha) << 24 | 0xFFFFFF;
        context.drawTextWithShadow(client.textRenderer, name, x + 44, y + 6, textColor);

        // 6. Draw Latency/Ping
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

        // 7. Draw Health Bar
        float health = targetPlayer.getHealth();
        float maxHealth = targetPlayer.getMaxHealth();
        float healthPercent = Math.min(1.0f, Math.max(0.0f, health / maxHealth));

        int barX = x + 44;
        int barY = y + 29;
        int barW = 108;
        int barH = 9;

        // Health bar background
        int barBgColor = (int) (0x33 * alpha) << 24 | 0x555555;
        context.fill(barX, barY, barX + barW, barY + barH, barBgColor);

        // Health bar fill color (smooth transition from green to red)
        int r = (int) (255 * (1.0f - healthPercent));
        int g = (int) (255 * healthPercent);
        int barColor = (int) (0xFF * alpha) << 24 | (r << 16) | (g << 8);
        context.fill(barX, barY, barX + (int) (barW * healthPercent), barY + barH, barColor);

        // Health bar text overlay (e.g. "14.2 HP")
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
