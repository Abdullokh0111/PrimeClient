package com.prime.client.modules;

import com.prime.client.api.Module;
import com.prime.client.config.ConfigManager;
import com.prime.client.config.ModConfig;
import com.prime.client.gui.HudPanel;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
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

public class EffectsHUD implements Module {
    private static final Logger LOGGER = LoggerFactory.getLogger("prime-effectshud");

    @Override
    public String getName() {
        return "EffectsHUD";
    }

    @Override
    public void init() {
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

    private void renderEffectsHUD(DrawContext context, MinecraftClient client) {
        Collection<StatusEffectInstance> activeEffects = client.player.getStatusEffects();
        if (activeEffects.isEmpty()) {
            return;
        }

        int screenWidth = client.getWindow().getScaledWidth();

        int x = screenWidth - 105;
        int startY = 10;
        int currentY = startY;

        TextRenderer textRenderer = client.textRenderer;

        for (StatusEffectInstance effect : activeEffects) {
            Sprite sprite = client.getStatusEffectSpriteManager().getSprite(effect.getEffectType());

            int panelW = 100;
            int panelH = 22;
            HudPanel.draw(context, x, currentY, panelW, panelH, 0x88000000, 0x22FFFFFF);

            context.drawSprite(x + 2, currentY + 2, 0, 18, 18, sprite);

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

            context.drawTextWithShadow(
                    textRenderer,
                    displayName,
                    x + 24,
                    currentY + 3,
                    0xFFFFFF
            );

            String durationText;
            if (effect.isInfinite()) {
                durationText = "∞";
            } else {
                durationText = formatDuration(effect.getDuration());
            }

            context.drawTextWithShadow(
                    textRenderer,
                    durationText,
                    x + 24,
                    currentY + 12,
                    0xAAAAAA
            );

            currentY += 25;
        }
    }

    private String formatDuration(int ticks) {
        int totalSecs = ticks / 20;
        int mins = totalSecs / 60;
        int secs = totalSecs % 60;
        return String.format("%02d:%02d", mins, secs);
    }
}
