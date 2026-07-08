package com.prime.launcher.gui;

import com.prime.launcher.Launcher;
import com.prime.launcher.utils.GameLauncher;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class LauncherGUI extends JFrame {
    private BufferedImage bgImage;
    
    // UI Elements
    private JTextField userField;
    private JComboBox<String> versionCombo;
    private JSlider ramSlider;
    private JLabel ramValLabel;
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JButton launchButton;

    // Theme Colors
    public static final Color COLOR_BG = new Color(0x08, 0x09, 0x0C);
    public static final Color COLOR_PANEL = new Color(0x0E, 0x10, 0x15);
    public static final Color COLOR_FG = new Color(0xE2, 0xE8, 0xF0);
    public static final Color COLOR_ACCENT = new Color(0x00, 0xE5, 0xFF);
    public static final Color COLOR_HOVER = new Color(0x00, 0xB0, 0xFF);
    public static final Color COLOR_MUTED = new Color(0x5E, 0x63, 0x77);

    public LauncherGUI() {
        setTitle("Prime Client Launcher");
        setSize(580, 350);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        // Load background image
        try (InputStream is = LauncherGUI.class.getResourceAsStream("/launcher_bg.png")) {
            if (is != null) {
                bgImage = ImageIO.read(is);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 1. Sidebar Panel (Left)
        JPanel sidebarPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (bgImage != null) {
                    g.drawImage(bgImage, 0, 0, 180, 350, null);
                } else {
                    g.setColor(COLOR_PANEL);
                    g.fillRect(0, 0, 180, 350);
                    g.setColor(COLOR_ACCENT);
                    g.setFont(new Font("Helvetica", Font.BOLD, 24));
                    g.drawString("PRIME", 45, 175);
                }
            }
        };
        sidebarPanel.setPreferredSize(new Dimension(180, 350));
        add(sidebarPanel, BorderLayout.WEST);

        // 2. Main Panel (Right)
        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(COLOR_BG);
        mainPanel.setLayout(null);
        add(mainPanel, BorderLayout.CENTER);

        // Title
        JLabel titleLabel = new JLabel("PRIME CLIENT");
        titleLabel.setFont(new Font("Helvetica", Font.BOLD, 20));
        titleLabel.setForeground(COLOR_ACCENT);
        titleLabel.setBounds(110, 20, 200, 30);
        mainPanel.add(titleLabel);

        // Form fields
        int startY = 70;
        int spacing = 35;

        // Username
        JLabel userLabel = new JLabel("Username:");
        userLabel.setForeground(COLOR_FG);
        userLabel.setFont(new Font("Helvetica", Font.PLAIN, 12));
        userLabel.setBounds(30, startY, 90, 20);
        mainPanel.add(userLabel);

        userField = new JTextField(Launcher.settings.username);
        userField.setBackground(new Color(0x14, 0x17, 0x22));
        userField.setForeground(COLOR_FG);
        userField.setCaretColor(COLOR_FG);
        userField.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        userField.setBounds(130, startY, 210, 22);
        mainPanel.add(userField);

        // Version
        JLabel versionLabel = new JLabel("Version:");
        versionLabel.setForeground(COLOR_FG);
        versionLabel.setFont(new Font("Helvetica", Font.PLAIN, 12));
        versionLabel.setBounds(30, startY + spacing, 90, 20);
        mainPanel.add(versionLabel);

        versionCombo = new JComboBox<>(new String[]{
                "1.20.1 (Prime Client)",
                "1.20.1 (Vanilla)",
                "1.16.5 (Vanilla)"
        });
        versionCombo.setSelectedItem(Launcher.settings.version);
        versionCombo.setBackground(new Color(0x14, 0x17, 0x22));
        versionCombo.setForeground(COLOR_FG);
        versionCombo.setBorder(BorderFactory.createEmptyBorder());
        versionCombo.setBounds(130, startY + spacing, 210, 22);
        mainPanel.add(versionCombo);

        // RAM Allocation
        JLabel ramLabel = new JLabel("Allocated RAM:");
        ramLabel.setForeground(COLOR_FG);
        ramLabel.setFont(new Font("Helvetica", Font.PLAIN, 12));
        ramLabel.setBounds(30, startY + (spacing * 2), 90, 20);
        mainPanel.add(ramLabel);

        ramSlider = new JSlider(2, 8, Launcher.settings.ramGb);
        ramSlider.setBackground(COLOR_BG);
        ramSlider.setPaintTicks(false);
        ramSlider.setPaintLabels(false);
        ramSlider.setBounds(130, startY + (spacing * 2), 170, 20);
        mainPanel.add(ramSlider);

        ramValLabel = new JLabel(Launcher.settings.ramGb + " GB");
        ramValLabel.setForeground(COLOR_FG);
        ramValLabel.setFont(new Font("Helvetica", Font.PLAIN, 12));
        ramValLabel.setBounds(305, startY + (spacing * 2), 40, 20);
        mainPanel.add(ramValLabel);

        ramSlider.addChangeListener(e -> ramValLabel.setText(ramSlider.getValue() + " GB"));

        // Status Label
        statusLabel = new JLabel("Ready to Play", SwingConstants.CENTER);
        statusLabel.setForeground(COLOR_MUTED);
        statusLabel.setFont(new Font("Helvetica", Font.PLAIN, 11));
        statusLabel.setBounds(20, 200, 360, 20);
        mainPanel.add(statusLabel);

        // Progress Bar
        progressBar = new JProgressBar();
        progressBar.setForeground(COLOR_ACCENT);
        progressBar.setBackground(new Color(0x17, 0x19, 0x23));
        progressBar.setBorderPainted(false);
        progressBar.setBounds(40, 225, 320, 8);
        mainPanel.add(progressBar);

        // LAUNCH Button
        launchButton = new JButton("LAUNCH GAME");
        launchButton.setFont(new Font("Helvetica", Font.BOLD, 12));
        launchButton.setBackground(COLOR_ACCENT);
        launchButton.setForeground(COLOR_BG);
        launchButton.setFocusPainted(false);
        launchButton.setBorderPainted(false);
        launchButton.setBounds(90, 245, 220, 30);
        mainPanel.add(launchButton);

        // Hover effects
        launchButton.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (launchButton.isEnabled()) {
                    launchButton.setBackground(COLOR_HOVER);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (launchButton.isEnabled()) {
                    launchButton.setBackground(COLOR_ACCENT);
                }
            }
        });

        // Launch click handler
        launchButton.addActionListener(e -> startLaunch());
    }

    private void startLaunch() {
        String username = userField.getText().trim();
        String version = (String) versionCombo.getSelectedItem();
        int ramGb = ramSlider.getValue();

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a username!", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Save settings
        Launcher.saveSettings(username, version, ramGb);

        // Disable controls
        userField.setEnabled(false);
        versionCombo.setEnabled(false);
        ramSlider.setEnabled(false);
        launchButton.setEnabled(false);
        launchButton.setBackground(new Color(0x2A, 0x2C, 0x35));

        // Start thread
        new Thread(() -> {
            try {
                GameLauncher.launch(username, version, ramGb, this);
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("Error: " + e.getMessage());
                    statusLabel.setForeground(Color.RED);
                    resetControls();
                });
            }
        }).start();
    }

    public void setStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }

    public void setProgress(int pct) {
        SwingUtilities.invokeLater(() -> progressBar.setValue(pct));
    }

    public void onLaunchSuccess() {
        SwingUtilities.invokeLater(() -> {
            setVisible(false); // Hide window when game starts
        });
    }

    public void onGameExit() {
        SwingUtilities.invokeLater(() -> {
            setVisible(true); // Restore window when game exits
            resetControls();
        });
    }

    private void resetControls() {
        userField.setEnabled(true);
        versionCombo.setEnabled(true);
        ramSlider.setEnabled(true);
        launchButton.setEnabled(true);
        launchButton.setBackground(COLOR_ACCENT);
        statusLabel.setText("Ready to Play");
        statusLabel.setForeground(COLOR_MUTED);
        progressBar.setValue(0);
    }
}
