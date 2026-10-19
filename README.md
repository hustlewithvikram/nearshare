<div align="center">
  <img src="docs/nearshare-banner.svg" alt="NearShare" width="100%" />
  <h1>NearShare</h1>
  <p><strong>Share more. Need less.</strong></p>
  <p>A neighborhood sharing app for borrowing, lending, and giving away useful items nearby.</p>
</div>

## About
NearShare is an Android application built natively with Kotlin and Jetpack Compose. It focuses on making it easier for people in nearby communities to share useful items.

## Tech stack
- Kotlin
- Jetpack Compose and Material 3
- Android Gradle Plugin and Gradle Kotlin DSL

## Application ID
`com.vikram.nearshare`

## Build locally
1. Install Android Studio and JDK 17.
2. Clone this repository and open it in Android Studio.
3. Let Gradle sync, then run the `app` configuration.

Build a debug APK:
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions
- **Build Debug APK** runs on pushes and uploads `nearshare-debug.apk`.
- **Build Release APK** can be started manually and uploads `nearshare-release.apk`. Without signing configuration, this artifact may be unsigned and is not ready for store distribution.
- **Build Play Store APK** can be started manually and requires the Android signing secrets documented in the workflow.

Google Play generally prefers Android App Bundles (AAB) for new apps; this workflow produces the requested APK.

## License
No license has been specified for this repository yet.
