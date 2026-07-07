package com.prime.client.gui;

import com.prime.client.config.ConfigManager;
import com.prime.client.config.ModConfig;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private final List<TextFieldWidget> textFieldsA = new ArrayList<>();
    private final List<TextFieldWidget> textFieldsB = new ArrayList<>();

    public ConfigScreen(Screen parent) {
        super(Text.literal("Prime Settings Menu"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.textFieldsA.clear();
        this.textFieldsB.clear();

        int startY = this.height / 2 - 95;
        int center = this.width / 2;

        ModConfig config = ConfigManager.getConfig();

        // 5 Custom A/B Swap Slots inputs
        for (int i = 0; i < config.swaps.size(); i++) {
            ModConfig.SwapSlot slot = config.swaps.get(i);
            
            // TextField A (Left Column)
            TextFieldWidget tfA = new TextFieldWidget(
                    this.textRenderer,
                    center - 120, startY + (i * 26), 100, 18,
                    Text.literal(slot.name + " A")
            );
            tfA.setMaxLength(64);
            tfA.setText(slot.queryA);
            this.textFieldsA.add(tfA);
            this.addDrawableChild(tfA);

            // TextField B (Right Column)
            TextFieldWidget tfB = new TextFieldWidget(
                    this.textRenderer,
                    center + 20, startY + (i * 26), 100, 18,
                    Text.literal(slot.name + " B")
            );
            tfB.setMaxLength(64);
            tfB.setText(slot.queryB);
            this.textFieldsB.add(tfB);
            this.addDrawableChild(tfB);
        }

        // Toggle buttons for HUDs and Alerts
        int toggleY = startY + (config.swaps.size() * 26) + 10;

        // 1. Armor HUD Toggle
        ButtonWidget armorBtn = ButtonWidget.builder(
                Text.literal("Armor HUD: " + (config.enableArmorHUD ? "ON" : "OFF")),
                button -> {
                    config.enableArmorHUD = !config.enableArmorHUD;
                    button.setMessage(Text.literal("Armor HUD: " + (config.enableArmorHUD ? "ON" : "OFF")));
                }
        ).dimensions(center - 135, toggleY, 85, 20).build();
        this.addDrawableChild(armorBtn);

        // 2. Effects HUD Toggle
        ButtonWidget effectsBtn = ButtonWidget.builder(
                Text.literal("Potion HUD: " + (config.enableEffectsHUD ? "ON" : "OFF")),
                button -> {
                    config.enableEffectsHUD = !config.enableEffectsHUD;
                    button.setMessage(Text.literal("Potion HUD: " + (config.enableEffectsHUD ? "ON" : "OFF")));
                }
        ).dimensions(center - 40, toggleY, 80, 20).build();
        this.addDrawableChild(effectsBtn);

        // 3. Alerts Toggle
        ButtonWidget alertsBtn = ButtonWidget.builder(
                Text.literal("Alerts: " + (config.enableArmorWarning ? "ON" : "OFF")),
                button -> {
                    config.enableArmorWarning = !config.enableArmorWarning;
                    button.setMessage(Text.literal("Alerts: " + (config.enableArmorWarning ? "ON" : "OFF")));
                }
        ).dimensions(center + 50, toggleY, 85, 20).build();
        this.addDrawableChild(alertsBtn);

        // 4. Save & Close Button
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save & Close"), button -> {
            this.saveAndClose();
        }).dimensions(center - 60, toggleY + 25, 120, 20).build());
    }

    private void saveAndClose() {
        ModConfig config = ConfigManager.getConfig();
        for (int i = 0; i < config.swaps.size(); i++) {
            if (i < this.textFieldsA.size() && i < this.textFieldsB.size()) {
                config.swaps.get(i).queryA = this.textFieldsA.get(i).getText();
                config.swaps.get(i).queryB = this.textFieldsB.get(i).getText();
            }
        }
        ConfigManager.save();
        this.client.setScreen(this.parent);
    }

    @Override
    public void close() {
        this.saveAndClose();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Render screen background
        this.renderBackground(context);
        
        super.render(context, mouseX, mouseY, delta);

        int startY = this.height / 2 - 95;
        int center = this.width / 2;

        // Draw Title
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, center, startY - 22, 0xFFFFFF);

        ModConfig config = ConfigManager.getConfig();
        for (int i = 0; i < config.swaps.size(); i++) {
            ModConfig.SwapSlot slot = config.swaps.get(i);
            
            // Draw Keybind Category Label (e.g. "Key G:")
            String keyLabel = slot.name.substring(slot.name.indexOf("(") + 1, slot.name.indexOf(")"));
            context.drawTextWithShadow(
                    this.textRenderer,
                    "Key " + keyLabel + ":",
                    center - 170, startY + (i * 26) + 5,
                    0xCCCCCC
            );

            // Draw Swap Symbol <-> in the middle of columns
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    "<->",
                    center, startY + (i * 26) + 5,
                    0x888888
            );
        }
    }
}
