package com.vikram.nearshare.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vikram.nearshare.R
import com.vikram.nearshare.core.navigation.AppDestination
import com.vikram.nearshare.ui.screens.CreateListingScreen
import com.vikram.nearshare.ui.screens.ExploreScreen
import com.vikram.nearshare.ui.screens.HomeScreen
import com.vikram.nearshare.ui.screens.ProfileScreen
import com.vikram.nearshare.ui.screens.SavedScreen
import com.vikram.nearshare.ui.screens.UpdatesScreen
import com.vikram.nearshare.ui.screens.InboxScreen
import com.vikram.nearshare.ui.screens.RequestsScreen
import com.vikram.nearshare.ui.screens.MessagesScreen
import com.vikram.nearshare.ui.screens.NotificationsScreen

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
                    if (secondaryDestination) IconButton(onClick = { goBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    if (destination == AppDestination.HOME) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            androidx.compose.foundation.Image(painterResource(R.drawable.nearshare_icon), contentDescription = "NearShare logo", modifier = Modifier.size(34.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text("NearShare", fontWeight = FontWeight.Bold)
                                Text("Share more. Need less.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else Text(destination.title, fontWeight = FontWeight.SemiBold)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        bottomBar = {
            if (destination.showInNavigation) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 10.dp).shadow(8.dp, RoundedCornerShape(36.dp)),
                    shape = RoundedCornerShape(36.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 3.dp
                ) {
                    Row(Modifier.fillMaxWidth().padding(5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                        AppDestination.mainDestinations.forEach { item ->
                            val selected = destination == item
                            Surface(
                                modifier = Modifier.weight(1f).animateContentSize().clickable { destination = item },
                                shape = RoundedCornerShape(28.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = if (selected) 14.dp else 10.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (selected) {
                                        Text(item.title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    } else {
                                        Icon(
                                            imageVector = when (item) {
                                                AppDestination.HOME -> Icons.Default.Home
                                                AppDestination.EXPLORE -> Icons.Default.Explore
                                                AppDestination.INBOX -> Icons.Default.Inbox
                                                AppDestination.PROFILE -> Icons.Default.Person
                                                else -> Icons.Default.Home
                                            },
                                            contentDescription = item.title,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            if (!secondaryDestination && destination != AppDestination.INBOX) {
                FloatingActionButton(
                    onClick = { destination = AppDestination.CREATE_LISTING },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) { Icon(Icons.Default.Add, contentDescription = "Create a listing") }
            }
        }
    ) { contentPadding ->
        val screenModifier = Modifier.padding(contentPadding)
        when (destination) {
            AppDestination.HOME -> HomeScreen(onExplore = { destination = AppDestination.EXPLORE }, onCreateListing = { destination = AppDestination.CREATE_LISTING }, modifier = screenModifier)
            AppDestination.EXPLORE -> ExploreScreen(modifier = screenModifier)
            AppDestination.INBOX -> InboxScreen(modifier = screenModifier)
            AppDestination.SAVED -> SavedScreen(modifier = screenModifier)
            AppDestination.PROFILE -> ProfileScreen(modifier = screenModifier, onOpenUpdates = { destination = AppDestination.UPDATES }, onOpenRequests = { destination = AppDestination.INBOX }, onOpenMessages = { destination = AppDestination.INBOX }, onOpenNotifications = { destination = AppDestination.INBOX })
            AppDestination.UPDATES -> UpdatesScreen(modifier = screenModifier)
            AppDestination.REQUESTS -> RequestsScreen(modifier = screenModifier)
            AppDestination.MESSAGES -> MessagesScreen(modifier = screenModifier)
            AppDestination.NOTIFICATIONS -> NotificationsScreen(modifier = screenModifier)
            AppDestination.CREATE_LISTING -> CreateListingScreen(modifier = screenModifier, onCreated = { destination = AppDestination.EXPLORE })
        }
    }
}
