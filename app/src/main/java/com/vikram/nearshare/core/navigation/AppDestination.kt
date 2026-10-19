package com.vikram.nearshare.core.navigation

/**
 * Main destinations are the four primary areas. Exchange activity is grouped
 * in Inbox instead of being scattered across separate navigation destinations.
 */
enum class AppDestination(
    val title: String,
    val showInNavigation: Boolean = true
) {
    HOME("Home"),
    EXPLORE("Explore"),
    INBOX("Inbox"),
    SAVED("Saved", showInNavigation = false),
    PROFILE("Profile"),
    UPDATES("App updates", showInNavigation = false),
    REQUESTS("My requests", showInNavigation = false),
    MESSAGES("Messages", showInNavigation = false),
    NOTIFICATIONS("Notifications", showInNavigation = false),
    CREATE_LISTING("Create a listing", showInNavigation = false);

    companion object {
        val mainDestinations: List<AppDestination>
            get() = listOf(HOME, EXPLORE, INBOX, PROFILE)
    }
}
