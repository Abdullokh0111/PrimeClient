"""Modrinth API client.

Fixes:
  - the old code took ``file_info["url"]`` and downloaded it with no
    integrity check at all. Modrinth's API returns a ``sha1`` hash for
    every file in the version manifest - we now fetch and verify it.
  - the old per-mod download loop built a ``reporthook`` closure inside
    ``for slug in opt_mods: ...`` without binding ``slug`` to the current
    iteration. It happened to "work" only because downloads ran serially;
    the moment this loop is parallelized (threads/asyncio), every
    in-flight closure would end up reading whatever ``slug`` the loop
    variable had *by the time the callback fires*, not the value it had
    when the closure was created. Fixed below by binding both ``slug`` and
    ``target_path`` as default arguments, which freezes their value at
    closure-creation time.
"""
import json
import os

from .constants import OPTIONAL_MODS
from .net import download_verified, fetch_text


def get_modrinth_version_info(project_slug, mc_version):
    """Return the full version JSON data for the newest Fabric build
    of ``project_slug`` matching ``mc_version``, or None.
    """
    url = f"https://api.modrinth.com/v2/project/{project_slug}/version"
    try:
        data = json.loads(fetch_text(url))
    except Exception as e:
        print(f"Modrinth API error for {project_slug}: {e}")
        return None

    for v in data:
        if mc_version in v.get("game_versions", []) and "fabric" in v.get("loaders", []):
            return v

    return None


def get_modrinth_file_info(project_slug, mc_version):
    """Return ``(download_url, filename, sha1, version_id, dependencies)`` for the newest Fabric build."""
    v = get_modrinth_version_info(project_slug, mc_version)
    if not v:
        return None, None, None, None, []
    
    files = v.get("files", [])
    if not files:
        return None, None, None, None, []
    
    file_info = files[0]
    sha1 = file_info.get("hashes", {}).get("sha1")
    deps = [d for d in v.get("dependencies", []) if d.get("dependency_type") == "required"]
    return file_info["url"], file_info["filename"], sha1, v.get("id"), deps

def _get_all_required_files(project_slug, mc_version, _visited=None):
    if _visited is None:
        _visited = set()
    
    if project_slug in _visited:
        return []
    _visited.add(project_slug)
    
    url, filename, sha1, version_id, deps = get_modrinth_file_info(project_slug, mc_version)
    if not url:
        return []
        
    results = [(project_slug, url, filename, sha1, version_id)]
    
    for dep in deps:
        if "project_id" in dep and dep["project_id"]:
            results.extend(_get_all_required_files(dep["project_id"], mc_version, _visited))
            
    return results

def load_mod_cache(mods_dir):
    cache_file = os.path.join(mods_dir, "installed_mods.json")
    if os.path.exists(cache_file):
        try:
            with open(cache_file, "r") as f:
                return json.load(f)
        except Exception:
            pass
    return {}

def save_mod_cache(mods_dir, cache):
    cache_file = os.path.join(mods_dir, "installed_mods.json")
    try:
        with open(cache_file, "w") as f:
            json.dump(cache, f)
    except Exception as e:
        print(f"Error saving mod cache: {e}")


def download_optional_mods(mods_dir, mc_version, status_cb, progress_cb):
    """Download every configured optional mod, including their dependencies."""
    cache = load_mod_cache(mods_dir)
    
    for slug in OPTIONAL_MODS:
        status_cb(f"Fetching {slug} on Modrinth...")
        all_files = _get_all_required_files(slug, mc_version)
        if not all_files:
            continue
            
        main_mod = all_files[0]
        version_id = main_mod[4]
        
        # Check cache
        if slug in cache and cache[slug].get("version_id") == version_id:
            # Check if all files actually exist
            all_exist = True
            for f in cache[slug].get("files", []):
                if not os.path.exists(os.path.join(mods_dir, f)):
                    all_exist = False
                    break
            if all_exist:
                continue
                
        downloaded_files = []
        for p_slug, url, filename, sha1, v_id in all_files:
            if not sha1:
                status_cb(f"Warning: no checksum published for {p_slug}, skipping for safety", True)
                continue
                
            status_cb(f"Downloading {p_slug}...")
            target_path = os.path.join(mods_dir, filename)
            
            def _progress(cur, total, _slug=p_slug, _target=target_path):
                progress_cb(cur, total)
                
            try:
                download_verified(url, target_path, expected_hash=sha1, algo="sha1", progress_cb=_progress)
                downloaded_files.append(filename)
            except Exception as e:
                status_cb(f"Error downloading {p_slug}: {e}", True)
                
        if downloaded_files:
            cache[slug] = {
                "version_id": version_id,
                "files": downloaded_files
            }
            save_mod_cache(mods_dir, cache)
