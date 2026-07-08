"""Resolve bundled asset paths for both dev and PyInstaller-built binaries."""
import os
import sys


def get_asset_path(relative_path):
    if hasattr(sys, "_MEIPASS"):
        return os.path.join(sys._MEIPASS, relative_path)
    # dev mode: assets live at the project root, one level above this package
    project_root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    return os.path.join(project_root, relative_path)
