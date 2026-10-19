<div align="center">
  <img src="docs/nearshare-banner.svg" alt="NearShare app banner" width="100%" />
  <h1>NearShare</h1>
  <p><strong>Share more. Need less.</strong></p>
  <p>A neighborhood sharing app for borrowing, lending, and giving away useful items nearby.</p>
</div>

## About
NearShare is a native Android application built with Kotlin, Jetpack Compose and Material 3. The goal is to help neighbors share useful items instead of buying everything themselves.

## Tech stack
- Kotlin and Jetpack Compose
- Material 3
- Gradle Kotlin DSL
- Android application ID: `com.vikram.nearshare`

## Architecture
The app is being built incrementally with a clear separation between:
- `core/`: shared app-level concepts such as navigation destinations.
- `ui/components/`: reusable UI components such as empty states.
- `ui/screens/`: individual screens, kept separate from the app shell.
- `ui/theme/`: Material 3 color and theme configuration.

The app shell currently provides Home, Explore, Saved, Profile and Create Listing destinations. Listing data, accounts, persistence, location discovery and messaging are not wired up yet; these will be added in later phases rather than simulated with fake data.

## Build locally
1. Install Android Studio and JDK 17.
2. Clone this repository and open it in Android Studio.
3. Let Gradle sync, then run the `app` configuration.

Build a debug APK:
```bash
sh ./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions
- **Build Debug APK** runs on pushes, publishes the latest signed debug APK in the [latest debug prerelease](https://github.com/hustlewithvikram/nearshare/releases/tag/latest-debug), and uploads a commit-specific artifact.
- **Build Release APK** can be started manually.
- **Build Play Store APK** can be started manually.

Google Play generally prefers Android App Bundles (AAB) for new apps. The current Play Store workflow is configured to produce the requested APK.

## Development roadmap
1. App foundation, navigation and reusable UI states.
2. Authentication and profiles.
3. Listings and image upload.
4. Nearby discovery, search and filters.
5. Borrow/giveaway requests and exchange status.
6. Notifications, messaging and safety.
7. Security, testing and release readiness.

## License
No license has been specified for this repository yet.
