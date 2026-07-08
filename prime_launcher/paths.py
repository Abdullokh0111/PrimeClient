"""Cross-platform filesystem paths for the launcher.

Replaces the old hardcoded ``%APPDATA%`` lookup (Windows-only, crashes on
macOS/Linux where that env var doesn't exist) with ``platformdirs``, which
resolves the correct per-OS location:
  Windows -> %APPDATA%\\PrimeClient
  macOS   -> ~/Library/Application Support/PrimeClient
  Linux   -> ~/.local/share/PrimeClient (respects $XDG_DATA_HOME)
"""
import os

import platformdirs

from .constants import APP_NAME, APP_AUTHOR

GAME_DIR = platformdirs.user_data_dir(APP_NAME, APP_AUTHOR, roaming=True)

SETTINGS_FILE = os.path.join(GAME_DIR, "launcher_settings.json")
MODS_DIR = os.path.join(GAME_DIR, "mods")
JDK_DIR = os.path.join(GAME_DIR, "jdk17")
MOD_VERSION_FILE = os.path.join(GAME_DIR, "prime_mod_version.json")


def ensure_dirs():
    os.makedirs(GAME_DIR, exist_ok=True)
    os.makedirs(MODS_DIR, exist_ok=True)
