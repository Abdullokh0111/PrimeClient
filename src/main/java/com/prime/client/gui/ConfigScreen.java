package com.prime.client.gui;

import com.prime.client.config.ConfigManager;
import com.prime.client.config.ModConfig;
import com.prime.client.modules.AutoSwap;
import com.prime.client.modules.NickChanger;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class ConfigScreen extends Screen {
    private final Screen parent;
    private int currentTab = 0; // 0 = Features, 1 = AutoSwap, 2 = Misc
    
    private final List<TextFieldWidget> textFieldsA = new ArrayList<>();
    private final List<TextFieldWidget> textFieldsB = new ArrayList<>();
    private TextFieldWidget itemWatchField;
    private TextFieldWidget nickField;

    public ConfigScreen(Screen parent) {
        super(Text.literal("Prime Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        this.textFieldsA.clear();
        this.textFieldsB.clear();
        
        int center = this.width / 2;
        int topY = 40;
        
        // Tabs
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Features"), b -> switchTab(0))
                .dimensions(center - 155, 10, 100, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("AutoSwap"), b -> switchTab(1))
                .dimensions(center - 50, 10, 100, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Misc & Profiles"), b -> switchTab(2))
                .dimensions(center + 55, 10, 100, 20).build());

        ModConfig config = ConfigManager.getConfig();

        if (currentTab == 0) {
            // Features Tab (3 columns)
            int col1 = center - 170;
            int col2 = center - 25;
            int col3 = center + 120;
            int bw = 135;
            
            addToggleButton(col1, topY, bw, "Armor HUD", config.enableArmorHUD, val -> config.enableArmorHUD = val);
            addToggleButton(col1, topY + 25, bw, "Potion HUD", config.enableEffectsHUD, val -> config.enableEffectsHUD = val);
            addToggleButton(col1, topY + 50, bw, "Armor Alerts", config.enableArmorWarning, val -> config.enableArmorWarning = val);
            addToggleButton(col1, topY + 75, bw, "Tab HP", config.enableTabListHealth, val -> config.enableTabListHealth = val);
            
            addToggleButton(col2, topY, bw, "Item Notify", config.enableItemNotifier, val -> config.enableItemNotifier = val);
            addToggleButton(col2, topY + 25, bw, "Death Loc", config.enableDeathLocation, val -> config.enableDeathLocation = val);
            addToggleButton(col2, topY + 50, bw, "Auto Sprint", config.enableAutoSprint, val -> config.enableAutoSprint = val);
            addToggleButton(col2, topY + 75, bw, "Fullbright", config.enableFullbright, val -> config.enableFullbright = val);

            addToggleButton(col3, topY, bw, "No Fog", config.enableNoFog, val -> config.enableNoFog = val);
            addToggleButton(col3, topY + 25, bw, "Freecam [G]", config.enableFreecam, val -> config.enableFreecam = val);
            addToggleButton(col3, topY + 50, bw, "Hitbox ESP", config.enableHitboxESP, val -> config.enableHitboxESP = val);

            // Item watch field
            this.itemWatchField = new TextFieldWidget(this.textRenderer, center - 170, topY + 120, 425, 18, Text.literal(""));
            this.itemWatchField.setMaxLength(256);
            this.itemWatchField.setText(config.itemWatchList);
            this.addDrawableChild(itemWatchField);
            
        } else if (currentTab == 1) {
            // AutoSwap Tab
            int rowHeight = config.swaps.size() > 6 ? 16 : 24;
            int startY = topY + 10;
            
            for (int i = 0; i < config.swaps.size(); i++) {
                ModConfig.SwapSlot slot = config.swaps.get(i);
                int y = startY + (i * rowHeight);

                TextFieldWidget tfA = new TextFieldWidget(this.textRenderer, center - 120, y, 100, 18, Text.literal(""));
                tfA.setMaxLength(64); tfA.setText(slot.queryA);
                this.textFieldsA.add(tfA); this.addDrawableChild(tfA);

                TextFieldWidget tfB = new TextFieldWidget(this.textRenderer, center + 20, y, 100, 18, Text.literal(""));
                tfB.setMaxLength(64); tfB.setText(slot.queryB);
                this.textFieldsB.add(tfB); this.addDrawableChild(tfB);

                int slotIndex = i;
                ButtonWidget removeBtn = ButtonWidget.builder(Text.literal("x"), b -> removeSlot(slotIndex))
                        .dimensions(center + 128, y, 16, 18).build();
                removeBtn.active = config.swaps.size() > 1;
                this.addDrawableChild(removeBtn);
            }
            
            ButtonWidget addBtn = ButtonWidget.builder(
                    Text.literal(config.swaps.size() >= AutoSwap.MAX_SLOTS ? "Max slots reached" : "+ Add Slot"),
                    b -> addSlot()
            ).dimensions(center - 60, startY + (config.swaps.size() * rowHeight) + 10, 120, 18).build();
            addBtn.active = config.swaps.size() < AutoSwap.MAX_SLOTS;
            this.addDrawableChild(addBtn);
            
        } else if (currentTab == 2) {
            // Misc Tab (Profiles & Nickname)
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Profile: PvP"), b -> applyProfile(config, "pvp"))
                    .dimensions(center - 150, topY, 90, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Profile: Farm"), b -> applyProfile(config, "farm"))
                    .dimensions(center - 45, topY, 90, 20).build());
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Profile: Quest"), b -> applyProfile(config, "quest"))
                    .dimensions(center + 60, topY, 90, 20).build());
                    
            this.nickField = new TextFieldWidget(this.textRenderer, center - 150, topY + 60, 180, 18, Text.literal(""));
            this.nickField.setMaxLength(16);
            this.nickField.setText(this.client.getSession().getUsername());
            this.addDrawableChild(nickField);

            this.addDrawableChild(ButtonWidget.builder(Text.literal("Change Nick"), b -> onChangeNickClicked())
                    .dimensions(center + 40, topY + 59, 110, 20).build());
        }

        // Save & Close button (always visible)
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Save & Close"), b -> saveAndClose())
                .dimensions(center - 75, this.height - 30, 150, 20).build());
    }
    
    private void addToggleButton(int x, int y, int w, String label, boolean state, java.util.function.Consumer<Boolean> action) {
        String text = label + ": " + (state ? "ON" : "OFF");
        ButtonWidget btn = ButtonWidget.builder(Text.literal(text), button -> {
            boolean newState = !button.getMessage().getString().endsWith("ON");
            action.accept(newState);
            button.setMessage(Text.literal(label + ": " + (newState ? "ON" : "OFF")));
        }).dimensions(x, y, w, 20).build();
        this.addDrawableChild(btn);
    }

    private void switchTab(int tab) {
        commitTextFields();
        this.currentTab = tab;
        this.clearAndInit();
    }

    private void applyProfile(ModConfig config, String profile) {
        switch (profile) {
            case "pvp" -> {
                config.enableArmorHUD = true; config.enableEffectsHUD = true;
                config.enableArmorWarning = true; config.enableTabListHealth = true;
                config.enableDeathLocation = false; config.enableItemNotifier = false;
                config.enableAutoSprint = true; config.enableFullbright = true;
            }
            case "farm" -> {
                config.enableArmorHUD = false; config.enableEffectsHUD = false;
                config.enableArmorWarning = false; config.enableTabListHealth = false;
                config.enableDeathLocation = true; config.enableItemNotifier = true;
                config.enableAutoSprint = true; config.enableFullbright = true;
            }
            case "quest" -> {
                config.enableArmorHUD = true; config.enableEffectsHUD = true;
                config.enableArmorWarning = true; config.enableTabListHealth = true;
                config.enableDeathLocation = true; config.enableItemNotifier = true;
                config.enableAutoSprint = false; config.enableFullbright = false;
            }
        }
        commitTextFields();
        this.clearAndInit();
    }

    private void onChangeNickClicked() {
        if (nickField != null) NickChanger.changeNickname(nickField.getText().trim());
    }

    private void addSlot() {
        ModConfig config = ConfigManager.getConfig();
        if (config.swaps.size() >= AutoSwap.MAX_SLOTS) return;
        commitTextFields();
        config.swaps.add(new ModConfig.SwapSlot("Slot " + (config.swaps.size() + 1), "", ""));
        this.clearAndInit();
    }

    private void removeSlot(int index) {
        ModConfig config = ConfigManager.getConfig();
        if (config.swaps.size() <= 1 || index < 0 || index >= config.swaps.size()) return;
        commitTextFields();
        config.swaps.remove(index);
        this.clearAndInit();
    }

    private void commitTextFields() {
        ModConfig config = ConfigManager.getConfig();
        if (currentTab == 1) {
            for (int i = 0; i < config.swaps.size() && i < textFieldsA.size() && i < textFieldsB.size(); i++) {
                config.swaps.get(i).queryA = this.textFieldsA.get(i).getText();
                config.swaps.get(i).queryB = this.textFieldsB.get(i).getText();
            }
        } else if (currentTab == 0 && itemWatchField != null) {
            config.itemWatchList = itemWatchField.getText();
        }
    }

    private void saveAndClose() {
        commitTextFields();
        ConfigManager.save();
        this.client.setScreen(this.parent);
    }

    @Override
    public void close() {
        this.saveAndClose();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        
        int center = this.width / 2;
        
        // Draw tab background
        HudPanel.draw(context, center - 160, 5, 320, 30, 0xAA000000, 0x44FFFFFF);
        
        // Draw main area background
        HudPanel.draw(context, center - 160, 35, 320, this.height - 75, 0x88000000, 0x22FFFFFF);

        super.render(context, mouseX, mouseY, delta);

        if (currentTab == 0) {
            if (itemWatchField != null) {
                context.drawTextWithShadow(this.textRenderer, "Item Watch List (comma-separated, пустой = все):", center - 150, itemWatchField.getY() - 12, 0xAAAAAA);
            }
        } else if (currentTab == 1) {
            ModConfig config = ConfigManager.getConfig();
            int rowHeight = config.swaps.size() > 6 ? 16 : 24;
            int startY = 50;
            for (int i = 0; i < config.swaps.size(); i++) {
                int y = startY + (i * rowHeight);
                String conflict = AutoSwap.getConflict(i);
                String keyLabel = "Key " + AutoSwap.getKeyName(i) + ":";
                context.drawTextWithShadow(this.textRenderer, keyLabel, center - 170, y + 5, conflict != null ? 0xFF5555 : 0xCCCCCC);
                context.drawCenteredTextWithShadow(this.textRenderer, "<->", center, y + 5, 0x888888);
            }
        } else if (currentTab == 2) {
            if (nickField != null) {
                context.drawTextWithShadow(this.textRenderer, "Set Nickname (disconnects to apply):", center - 150, nickField.getY() - 12, 0xAAAAAA);
            }
        }
    }
}
