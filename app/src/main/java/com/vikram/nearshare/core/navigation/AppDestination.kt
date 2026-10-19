package com.vikram.nearshare.core.navigation

/**
 * Destinations available in the app shell.
 *
 * Keep the main navigation destinations separate from task destinations such as
 * creating a listing, which should not appear as a permanent bottom-nav item.
 */
enum class AppDestination(
    val title: String,
    val showInNavigation: Boolean = true
) {
    HOME("NearShare"),
    EXPLORE("Explore"),
    SAVED("Saved"),
    PROFILE("Profile"),
    UPDATES("App updates", showInNavigation = false),
    REQUESTS("My requests", showInNavigation = false),
    MESSAGES("Messages", showInNavigation = false),
    NOTIFICATIONS("Notifications", showInNavigation = false),
    CREATE_LISTING("Create a listing", showInNavigation = false);

    companion object {
        val mainDestinations: List<AppDestination>
            get() = entries.filter(AppDestination::showInNavigation)
    }
}
