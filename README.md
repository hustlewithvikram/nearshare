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
- Supabase Auth for email/password accounts
- Encrypted Android preferences for locally persisted auth tokens
- Gradle Kotlin DSL
- Android application ID: `com.vikram.nearshare`

## Architecture
- `core/`: shared app-level concepts such as navigation destinations.
- `data/auth/`: Supabase Auth API and session persistence.
- `ui/components/`: reusable UI components.
- `ui/screens/`: individual screens.
- `ui/theme/`: Material 3 theme configuration.

The app shell provides Home, Explore, Saved, Profile, App Updates and Create Listing destinations. Phase 2 adds email sign-up/sign-in and editable profiles. Phase 4 adds Supabase-backed listings, image uploads, search and listing-type filters. Phase 5 adds item requests and request status management.

## Set up the listings database (Phase 3)
1. Open the Supabase project SQL Editor.
2. Run [`supabase/schema.sql`](supabase/schema.sql) to create the listings table, row-level security policies, and public image bucket policies.
3. The `nearshare-listing-images` bucket is public for reading listing photos; uploads are restricted to the authenticated user’s own folder.

## Configure Supabase
1. Create or open a Supabase project.
2. In **Project Settings → API**, copy the Project URL and the **publishable/anon key**. Never put a service-role key in the app.
3. For local builds, add these to your user-level `~/.gradle/gradle.properties` (or the project's untracked `gradle.properties`):
   ```properties
   SUPABASE_URL=https://YOUR_PROJECT_ID.supabase.co
   SUPABASE_ANON_KEY=YOUR_SUPABASE_PUBLISHABLE_OR_ANON_KEY
   ```
4. In Supabase Authentication settings, configure email confirmation and your allowed sign-in policies. If email confirmation is enabled, a new account may need to confirm the email before signing in.
5. For GitHub Actions builds, add repository secrets named `SUPABASE_URL` and `SUPABASE_ANON_KEY`, then ensure the build workflow passes them to Gradle as environment variables. Without them, the app builds but authentication shows a setup message.

The client uses only the public anon/publishable key. Apply strict authorization policies to any future database tables and never ship a service-role key.

## Build locally
1. Install Android Studio and JDK 17.
2. Clone this repository and open it in Android Studio.
3. Let Gradle sync, then run the `app` configuration.

Build a debug APK:
```bash
sh ./gradlew assembleDebug
``
Output: `app/build/outputs/apk/debug/app-debug.apk`.

## GitHub Actions
- **Build Debug APK** runs on pushes, publishes the latest signed debug prerelease, and uploads a commit-specific artifact.
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


### Nearby discovery (Phase 4)
Listings can optionally store approximate latitude/longitude when the creator grants location permission. Explore supports category and borrow/give-away filters, and sorts listings with coordinates by distance when the user chooses Find nearby. Listings without coordinates remain discoverable by text and filters. Re-run `supabase/schema.sql` to add the optional coordinate columns to an existing project.
