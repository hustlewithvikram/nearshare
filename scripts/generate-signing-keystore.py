#!/usr/bin/env python3
"""Generate a local NearShare signing keystore and GitHub Actions secret files.

All generated sensitive files are written to .nearshare-signing/ (gitignored).
Never upload that directory or commit its contents.
"""
from __future__ import annotations

import base64
import os
import secrets
import shutil
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / ".nearshare-signing"
KEYSTORE = OUTPUT / "nearshare-release.jks"
ALIAS = "nearshare"


def secure_write(path: Path, content: str) -> None:
    path.write_text(content, encoding="utf-8")
    try:
        path.chmod(0o600)
    except OSError:
        # Windows permissions differ; the directory is still gitignored.
        pass


def main() -> int:
    if shutil.which("keytool") is None:
        print("ERROR: keytool was not found. Install/configure JDK 17+ and try again.", file=sys.stderr)
        return 1
    if OUTPUT.exists() and any(OUTPUT.iterdir()):
        print(
            f"ERROR: {OUTPUT.relative_to(ROOT)} already exists and is not empty. "
            "Refusing to overwrite existing signing material.",
            file=sys.stderr,
        )
        return 1

    OUTPUT.mkdir(mode=0o700, parents=True, exist_ok=True)
    try:
        password = secrets.token_urlsafe(36)
        command = [
            "keytool", "-genkeypair",
            "-alias", ALIAS,
            "-keyalg", "RSA",
            "-keysize", "4096",
            "-validity", "10000",
            "-keystore", str(KEYSTORE),
            "-storetype", "JKS",
            "-storepass", password,
            "-keypass", password,
            "-dname", "CN=NearShare, OU=Android, O=NearShare, L=Pune, ST=Maharashtra, C=IN",
        ]
        subprocess.run(command, check=True, stdout=subprocess.DEVNULL)
        keystore_bytes = KEYSTORE.read_bytes()

        secure_write(OUTPUT / "ANDROID_KEYSTORE_BASE64.txt",
                     base64.b64encode(keystore_bytes).decode("ascii"))
        secure_write(OUTPUT / "ANDROID_KEYSTORE_PASSWORD.txt", password)
        secure_write(OUTPUT / "ANDROID_KEY_ALIAS.txt", ALIAS)
        secure_write(OUTPUT / "ANDROID_KEY_PASSWORD.txt", password)
        secure_write(OUTPUT / "KEEP-PRIVATE.txt",
                     "Private signing material. Never commit, upload, or share this directory. "
                     "Keep an encrypted offline backup of nearshare-release.jks and its password.\n")
        print("NearShare signing material created successfully.")
        print(f"Private files: {OUTPUT.relative_to(ROOT)}")
        print("Add the contents of the four ANDROID_*.txt files as GitHub Actions repository secrets.")
        print("Passwords and the Base64 keystore value were not printed.")
        print("Back up the keystore and password securely before relying on this key.")
        return 0
    except subprocess.CalledProcessError as exc:
        print(f"ERROR: keytool failed with exit code {exc.returncode}.", file=sys.stderr)
        return exc.returncode or 1
    except Exception:
        print("ERROR: setup failed. Check JDK/keytool and local folder permissions.", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
