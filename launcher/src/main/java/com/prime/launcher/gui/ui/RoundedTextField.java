package com.prime.launcher.gui.ui;

import javax.swing.JTextField;
import javax.swing.BorderFactory;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * A single-line text field with a rounded, filled background. Draws its own
 * backdrop (transparent component, custom fill) and highlights the border in
 * the accent color while focused.
 */
public class RoundedTextField extends JTextField {
    private static final int ARC = 10;

    public RoundedTextField(String text) {
        super(text);
        setOpaque(false);
        setForeground(Theme.FG);
        setCaretColor(Theme.ACCENT);
        setFont(Theme.INPUT);
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        setSelectionColor(Theme.ACCENT_DIM);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            g2.setColor(Theme.FIELD_BG);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, ARC, ARC));

            g2.setColor(isFocusOwner() ? Theme.ACCENT : Theme.BORDER);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, ARC, ARC));
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }
}
