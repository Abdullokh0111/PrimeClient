package com.prime.launcher.gui.ui;

import javax.swing.JButton;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

/**
 * A filled, rounded accent button that animates a soft glow on hover and a
 * quick "press-in" dim on click. The hover intensity is eased toward its
 * target every frame by a short {@link Timer}, so the transition is smooth
 * rather than an instant color swap.
 *
 * The button paints itself entirely; standard {@link JButton} chrome
 * (content-area fill, border, focus ring) is disabled in the constructor.
 */
public class GlowButton extends JButton {
    private static final int ARC = 12;
    private static final float EASE = 0.18f; // higher = snappier

    private float hover = 0f;      // current eased hover amount [0,1]
    private float hoverTarget = 0f;
    private boolean pressed = false;
    private final Timer animator;

    public GlowButton(String text) {
        super(text);
        setFont(Theme.BUTTON);
        setForeground(Theme.BG_BOTTOM);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));

        this.animator = new Timer(16, e -> {
            hover += (hoverTarget - hover) * EASE;
            if (Math.abs(hoverTarget - hover) < 0.01f) {
                hover = hoverTarget;
                if (hover == hoverTarget) {
                    ((Timer) e.getSource()).stop();
                }
            }
            repaint();
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hoverTarget = isEnabled() ? 1f : 0f;
                animator.start();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoverTarget = 0f;
                animator.start();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            Color base;
            if (!isEnabled()) {
                base = Theme.PANEL_LIGHT;
            } else if (pressed) {
                base = Theme.ACCENT_DIM;
            } else {
                base = Theme.lerp(Theme.ACCENT_DIM, Theme.ACCENT, hover);
            }

            // Outer glow halo grows with hover.
            if (isEnabled() && hover > 0.01f) {
                int glowAlpha = (int) (90 * hover);
                for (int i = 3; i >= 1; i--) {
                    g2.setColor(Theme.withAlpha(Theme.ACCENT, glowAlpha / (i + 1)));
                    g2.fill(new RoundRectangle2D.Float(-i, -i, w + 2f * i, h + 2f * i, ARC + i, ARC + i));
                }
            }

            g2.setColor(base);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, ARC, ARC));

            // Centered label.
            g2.setFont(getFont());
            g2.setColor(isEnabled() ? getForeground() : Theme.MUTED);
            java.awt.FontMetrics fm = g2.getFontMetrics();
            String text = getText();
            int tx = (w - fm.stringWidth(text)) / 2;
            int ty = (h - fm.getHeight()) / 2 + fm.getAscent();
            g2.drawString(text, tx, ty);
        } finally {
            g2.dispose();
        }
    }
}
