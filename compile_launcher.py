import subprocess
import sys
import os

def install_dependencies():
    print("Installing Python dependencies (requests, minecraft-launcher-lib, pyinstaller, pillow)...")
    try:
        # Use pip with the current python interpreter to avoid path conflicts
        subprocess.check_call([sys.executable, "-m", "pip", "install", "requests", "minecraft-launcher-lib", "pyinstaller", "pillow"])
        print("Dependencies installed successfully!")
    except Exception as e:
        print(f"Error installing dependencies: {e}")
        sys.exit(1)

def build_launcher():
    print("Building standalone PrimeLauncher.exe using PyInstaller...")
    
    # Check if mod file exists
    mod_jar = os.path.join("build", "libs", "prime-1.0.0.jar")
    if not os.path.exists(mod_jar):
        print(f"Error: {mod_jar} does not exist! Please compile the mod first using Gradle.")
        sys.exit(1)

    # Check if background image exists
    bg_img = "launcher_bg.png"
    if not os.path.exists(bg_img):
        print(f"Error: {bg_img} does not exist! Background image is required.")
        sys.exit(1)

    pyinstaller_cmd = [
        "pyinstaller",
        "--onefile",
        "--noconsole",
        "--add-data", f"{mod_jar};.",
        "--add-data", f"{bg_img};.",
        "--name", "PrimeLauncher",
        "launcher.py"
    ]

    print(f"Executing: {' '.join(pyinstaller_cmd)}")
    try:
        subprocess.check_call(pyinstaller_cmd)
        print("--------------------------------------------------")
        print("Success! Standalone launcher generated successfully!")
        print("You can find the executable at: dist/PrimeLauncher.exe")
        print("--------------------------------------------------")
    except Exception as e:
        print(f"Error compiling launcher with PyInstaller: {e}")
        sys.exit(1)

if __name__ == "__main__":
    install_dependencies()
    build_launcher()
