package com.prime.client.gui;

import net.minecraft.client.gui.DrawContext;

/**
 * Shared drawing helper for the flat bordered panels used across the HUD
 * modules (EffectsHUD, TargetHUD). Previously each module reimplemented the
 * same four context.fill() calls for a 1px border; this is the one place to
 * change if the panel style needs to change later.
 */
public final class HudPanel {
    private HudPanel() {
    }

    /**
     * Draws a filled rectangle with a 1px border.
     *
     * @param x           left edge
     * @param y           top edge
     * @param width       panel width
     * @param height      panel height
     * @param bgColor     ARGB background fill color
     * @param borderColor ARGB border color
     */
    public static void draw(DrawContext context, int x, int y, int width, int height, int bgColor, int borderColor) {
        context.fill(x, y, x + width, y + height, bgColor);

        // Top / bottom
        context.fill(x, y, x + width, y + 1, borderColor);
        context.fill(x, y + height - 1, x + width, y + height, borderColor);
        // Left / right
        context.fill(x, y, x + 1, y + height, borderColor);
        context.fill(x + width - 1, y, x + width, y + height, borderColor);
    }

    /** Same as {@link #draw}, but also draws a 2px accent line along the top edge (used by TargetHUD). */
    public static void drawWithTopAccent(DrawContext context, int x, int y, int width, int height,
                                          int bgColor, int borderColor, int accentColor) {
        draw(context, x, y, width, height, bgColor, borderColor);
        context.fill(x, y, x + width, y + 2, accentColor);
    }
}
