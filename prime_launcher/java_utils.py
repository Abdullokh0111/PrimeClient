"""Cross-platform Java discovery and portable JDK installation.

Fixes:
  - old code only ever looked for ``javaw.exe`` / a Windows-shaped path,
    so it silently found nothing on macOS/Linux.
  - now checks system PATH, then JAVA_HOME, then a previously-installed
    portable JDK, in that order, using the right binary name per OS.
  - the portable JDK download itself is hash-verified (sha256) instead of
    trusted blindly - see net.download_verified.
"""
import os
import platform
import shutil
import tarfile
import tempfile
import zipfile

from . import paths
from .constants import JDK_DOWNLOADS
from .net import download_verified


def detect_os_key():
    system = platform.system().lower()
    machine = platform.machine().lower()
    if system == "windows":
        return "windows"
    if system == "darwin":
        return "macos_arm64" if machine in ("arm64", "aarch64") else "macos"
    return "linux"


def _java_binary_names():
    """``javaw`` is a Windows-only, console-less variant; elsewhere it's just ``java``."""
    if platform.system().lower() == "windows":
        return ["javaw.exe", "java.exe"]
    return ["java"]


def get_java_executable():
    """Look for a usable Java runtime, in order:
       1. system PATH
       2. $JAVA_HOME
       3. a portable JDK previously installed by this launcher
    Returns a path to a runnable java(w) binary, or None if nothing was found.
    """
    for name in _java_binary_names():
        found = shutil.which(name)
        if found:
            return found

    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        for name in _java_binary_names():
            candidate = os.path.join(java_home, "bin", name)
            if os.path.exists(candidate):
                return candidate

    for name in _java_binary_names():
        candidate = os.path.join(paths.JDK_DIR, "bin", name)
        if os.path.exists(candidate):
            return candidate

    return None


def install_portable_jdk(progress_callback, status_callback):
    """Download, verify, and extract a portable JDK 17 for the current platform.

    Returns the path to the newly-installed java(w) binary.
    """
    os_key = detect_os_key()
    jdk_info = JDK_DOWNLOADS.get(os_key)
    if not jdk_info:
        raise RuntimeError(f"No portable JDK configured for platform '{os_key}'.")

    url = jdk_info["url"]
    expected_sha256 = jdk_info.get("sha256")
    if not expected_sha256:
        # Fail loud rather than fail silent: an unpinned checksum here is
        # exactly the supply-chain gap this rewrite is meant to close.
        status_callback(
            "Warning: JDK checksum is not pinned in constants.py - "
            "downloading WITHOUT verification!",
            "#FFAA00",
        )

    archive_name = os.path.basename(url)
    archive_path = os.path.join(paths.GAME_DIR, archive_name)

    status_callback("Downloading Java 17 Runtime...", "#00E5FF")
    download_verified(
        url,
        archive_path,
        expected_hash=expected_sha256,
        algo="sha256",
        progress_cb=lambda cur, total: progress_callback(cur, total),
    )

    status_callback("Extracting Java 17...", "#00E5FF")
    with tempfile.TemporaryDirectory(dir=paths.GAME_DIR) as temp_extract:
        if archive_name.endswith(".zip"):
            with zipfile.ZipFile(archive_path, "r") as zf:
                zf.extractall(temp_extract)
        else:
            with tarfile.open(archive_path, "r:gz") as tf:
                tf.extractall(temp_extract)

        subfolders = [
            f for f in os.listdir(temp_extract)
            if os.path.isdir(os.path.join(temp_extract, f))
        ]
        if not subfolders:
            raise RuntimeError("JDK archive did not contain the expected folder structure.")

        src = os.path.join(temp_extract, subfolders[0])
        # macOS JDK builds nest the real JDK under Contents/Home
        mac_home = os.path.join(src, "Contents", "Home")
        if os.path.isdir(mac_home):
            src = mac_home

        if os.path.exists(paths.JDK_DIR):
            shutil.rmtree(paths.JDK_DIR)
        shutil.copytree(src, paths.JDK_DIR)

    os.remove(archive_path)

    for name in _java_binary_names():
        candidate = os.path.join(paths.JDK_DIR, "bin", name)
        if os.path.exists(candidate):
            if platform.system().lower() != "windows":
                os.chmod(candidate, 0o755)
            return candidate

    raise RuntimeError("Portable JDK installed but no java binary was found inside it.")
