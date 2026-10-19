package com.vikram.nearshare.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vikram.nearshare.R
import com.vikram.nearshare.core.navigation.AppDestination
import com.vikram.nearshare.ui.screens.CreateListingScreen
import com.vikram.nearshare.ui.screens.ExploreScreen
import com.vikram.nearshare.ui.screens.HomeScreen
import com.vikram.nearshare.ui.screens.ProfileScreen
import com.vikram.nearshare.ui.screens.RequestsScreen
import com.vikram.nearshare.ui.screens.MessagesScreen
import com.vikram.nearshare.ui.screens.NotificationsScreen
import com.vikram.nearshare.ui.screens.SavedScreen
import com.vikram.nearshare.ui.screens.UpdatesScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearShareApp() {
    var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }

    val secondaryDestination = destination == AppDestination.CREATE_LISTING ||
        destination == AppDestination.UPDATES || destination == AppDestination.REQUESTS ||
        destination == AppDestination.MESSAGES || destination == AppDestination.NOTIFICATIONS

    fun goBack() {
        destination = when (destination) {
            AppDestination.CREATE_LISTING -> AppDestination.HOME
            AppDestination.UPDATES, AppDestination.REQUESTS, AppDestination.MESSAGES,
            AppDestination.NOTIFICATIONS -> AppDestination.PROFILE
            else -> AppDestination.HOME
        }
    }

    BackHandler(enabled = secondaryDestination) { goBack() }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (secondaryDestination) {
                        IconButton(onClick = { goBack() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                title = {
                    if (destination == AppDestination.HOME) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.nearshare_icon),
                                contentDescription = "NearShare logo",
                                modifier = Modifier.size(36.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text("NearShare", fontWeight = FontWeight.Bold)
                                Text("Share more. Need less.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        Text(destination.title, fontWeight = FontWeight.SemiBold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            if (destination.showInNavigation) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    AppDestination.mainDestinations.forEach { item ->
                        NavigationBarItem(
                            selected = destination == item,
                            onClick = { destination = item },
                            icon = {
                                Icon(
                                    imageVector = when (item) {
                                        AppDestination.HOME -> Icons.Default.Home
                                        AppDestination.EXPLORE -> Icons.Default.Explore
                                        AppDestination.SAVED -> Icons.Default.FavoriteBorder
                                        AppDestination.PROFILE -> Icons.Default.Person
                                        AppDestination.UPDATES -> Icons.Default.SystemUpdate
                                        AppDestination.REQUESTS -> Icons.Default.Inbox
                                        AppDestination.MESSAGES -> Icons.Default.ChatBubbleOutline
                                        AppDestination.NOTIFICATIONS -> Icons.Default.Notifications
                                        AppDestination.CREATE_LISTING -> Icons.Default.Add
                                    },
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title, maxLines = 1) },
                            alwaysShowLabel = false
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (!secondaryDestination) {
                FloatingActionButton(
                    onClick = { destination = AppDestination.CREATE_LISTING },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create a listing")
                }
            }
        }
    ) { contentPadding ->
        val screenModifier = Modifier.padding(contentPadding)
        when (destination) {
            AppDestination.HOME -> HomeScreen(onExplore = { destination = AppDestination.EXPLORE }, onCreateListing = { destination = AppDestination.CREATE_LISTING }, modifier = screenModifier)
            AppDestination.EXPLORE -> ExploreScreen(modifier = screenModifier)
            AppDestination.SAVED -> SavedScreen(modifier = screenModifier)
            AppDestination.PROFILE -> ProfileScreen(modifier = screenModifier, onOpenUpdates = { destination = AppDestination.UPDATES }, onOpenRequests = { destination = AppDestination.REQUESTS }, onOpenMessages = { destination = AppDestination.MESSAGES }, onOpenNotifications = { destination = AppDestination.NOTIFICATIONS })
            AppDestination.UPDATES -> UpdatesScreen(modifier = screenModifier)
            AppDestination.REQUESTS -> RequestsScreen(modifier = screenModifier)
            AppDestination.MESSAGES -> MessagesScreen(modifier = screenModifier)
            AppDestination.NOTIFICATIONS -> NotificationsScreen(modifier = screenModifier)
            AppDestination.CREATE_LISTING -> CreateListingScreen(modifier = screenModifier, onCreated = { destination = AppDestination.EXPLORE })
        }
    }
}
