"""Download helpers with mandatory hash verification.

This is the fix for the "downloads without checking hash/signature" issue:
every file the launcher pulls down (Fabric API, JDK, Modrinth mods, the
Prime mod jar) should go through ``download_verified`` with the checksum the
source publishes, instead of being trusted blindly.
"""
import hashlib
import os
import tempfile
import urllib.error
import urllib.request

USER_AGENT = "PrimeClientLauncher/2.0"


class VerificationError(Exception):
    """Raised when a downloaded file does not match its expected hash."""


class DownloadError(Exception):
    """Raised when a download fails (network) after all retries."""


def _hash_file(path, algo):
    h = hashlib.new(algo)
    with open(path, "rb") as f:
        for chunk in iter(lambda: f.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def _safe_remove(path):
    try:
        if os.path.exists(path):
            os.remove(path)
    except OSError:
        pass


def download_verified(url, dest_path, expected_hash=None, algo="sha1",
                       progress_cb=None, retries=3, user_agent=USER_AGENT):
    """Download ``url`` to ``dest_path``, verifying its checksum first.

    The file is downloaded to a temp file in the same directory and only
    moved into place (``os.replace``, atomic on both Windows and POSIX)
    once the hash has been confirmed - so a half-written or tampered
    download is never left sitting at ``dest_path``.

    If ``expected_hash`` is ``None`` the download is NOT verified. Callers
    should only do this when no checksum is available at all, and should
    make sure the user sees a warning when that happens (see callers in
    ``java_utils.py`` / ``modrinth.py``) - silently skipping verification
    defeats the whole point.

    A hash mismatch is treated as a possible tampering event, not a
    transient error, so it is NOT retried.
    """
    dest_dir = os.path.dirname(dest_path) or "."
    os.makedirs(dest_dir, exist_ok=True)

    last_error = None
    for _attempt in range(1, retries + 1):
        tmp_fd, tmp_path = tempfile.mkstemp(dir=dest_dir)
        os.close(tmp_fd)
        try:
            req = urllib.request.Request(url, headers={"User-Agent": user_agent})
            with urllib.request.urlopen(req, timeout=30) as response, open(tmp_path, "wb") as out:
                total = int(response.headers.get("Content-Length", 0) or 0)
                downloaded = 0
                block_size = 65536
                while True:
                    chunk = response.read(block_size)
                    if not chunk:
                        break
                    out.write(chunk)
                    downloaded += len(chunk)
                    if progress_cb:
                        progress_cb(downloaded, total)

            if expected_hash:
                actual = _hash_file(tmp_path, algo)
                if actual.lower() != expected_hash.lower():
                    _safe_remove(tmp_path)
                    raise VerificationError(
                        f"Hash mismatch for {os.path.basename(dest_path)}: "
                        f"expected {expected_hash}, got {actual}. "
                        "Refusing to install - the download may be corrupted or tampered with."
                    )

            os.replace(tmp_path, dest_path)
            return dest_path

        except VerificationError:
            raise
        except (urllib.error.URLError, OSError, TimeoutError) as e:
            last_error = e
            _safe_remove(tmp_path)
            continue

    raise DownloadError(f"Failed to download {url} after {retries} attempts: {last_error}")


def fetch_text(url, user_agent=USER_AGENT, timeout=15):
    req = urllib.request.Request(url, headers={"User-Agent": user_agent})
    with urllib.request.urlopen(req, timeout=timeout) as response:
        return response.read().decode("utf-8")
