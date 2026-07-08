package com.prime.launcher.utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.prime.launcher.Launcher;
import com.prime.launcher.gui.LauncherGUI;
import fr.flowarg.flowlogger.ILogger;
import fr.flowarg.flowlogger.Logger;
import fr.flowarg.flowupdater.FlowUpdater;
import fr.flowarg.flowupdater.download.IProgressCallback;
import fr.flowarg.flowupdater.download.Step;
import fr.flowarg.flowupdater.versions.VanillaVersion;
import fr.flowarg.flowupdater.versions.fabric.FabricVersion;
import fr.flowarg.flowupdater.versions.fabric.FabricVersionBuilder;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class GameLauncher {
    public static final String FABRIC_API_URL = "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.92.0+1.20.1/fabric-api-0.92.0+1.20.1.jar";

    public static void launch(String username, String version, int ramGb, LauncherGUI gui) throws Exception {
        boolean isPrime = version.contains("Prime Client");
        boolean isFabric = isPrime || version.contains("Fabric");
        String vanillaVersion = version.contains("1.20.1") ? "1.20.1" : "1.16.5";

        File modsDir = new File(Launcher.GAME_DIR, "mods");
        if (!modsDir.exists()) {
            modsDir.mkdirs();
        }

        // 1. Install Java dependencies (mods)
        File targetMod = new File(modsDir, "prime-1.0.0.jar");
        if (isPrime) {
            gui.setStatus("Extracting Prime Client mod...");
            // Extract from launcher jar resources
            try (InputStream is = GameLauncher.class.getResourceAsStream("/prime-1.0.0.jar")) {
                if (is != null) {
                    Files.copy(is, targetMod.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    // Fallback to dev workspace path if resources are empty
                    File devJar = new File("../build/libs/prime-1.0.0.jar");
                    if (devJar.exists()) {
                        Files.copy(devJar.toPath(), targetMod.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } else {
                        throw new FileNotFoundException("prime-1.0.0.jar resource not found!");
                    }
                }
            }
        } else {
            if (targetMod.exists()) {
                targetMod.delete();
            }
        }

        // Handle Fabric API download
        File fabricApiFile = new File(modsDir, "fabric-api-0.92.0+1.20.1.jar");
        if (isFabric) {
            if (!fabricApiFile.exists()) {
                gui.setStatus("Downloading Fabric API...");
                downloadFile(FABRIC_API_URL, fabricApiFile.toPath(), gui);
            }

            // Download OptiFine alternative optimization mods
            String[] optMods = {"sodium", "iris", "lithium", "indium", "zoomify", "lambdynamiclights", "fabric-language-kotlin", "yacl"};
            for (String slug : optMods) {
                // Check if mod exists
                boolean exists = false;
                File[] modFiles = modsDir.listFiles();
                if (modFiles != null) {
                    for (File f : modFiles) {
                        if (f.getName().toLowerCase().startsWith(slug)) {
                            exists = true;
                            break;
                        }
                    }
                }
                if (!exists) {
                    gui.setStatus("Fetching " + slug + " on Modrinth...");
                    String[] downloadInfo = getModrinthDownload(slug, vanillaVersion);
                    if (downloadInfo != null) {
                        gui.setStatus("Downloading " + slug + " (OptiFine Alternative)...");
                        downloadFile(downloadInfo[0], new File(modsDir, downloadInfo[1]).toPath(), gui);
                    }
                }
            }
        } else {
            // Clean up Fabric mods if vanilla
            if (fabricApiFile.exists()) {
                fabricApiFile.delete();
            }
            String[] optMods = {"sodium", "iris", "lithium", "indium", "zoomify", "lambdynamiclights", "fabric-language-kotlin", "yacl"};
            File[] modFiles = modsDir.listFiles();
            if (modFiles != null) {
                for (File f : modFiles) {
                    for (String slug : optMods) {
                        if (f.getName().toLowerCase().startsWith(slug)) {
                            f.delete();
                        }
                    }
                }
            }
        }

        // 2. Setup FlowUpdater configuration
        gui.setStatus("Initializing FlowUpdater...");
        VanillaVersion vanilla = new VanillaVersion.VanillaVersionBuilder()
                .withName(vanillaVersion)
                .build();

        FlowUpdater.FlowUpdaterBuilder updaterBuilder = new FlowUpdater.FlowUpdaterBuilder()
                .withVanillaVersion(vanilla);

        if (isFabric) {
            FabricVersion fabric = new FabricVersionBuilder()
                    .withFabricVersion("0.18.0")
                    .build();
            updaterBuilder.withModLoaderVersion(fabric);
        }

        // Progress callback to GUI
        updaterBuilder.withProgressCallback(new IProgressCallback() {
            private ILogger logger;

            @Override
            public void init(ILogger logger) {
                this.logger = logger;
            }

            @Override
            public void step(Step step) {
                gui.setStatus("Updating: " + step.name());
            }

            @Override
            public void update(fr.flowarg.flowupdater.download.DownloadList.DownloadInfo info) {
                if (info.getTotalToDownloadFiles() > 0) {
                    int pct = (int) (((double) info.getDownloadedFiles() / info.getTotalToDownloadFiles()) * 100);
                    gui.setProgress(pct);
                    gui.setStatus("Downloading: " + info.getDownloadedFiles() + " / " + info.getTotalToDownloadFiles() + " files");
                }
            }
        });

        ILogger logger = new Logger("[PrimeLauncher]", Paths.get(Launcher.GAME_DIR, "launcher.log"));
        updaterBuilder.withLogger(logger);

        FlowUpdater updater = updaterBuilder.build();
        gui.setStatus("Updating Minecraft files...");
        updater.update(Paths.get(Launcher.GAME_DIR));

        // 3. Launch Process
        gui.setStatus("Launching Minecraft...");
        gui.setProgress(100);

        // Find java executable
        String javaExe = System.getProperty("java.home") + File.separator + "bin" + File.separator + "javaw.exe";
        File je = new File(javaExe);
        if (!je.exists()) {
            javaExe = "javaw.exe"; // Fallback to PATH
        }

        // Get fabric version ID
        String versionId = vanillaVersion;
        if (isFabric) {
            versionId = "fabric-loader-0.15.11-" + vanillaVersion;
        }

        // Classpath construction
        List<String> classpath = new ArrayList<>();
        // Add all libraries recursively
        File libDir = new File(Launcher.GAME_DIR, "libraries");
        findJars(libDir, classpath);

        // Add vanilla client jar
        classpath.add(new File(Launcher.GAME_DIR, "versions/" + vanillaVersion + "/" + vanillaVersion + ".jar").getAbsolutePath());

        // Add fabric loader jar
        if (isFabric) {
            File versionsDir = new File(Launcher.GAME_DIR, "versions");
            File[] versions = versionsDir.listFiles();
            if (versions != null) {
                for (File v : versions) {
                    if (v.getName().contains("fabric-loader")) {
                        File[] vf = v.listFiles();
                        if (vf != null) {
                            for (File f : vf) {
                                if (f.getName().endsWith(".jar")) {
                                    classpath.add(f.getAbsolutePath());
                                }
                            }
                        }
                    }
                }
            }
        }

        // Join classpath
        String cpSeparator = System.getProperty("path.separator", ";");
        StringBuilder cpBuilder = new StringBuilder();
        for (int i = 0; i < classpath.size(); i++) {
            cpBuilder.append(classpath.get(i));
            if (i < classpath.size() - 1) {
                cpBuilder.append(cpSeparator);
            }
        }

        List<String> cmd = new ArrayList<>();
        cmd.add(javaExe);
        cmd.add("-Xmx" + ramGb + "G");
        cmd.add("-Djava.library.path=" + new File(Launcher.GAME_DIR, "versions/" + versionId + "/natives").getAbsolutePath());
        cmd.add("-Dminecraft.launcher.brand=prime");
        cmd.add("-Dminecraft.launcher.version=1.0.0");
        cmd.add("-cp");
        cmd.add(cpBuilder.toString());

        if (isFabric) {
            cmd.add("net.fabricmc.loader.impl.launch.knot.KnotClient");
        } else {
            cmd.add("net.minecraft.client.main.Main");
        }

        // Game args
        cmd.add("--username");
        cmd.add(username);
        cmd.add("--version");
        cmd.add(vanillaVersion);
        cmd.add("--gameDir");
        cmd.add(Launcher.GAME_DIR);
        cmd.add("--assetsDir");
        cmd.add(new File(Launcher.GAME_DIR, "assets").getAbsolutePath());
        cmd.add("--assetIndex");
        cmd.add(vanillaVersion.equals("1.20.1") ? "3" : "1.16.5"); // standard asset indices
        cmd.add("--uuid");
        cmd.add("00000000-0000-0000-0000-000000000000");
        cmd.add("--accessToken");
        cmd.add("0");

        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(new File(Launcher.GAME_DIR));

        gui.onLaunchSuccess();

        Process process = pb.start();
        process.waitFor(); // Wait for game to exit

        gui.onGameExit();
    }

    private static void findJars(File dir, List<String> list) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    findJars(f, list);
                } else if (f.getName().endsWith(".jar")) {
                    list.add(f.getAbsolutePath());
                }
            }
        }
    }

    private static void downloadFile(String urlStr, Path dest, LauncherGUI gui) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestProperty("User-Agent", "PrimeClientLauncher/1.0");
        int length = conn.getContentLength();

        try (InputStream in = conn.getInputStream();
             OutputStream out = new FileOutputStream(dest.toFile())) {
            byte[] buffer = new byte[4096];
            int bytesRead;
            int totalRead = 0;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
                totalRead += bytesRead;
                if (length > 0) {
                    gui.setProgress((int) (((double) totalRead / length) * 100));
                }
            }
        }
    }

    private static String[] getModrinthDownload(String slug, String mcVersion) {
        try {
            URL url = new URL("https://api.modrinth.com/v2/project/" + slug + "/version");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "PrimeClientLauncher/1.0");
            conn.setRequestMethod("GET");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }

                JsonArray versions = JsonParser.parseString(sb.toString()).getAsJsonArray();
                for (JsonElement el : versions) {
                    JsonObject v = el.getAsJsonObject();
                    JsonArray gameVersions = v.getAsJsonArray("game_versions");
                    JsonArray loaders = v.getAsJsonArray("loaders");

                    boolean supportsMc = false;
                    for (JsonElement gv : gameVersions) {
                        if (gv.getAsString().equals(mcVersion)) {
                            supportsMc = true;
                            break;
                        }
                    }

                    boolean supportsFabric = false;
                    for (JsonElement l : loaders) {
                        if (l.getAsString().equals("fabric")) {
                            supportsFabric = true;
                            break;
                        }
                    }

                    if (supportsMc && supportsFabric) {
                        JsonObject fileObj = v.getAsJsonArray("files").get(0).getAsJsonObject();
                        return new String[]{
                                fileObj.get("url").getAsString(),
                                fileObj.get("filename").getAsString()
                        };
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
