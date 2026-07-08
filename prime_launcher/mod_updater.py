"""Auto-update for the Prime Client mod jar.

The exe used to have ``prime-1.0.0.jar`` baked in via PyInstaller's
``--add-data``, which meant any mod update required rebuilding and
redistributing an entirely new exe to every player. This module replaces
that with: on every launch, ask GitHub for the latest release of
``MOD_GITHUB_REPO``, compare its tag to what's installed locally, and only
download a new jar if one is available. The exe itself stays a thin,
rarely-changing stub.

If GitHub is unreachable and a jar is already installed, the launcher plays
offline with whatever's on disk instead of blocking the user.
"""
import json
import os

from . import paths
from .constants import MOD_ASSET_PREFIX, MOD_GITHUB_REPO
from .net import DownloadError, download_verified, fetch_text


def _read_local_tag():
    if not os.path.exists(paths.MOD_VERSION_FILE):
        return None
    try:
        with open(paths.MOD_VERSION_FILE, "r") as f:
            return json.load(f).get("tag")
    except Exception:
        return None


def _write_local_tag(tag):
    with open(paths.MOD_VERSION_FILE, "w") as f:
        json.dump({"tag": tag}, f)


def _find_checksum(release, asset_name):
    """Look for a ``checksums.txt`` release asset (``<sha256>  <filename>``
    per line, the standard ``sha256sum`` output format) and return the hash
    matching ``asset_name`` if present.
    """
    checksum_asset = next(
        (a for a in release.get("assets", []) if a["name"].lower() == "checksums.txt"),
        None,
    )
    if not checksum_asset:
        return None
    try:
        text = fetch_text(checksum_asset["browser_download_url"])
    except Exception:
        return None
    for line in text.splitlines():
        parts = line.split()
        if len(parts) == 2 and parts[1].strip("*") == asset_name:
            return parts[0]
    return None


def get_latest_release():
    """Returns the parsed GitHub 'latest release' JSON, or None if unreachable."""
    url = f"https://api.github.com/repos/{MOD_GITHUB_REPO}/releases/latest"
    try:
        return json.loads(fetch_text(url))
    except Exception as e:
        print(f"GitHub release check failed: {e}")
        return None


def ensure_latest_mod(mods_dir, status_cb, progress_cb):
    """Make sure the newest known ``prime-*.jar`` is installed in ``mods_dir``.

    Returns the path to the installed jar, or None if nothing could be installed.
    """
    installed_jars = [
        f for f in os.listdir(mods_dir)
        if f.startswith(MOD_ASSET_PREFIX) and f.endswith(".jar")
    ]
    local_tag = _read_local_tag()

    release = get_latest_release()
    if release is None:
        if installed_jars:
            status_cb(f"Offline: using installed {installed_jars[0]}")
            return os.path.join(mods_dir, installed_jars[0])
        status_cb("Error: cannot reach mod update server and no mod is installed", True)
        return None

    remote_tag = release.get("tag_name", "")
    asset = next(
        (
            a for a in release.get("assets", [])
            if a["name"].startswith(MOD_ASSET_PREFIX) and a["name"].endswith(".jar")
        ),
        None,
    )
    if not asset:
        status_cb("Error: latest release has no mod jar asset", True)
        return os.path.join(mods_dir, installed_jars[0]) if installed_jars else None

    up_to_date = local_tag == remote_tag and asset["name"] in installed_jars
    if up_to_date:
        return os.path.join(mods_dir, asset["name"])

    status_cb(f"Downloading Prime Mod {remote_tag}...")
    sha256 = _find_checksum(release, asset["name"])
    if not sha256:
        status_cb("Warning: release has no checksums.txt, mod jar unverified", True)

    for old in installed_jars:
        try:
            os.remove(os.path.join(mods_dir, old))
        except OSError:
            pass

    target_path = os.path.join(mods_dir, asset["name"])
    try:
        download_verified(
            asset["browser_download_url"],
            target_path,
            expected_hash=sha256,
            algo="sha256",
            progress_cb=progress_cb,
        )
    except DownloadError as e:
        status_cb(f"Error downloading mod: {e}", True)
        return None

    _write_local_tag(remote_tag)
    return target_path
