package com.prime.launcher.gui.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;

/**
 * Single source of truth for the launcher's visual style.
 *
 * Every color/font/metric lives here so the whole UI can be re-skinned from
 * one place instead of hunting for hard-coded {@code new Color(...)} calls
 * scattered across the widgets. Values lean on the "gamer client" palette
 * (deep near-black background, single bright accent) used by clients like
 * Nursultan / Pulse.
 */
public final class Theme {
    private Theme() {
    }

    // --- Window geometry -------------------------------------------------
    public static final int WINDOW_W = 900;
    public static final int WINDOW_H = 560;
    public static final int TITLE_BAR_H = 40;
    public static final int SIDEBAR_W = 320;
    public static final int CORNER_ARC = 18;

    // --- Core palette ----------------------------------------------------
    public static final Color BG_TOP = new Color(0x0B, 0x0D, 0x14);
    public static final Color BG_BOTTOM = new Color(0x05, 0x06, 0x0A);
    public static final Color PANEL = new Color(0x11, 0x14, 0x1D);
    public static final Color PANEL_LIGHT = new Color(0x18, 0x1C, 0x28);
    public static final Color FIELD_BG = new Color(0x14, 0x17, 0x22);
    public static final Color BORDER = new Color(0x26, 0x2B, 0x3A);

    public static final Color FG = new Color(0xE6, 0xEB, 0xF5);
    public static final Color MUTED = new Color(0x6B, 0x72, 0x87);

    // Bright cyan accent + its darker hover/pressed shades.
    public static final Color ACCENT = new Color(0x00, 0xE5, 0xFF);
    public static final Color ACCENT_DIM = new Color(0x00, 0xA8, 0xC8);
    public static final Color ACCENT_SOFT = new Color(0x00, 0xE5, 0xFF, 0x33);

    public static final Color DANGER = new Color(0xFF, 0x5C, 0x5C);
    public static final Color SUCCESS = new Color(0x4C, 0xD9, 0x7B);

    // --- Fonts -----------------------------------------------------------
    // Prefer a clean UI font that ships with Windows; fall back gracefully so
    // the launcher still renders on machines where it is missing.
    private static final String UI_FAMILY = firstAvailable("Segoe UI", "Inter", "Helvetica Neue", "Arial");
    private static final String DISPLAY_FAMILY = firstAvailable("Bahnschrift", "Segoe UI Semibold", "Segoe UI", "Arial");

    public static final Font LOGO = new Font(DISPLAY_FAMILY, Font.BOLD, 30);
    public static final Font TITLE = new Font(DISPLAY_FAMILY, Font.BOLD, 20);
    public static final Font SUBTITLE = new Font(UI_FAMILY, Font.PLAIN, 13);
    public static final Font LABEL = new Font(UI_FAMILY, Font.PLAIN, 13);
    public static final Font INPUT = new Font(UI_FAMILY, Font.PLAIN, 14);
    public static final Font BUTTON = new Font(DISPLAY_FAMILY, Font.BOLD, 15);
    public static final Font SMALL = new Font(UI_FAMILY, Font.PLAIN, 11);

    /**
     * Returns the first font family that is actually installed, or the last
     * candidate as a last resort (AWT will substitute a default if even that
     * is missing).
     */
    private static String firstAvailable(String... candidates) {
        String[] installed = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        for (String candidate : candidates) {
            for (String name : installed) {
                if (name.equalsIgnoreCase(candidate)) {
                    return candidate;
                }
            }
        }
        return candidates[candidates.length - 1];
    }

    /** Linearly interpolates between two colors ({@code t} clamped to [0,1]). */
    public static Color lerp(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                Math.round(a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t)
        );
    }

    /** Same color with a different alpha (0-255). */
    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}
