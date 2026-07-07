import os
import sys
import shutil
import zipfile
import json
import urllib.request
import threading
import subprocess
import tkinter as tk
from tkinter import ttk, messagebox
from PIL import Image, ImageTk
import minecraft_launcher_lib

# isolated game directory: %appdata%/.prime-client
GAME_DIR = os.path.join(os.environ["APPDATA"], ".prime-client")
SETTINGS_FILE = os.path.join(GAME_DIR, "launcher_settings.json")

# Fabric API and Microsoft JDK 17 download links
FABRIC_API_URL = "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/0.92.0+1.20.1/fabric-api-0.92.0+1.20.1.jar"
JDK_URL = "https://aka.ms/download-jdk/microsoft-jdk-17-windows-x64.zip"

def get_asset_path(relative_path):
    """ Get absolute path to resource, works for dev and for PyInstaller """
    if hasattr(sys, '_MEIPASS'):
        return os.path.join(sys._MEIPASS, relative_path)
    return os.path.join(os.path.abspath("."), relative_path)

def get_java_executable():
    """ Check system PATH or local portable JDK for javaw.exe / java.exe """
    # 1. Check system PATH
    system_javaw = shutil.which("javaw")
    if system_javaw:
        return system_javaw
        
    system_java = shutil.which("java")
    if system_java:
        return system_java

    # 2. Check local portable JDK 17
    local_javaw = os.path.join(GAME_DIR, "jdk17", "bin", "javaw.exe")
    if os.path.exists(local_javaw):
        return local_javaw

    return None

def install_portable_jdk(progress_callback, status_callback):
    """ Download and extract Microsoft OpenJDK 17 to isolated directory """
    jdk_dir = os.path.join(GAME_DIR, "jdk17")
    zip_path = os.path.join(GAME_DIR, "jdk17.zip")
    
    status_callback("Downloading Java 17 Runtime...", "#00E5FF")
    
    def reporthook(count, block_size, total_size):
        progress_callback(count * block_size, total_size)

    # Download JDK ZIP
    urllib.request.urlretrieve(JDK_URL, zip_path, reporthook)
    
    status_callback("Extracting Java 17...", "#00E5FF")
    temp_extract = os.path.join(GAME_DIR, "jdk_temp")
    if os.path.exists(temp_extract):
        shutil.rmtree(temp_extract)
    os.makedirs(temp_extract)
    
    with zipfile.ZipFile(zip_path, 'r') as zip_ref:
        zip_ref.extractall(temp_extract)
        
    # Move files up
    subfolders = os.listdir(temp_extract)
    if subfolders:
        src = os.path.join(temp_extract, subfolders[0])
        if os.path.exists(jdk_dir):
            shutil.rmtree(jdk_dir)
        shutil.copytree(src, jdk_dir)
        
    # Cleanup
    shutil.rmtree(temp_extract)
    os.remove(zip_path)
    
    return os.path.join(jdk_dir, "bin", "javaw.exe")

def get_modrinth_download_url(project_slug, mc_version):
    """ Fetch the direct CDN download URL for a mod on Modrinth """
    try:
        url = f"https://api.modrinth.com/v2/project/{project_slug}/version"
        req = urllib.request.Request(
            url,
            headers={'User-Agent': 'PrimeClientLauncher/1.0'}
        )
        with urllib.request.urlopen(req) as response:
            versions = json.loads(response.read().decode())
            for v in versions:
                if mc_version in v["game_versions"] and "fabric" in v["loaders"]:
                    file_info = v["files"][0]
                    return file_info["url"], file_info["filename"]
    except Exception as e:
        print(f"Modrinth API error for {project_slug}: {e}")
    return None, None

class LauncherGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("Prime Client Launcher")
        self.root.geometry("580x350")
        self.root.resizable(False, False)
        
        # Sleek Premium Gamer Theme Colors
        self.bg_color = "#08090C"       # Ultra dark blue-gray
        self.panel_color = "#0E1015"    # Dark panel color
        self.fg_color = "#E2E8F0"       # Off-white
        self.accent_color = "#00E5FF"   # Electric neon cyan
        self.hover_color = "#00B0FF"    # Deep sky blue
        
        self.root.configure(bg=self.bg_color)
        
        # Load saved settings
        self.load_settings()

        # Custom TTK styles for dropdowns and progress bars
        style = ttk.Style()
        style.theme_use('default')
        style.configure("TProgressbar", thickness=8, troughcolor="#171923", background=self.accent_color)
        style.configure(
            "TCombobox",
            fieldbackground="#1A1C23",
            background=self.accent_color,
            foreground=self.fg_color,
            arrowcolor=self.accent_color
        )
        
        # Layout Frames
        # 1. Left Sidebar (Anime Image Panel)
        self.sidebar_frame = tk.Frame(root, bg=self.panel_color, width=180, height=350)
        self.sidebar_frame.pack(side=tk.LEFT, fill=tk.Y)
        self.sidebar_frame.pack_propagate(False)
        
        try:
            bg_path = get_asset_path("launcher_bg.png")
            raw_img = Image.open(bg_path)
            # Resize image to fit sidebar perfectly
            resized_img = raw_img.resize((180, 350), Image.Resampling.LANCZOS)
            self.sidebar_photo = ImageTk.PhotoImage(resized_img)
            
            self.sidebar_label = tk.Label(self.sidebar_frame, image=self.sidebar_photo, bg=self.panel_color, bd=0)
            self.sidebar_label.pack(fill=tk.BOTH, expand=True)
        except Exception as e:
            print(f"Sidebar image failed to load: {e}")
            fallback_label = tk.Label(
                self.sidebar_frame, text="PRIME", font=("Helvetica", 24, "bold"),
                bg=self.panel_color, fg=self.accent_color
            )
            fallback_label.pack(pady=150)
            
        # 2. Right Main Panel
        self.main_frame = tk.Frame(root, bg=self.bg_color, width=400, height=350)
        self.main_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        self.main_frame.pack_propagate(False)
        
        # Title
        self.title_label = tk.Label(
            self.main_frame, text="PRIME CLIENT", font=("Helvetica", 18, "bold"),
            bg=self.bg_color, fg=self.accent_color
        )
        self.title_label.pack(pady=(20, 15))
        
        # Form Container
        self.form_frame = tk.Frame(self.main_frame, bg=self.bg_color)
        self.form_frame.pack(pady=5, padx=20, fill=tk.X)
        
        # Form grid configuration
        self.form_frame.columnconfigure(0, weight=1)
        self.form_frame.columnconfigure(1, weight=2)
        
        # Row 1: Username Input
        self.user_label = tk.Label(
            self.form_frame, text="Username:", font=("Helvetica", 10),
            bg=self.bg_color, fg=self.fg_color
        )
        self.user_label.grid(row=0, column=0, sticky=tk.W, pady=6)
        
        self.user_entry = tk.Entry(
            self.form_frame, font=("Helvetica", 10), bg="#141722", fg=self.fg_color,
            insertbackground=self.fg_color, bd=1, relief=tk.FLAT, width=22
        )
        self.user_entry.grid(row=0, column=1, sticky=tk.W, pady=6)
        self.user_entry.insert(0, self.saved_username)
        
        # Row 2: Version Combobox
        self.version_label = tk.Label(
            self.form_frame, text="Version:", font=("Helvetica", 10),
            bg=self.bg_color, fg=self.fg_color
        )
        self.version_label.grid(row=1, column=0, sticky=tk.W, pady=6)
        
        self.version_combo = ttk.Combobox(
            self.form_frame, font=("Helvetica", 9), state="readonly", width=20,
            values=["1.20.1 (Prime Client)", "1.20.1 (Vanilla)", "1.16.5 (Vanilla)"]
        )
        self.version_combo.grid(row=1, column=1, sticky=tk.W, pady=6)
        self.version_combo.set(self.saved_version)
        
        # Row 3: RAM Slider
        self.ram_label = tk.Label(
            self.form_frame, text="Allocated RAM:", font=("Helvetica", 10),
            bg=self.bg_color, fg=self.fg_color
        )
        self.ram_label.grid(row=2, column=0, sticky=tk.W, pady=6)
        
        self.ram_scale_frame = tk.Frame(self.form_frame, bg=self.bg_color)
        self.ram_scale_frame.grid(row=2, column=1, sticky=tk.W, pady=6)
        
        self.ram_scale = tk.Scale(
            self.ram_scale_frame, from_=2, to=8, orient=tk.HORIZONTAL, length=120,
            bg=self.bg_color, fg=self.accent_color, highlightthickness=0,
            activebackground=self.accent_color, troughcolor="#171923", bd=0, relief=tk.FLAT
        )
        self.ram_scale.pack(side=tk.LEFT)
        self.ram_scale.set(self.saved_ram)
        
        self.ram_unit_label = tk.Label(
            self.ram_scale_frame, text="GB", font=("Helvetica", 10),
            bg=self.bg_color, fg=self.fg_color
        )
        self.ram_unit_label.pack(side=tk.LEFT, padx=3)
        
        # Status Text
        self.status_label = tk.Label(
            self.main_frame, text="Ready to Play", font=("Helvetica", 9),
            bg=self.bg_color, fg="#5E6377"
        )
        self.status_label.pack(pady=(15, 2))
        
        # Progress Bar
        self.progress = ttk.Progressbar(
            self.main_frame, orient="horizontal", length=320,
            mode="determinate", style="TProgressbar"
        )
        self.progress.pack(pady=3)
        
        # PLAY Button
        self.play_button = tk.Button(
            self.main_frame, text="LAUNCH GAME", font=("Helvetica", 11, "bold"),
            bg=self.accent_color, fg="#08090C", activebackground=self.hover_color,
            activeforeground="#08090C", bd=0, relief=tk.FLAT, width=22, height=1,
            command=self.start_launch_thread
        )
        self.play_button.pack(pady=(10, 10))
        
        # Dynamic Hover effects on Launch Button
        self.play_button.bind("<Enter>", lambda e: self.play_button.config(bg=self.hover_color))
        self.play_button.bind("<Leave>", lambda e: self.play_button.config(bg=self.accent_color))
        
        # Progress state
        self.max_progress = 0
        self.current_progress = 0

    def load_settings(self):
        self.saved_username = "Player"
        self.saved_version = "1.20.1 (Prime Client)"
        self.saved_ram = 4
        
        if os.path.exists(SETTINGS_FILE):
            try:
                with open(SETTINGS_FILE, "r") as f:
                    data = json.load(f)
                    self.saved_username = data.get("username", "Player")
                    self.saved_version = data.get("version", "1.20.1 (Prime Client)")
                    self.saved_ram = data.get("ram_gb", 4)
            except Exception as e:
                print(f"Error loading config file: {e}")

    def save_settings(self, username, version, ram_gb):
        if not os.path.exists(GAME_DIR):
            os.makedirs(GAME_DIR)
        try:
            with open(SETTINGS_FILE, "w") as f:
                json.dump({
                    "username": username,
                    "version": version,
                    "ram_gb": ram_gb
                }, f, indent=4)
        except Exception as e:
            print(f"Error saving config file: {e}")

    def set_status(self, text, color="#5E6377"):
        self.status_label.config(text=text, fg=color)
        self.root.update_idletasks()

    def set_progress(self, current, max_val):
        if max_val > 0:
            pct = (current / max_val) * 100
            self.progress['value'] = pct
        else:
            self.progress['value'] = 0
        self.root.update_idletasks()

    def disable_controls(self):
        self.play_button.config(state=tk.DISABLED, bg="#2A2C35")
        self.user_entry.config(state=tk.DISABLED)
        self.version_combo.config(state=tk.DISABLED)
        self.ram_scale.config(state=tk.DISABLED)

    def enable_controls(self):
        self.play_button.config(state=tk.NORMAL, bg=self.accent_color)
        self.user_entry.config(state=tk.NORMAL)
        self.version_combo.config(state="readonly")
        self.ram_scale.config(state=tk.NORMAL)

    def start_launch_thread(self):
        username = self.user_entry.get().strip()
        version = self.version_combo.get()
        ram_gb = int(self.ram_scale.get())
        
        if not username:
            messagebox.showerror("Error", "Please enter a username!")
            return
            
        self.save_settings(username, version, ram_gb)
        self.disable_controls()
        threading.Thread(target=self.launch_game, args=(username, version, ram_gb), daemon=True).start()

    def launch_game(self, username, version, ram_gb):
        try:
            # 1. Setup game directory
            if not os.path.exists(GAME_DIR):
                os.makedirs(GAME_DIR)
                
            mods_dir = os.path.join(GAME_DIR, "mods")
            if not os.path.exists(mods_dir):
                os.makedirs(mods_dir)

            # Progress callbacks
            def set_status_cb(status):
                self.set_status(status, self.accent_color)
            def set_progress_cb(current, max_val=100):
                self.set_progress(current, max_val)

            # 2. Check and Install Portable Java 17 if missing on host
            java_exe = get_java_executable()
            if not java_exe:
                java_exe = install_portable_jdk(set_progress_cb, set_status_cb)

            # Define Version Target based on selection
            is_prime = "Prime Client" in version
            is_fabric = is_prime or "Fabric" in version
            vanilla_version = "1.20.1" if "1.20.1" in version else "1.16.5"
            
            # Remove or add Prime Mod jar based on choice
            target_mod = os.path.join(mods_dir, "prime-1.0.0.jar")
            if is_prime:
                self.set_status("Installing Prime Mod...", self.accent_color)
                embedded_mod = get_asset_path("prime-1.0.0.jar")
                
                if os.path.exists(embedded_mod):
                    shutil.copy2(embedded_mod, target_mod)
                else:
                    dev_mod = os.path.join(os.path.dirname(__file__), "build", "libs", "prime-1.0.0.jar")
                    if os.path.exists(dev_mod):
                        shutil.copy2(dev_mod, target_mod)
                    else:
                        self.set_status("Error: prime-1.0.0.jar not found!", "#FF5555")
                        self.enable_controls()
                        return
            else:
                # Remove Prime mod if playing vanilla
                if os.path.exists(target_mod):
                    os.remove(target_mod)

            # 3. Handle Fabric / Fabric API dependencies if needed
            target_fabric_api = os.path.join(mods_dir, "fabric-api-0.92.0+1.20.1.jar")
            opt_mods = ["sodium", "iris", "lithium", "indium", "zoomify", "lambdynamiclights"]
            if is_fabric:
                if not os.path.exists(target_fabric_api):
                    self.set_status("Downloading Fabric API dependency...", self.accent_color)
                    def reporthook(count, block_size, total_size):
                        self.set_progress(count * block_size, total_size)
                    urllib.request.urlretrieve(FABRIC_API_URL, target_fabric_api, reporthook)

                # Download OptiFine alternative optimization mods
                for slug in opt_mods:
                    existing = [f for f in os.listdir(mods_dir) if f.lower().startswith(slug)]
                    if not existing:
                        self.set_status(f"Fetching {slug} on Modrinth...", self.accent_color)
                        url, filename = get_modrinth_download_url(slug, vanilla_version)
                        if url and filename:
                            self.set_status(f"Downloading {slug} (OptiFine Alternative)...", self.accent_color)
                            def reporthook(count, block_size, total_size):
                                self.set_progress(count * block_size, total_size)
                            target_path = os.path.join(mods_dir, filename)
                            urllib.request.urlretrieve(url, target_path, reporthook)
            else:
                # Remove Fabric API if playing vanilla
                if os.path.exists(target_fabric_api):
                    os.remove(target_fabric_api)
                # Remove optimization mods if playing vanilla
                for slug in opt_mods:
                    for f in os.listdir(mods_dir):
                        if f.lower().startswith(slug):
                            try:
                                os.remove(os.path.join(mods_dir, f))
                            except:
                                pass

            # 4. Install Minecraft Base Version if missing
            self.set_status(f"Installing Minecraft {vanilla_version}...", self.accent_color)
            
            # Callbacks for minecraft_launcher_lib
            def set_progress_mcl(current):
                self.current_progress = current
                self.set_progress(self.current_progress, self.max_progress)
            def set_max_mcl(maximum):
                self.max_progress = maximum
                self.set_progress(self.current_progress, self.max_progress)

            mcl_callback = {
                "setStatus": set_status_cb,
                "setProgress": set_progress_mcl,
                "setMax": set_max_mcl
            }

            minecraft_launcher_lib.install.install_minecraft_version(
                vanilla_version,
                GAME_DIR,
                callback=mcl_callback
            )

            # 5. Install Fabric Loader if requested
            version_id = vanilla_version
            if is_fabric:
                self.set_status("Installing Fabric Loader...", self.accent_color)
                if not minecraft_launcher_lib.fabric.is_minecraft_version_supported(vanilla_version):
                    self.set_status(f"Error: Fabric not supported for {vanilla_version}", "#FF5555")
                    self.enable_controls()
                    return

                fabric_loader_version = minecraft_launcher_lib.fabric.get_latest_loader_version()
                minecraft_launcher_lib.fabric.install_fabric(
                    vanilla_version,
                    GAME_DIR,
                    loader_version=fabric_loader_version,
                    callback=mcl_callback,
                    java=java_exe
                )
                version_id = f"fabric-loader-{fabric_loader_version}-{vanilla_version}"

            # 6. Launch the Game
            self.set_status("Launching Minecraft...", "#55FF55")
            self.progress['value'] = 100
            
            options = {
                "username": username,
                "uuid": "",
                "token": "",
                "executablePath": java_exe,
                "jvmArguments": [f"-Xmx{ram_gb}G"]
            }

            launch_cmd = minecraft_launcher_lib.command.get_minecraft_command(
                version_id,
                GAME_DIR,
                options
            )

            # Hide launcher window while game is running
            self.root.withdraw()

            # Execute the game process
            subprocess.run(launch_cmd)

            # Show launcher window again when game closes
            self.root.deiconify()
            self.set_status("Ready to Play")
            self.progress['value'] = 0
            self.enable_controls()

        except Exception as e:
            print(f"Launcher Error: {e}")
            self.set_status(f"Error: {str(e)[:40]}...", "#FF5555")
            self.enable_controls()
            messagebox.showerror("Launcher Error", str(e))

if __name__ == "__main__":
    root = tk.Tk()
    app = LauncherGUI(root)
    root.mainloop()
