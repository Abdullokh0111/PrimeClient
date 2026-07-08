"""Cross-platform build script for the Prime Client launcher.

The binary is now a thin stub: the mod jar is fetched at runtime from
GitHub Releases (see prime_launcher/mod_updater.py) instead of being baked
in via --add-data, so a mod update no longer requires rebuilding and
redistributing this binary to every player.

Run this ON each target OS (PyInstaller does not cross-compile):
  Windows -> produces dist/PrimeLauncher.exe
  macOS   -> produces dist/PrimeLauncher (or a .app bundle with --windowed)
  Linux   -> produces dist/PrimeLauncher
"""
import os
import platform
import subprocess
import sys

REQUIRED_PACKAGES = [
    "minecraft-launcher-lib",
    "pillow",
    "platformdirs",
    "psutil",
    "pyinstaller",
]


def install_dependencies():
    print("Installing build dependencies...")
    try:
        subprocess.check_call([sys.executable, "-m", "pip", "install", *REQUIRED_PACKAGES])
    except subprocess.CalledProcessError as e:
        print(f"Error installing dependencies: {e}")
        sys.exit(1)


def _data_separator():
    # PyInstaller --add-data uses ';' on Windows, ':' everywhere else.
    return ";" if platform.system() == "Windows" else ":"


def build_launcher():
    system = platform.system()
    print(f"Building Prime Launcher for {system}...")

    bg_img = "bruh.jpg"
    if not os.path.exists(bg_img):
        print(f"Error: {bg_img} does not exist! Background image is required.")
        sys.exit(1)

    sep = _data_separator()
    cmd = [
        "pyinstaller",
        "--onefile",
        "--name", "PrimeLauncher",
        "--add-data", f"{bg_img}{sep}.",
    ]

    if system == "Windows":
        cmd.append("--noconsole")
        if os.path.exists("assets/icon.ico"):
            cmd += ["--icon", "assets/icon.ico"]
    elif system == "Darwin":
        cmd.append("--windowed")
        if os.path.exists("assets/icon.icns"):
            cmd += ["--icon", "assets/icon.icns"]
    else:
        # Linux: Tk apps run fine without any console/windowed flag equivalent.
        pass

    cmd.append("launcher.py")

    print(f"Executing: {' '.join(cmd)}")
    try:
        subprocess.check_call(cmd)
        out_name = "PrimeLauncher.exe" if system == "Windows" else "PrimeLauncher"
        print("--------------------------------------------------")
        print(f"Success! Binary generated at: dist/{out_name}")
        print("--------------------------------------------------")
    except subprocess.CalledProcessError as e:
        print(f"Error compiling launcher with PyInstaller: {e}")
        sys.exit(1)


if __name__ == "__main__":
    install_dependencies()
    build_launcher()
