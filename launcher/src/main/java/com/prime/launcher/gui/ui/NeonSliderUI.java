package com.prime.launcher.gui.ui;

import javax.swing.JSlider;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Flat "neon" look for a horizontal {@link JSlider}: a thin rounded track with
 * the filled portion in the accent color and a round accent thumb with a soft
 * halo. Only horizontal sliders are targeted (the RAM selector).
 */
public class NeonSliderUI extends BasicSliderUI {
    private static final int TRACK_H = 4;
    private static final int THUMB_D = 14;

    public NeonSliderUI(JSlider slider) {
        super(slider);
    }

    @Override
    protected Dimension getThumbSize() {
        return new Dimension(THUMB_D, THUMB_D);
    }

    @Override
    public void paintFocus(Graphics g) {
        // No focus rectangle — keep it clean.
    }

    @Override
    public void paintTrack(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int cy = trackRect.y + trackRect.height / 2 - TRACK_H / 2;
            // Empty track.
            g2.setColor(Theme.PANEL_LIGHT);
            g2.fill(new RoundRectangle2D.Float(trackRect.x, cy, trackRect.width, TRACK_H, TRACK_H, TRACK_H));

            // Filled portion up to the thumb.
            int thumbCenter = thumbRect.x + thumbRect.width / 2;
            int fillW = Math.max(0, thumbCenter - trackRect.x);
            g2.setColor(Theme.ACCENT);
            g2.fill(new RoundRectangle2D.Float(trackRect.x, cy, fillW, TRACK_H, TRACK_H, TRACK_H));
        } finally {
            g2.dispose();
        }
    }

    @Override
    public void paintThumb(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Rectangle t = thumbRect;
            int cx = t.x + t.width / 2;
            int cy = t.y + t.height / 2;

            // Halo.
            g2.setColor(Theme.withAlpha(Theme.ACCENT, 60));
            g2.fillOval(cx - THUMB_D / 2 - 3, cy - THUMB_D / 2 - 3, THUMB_D + 6, THUMB_D + 6);

            // Core.
            g2.setColor(Theme.ACCENT);
            g2.fillOval(cx - THUMB_D / 2, cy - THUMB_D / 2, THUMB_D, THUMB_D);
            g2.setColor(Color.WHITE);
            g2.fillOval(cx - 2, cy - 2, 4, 4);
        } finally {
            g2.dispose();
        }
    }
}
