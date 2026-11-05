"""Optional local-only authoring aid. It never reads a shop DB or sends text out.

Dependencies and the model live in ignored data/localization-tools, not production.
Upstream: Helsinki-NLP OPUS-MT (ru-en); runtime CTranslate2. Generated copy still
requires vocabulary and browser review. Model weights are not shipped with ARIA.
"""
import hashlib
import json
from pathlib import Path
import sys
import subprocess
import urllib.request
import zipfile

root = Path(__file__).resolve().parents[2]
area = root / "data" / "localization-tools"
area.mkdir(parents=True, exist_ok=True)
sys.path.insert(0, str(area / "python"))
import ctranslate2

url = "https://object.pouta.csc.fi/OPUS-MT-models/ru-en/opus-2020-02-26.zip"
archive = area / "opus-ru-en.zip"
if not archive.exists():
    try:
        with urllib.request.urlopen(url, timeout=120) as source, archive.with_suffix(".partial").open("wb") as out:
            while block := source.read(1024 * 1024):
                out.write(block)
    except urllib.error.URLError:
        # Schannel can resolve the publisher's intermediate certificate on Windows.
        # Certificate verification remains enabled; no insecure download fallback.
        subprocess.run(["curl.exe", "--fail", "--location", "--silent", "--show-error", "--max-time", "180", "--output", str(archive.with_suffix(".partial")), url], check=True)
    archive.with_suffix(".partial").rename(archive)
model = area / "opus-source"
with zipfile.ZipFile(archive) as package:
    for entry in package.infolist():
        if not (model / entry.filename).resolve().is_relative_to(model.resolve()):
            raise RuntimeError("Unsafe model archive")
    package.extractall(model)
target = area / "opus-int8"
if not (target / "model.bin").exists():
    ctranslate2.converters.OpusMTConverter(str(model)).convert(str(target), quantization="int8")
(area / "source.json").write_text(json.dumps({"source": url, "sha256": hashlib.file_digest(archive.open("rb"), "sha256").hexdigest(), "runtime": ctranslate2.__version__, "input": "static application copy only", "textUploaded": False}, indent=2), encoding="utf-8")
print("Local translation authoring model ready; no text sent to any service", flush=True)
