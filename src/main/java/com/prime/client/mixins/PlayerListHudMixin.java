package com.prime.client.mixins;

import com.prime.client.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.PlayerListHud;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Appends a color-coded HP value to each entry in the Tab player list, mirroring
 * the ping/health info TargetHUD and the nametag mixin already show elsewhere -
 * so the same read is available whether you're looking at someone or just
 * holding Tab.
 */
@Mixin(PlayerListHud.class)
public abstract class PlayerListHudMixin {

    @Inject(method = "getPlayerName", at = @At("RETURN"), cancellable = true)
    private void onGetPlayerName(PlayerListEntry entry, CallbackInfoReturnable<Text> cir) {
        if (!ConfigManager.getConfig().enableTabListHealth) {
            return;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }

        PlayerEntity player = client.world.getPlayerByUuid(entry.getProfile().getId());
        if (player == null) {
            return; // out of render distance / not loaded - nothing to show
        }

        float health = player.getHealth();
        Formatting color = Formatting.GREEN;
        if (health <= 6.0F) {
            color = Formatting.RED;
        } else if (health <= 12.0F) {
            color = Formatting.YELLOW;
        }

        MutableText suffix = Text.literal(String.format(" %.0f\u2764", health)).formatted(color);
        cir.setReturnValue(cir.getReturnValue().copy().append(suffix));
    }
}
