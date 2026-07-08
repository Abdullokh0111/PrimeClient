package com.prime.launcher.gui.ui;

import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * A translucent, rounded "card" panel used to group the form controls over the
 * animated background. Non-opaque so the particles show through faintly; paints
 * a soft fill plus a 1px accent-tinted border.
 */
public class RoundedPanel extends JPanel {
    private final int arc;

    public RoundedPanel(LayoutManager layout, int arc) {
        super(layout);
        this.arc = arc;
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            g2.setColor(Theme.withAlpha(Theme.PANEL, 225));
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, arc, arc));

            g2.setColor(Theme.withAlpha(Theme.BORDER, 200));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, arc, arc));
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
