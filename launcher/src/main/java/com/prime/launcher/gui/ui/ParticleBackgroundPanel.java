package com.prime.launcher.gui.ui;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.AlphaComposite;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 * The launcher's living backdrop: a vertical dark gradient with a drifting
 * "constellation" of particles that link up with thin lines when they come
 * close. This is the effect that gives cheat/gamer launchers their signature
 * animated look, done entirely in Java2D so it needs no external assets.
 *
 * The animation is driven by a single Swing {@link Timer} on the EDT (~60 fps).
 * A deterministic {@link Random} seed keeps particle motion reproducible and
 * avoids any dependency on wall-clock randomness.
 */
public class ParticleBackgroundPanel extends JPanel {
    private static final int PARTICLE_COUNT = 60;
    private static final double LINK_DISTANCE = 150.0;
    private static final double LINK_DISTANCE_SQ = LINK_DISTANCE * LINK_DISTANCE;
    private static final int FRAME_DELAY_MS = 16; // ~60 fps

    private final Particle[] particles = new Particle[PARTICLE_COUNT];
    private final Random random = new Random(0xC0FFEE);
    private final Timer timer;

    /** Optional sidebar artwork, blended into the left edge. May be null. */
    private BufferedImage sidebarImage;

    public ParticleBackgroundPanel() {
        setOpaque(true);
        // Particles are seeded lazily on first paint once we know our real size.
        this.timer = new Timer(FRAME_DELAY_MS, e -> {
            step();
            repaint();
        });
    }

    public void setSidebarImage(BufferedImage image) {
        this.sidebarImage = image;
    }

    /** Starts the animation. Safe to call once the panel is displayable. */
    public void startAnimation() {
        if (!timer.isRunning()) {
            timer.start();
        }
    }

    /** Stops the animation timer so a hidden window doesn't burn CPU. */
    public void stopAnimation() {
        timer.stop();
    }

    private boolean seeded = false;

    private void seed() {
        int w = Math.max(getWidth(), Theme.WINDOW_W);
        int h = Math.max(getHeight(), Theme.WINDOW_H);
        for (int i = 0; i < particles.length; i++) {
            particles[i] = new Particle(
                    random.nextDouble() * w,
                    random.nextDouble() * h,
                    (random.nextDouble() - 0.5) * 0.6,
                    (random.nextDouble() - 0.5) * 0.6,
                    1.2f + random.nextFloat() * 1.8f
            );
        }
        seeded = true;
    }

    private void step() {
        if (!seeded) {
            seed();
        }
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        for (Particle p : particles) {
            p.x += p.vx;
            p.y += p.vy;
            // Wrap around the edges so the field never empties out.
            if (p.x < 0) p.x += w;
            if (p.x > w) p.x -= w;
            if (p.y < 0) p.y += h;
            if (p.y > h) p.y -= h;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // 1. Base vertical gradient.
            g2.setPaint(new GradientPaint(0, 0, Theme.BG_TOP, 0, h, Theme.BG_BOTTOM));
            g2.fillRect(0, 0, w, h);

            // 2. Sidebar artwork on the left, faded into the background so the
            //    branding text stays readable.
            if (sidebarImage != null) {
                Graphics2D img = (Graphics2D) g2.create();
                img.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
                img.drawImage(sidebarImage, 0, 0, Theme.SIDEBAR_W, h, null);
                img.dispose();
                // Dark gradient sweeping right so particles/text pop over it.
                g2.setPaint(new GradientPaint(
                        0, 0, Theme.withAlpha(Theme.BG_BOTTOM, 120),
                        Theme.SIDEBAR_W, 0, Theme.withAlpha(Theme.BG_BOTTOM, 235)));
                g2.fillRect(0, 0, Theme.SIDEBAR_W, h);
            }

            // 3. Particle links (drawn first so nodes sit on top).
            if (seeded) {
                for (int i = 0; i < particles.length; i++) {
                    for (int j = i + 1; j < particles.length; j++) {
                        double dx = particles[i].x - particles[j].x;
                        double dy = particles[i].y - particles[j].y;
                        double distSq = dx * dx + dy * dy;
                        if (distSq < LINK_DISTANCE_SQ) {
                            double dist = Math.sqrt(distSq);
                            int alpha = (int) (60 * (1.0 - dist / LINK_DISTANCE));
                            if (alpha > 0) {
                                g2.setColor(Theme.withAlpha(Theme.ACCENT, alpha));
                                g2.drawLine((int) particles[i].x, (int) particles[i].y,
                                        (int) particles[j].x, (int) particles[j].y);
                            }
                        }
                    }
                }
                // 4. Particle nodes.
                for (Particle p : particles) {
                    int size = Math.round(p.size * 2);
                    g2.setColor(Theme.withAlpha(Theme.ACCENT, 150));
                    g2.fillOval((int) (p.x - p.size), (int) (p.y - p.size), size, size);
                }
            }

            // 5. Subtle rounded vignette/inner border to frame the window.
            g2.setColor(Theme.withAlpha(Theme.ACCENT, 22));
            g2.setStroke(new java.awt.BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1f, h - 1f,
                    Theme.CORNER_ARC, Theme.CORNER_ARC));
        } finally {
            g2.dispose();
        }
    }

    /** A single drifting node. */
    private static final class Particle {
        double x, y, vx, vy;
        final float size;

        Particle(double x, double y, double vx, double vy, float size) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.size = size;
        }
    }
}
