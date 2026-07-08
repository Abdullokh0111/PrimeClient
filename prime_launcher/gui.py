"""Tkinter GUI for the Prime Client launcher."""
import os
import shutil
import subprocess
import threading
import tkinter as tk
from tkinter import messagebox, ttk

import minecraft_launcher_lib
from PIL import Image, ImageTk

try:
    import psutil
    _HAS_PSUTIL = True
except ImportError:
    _HAS_PSUTIL = False

from . import java_utils, mod_updater, modrinth, paths, settings
from .assets import get_asset_path
from .constants import FABRIC_API_SHA1_URL, FABRIC_API_URL, LAUNCHER_VERSION, OPTIONAL_MODS
from .net import download_verified, fetch_text

VERSIONS = ["1.20.1 (Prime Client)", "1.20.1 (Vanilla)", "1.16.5 (Vanilla)"]


def _system_ram_gb():
    if _HAS_PSUTIL:
        try:
            return max(8, int(psutil.virtual_memory().total / (1024 ** 3)))
        except Exception:
            pass
    return 8  # sane ceiling default when psutil is unavailable


class LauncherGUI:
    def __init__(self, root):
        self.root = root
        self.root.title("Prime Client Launcher")
        self.root.geometry("580x410")
        self.root.resizable(False, False)

        self.bg_color = "#08090C"
        self.panel_color = "#0E1015"
        self.fg_color = "#E2E8F0"
        self.accent_color = "#00E5FF"
        self.hover_color = "#00B0FF"
        self.error_color = "#FF5555"
        self.warn_color = "#FFAA00"
        self.ok_color = "#55FF55"
        self.muted_color = "#5E6377"

        self.root.configure(bg=self.bg_color)

        paths.ensure_dirs()
        self.saved = settings.load_settings()
        self.max_ram = _system_ram_gb()

        self._build_styles()
        self._build_layout()

        self.max_progress = 0
        self.current_progress = 0

        # Non-blocking: don't make the window stall on startup just to show
        # which mod version is current.
        threading.Thread(target=self._refresh_mod_version_label, daemon=True).start()

    # ---------------------------------------------------------------- styles
    def _build_styles(self):
        style = ttk.Style()
        style.theme_use("default")
        style.configure("TProgressbar", thickness=8, troughcolor="#171923", background=self.accent_color)
        style.configure(
            "TCombobox",
            fieldbackground="#1A1C23",
            background=self.accent_color,
            foreground=self.fg_color,
            arrowcolor=self.accent_color,
        )

    # ---------------------------------------------------------------- layout
    def _build_layout(self):
        self.sidebar_frame = tk.Frame(self.root, bg=self.panel_color, width=180, height=410)
        self.sidebar_frame.pack(side=tk.LEFT, fill=tk.Y)
        self.sidebar_frame.pack_propagate(False)

        try:
            bg_path = get_asset_path("launcher_bg.png")
            raw_img = Image.open(bg_path)
            resized_img = raw_img.resize((180, 410), Image.Resampling.LANCZOS)
            self.sidebar_photo = ImageTk.PhotoImage(resized_img)
            tk.Label(self.sidebar_frame, image=self.sidebar_photo, bg=self.panel_color, bd=0).pack(
                fill=tk.BOTH, expand=True
            )
        except Exception as e:
            print(f"Sidebar image failed to load: {e}")
            tk.Label(
                self.sidebar_frame, text="PRIME", font=("Helvetica", 24, "bold"),
                bg=self.panel_color, fg=self.accent_color,
            ).pack(pady=185)

        self.main_frame = tk.Frame(self.root, bg=self.bg_color, width=400, height=410)
        self.main_frame.pack(side=tk.LEFT, fill=tk.BOTH, expand=True)
        self.main_frame.pack_propagate(False)

        # --- Top bar: title + utility buttons ---------------------------
        self.top_bar = tk.Frame(self.main_frame, bg=self.bg_color)
        self.top_bar.pack(fill=tk.X, padx=20, pady=(15, 0))

        tk.Label(
            self.top_bar, text="PRIME CLIENT", font=("Helvetica", 18, "bold"),
            bg=self.bg_color, fg=self.accent_color,
        ).pack(side=tk.LEFT)

        self.utility_frame = tk.Frame(self.top_bar, bg=self.bg_color)
        self.utility_frame.pack(side=tk.RIGHT)
        self._small_button(self.utility_frame, "⟲", self.on_repair_clicked).pack(side=tk.LEFT, padx=2)
        self._small_button(self.utility_frame, "📁", self.on_open_folder_clicked).pack(side=tk.LEFT, padx=2)

        # --- Form ---------------------------------------------------------
        self.form_frame = tk.Frame(self.main_frame, bg=self.bg_color)
        self.form_frame.pack(pady=10, padx=20, fill=tk.X)
        self.form_frame.columnconfigure(0, weight=1)
        self.form_frame.columnconfigure(1, weight=2)

        tk.Label(
            self.form_frame, text="Username:", font=("Helvetica", 10), bg=self.bg_color, fg=self.fg_color
        ).grid(row=0, column=0, sticky=tk.W, pady=6)
        self.user_entry = tk.Entry(
            self.form_frame, font=("Helvetica", 10), bg="#141722", fg=self.fg_color,
            insertbackground=self.fg_color, bd=1, relief=tk.FLAT, width=22,
        )
        self.user_entry.grid(row=0, column=1, sticky=tk.W, pady=6)
        self.user_entry.insert(0, self.saved["username"])

        tk.Label(
            self.form_frame, text="Version:", font=("Helvetica", 10), bg=self.bg_color, fg=self.fg_color
        ).grid(row=1, column=0, sticky=tk.W, pady=6)
        self.version_combo = ttk.Combobox(
            self.form_frame, font=("Helvetica", 9), state="readonly", width=20, values=VERSIONS
        )
        self.version_combo.grid(row=1, column=1, sticky=tk.W, pady=6)
        self.version_combo.set(self.saved["version"])

        tk.Label(
            self.form_frame, text="Allocated RAM:", font=("Helvetica", 10), bg=self.bg_color, fg=self.fg_color
        ).grid(row=2, column=0, sticky=tk.W, pady=6)
        ram_frame = tk.Frame(self.form_frame, bg=self.bg_color)
        ram_frame.grid(row=2, column=1, sticky=tk.W, pady=6)
        self.ram_scale = tk.Scale(
            ram_frame, from_=2, to=self.max_ram, orient=tk.HORIZONTAL, length=120,
            bg=self.bg_color, fg=self.accent_color, highlightthickness=0,
            activebackground=self.accent_color, troughcolor="#171923", bd=0, relief=tk.FLAT,
        )
        self.ram_scale.pack(side=tk.LEFT)
        self.ram_scale.set(min(self.saved["ram_gb"], self.max_ram))
        tk.Label(ram_frame, text="GB", font=("Helvetica", 10), bg=self.bg_color, fg=self.fg_color).pack(
            side=tk.LEFT, padx=3
        )

        # Advanced settings (collapsible): custom JVM args
        self.advanced_visible = tk.BooleanVar(value=bool(self.saved.get("jvm_args")))
        self.advanced_toggle = tk.Checkbutton(
            self.form_frame, text="Advanced: custom JVM arguments", variable=self.advanced_visible,
            command=self._toggle_advanced, font=("Helvetica", 8), bg=self.bg_color, fg=self.muted_color,
            selectcolor=self.bg_color, activebackground=self.bg_color, activeforeground=self.fg_color,
            bd=0, highlightthickness=0,
        )
        self.advanced_toggle.grid(row=3, column=0, columnspan=2, sticky=tk.W, pady=(4, 0))

        self.jvm_args_entry = tk.Entry(
            self.form_frame, font=("Helvetica", 9), bg="#141722", fg=self.fg_color,
            insertbackground=self.fg_color, bd=1, relief=tk.FLAT,
        )
        self.jvm_args_entry.insert(0, self.saved.get("jvm_args", ""))
        if self.advanced_visible.get():
            self.jvm_args_entry.grid(row=4, column=0, columnspan=2, sticky=tk.EW, pady=(2, 0))

        # --- Status / progress / play --------------------------------------
        self.status_label = tk.Label(
            self.main_frame, text="Ready to Play", font=("Helvetica", 9), bg=self.bg_color, fg=self.muted_color
        )
        self.status_label.pack(pady=(15, 2))

        self.progress = ttk.Progressbar(
            self.main_frame, orient="horizontal", length=320, mode="determinate", style="TProgressbar"
        )
        self.progress.pack(pady=3)

        self.play_button = tk.Button(
            self.main_frame, text="LAUNCH GAME", font=("Helvetica", 11, "bold"),
            bg=self.accent_color, fg="#08090C", activebackground=self.hover_color,
            activeforeground="#08090C", bd=0, relief=tk.FLAT, width=22, height=1,
            command=self.start_launch_thread,
        )
        self.play_button.pack(pady=(10, 5))
        self.play_button.bind("<Enter>", lambda e: self.play_button.config(bg=self.hover_color))
        self.play_button.bind("<Leave>", lambda e: self.play_button.config(bg=self.accent_color))

        # --- Footer: mod / launcher version ---------------------------------
        self.footer_label = tk.Label(
            self.main_frame, text=f"Launcher v{LAUNCHER_VERSION} · Prime Mod: checking...",
            font=("Helvetica", 8), bg=self.bg_color, fg=self.muted_color,
        )
        self.footer_label.pack(side=tk.BOTTOM, pady=(0, 8))

    def _small_button(self, parent, symbol, command):
        btn = tk.Button(
            parent, text=symbol, font=("Helvetica", 10), bg=self.bg_color, fg=self.muted_color,
            activebackground=self.bg_color, activeforeground=self.accent_color, bd=0,
            relief=tk.FLAT, cursor="hand2", command=command,
        )
        btn.bind("<Enter>", lambda e: btn.config(fg=self.accent_color))
        btn.bind("<Leave>", lambda e: btn.config(fg=self.muted_color))
        return btn

    def _toggle_advanced(self):
        if self.advanced_visible.get():
            self.jvm_args_entry.grid(row=4, column=0, columnspan=2, sticky=tk.EW, pady=(2, 0))
        else:
            self.jvm_args_entry.grid_forget()

    # ---------------------------------------------------------------- misc UI
    def _refresh_mod_version_label(self):
        release = mod_updater.get_latest_release()
        if release:
            tag = release.get("tag_name", "unknown")
            text = f"Launcher v{LAUNCHER_VERSION} · Prime Mod: {tag}"
        else:
            text = f"Launcher v{LAUNCHER_VERSION} · Prime Mod: offline"
        self.root.after(0, lambda: self.footer_label.config(text=text))

    def on_open_folder_clicked(self):
        paths.ensure_dirs()
        try:
            if os.name == "nt":
                os.startfile(paths.GAME_DIR)  # noqa: S606 - user-triggered, own data dir
            elif shutil.which("open"):  # macOS
                subprocess.run(["open", paths.GAME_DIR])
            else:  # Linux
                subprocess.run(["xdg-open", paths.GAME_DIR])
        except Exception as e:
            messagebox.showerror("Error", f"Could not open folder: {e}")

    def on_repair_clicked(self):
        if not messagebox.askyesno(
            "Verify / Repair Installation",
            "This deletes downloaded mods and the portable Java runtime so "
            "they get re-downloaded and re-verified on next launch. Your "
            "Minecraft install itself is untouched. Continue?",
        ):
            return
        try:
            if os.path.exists(paths.MODS_DIR):
                shutil.rmtree(paths.MODS_DIR)
            os.makedirs(paths.MODS_DIR, exist_ok=True)
            if os.path.exists(paths.JDK_DIR):
                shutil.rmtree(paths.JDK_DIR)
            if os.path.exists(paths.MOD_VERSION_FILE):
                os.remove(paths.MOD_VERSION_FILE)
            self.set_status("Repaired. Everything will be re-verified on next launch.")
        except Exception as e:
            messagebox.showerror("Error", f"Repair failed: {e}")

    # ---------------------------------------------------------------- state
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

    def set_status(self, text, color=None):
        self.status_label.config(text=text, fg=color or self.muted_color)
        self.root.update_idletasks()

    def set_progress(self, current, max_val):
        self.progress["value"] = (current / max_val) * 100 if max_val > 0 else 0
        self.root.update_idletasks()

    # ---------------------------------------------------------------- launch
    def start_launch_thread(self):
        username = self.user_entry.get().strip()
        version = self.version_combo.get()
        ram_gb = int(self.ram_scale.get())
        jvm_extra = self.jvm_args_entry.get().strip()

        if not username:
            messagebox.showerror("Error", "Please enter a username!")
            return

        settings.save_settings(username, version, ram_gb, jvm_extra)
        self.disable_controls()
        threading.Thread(
            target=self.launch_game, args=(username, version, ram_gb, jvm_extra), daemon=True
        ).start()

    def launch_game(self, username, version, ram_gb, jvm_extra):
        try:
            paths.ensure_dirs()

            def status_cb(text, is_error=False):
                self.set_status(text, self.error_color if is_error else self.accent_color)

            def progress_cb(cur, total):
                self.set_progress(cur, total)

            # 1. Java: system PATH -> JAVA_HOME -> portable JDK (hash-verified)
            java_exe = java_utils.get_java_executable()
            if not java_exe:
                java_exe = java_utils.install_portable_jdk(progress_cb, status_cb)

            is_prime = "Prime Client" in version
            is_fabric = is_prime or "Fabric" in version
            vanilla_version = "1.20.1" if "1.20.1" in version else "1.16.5"

            # 2. Prime mod: fetch/verify newest release, or clean it up on vanilla
            if is_prime:
                status_cb("Checking Prime Mod updates...")
                mod_path = mod_updater.ensure_latest_mod(paths.MODS_DIR, status_cb, progress_cb)
                if not mod_path:
                    self.enable_controls()
                    return
            else:
                for f in os.listdir(paths.MODS_DIR):
                    if f.startswith("prime-") and f.endswith(".jar"):
                        os.remove(os.path.join(paths.MODS_DIR, f))
                if os.path.exists(paths.MOD_VERSION_FILE):
                    os.remove(paths.MOD_VERSION_FILE)

            # 3. Fabric API + optional performance mods (both hash-verified)
            target_fabric_api = os.path.join(paths.MODS_DIR, os.path.basename(FABRIC_API_URL))
            if is_fabric:
                if not os.path.exists(target_fabric_api):
                    status_cb("Downloading Fabric API dependency...")
                    fabric_sha1 = None
                    try:
                        fabric_sha1 = fetch_text(FABRIC_API_SHA1_URL).strip().split()[0]
                    except Exception:
                        status_cb("Warning: could not fetch Fabric API checksum", True)
                    download_verified(
                        FABRIC_API_URL, target_fabric_api,
                        expected_hash=fabric_sha1, algo="sha1", progress_cb=progress_cb,
                    )
                modrinth.download_optional_mods(paths.MODS_DIR, vanilla_version, status_cb, progress_cb)
            else:
                if os.path.exists(target_fabric_api):
                    os.remove(target_fabric_api)
                
                # Cleanup managed mods
                cache = modrinth.load_mod_cache(paths.MODS_DIR)
                for slug in OPTIONAL_MODS:
                    if slug in cache:
                        for f in cache[slug].get("files", []):
                            try:
                                os.remove(os.path.join(paths.MODS_DIR, f))
                            except OSError:
                                pass
                        del cache[slug]
                modrinth.save_mod_cache(paths.MODS_DIR, cache)

            # 4. Base Minecraft install
            mc_version_json = os.path.join(paths.GAME_DIR, "versions", vanilla_version, f"{vanilla_version}.json")
            if not os.path.exists(mc_version_json):
                status_cb(f"Installing Minecraft {vanilla_version}...")

                def set_progress_mcl(current):
                    self.current_progress = current
                    self.set_progress(self.current_progress, self.max_progress)

                def set_max_mcl(maximum):
                    self.max_progress = maximum
                    self.set_progress(self.current_progress, self.max_progress)

                mcl_callback = {
                    "setStatus": lambda s: status_cb(s),
                    "setProgress": set_progress_mcl,
                    "setMax": set_max_mcl,
                }

                minecraft_launcher_lib.install.install_minecraft_version(
                    vanilla_version, paths.GAME_DIR, callback=mcl_callback
                )
            else:
                status_cb(f"Minecraft {vanilla_version} already installed")

            # 5. Fabric loader
            version_id = vanilla_version
            if is_fabric:
                if not minecraft_launcher_lib.fabric.is_minecraft_version_supported(vanilla_version):
                    status_cb(f"Error: Fabric not supported for {vanilla_version}", True)
                    self.enable_controls()
                    return
                fabric_loader_version = minecraft_launcher_lib.fabric.get_latest_loader_version()
                version_id = f"fabric-loader-{fabric_loader_version}-{vanilla_version}"
                
                fabric_json = os.path.join(paths.GAME_DIR, "versions", version_id, f"{version_id}.json")
                if not os.path.exists(fabric_json):
                    status_cb("Installing Fabric Loader...")
                    
                    # Ensure mcl_callback exists if base MC was already installed
                    def set_progress_mcl(current):
                        self.current_progress = current
                        self.set_progress(self.current_progress, self.max_progress)

                    def set_max_mcl(maximum):
                        self.max_progress = maximum
                        self.set_progress(self.current_progress, self.max_progress)

                    mcl_callback = {
                        "setStatus": lambda s: status_cb(s),
                        "setProgress": set_progress_mcl,
                        "setMax": set_max_mcl,
                    }
                    
                    minecraft_launcher_lib.fabric.install_fabric(
                        vanilla_version, paths.GAME_DIR, loader_version=fabric_loader_version,
                        callback=mcl_callback, java=java_exe,
                    )
                else:
                    status_cb("Fabric Loader already installed")

            # 6. Launch
            self.set_status("Launching Minecraft...", self.ok_color)
            self.progress["value"] = 100

            jvm_arguments = [f"-Xmx{ram_gb}G"]
            if jvm_extra:
                jvm_arguments += jvm_extra.split()

            options = {
                "username": username,
                "uuid": "",
                "token": "",
                "executablePath": java_exe,
                "jvmArguments": jvm_arguments,
            }
            launch_cmd = minecraft_launcher_lib.command.get_minecraft_command(
                version_id, paths.GAME_DIR, options
            )

            self.root.withdraw()
            subprocess.run(launch_cmd)
            self.root.deiconify()

            self.set_status("Ready to Play")
            self.progress["value"] = 0
            self.enable_controls()

        except Exception as e:
            print(f"Launcher Error: {e}")
            self.set_status(f"Error: {str(e)[:60]}", self.error_color)
            self.enable_controls()
            messagebox.showerror("Launcher Error", str(e))
