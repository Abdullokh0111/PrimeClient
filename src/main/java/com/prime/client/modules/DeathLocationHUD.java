package com.prime.client.modules;

import com.prime.client.api.Module;
import com.prime.client.config.ConfigManager;
import com.prime.client.config.ModConfig;
import com.prime.client.gui.HudPanel;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DeathLocationHUD implements Module {
    private static final Logger LOGGER = LoggerFactory.getLogger("prime-deathloc");


    private static final double RECOVERED_DISTANCE = 3.0;
    private static final int MAX_LIFETIME_TICKS = 20 * 60 * 20;

    private Vec3d deathPos = null;
    private RegistryKey<World> deathDimension = null;
    private int lifetimeTicks = 0;

    private boolean wasAlive = false;
    private Vec3d lastKnownPos = null;
    private RegistryKey<World> lastKnownDimension = null;

    @Override
    public String getName() {
        return "DeathLocationHUD";
    }

    @Override
    public void init() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                return;
            }

            float health = client.player.getHealth();
            if (health > 0) {
                wasAlive = true;
                lastKnownPos = client.player.getPos();
                lastKnownDimension = client.world.getRegistryKey();
            } else if (health <= 0 && wasAlive) {
                deathPos = lastKnownPos;
                deathDimension = lastKnownDimension;
                lifetimeTicks = 0;
                wasAlive = false;
                if (deathPos != null && deathDimension != null) {
                    LOGGER.info("Death location recorded at {} in {}.", deathPos, deathDimension.getValue());
                }
            }

            if (deathPos == null) {
                return;
            }

            lifetimeTicks++;
            if (lifetimeTicks > MAX_LIFETIME_TICKS) {
                clear();
                return;
            }

            if (client.player.getHealth() > 0 
                    && client.world.getRegistryKey().equals(deathDimension)
                    && client.player.getPos().distanceTo(deathPos) < RECOVERED_DISTANCE) {
                LOGGER.info("Player returned to death location, clearing marker.");
                clear();
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null || client.world == null || client.options.hudHidden) {
                return;
            }

            ModConfig config = ConfigManager.getConfig();
            if (!config.enableDeathLocation || deathPos == null) {
                return;
            }

            if (!client.world.getRegistryKey().equals(deathDimension)) {
                return;
            }

            render(drawContext, client);
        });
    }



    private void clear() {
        deathPos = null;
        deathDimension = null;
        lifetimeTicks = 0;
    }

    private void render(DrawContext context, MinecraftClient client) {
        Vec3d playerPos = client.player.getPos();
        double dx = deathPos.x - playerPos.x;
        double dz = deathPos.z - playerPos.z;
        double dy = deathPos.y - playerPos.y;
        double distance = Math.sqrt(dx * dx + dz * dz + dy * dy);

        String direction = cardinalDirection(dx, dz);
        String text = String.format("\u2620 %.0fm %s", distance, direction);

        int screenWidth = client.getWindow().getScaledWidth();
        int panelW = 90;
        int panelH = 16;
        int x = screenWidth - panelW - 10;
        int y = 10 + panelH + 6;

        HudPanel.draw(context, x, y, panelW, panelH, 0x88000000, 0x22FFFFFF);
        context.drawTextWithShadow(client.textRenderer, text, x + 6, y + 4, 0xCCCCCC);
    }

    private String cardinalDirection(double dx, double dz) {
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        if (angle < 0) angle += 360.0;

        String[] labels = {"N", "NE", "E", "SE", "S", "SW", "W", "NW"};
        int index = (int) Math.round(angle / 45.0) % 8;
        return labels[index];
    }
}
