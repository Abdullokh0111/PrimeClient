package com.prime.launcher;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.prime.launcher.gui.LauncherGUI;

import javax.swing.*;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class Launcher {
    public static final String GAME_DIR = System.getenv("APPDATA") + File.separator + ".prime-client";
    public static final String SETTINGS_FILE = GAME_DIR + File.separator + "launcher_settings.json";

    public static Settings settings;
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        // Create game directory if missing
        File gd = new File(GAME_DIR);
        if (!gd.exists()) {
            gd.mkdirs();
        }

        // Setup global error logging to file
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            try (java.io.PrintWriter pw = new java.io.PrintWriter(new FileWriter(new File(GAME_DIR, "error.log"), true))) {
                pw.println("=== UNCAUGHT EXCEPTION IN THREAD: " + thread.getName() + " ===");
                throwable.printStackTrace(pw);
                pw.println();
                pw.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        // Load settings
        loadSettings();

        // Start GUI in EDT thread
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new LauncherGUI().setVisible(true);
        });
    }

    public static void loadSettings() {
        File file = new File(SETTINGS_FILE);
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                settings = gson.fromJson(reader, Settings.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (settings == null) {
            settings = new Settings();
        }
    }

    public static void saveSettings(String username, String version, int ramGb) {
        settings.username = username;
        settings.version = version;
        settings.ramGb = ramGb;

        try (FileWriter writer = new FileWriter(SETTINGS_FILE)) {
            gson.toJson(settings, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static class Settings {
        public String username = "Player";
        public String version = "1.20.1 (Prime Client)";
        public int ramGb = 4;
    }
}
