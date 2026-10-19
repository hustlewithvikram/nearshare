# NearShare signing setup

The maintainer has generated the shared NearShare signing keystore locally. Do not regenerate it for future builds.

## GitHub Actions secrets

Add the following repository secrets under Settings → Secrets and variables → Actions:
https://github.com/hustlewithvikram/nearshare/settings/secrets/actions

| Secret | Value source |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64 contents of the private keystore file |
| `ANDROID_KEYSTORE_PASSWORD` | Generated keystore password |
| `ANDROID_KEY_ALIAS` | `nearshare` |
| `ANDROID_KEY_PASSWORD` | Same value as the generated keystore password |

Keep the `nearshare-release.jks` file and password in a secure, encrypted offline backup. Never commit the keystore, passwords, or Base64 value to source control.

The debug, release, and Play Store workflows use these same four secrets. They validate that secrets exist, decode the keystore to the runner's temporary directory, build and verify the APK, and remove the temporary keystore.

Android updates require a matching package ID and signing certificate. If Google Play App Signing is enabled, Google may distribute APKs signed with its app-signing key; check Play Console → App integrity before relying on sideloaded builds to update Play-installed builds.
