package com.prime.client.modules;

import com.prime.client.api.Module;
import com.prime.client.config.ConfigManager;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Freecam module: detaches the camera from the player and allows free flight.
 * The real player body stays in place (server-side) while the camera moves
 * independently. Toggle on/off with a keybind (default: G).
 */
public class Freecam implements Module {
    private static final Logger LOGGER = LoggerFactory.getLogger("prime-freecam");

    private static KeyBinding toggleKey;

    // Freecam state
    private static boolean active = false;
    private static double camX, camY, camZ;
    private static float camYaw, camPitch;
    private static final double SPEED = 0.5;

    public static boolean isActive() {
        return active;
    }

    public static double getCamX() { return camX; }
    public static double getCamY() { return camY; }
    public static double getCamZ() { return camZ; }
    public static float getCamYaw() { return camYaw; }
    public static float getCamPitch() { return camPitch; }

    @Override
    public String getName() {
        return "Freecam";
    }

    @Override
    public void init() {
        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.prime.freecam",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category.prime"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) {
                if (active) deactivate();
                return;
            }

            if (!ConfigManager.getConfig().enableFreecam) {
                if (active) deactivate();
                return;
            }

            // Toggle
            while (toggleKey.wasPressed()) {
                if (active) {
                    deactivate();
                } else {
                    activate(client);
                }
            }

            if (!active) return;

            // Movement based on look direction and pressed keys
            float yawRad = (float) Math.toRadians(camYaw);
            float pitchRad = (float) Math.toRadians(camPitch);

            double forwardX = -Math.sin(yawRad) * Math.cos(pitchRad);
            double forwardY = -Math.sin(pitchRad);
            double forwardZ = Math.cos(yawRad) * Math.cos(pitchRad);

            double strafeX = Math.cos(yawRad);
            double strafeZ = Math.sin(yawRad);

            double dx = 0, dy = 0, dz = 0;

            if (client.options.forwardKey.isPressed()) {
                dx += forwardX; dy += forwardY; dz += forwardZ;
            }
            if (client.options.backKey.isPressed()) {
                dx -= forwardX; dy -= forwardY; dz -= forwardZ;
            }
            if (client.options.leftKey.isPressed()) {
                dx += strafeX; dz += strafeZ;
            }
            if (client.options.rightKey.isPressed()) {
                dx -= strafeX; dz -= strafeZ;
            }
            if (client.options.jumpKey.isPressed()) {
                dy += 1;
            }
            if (client.options.sneakKey.isPressed()) {
                dy -= 1;
            }

            // Normalize and apply speed
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > 0) {
                double speed = client.options.sprintKey.isPressed() ? SPEED * 2.5 : SPEED;
                dx = dx / len * speed;
                dy = dy / len * speed;
                dz = dz / len * speed;
                camX += dx;
                camY += dy;
                camZ += dz;
            }

            // Update yaw/pitch from mouse (player's current rotation values, which
            // still update even with our mixin since input handling runs before tick)
            camYaw = client.player.getYaw();
            camPitch = client.player.getPitch();
        });
    }

    private void activate(MinecraftClient client) {
        active = true;
        camX = client.player.getX();
        camY = client.player.getEyeY();
        camZ = client.player.getZ();
        camYaw = client.player.getYaw();
        camPitch = client.player.getPitch();
        LOGGER.info("Freecam activated at [{}, {}, {}]", camX, camY, camZ);
    }

    private static void deactivate() {
        active = false;
        LOGGER.info("Freecam deactivated");
    }
}
