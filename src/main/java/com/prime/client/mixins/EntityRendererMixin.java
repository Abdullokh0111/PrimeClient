package com.prime.client.mixins;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> {

    @Shadow @Final protected EntityRenderDispatcher dispatcher;
    @Shadow public abstract TextRenderer getTextRenderer();

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void onRenderLabelIfPresent(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (!(entity instanceof PlayerEntity)) {
            return;
        }

        // Cancel default vanilla label rendering
        ci.cancel();

        PlayerEntity player = (PlayerEntity) entity;
        MinecraftClient client = MinecraftClient.getInstance();

        // 1. Calculate player latency (ping)
        int ping = 0;
        if (client.getNetworkHandler() != null) {
            PlayerListEntry entry = client.getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry != null) {
                ping = entry.getLatency();
            }
        }

        // Color-code ping
        Formatting pingColor = Formatting.GREEN;
        if (ping > 150) {
            pingColor = Formatting.RED;
        } else if (ping > 60) {
            pingColor = Formatting.YELLOW;
        }

        // 2. Build custom nametag text
        Text customText = Text.literal(player.getName().getString() + " ")
                .append(Text.literal("[" + ping + "ms]").formatted(pingColor));

        // 3. Render setup
        double dist = this.dispatcher.getSquaredDistanceToCamera(player);
        if (dist > 4096.0) {
            return;
        }

        boolean isDiscrete = !player.isSneaking();
        float labelHeight = player.getNameLabelHeight();

        matrices.push();
        matrices.translate(0.0F, labelHeight, 0.0F);
        matrices.multiply(this.dispatcher.getRotation());
        matrices.scale(-0.025F, -0.025F, 0.025F);
        Matrix4f posMatrix = matrices.peek().getPositionMatrix();

        float bgOpacity = client.options.getTextBackgroundOpacity(0.25F);
        int bgColor = (int) (bgOpacity * 255.0F) << 24;

        TextRenderer textRenderer = this.getTextRenderer();
        float textWidth = (float) textRenderer.getWidth(customText);
        float h = -textWidth / 2.0F;

        // Draw standard nameplate background box and text
        textRenderer.draw(
                customText,
                h, 0.0F,
                553648127,
                false,
                posMatrix,
                vertexConsumers,
                isDiscrete ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL,
                bgColor,
                light
        );

        if (isDiscrete) {
            textRenderer.draw(
                    customText,
                    h, 0.0F,
                    -1,
                    false,
                    posMatrix,
                    vertexConsumers,
                    TextRenderer.TextLayerType.NORMAL,
                    0,
                    light
            );
        }

        // 4. Render Armor & Hand Items above the nametag
        List<ItemStack> equipment = new ArrayList<>();
        ItemStack mainHand = player.getEquippedStack(EquipmentSlot.MAINHAND);
        ItemStack helmet = player.getEquippedStack(EquipmentSlot.HEAD);
        ItemStack chest = player.getEquippedStack(EquipmentSlot.CHEST);
        ItemStack legs = player.getEquippedStack(EquipmentSlot.LEGS);
        ItemStack boots = player.getEquippedStack(EquipmentSlot.FEET);
        ItemStack offHand = player.getEquippedStack(EquipmentSlot.OFFHAND);

        if (!mainHand.isEmpty()) equipment.add(mainHand);
        if (!helmet.isEmpty()) equipment.add(helmet);
        if (!chest.isEmpty()) equipment.add(chest);
        if (!legs.isEmpty()) equipment.add(legs);
        if (!boots.isEmpty()) equipment.add(boots);
        if (!offHand.isEmpty()) equipment.add(offHand);

        if (!equipment.isEmpty()) {
            int itemSize = 16;
            int spacer = 2;
            int totalWidth = (equipment.size() * itemSize) + ((equipment.size() - 1) * spacer);
            int startX = -totalWidth / 2;
            int itemY = -24; // Renders items 24 pixels above the name text

            for (int i = 0; i < equipment.size(); i++) {
                ItemStack stack = equipment.get(i);
                int currentX = startX + (i * (itemSize + spacer));

                // Draw Item Model using ItemRenderer
                matrices.push();
                matrices.translate(currentX + 8.0F, itemY + 8.0F, 0.0F); // Center of 16x16 slot
                matrices.scale(16.0F, -16.0F, 16.0F); // Minecraft items are usually 1x1x1 unit inside renderItem, we scale to match pixel dimensions
                
                client.getItemRenderer().renderItem(
                        stack,
                        ModelTransformationMode.FIXED,
                        light,
                        OverlayTexture.DEFAULT_UV,
                        matrices,
                        vertexConsumers,
                        player.getWorld(),
                        0
                );
                matrices.pop();

                // Draw Durability Value below the item if damageable
                if (stack.isDamageable()) {
                    int maxDamage = stack.getMaxDamage();
                    int currentDamage = stack.getDamage();
                    int remaining = maxDamage - currentDamage;
                    float pct = (float) remaining / maxDamage;

                    // Durability color transition
                    int durColor = 0xFF55FF55; // Green
                    if (pct < 0.15F) {
                        durColor = 0xFFFF5555; // Red
                    } else if (pct < 0.40F) {
                        durColor = 0xFFFFAA00; // Orange
                    } else if (pct < 0.70F) {
                        durColor = 0xFFFFFF55; // Yellow
                    }

                    String durText = String.valueOf(remaining);
                    float durW = textRenderer.getWidth(durText);

                    matrices.push();
                    matrices.translate(currentX + 8.0F, itemY + 18.0F, 0.0F); // Positioned directly below the item icon
                    matrices.scale(0.5F, 0.5F, 1.0F); // Render small text (50% scale) for a clean UI
                    
                    textRenderer.draw(
                            durText,
                            -durW / 2.0F, 0.0F,
                            durColor,
                            false,
                            matrices.peek().getPositionMatrix(),
                            vertexConsumers,
                            TextRenderer.TextLayerType.NORMAL,
                            0,
                            light
                    );
                    matrices.pop();
                }
            }
        }

        matrices.pop();
    }
}
