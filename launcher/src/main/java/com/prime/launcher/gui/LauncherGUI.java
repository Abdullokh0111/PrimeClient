package com.prime.launcher.gui;

import com.prime.launcher.Launcher;
import com.prime.launcher.gui.ui.GlowButton;
import com.prime.launcher.gui.ui.NeonComboBoxUI;
import com.prime.launcher.gui.ui.NeonProgressBarUI;
import com.prime.launcher.gui.ui.NeonSliderUI;
import com.prime.launcher.gui.ui.ParticleBackgroundPanel;
import com.prime.launcher.gui.ui.RoundedPanel;
import com.prime.launcher.gui.ui.RoundedTextField;
import com.prime.launcher.gui.ui.Theme;
import com.prime.launcher.gui.ui.WindowButton;
import com.prime.launcher.utils.GameLauncher;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

/**
 * Modern, "gamer client"-styled launcher window (Nursultan / Pulse aesthetic):
 * an undecorated, rounded frame with a custom draggable title bar, an animated
 * particle background, and rounded/neon form controls.
 *
 * Only the presentation was reworked; the launch flow and the callbacks used by
 * {@link GameLauncher} ({@link #setStatus}, {@link #setProgress},
 * {@link #onLaunchSuccess}, {@link #onGameExit}) keep their original contract.
 */
public class LauncherGUI extends JFrame {

    private final ParticleBackgroundPanel root;

    // Form controls.
    private RoundedTextField userField;
    private JComboBox<String> versionCombo;
    private JSlider ramSlider;
    private JLabel ramValLabel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private GlowButton launchButton;

    // Title-bar drag state.
    private Point dragOffset;

    public LauncherGUI() {
        setTitle("Prime Client Launcher");
        setSize(Theme.WINDOW_W, Theme.WINDOW_H);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setUndecorated(true);

        // Rounded window corners via a clip shape. On the rare platform that
        // doesn't support per-pixel shaping this is a silent no-op (square
        // window), which is still fully functional.
        setShape(new RoundRectangle2D.Double(0, 0, Theme.WINDOW_W, Theme.WINDOW_H,
                Theme.CORNER_ARC * 2, Theme.CORNER_ARC * 2));

        root = new ParticleBackgroundPanel();
        root.setLayout(null);
        root.setSidebarImage(loadBackground());
        setContentPane(root);

        buildTitleBar();
        buildSidebar();
        buildFormCard();
    }

