package com.prime.client.modules;

import com.prime.client.api.Module;
import com.prime.client.config.ConfigManager;
import com.prime.client.config.ModConfig;
import com.prime.client.gui.HudPanel;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class ItemNotifier implements Module {
    private static final Logger LOGGER = LoggerFactory.getLogger("prime-itemnotifier");
    private static final int NOTIFICATION_TICKS = 60; // 3 seconds
    private static final int MAX_QUEUED = 4;


    private Map<String, Integer> lastCounts = new HashMap<>();
    private boolean primed = false;

    private static class Notification {
        final String text;
        int ticksLeft;

        Notification(String text) {
            this.text = text;
            this.ticksLeft = NOTIFICATION_TICKS;
        }
    }

    private final Deque<Notification> active = new ArrayDeque<>();

    @Override
    public String getName() {
        return "ItemNotifier";
    }

    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            active.removeIf(n -> --n.ticksLeft <= 0);

            if (client.player == null) {
                primed = false;
                lastCounts.clear();
                return;
            }

            ModConfig config = ConfigManager.getConfig();
            if (!config.enableItemNotifier) {
                return;
            }

            List<String> keywords = parseKeywords(config.itemWatchList);
            checkInventory(client, keywords);
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.options.hudHidden || active.isEmpty()) {
                return;
            }
            render(drawContext, client);
        });
    }

    private List<String> parseKeywords(String raw) {
        List<String> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        for (String part : raw.split(",")) {
            String trimmed = part.trim().toLowerCase();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private void checkInventory(MinecraftClient client, List<String> keywords) {
        Map<String, Integer> currentCounts = new HashMap<>();

        for (int i = 0; i < 36; i++) {
            ItemStack stack = client.player.getInventory().getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            String name = stack.getName().getString();
            currentCounts.merge(name, stack.getCount(), Integer::sum);
        }

        if (!primed) {
            lastCounts = currentCounts;
            primed = true;
            return;
        }

        for (Map.Entry<String, Integer> entry : currentCounts.entrySet()) {
            String name = entry.getKey();
            int count = entry.getValue();
            int previous = lastCounts.getOrDefault(name, 0);

            if (count > previous) {
                int gained = count - previous;
                if (keywords.isEmpty()) {
                    queueNotification(name, gained);
                    LOGGER.info("Watched item gained (all): {} x{}", name, gained);
                } else {
                    String lowerName = name.toLowerCase();
                    for (String keyword : keywords) {
                        if (lowerName.contains(keyword)) {
                            queueNotification(name, gained);
                            LOGGER.info("Watched item gained (match): {} x{}", name, gained);
                            break;
                        }
                    }
                }
            }
        }

        lastCounts = currentCounts;
    }

    private void queueNotification(String itemName, int gained) {
        String text = gained > 1 ? ("+" + gained + " " + itemName) : itemName;
        if (active.size() >= MAX_QUEUED) {
            active.pollFirst();
        }
        active.addLast(new Notification(text));
    }

    private void render(DrawContext context, MinecraftClient client) {
        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();

        int panelW = 150;
        int panelH = 18;
        int x = screenWidth - panelW - 10;
        int y = 10;

        int i = 0;
        for (Notification n : active) {
            float alpha = n.ticksLeft < 15 ? n.ticksLeft / 15.0f : 1.0f;
            int bgAlpha = (int) (0x99 * alpha) << 24;
            int borderAlpha = (int) (0x33 * alpha) << 24;
            int textAlpha = (int) (0xFF * alpha) << 24;

            int rowY = y + i * (panelH + 3);
            HudPanel.draw(context, x, rowY, panelW, panelH, bgAlpha | 0x101010, borderAlpha | 0xFFAA00);
            context.drawCenteredTextWithShadow(
                    client.textRenderer,
                    n.text,
                    x + panelW / 2,
                    rowY + 5,
                    textAlpha | 0xFFDD88
            );
            i++;
        }
    }
}
