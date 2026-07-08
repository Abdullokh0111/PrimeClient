"""Central configuration for the Prime Client Launcher."""

APP_NAME = "PrimeClient"
APP_AUTHOR = "PrimeClientTeam"
LAUNCHER_VERSION = "2.0.0"

# --------------------------------------------------------------------------
# Mod distribution
# --------------------------------------------------------------------------
# GitHub repo that hosts prime-<version>.jar releases. The launcher no longer
# ships the mod jar embedded in the exe - it is fetched (and hash-checked) at
# runtime, so shipping a mod update no longer requires rebuilding the exe.
#
# For verification to work, every GitHub release should also include a
# `checksums.txt` asset with lines formatted as:
#   <sha256>  <filename>
# one per released file (this is the standard convention used by e.g.
# `sha256sum *.jar > checksums.txt`). If it's missing, the launcher still
# works but warns the user that the jar could not be verified.
MOD_GITHUB_REPO = "PrimeClientTeam/prime-client-mod"  # TODO: point at the real repo
MOD_ASSET_PREFIX = "prime-"

# --------------------------------------------------------------------------
# Fabric API
# --------------------------------------------------------------------------
FABRIC_API_VERSION = "0.92.0+1.20.1"
FABRIC_API_URL = (
    "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/"
    f"{FABRIC_API_VERSION}/fabric-api-{FABRIC_API_VERSION}.jar"
)
# fabricmc's maven publishes a matching .sha1 file next to every artifact -
# use it instead of trusting the jar blindly.
FABRIC_API_SHA1_URL = FABRIC_API_URL + ".sha1"

# --------------------------------------------------------------------------
# Optional performance mods (Modrinth)
# --------------------------------------------------------------------------
OPTIONAL_MODS = ["sodium", "iris", "lithium", "indium", "zoomify", "lambdynamiclights"]

# --------------------------------------------------------------------------
# Portable JDK 17 (Microsoft Build of OpenJDK)
# --------------------------------------------------------------------------
# IMPORTANT: never point at a "latest" / rolling redirect link (e.g. the old
# aka.ms/download-jdk/... URL that has no version pinned to a checksum)
# without a hash to verify against - the file behind that link can change at
# any time server-side, which is exactly the MITM / compromised-CDN scenario
# we're defending against. Pin an exact build below and verify sha256 on
# every download.
#
# Fill in real values from Microsoft's published checksums page before
# shipping: https://learn.microsoft.com/en-us/java/openjdk/download
# (These are placeholders - the launcher will refuse to silently skip
# verification and will surface a loud warning instead if left unset.)
JDK_VERSION = "17.0.13"
JDK_DOWNLOADS = {
    "windows": {
        "url": f"https://aka.ms/download-jdk/microsoft-jdk-{JDK_VERSION}-windows-x64.zip",
        "sha256": None,  # TODO: fill in published sha256
    },
    "macos": {
        "url": f"https://aka.ms/download-jdk/microsoft-jdk-{JDK_VERSION}-macos-x64.tar.gz",
        "sha256": None,  # TODO: fill in published sha256
    },
    "macos_arm64": {
        "url": f"https://aka.ms/download-jdk/microsoft-jdk-{JDK_VERSION}-macos-aarch64.tar.gz",
        "sha256": None,  # TODO: fill in published sha256
    },
    "linux": {
        "url": f"https://aka.ms/download-jdk/microsoft-jdk-{JDK_VERSION}-linux-x64.tar.gz",
        "sha256": None,  # TODO: fill in published sha256
    },
}