    private BufferedImage loadBackground() {
        try (InputStream is = LauncherGUI.class.getResourceAsStream("/launcher_bg.png")) {
            if (is != null) {
                return ImageIO.read(is);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    // ---------------------------------------------------------------- title bar
    private void buildTitleBar() {
        JPanel titleBar = new JPanel(null);
        titleBar.setOpaque(false);
        titleBar.setBounds(0, 0, Theme.WINDOW_W, Theme.TITLE_BAR_H);

        JLabel brand = new JLabel("◆  PRIME");
        brand.setFont(new Font(Theme.TITLE.getFamily(), Font.BOLD, 15));
        brand.setForeground(Theme.ACCENT);
        brand.setBounds(20, 0, 200, Theme.TITLE_BAR_H);
        titleBar.add(brand);

        WindowButton minimize = new WindowButton(WindowButton.Glyph.MINIMIZE);
        minimize.setBounds(Theme.WINDOW_W - 80, 6, 32, 28);
        minimize.addActionListener(e -> setExtendedState(JFrame.ICONIFIED));
        titleBar.add(minimize);

        WindowButton close = new WindowButton(WindowButton.Glyph.CLOSE);
        close.setBounds(Theme.WINDOW_W - 42, 6, 32, 28);
        close.addActionListener(e -> System.exit(0));
        titleBar.add(close);

        // Drag the whole window by the title bar.
        titleBar.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                dragOffset = e.getPoint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragOffset = null;
            }
        });
        titleBar.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (dragOffset != null) {
                    Point onScreen = e.getLocationOnScreen();
                    setLocation(onScreen.x - dragOffset.x, onScreen.y - dragOffset.y);
                }
            }
        });

        root.add(titleBar);
    }

    // ------------------------------------------------------------------ sidebar
    private void buildSidebar() {
        JLabel logo = new JLabel("PRIME");
        logo.setFont(Theme.LOGO);
        logo.setForeground(Theme.FG);
        logo.setBounds(36, 150, 260, 40);
        root.add(logo);

        JLabel logo2 = new JLabel("CLIENT");
        logo2.setFont(Theme.LOGO);
        logo2.setForeground(Theme.ACCENT);
        logo2.setBounds(36, 188, 260, 40);
        root.add(logo2);

        JLabel tagline = new JLabel("<html>Next-gen utility client<br>for Minecraft 1.20.1</html>");
        tagline.setFont(Theme.SUBTITLE);
        tagline.setForeground(Theme.MUTED);
        tagline.setBounds(38, 240, 260, 48);
        root.add(tagline);

        JLabel version = new JLabel("v1.0.0  •  Fabric");
        version.setFont(Theme.SMALL);
        version.setForeground(Theme.MUTED);
        version.setBounds(38, Theme.WINDOW_H - 44, 260, 20);
        root.add(version);
    }

    // ---------------------------------------------------------------- form card
    private void buildFormCard() {
        int cardX = Theme.SIDEBAR_W + 40;
        int cardY = 70;
        int cardW = Theme.WINDOW_W - cardX - 40;
        int cardH = Theme.WINDOW_H - cardY - 40;

        RoundedPanel card = new RoundedPanel(null, 16);
        card.setBounds(cardX, cardY, cardW, cardH);

        int pad = 30;
        int fieldW = cardW - pad * 2;
        int fieldH = 34;

        JLabel header = new JLabel("LAUNCH SETTINGS");
        header.setFont(Theme.TITLE);
        header.setForeground(Theme.FG);
        header.setBounds(pad, 22, fieldW, 28);
        card.add(header);

        // Username.
        card.add(fieldLabel("USERNAME", pad, 74, fieldW));
        userField = new RoundedTextField(Launcher.settings.username);
        userField.setBounds(pad, 94, fieldW, fieldH);
        card.add(userField);

        // Version.
        card.add(fieldLabel("VERSION", pad, 148, fieldW));
        versionCombo = new JComboBox<>(new String[]{
                "1.20.1 (Prime Client)",
                "1.20.1 (Vanilla)",
                "1.16.5 (Vanilla)"
        });
        versionCombo.setSelectedItem(Launcher.settings.version);
        NeonComboBoxUI.apply(versionCombo);
        versionCombo.setBounds(pad, 168, fieldW, fieldH);
        card.add(versionCombo);

        // RAM.
        JLabel ramLabel = fieldLabel("ALLOCATED RAM", pad, 222, fieldW - 60);
        card.add(ramLabel);
        ramValLabel = new JLabel(Launcher.settings.ramGb + " GB", SwingConstants.RIGHT);
        ramValLabel.setFont(new Font(Theme.INPUT.getFamily(), Font.BOLD, 13));
        ramValLabel.setForeground(Theme.ACCENT);
        ramValLabel.setBounds(pad + fieldW - 60, 222, 60, 16);
        card.add(ramValLabel);

        ramSlider = new JSlider(2, 16, clamp(Launcher.settings.ramGb, 2, 16));
        ramSlider.setUI(new NeonSliderUI(ramSlider));
        ramSlider.setOpaque(false);
        ramSlider.setBounds(pad, 244, fieldW, 24);
        ramSlider.addChangeListener(e -> ramValLabel.setText(ramSlider.getValue() + " GB"));
        card.add(ramSlider);

        // Status.
        statusLabel = new JLabel("Ready to play", SwingConstants.CENTER);
        statusLabel.setFont(Theme.SMALL);
        statusLabel.setForeground(Theme.MUTED);
        statusLabel.setBounds(pad, cardH - 130, fieldW, 18);
        card.add(statusLabel);

        // Progress.
        progressBar = new JProgressBar();
        progressBar.setUI(new NeonProgressBarUI());
        progressBar.setOpaque(false);
        progressBar.setBorderPainted(false);
        progressBar.setBorder(BorderFactory.createEmptyBorder());
        progressBar.setBounds(pad, cardH - 108, fieldW, 8);
        card.add(progressBar);

        // Launch.
        launchButton = new GlowButton("LAUNCH GAME");
        launchButton.setBounds(pad, cardH - 88, fieldW, 46);
        launchButton.addActionListener(e -> startLaunch());
        card.add(launchButton);

        root.add(card);
    }

    private JLabel fieldLabel(String text, int x, int y, int w) {
        JLabel label = new JLabel(text);
        label.setFont(Theme.SMALL);
        label.setForeground(Theme.MUTED);
        label.setBounds(x, y, w, 16);
        return label;
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    // --------------------------------------------------------------- animation
    @Override
    public void addNotify() {
        super.addNotify();
        root.startAnimation();
    }

    @Override
    public void removeNotify() {
        root.stopAnimation();
        super.removeNotify();
    }

    // ------------------------------------------------------------ launch logic
    private void startLaunch() {
        String username = userField.getText().trim();
        String version = (String) versionCombo.getSelectedItem();
        int ramGb = ramSlider.getValue();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a username!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Launcher.saveSettings(username, version, ramGb);

        setControlsEnabled(false);

        new Thread(() -> {
            try {
                GameLauncher.launch(username, version, ramGb, this);
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    statusLabel.setForeground(Theme.DANGER);
                    resetControls();
                });
            }
        }, "prime-launch").start();
    }

    private void setControlsEnabled(boolean enabled) {
        userField.setEnabled(enabled);
        versionCombo.setEnabled(enabled);
        ramSlider.setEnabled(enabled);
        launchButton.setEnabled(enabled);
    }

    public void setStatus(String status) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(status);
            statusLabel.setForeground(Theme.MUTED);
        });
    }

    public void setProgress(int pct) {
        SwingUtilities.invokeLater(() -> progressBar.setValue(pct));
    }

    public void onLaunchSuccess() {
        SwingUtilities.invokeLater(() -> {
            root.stopAnimation(); // don't burn CPU while the window is hidden
            setVisible(false);
        });
    }

    public void onGameExit() {
        SwingUtilities.invokeLater(() -> {
            setVisible(true);
            root.startAnimation();
            resetControls();
        });
    }

    private void resetControls() {
        setControlsEnabled(true);
        statusLabel.setText("Ready to play");
        statusLabel.setForeground(Theme.MUTED);
        progressBar.setValue(0);
    }
}
