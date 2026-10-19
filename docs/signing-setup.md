# NearShare signing setup

The three GitHub Actions workflows use one Android signing identity. Generate it once on a trusted computer and keep the private keystore and password backed up securely.

## Requirements

- Python 3.9 or newer
- JDK 17 or newer with `keytool` on PATH

## Generate the keystore and secret values

From the repository root, run:

```bash
python scripts/generate-signing-keystore.py
```

The script creates `.nearshare-signing/` locally. It generates the `nearshare` alias and a strong random password shared by the keystore and key. It refuses to overwrite a non-empty output directory.

The folder contains the keystore plus four text files whose contents map to the repository secrets below. Open each text file locally and copy its contents directly into the matching GitHub Actions secret. Do not paste passwords or the keystore contents into chat, issues, pull requests, or source files.

| GitHub Actions secret | Local file |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | `.nearshare-signing/ANDROID_KEYSTORE_BASE64.txt` |
| `ANDROID_KEYSTORE_PASSWORD` | `.nearshare-signing/ANDROID_KEYSTORE_PASSWORD.txt` |
| `ANDROID_KEY_ALIAS` | `.nearshare-signing/ANDROID_KEY_ALIAS.txt` |
| `ANDROID_KEY_PASSWORD` | `.nearshare-signing/ANDROID_KEY_PASSWORD.txt` |

Add them under **Settings → Secrets and variables → Actions → New repository secret**:
https://github.com/hustlewithvikram/nearshare/settings/secrets/actions

After all four secrets exist, run each workflow from the Actions tab. The workflows validate that the secrets exist, decode the keystore into the runner's temporary directory, build and verify the APK signature, and remove the temporary keystore at the end.

## Protect the signing key

- The entire `.nearshare-signing/` directory is gitignored. Never force-add it.
- Keep an encrypted offline backup of `nearshare-release.jks` and the password in a separate secure location. If you lose this key, future updates may be blocked.
- Do not generate a new key for subsequent builds. Reuse this exact keystore.
- Android only permits in-place updates when package IDs and signing certificates match. NearShare's shared debug/release builds use `com.vikram.nearshare`.
- If Google Play App Signing is enabled, Google may distribute APKs signed with a separate app-signing key. Check Play Console → App integrity before deciding how sideloaded builds will update Play-installed builds.
