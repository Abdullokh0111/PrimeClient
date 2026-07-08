"""Persisted user settings: username, version, RAM, custom JVM args."""
import json
import os

from . import paths

_DEFAULTS = {
    "username": "Player",
    "version": "1.20.1 (Prime Client)",
    "ram_gb": 4,
    "jvm_args": "",
}


def load_settings():
    data = dict(_DEFAULTS)
    if os.path.exists(paths.SETTINGS_FILE):
        try:
            with open(paths.SETTINGS_FILE, "r") as f:
                saved = json.load(f)
            for key, default in _DEFAULTS.items():
                data[key] = saved.get(key, default)
        except Exception as e:
            print(f"Error loading config file: {e}")
    return data


def save_settings(username, version, ram_gb, jvm_args=""):
    paths.ensure_dirs()
    try:
        with open(paths.SETTINGS_FILE, "w") as f:
            json.dump(
                {"username": username, "version": version, "ram_gb": ram_gb, "jvm_args": jvm_args},
                f,
                indent=4,
            )
    except Exception as e:
        print(f"Error saving config file: {e}")
