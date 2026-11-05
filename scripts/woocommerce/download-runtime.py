"""Download an actual, isolated Windows WooCommerce runtime from official publishers.

No system installation, services, PATH, production database or host trust changes.
All binaries and generated secrets stay in the ignored data/woocommerce directory.
"""
import concurrent.futures
import hashlib
import json
from pathlib import Path
import urllib.request
import zipfile

ROOT = Path(__file__).resolve().parents[2]
DATA = ROOT / "data" / "woocommerce"
DATA.mkdir(parents=True, exist_ok=True)


def get(url):
    request = urllib.request.Request(url, headers={"User-Agent": "ARIA-isolated-WooCommerce-E2E"})
    return urllib.request.urlopen(request, timeout=90)


def download(item):
    name, url, expected, destination = item
    archive = DATA / (name + ".zip")
    marker = DATA / (name + ".installed.json")
    if marker.exists():
        saved = json.loads(marker.read_text(encoding="utf-8"))
        if saved["url"] == url and destination.exists():
            return saved
        raise RuntimeError("Existing runtime differs; preserved it. Use a separate checkout for another version.")
    if not archive.exists():
        with get(url) as response, archive.with_suffix(".partial").open("wb") as out:
            while block := response.read(1024 * 1024):
                out.write(block)
        archive.with_suffix(".partial").rename(archive)
    digest = hashlib.file_digest(archive.open("rb"), "sha256").hexdigest()
    if expected and digest != expected:
        raise RuntimeError(name + " publisher SHA256 does not match; archive not executed")
    with zipfile.ZipFile(archive) as source:
        if sum(entry.file_size for entry in source.infolist()) > 2_000_000_000:
            raise RuntimeError("Unexpected archive size")
        for entry in source.infolist():
            if not (destination / entry.filename).resolve().is_relative_to(destination.resolve()):
                raise RuntimeError("Unsafe archive path")
        source.extractall(destination)
    result = {"name": name, "url": url, "sha256": digest, "publisherHashVerified": bool(expected)}
    marker.write_text(json.dumps(result, indent=2), encoding="utf-8")
    print(name + " installed (official publisher, isolated files)", flush=True)
    return result


# Fixed versions: repeating the check uses exactly the same installed software.
assets = [
    ("php", "https://windows.php.net/downloads/releases/php-8.4.27-1-nts-Win32-vs17-x64.zip",
     "dbae154a0a89791f36c3d48e68bae797878333e7d6cb5721e74617de59b63ab7", DATA / "php"),
    ("mariadb", "https://archive.mariadb.org/mariadb-11.8.6/winx64-packages/mariadb-11.8.6-winx64.zip", None, DATA / "mariadb"),
    ("wordpress", "https://downloads.wordpress.org/release/wordpress-7.1.4.zip", None, DATA / "site"),
    ("woocommerce", "https://downloads.wordpress.org/plugin/woocommerce.11.2.2.zip", None,
     DATA / "site" / "wordpress" / "wp-content" / "plugins"),
]
# WordPress and plugin extraction touch the same tree, so install WordPress first.
with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
    results = list(pool.map(download, assets[:3]))
results.append(download(assets[3]))
(DATA / "runtime.json").write_text(json.dumps(results, indent=2), encoding="utf-8")
print("Runtime ready; no global configuration changed.", flush=True)
