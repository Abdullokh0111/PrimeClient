package com.prime.client.modules;

import com.prime.client.config.ConfigManager;
import com.prime.client.config.ModConfig;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.texture.Sprite;
import net.minecraft.entity.effect.StatusEffectInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class EffectsHUD {
    private static final Logger LOGGER = LoggerFactory.getLogger("prime-effectshud");

    public static void init() {
        LOGGER.info("Initializing Effects HUD...");

        // Register HUD rendering
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden) {
                return;
            }

            ModConfig config = ConfigManager.getConfig();
            if (!config.enableEffectsHUD) {
                return;
            }

            renderEffectsHUD(drawContext, client);
        });
    }

    private static void renderEffectsHUD(DrawContext context, MinecraftClient client) {
        Collection<StatusEffectInstance> activeEffects = client.player.getStatusEffects();
        if (activeEffects.isEmpty()) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();
        
        // Start rendering in the top-right corner
        int x = screenWidth - 105;
        int startY = 10;
        int currentY = startY;

        TextRenderer textRenderer = client.textRenderer;

        for (StatusEffectInstance effect : activeEffects) {
            // 1. Get status effect sprite
            Sprite sprite = client.getStatusEffectSpriteManager().getSprite(effect.getEffectType());
            
            // 2. Draw background panel for each effect
            int panelW = 100;
            int panelH = 22;
            int bgColor = 0x88000000; // Semi-transparent black background
            int borderColor = 0x22FFFFFF; // Very subtle border

            context.fill(x, currentY, x + panelW, currentY + panelH, bgColor);
            
            // Draw panel border
            context.fill(x, currentY, x + panelW, currentY + 1, borderColor);
            context.fill(x, currentY + panelH - 1, x + panelW, currentY + panelH, borderColor);
            context.fill(x, currentY, x + 1, currentY + panelH, borderColor);
            context.fill(x + panelW - 1, currentY, x + panelW, currentY + panelH, borderColor);

            // 3. Draw Sprite Icon (size 18x18, placed at offset 2,2)
            context.drawSprite(x + 2, currentY + 2, 0, 18, 18, sprite);

            // 4. Translate and format name
            String name = I18n.translate(effect.getEffectType().getTranslationKey());
            int amp = effect.getAmplifier();
            String levelText = "";
            if (amp > 0) {
                if (amp == 1) levelText = " II";
                else if (amp == 2) levelText = " III";
                else if (amp == 3) levelText = " IV";
                else if (amp == 4) levelText = " V";
                else levelText = " " + (amp + 1);
            }
            String displayName = name + levelText;

            // Draw effect name (white)
            context.drawTextWithShadow(
                    textRenderer,
                    displayName,
                    x + 24,
                    currentY + 3,
                    0xFFFFFF
            );

            // 5. Draw duration timer
            String durationText;
            if (effect.isInfinite()) {
                durationText = "∞";
            } else {
                durationText = formatDuration(effect.getDuration());
            }

            // Draw timer (light gray)
            context.drawTextWithShadow(
                    textRenderer,
                    durationText,
                    x + 24,
                    currentY + 12,
                    0xAAAAAA
            );

            currentY += 25; // 22 height + 3 spacing
        }
    }

    private static String formatDuration(int ticks) {
        int totalSecs = ticks / 20;
        int mins = totalSecs / 60;
        int secs = totalSecs % 60;
        return String.format("%02d:%02d", mins, secs);
    }
}
