package com.prime.launcher.gui.ui;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;

/**
 * Dark, rounded styling for a {@link JComboBox}: filled field background,
 * accent border on focus, a minimal chevron arrow, and an accent-highlighted
 * dropdown list. Apply with {@link #apply(JComboBox)}.
 */
public class NeonComboBoxUI extends BasicComboBoxUI {
    private static final int ARC = 10;

    /** Installs this UI plus the matching renderer/colors on the given combo. */
    public static void apply(JComboBox<?> combo) {
        combo.setUI(new NeonComboBoxUI());
        combo.setFont(Theme.INPUT);
        combo.setForeground(Theme.FG);
        combo.setBackground(Theme.FIELD_BG);
        // Non-opaque so the default square background fill doesn't square off
        // the rounded corners we paint in paintCurrentValueBackground.
        combo.setOpaque(false);
        combo.setBorder(new EmptyBorder(0, 6, 0, 0));
        combo.setFocusable(true);
        combo.setRenderer(new CellRenderer());
    }

    @Override
    protected JButton createArrowButton() {
        JButton button = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                try {
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int cx = getWidth() / 2;
                    int cy = getHeight() / 2;
                    g2.setColor(Theme.ACCENT);
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2.drawLine(cx - 4, cy - 2, cx, cy + 2);
                    g2.drawLine(cx + 4, cy - 2, cx, cy + 2);
                } finally {
                    g2.dispose();
                }
            }
        };
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setOpaque(false);
        return button;
    }

    @Override
    public void paintCurrentValueBackground(Graphics g, java.awt.Rectangle bounds, boolean hasFocus) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = comboBox.getWidth();
            int h = comboBox.getHeight();
            g2.setColor(Theme.FIELD_BG);
            g2.fill(new RoundRectangle2D.Float(0, 0, w, h, ARC, ARC));
            g2.setColor(comboBox.isFocusOwner() ? Theme.ACCENT : Theme.BORDER);
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f, ARC, ARC));
        } finally {
            g2.dispose();
        }
    }

    /** Dropdown row renderer with accent selection highlight. */
    private static final class CellRenderer implements ListCellRenderer<Object> {
        private final javax.swing.DefaultListCellRenderer delegate = new javax.swing.DefaultListCellRenderer();

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            javax.swing.JLabel label = (javax.swing.JLabel) delegate.getListCellRendererComponent(
                    list, value, index, isSelected, cellHasFocus);
            label.setFont(Theme.INPUT);
            label.setBorder(new EmptyBorder(6, 10, 6, 10));

            if (index == -1) {
                // Rendering the combo's own display area — stay transparent so
                // the rounded field background painted by the UI shows through.
                label.setOpaque(false);
                label.setForeground(Theme.FG);
            } else if (isSelected) {
                label.setOpaque(true);
                label.setBackground(Theme.ACCENT_DIM);
                label.setForeground(Color.WHITE);
            } else {
                label.setOpaque(true);
                label.setBackground(Theme.PANEL_LIGHT);
                label.setForeground(Theme.FG);
            }
            return label;
        }
    }
}
