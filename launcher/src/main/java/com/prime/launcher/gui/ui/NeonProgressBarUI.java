package com.prime.launcher.gui.ui;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.plaf.basic.BasicProgressBarUI;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Rounded, gradient-filled progress bar. Draws a dark rounded track and fills
 * the completed fraction with a left-to-right accent gradient. Indeterminate
 * mode falls back to a pulsing full-width accent fill.
 */
public class NeonProgressBarUI extends BasicProgressBarUI {

    @Override
    protected void paintDeterminate(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            JProgressBar bar = (JProgressBar) c;
            int w = bar.getWidth();
            int h = bar.getHeight();
            int arc = h;

            // Track.
            g2.setColor(Theme.PANEL_LIGHT);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));

            // Fill.
            double fraction = bar.getPercentComplete();
            int fillW = (int) Math.round(fraction * w);
            if (fillW > 0) {
                g2.setPaint(new GradientPaint(0, 0, Theme.ACCENT_DIM, fillW, 0, Theme.ACCENT));
                g2.fill(new RoundRectangle2D.Float(0, 0, fillW, h, arc, arc));
            }
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintIndeterminate(Graphics g, JComponent c) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = c.getWidth();
            int h = c.getHeight();
            int arc = h;
            g2.setColor(Theme.PANEL_LIGHT);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));
            g2.setColor(Theme.withAlpha(Theme.ACCENT, 160));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));
        } finally {
            g2.dispose();
        }
    }
}
