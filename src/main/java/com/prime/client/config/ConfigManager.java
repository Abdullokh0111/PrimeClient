package com.prime.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("prime-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;
    private static ModConfig config;

    public static void load() {
        File configDir = FabricLoader.getInstance().getConfigDir().toFile();
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
        configFile = new File(configDir, "prime.json");

        if (configFile.exists()) {
            try (FileReader reader = new FileReader(configFile)) {
                config = GSON.fromJson(reader, ModConfig.class);
                if (config == null) {
                    config = new ModConfig();
                }
                LOGGER.info("Configuration loaded successfully.");
            } catch (Exception e) {
                LOGGER.error("Failed to load configuration, using defaults.", e);
                config = new ModConfig();
            }
        } else {
            LOGGER.info("Configuration file not found, creating default.");
            config = new ModConfig();
            save();
        }
    }

    public static void save() {
        if (configFile == null || config == null) {
            return;
        }
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(config, writer);
            LOGGER.info("Configuration saved successfully.");
        } catch (IOException e) {
            LOGGER.error("Failed to save configuration.", e);
        }
    }

    public static ModConfig getConfig() {
        if (config == null) {
            load();
        }
        return config;
    }
}
