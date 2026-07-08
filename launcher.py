"""Entry point for the Prime Client Launcher.

Kept intentionally thin: all logic lives in prime_launcher/. The mod jar,
Fabric API, optional mods, and portable JDK are fetched (and hash-verified)
at runtime from prime_launcher/, so this stub rarely needs to be rebuilt.
"""
import tkinter as tk

from prime_launcher.gui import LauncherGUI

if __name__ == "__main__":
    root = tk.Tk()
    app = LauncherGUI(root)
    root.mainloop()
