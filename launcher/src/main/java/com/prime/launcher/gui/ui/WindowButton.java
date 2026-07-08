package com.prime.launcher.gui.ui;

import javax.swing.JButton;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A borderless title-bar button that paints a single glyph (minimize dash or
 * close cross) and highlights on hover. Close buttons highlight red; others use
 * the accent color.
 */
public class WindowButton extends JButton {

    public enum Glyph { MINIMIZE, CLOSE }

    private final Glyph glyph;
    private boolean hover = false;

    public WindowButton(Glyph glyph) {
        this.glyph = glyph;
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hover = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hover = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            if (hover) {
                Color bg = glyph == Glyph.CLOSE
                        ? Theme.withAlpha(Theme.DANGER, 60)
                        : Theme.withAlpha(Theme.ACCENT, 45);
                g2.setColor(bg);
                g2.fillRoundRect(2, 2, w - 4, h - 4, 8, 8);
            }

            Color fg = hover
                    ? (glyph == Glyph.CLOSE ? Theme.DANGER : Theme.ACCENT)
                    : Theme.MUTED;
            g2.setColor(fg);
            g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

            int cx = w / 2;
            int cy = h / 2;
            int r = 5;
            if (glyph == Glyph.MINIMIZE) {
                g2.drawLine(cx - r, cy, cx + r, cy);
            } else {
                g2.drawLine(cx - r, cy - r, cx + r, cy + r);
                g2.drawLine(cx - r, cy + r, cx + r, cy - r);
            }
        } finally {
            g2.dispose();
        }
    }
}
