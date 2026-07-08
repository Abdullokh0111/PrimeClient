package com.prime.client.modules;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.prime.client.api.Module;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.util.Session;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public class NickChanger implements Module {
    private static final Logger LOGGER = LoggerFactory.getLogger("prime-nick");
    private static final Pattern VALID_NAME = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    private static Field sessionFieldOnClient;

    @Override
    public String getName() {
        return "NickChanger";
    }

    @Override
    public void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(literal("nick")
                        .then(argument("username", StringArgumentType.word())
                                .executes(ctx -> {
                                    changeNickname(StringArgumentType.getString(ctx, "username"));
                                    return 1;
                                }))));
    }

    public static boolean changeNickname(String newName) {
        MinecraftClient client = MinecraftClient.getInstance();

        if (!VALID_NAME.matcher(newName).matches()) {
            feedback(client, "Invalid nickname", "Use 3-16 letters, numbers or underscore.", true);
            return false;
        }

        Session current = client.getSession();
        if (newName.equals(current.getUsername())) {
            feedback(client, "No change", "You're already using that name.", false);
            return false;
        }

        UUID offlineUuid = UUID.nameUUIDFromBytes(
                ("OfflinePlayer:" + newName).getBytes(StandardCharsets.UTF_8));

        try {
            applyNewIdentity(client, current, newName, offlineUuid);
        } catch (ReflectiveOperationException e) {
            LOGGER.error("Failed to swap session for nickname change", e);
            feedback(client, "Nickname change failed", "Internal error, see log.", true);
            return false;
        }

        boolean wasInGame = client.world != null;
        if (wasInGame) {
            LOGGER.info("Disconnecting to apply new nickname '{}'...", newName);
            client.world.disconnect();
            client.disconnect();
            client.setScreen(new net.minecraft.client.gui.screen.TitleScreen());
        }

        feedback(client, "Nickname changed",
                wasInGame ? "Now '" + newName + "'. Reconnect to use it." : "Now '" + newName + "'.",
                false);
        return true;
    }

    private static void applyNewIdentity(MinecraftClient client, Session oldSession, String newName, UUID newUuid)
            throws ReflectiveOperationException {
        ensureReflectionCached();
        Session newSession = new Session(
                newName,
                newUuid.toString(),
                oldSession.getAccessToken(),
                Optional.empty(),
                Optional.empty(),
                Session.AccountType.LEGACY
        );
        sessionFieldOnClient.set(client, newSession);
    }

    private static void ensureReflectionCached() throws ReflectiveOperationException {
        if (sessionFieldOnClient == null) {
            Field found = null;
            for (Field f : MinecraftClient.class.getDeclaredFields()) {
                if (f.getType() == Session.class) {
                    found = f;
                    break;
                }
            }
            if (found == null) {
                throw new NoSuchFieldException("No Session-typed field found on MinecraftClient");
            }
            found.setAccessible(true);
            sessionFieldOnClient = found;
        }
    }

    private static void feedback(MinecraftClient client, String title, String message, boolean error) {
        Formatting color = error ? Formatting.RED : Formatting.GREEN;
        if (client.player != null) {
            client.player.sendMessage(Text.literal("[Prime] " + title + ": " + message).formatted(color), false);
        }
        LOGGER.info("[Prime] {}: {}", title, message);
        client.execute(() -> SystemToast.add(
                client.getToastManager(),
                SystemToast.Type.PERIODIC_NOTIFICATION,
                Text.literal(title).formatted(color),
                Text.literal(message)
        ));
    }
}
