package com.vikram.nearshare.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SystemUpdate
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
import com.vikram.nearshare.ui.screens.SavedScreen
import com.vikram.nearshare.ui.screens.UpdatesScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NearShareApp() {
    var destination by rememberSaveable { mutableStateOf(AppDestination.HOME) }

    BackHandler(enabled = destination == AppDestination.CREATE_LISTING || destination == AppDestination.UPDATES) {
        destination = if (destination == AppDestination.UPDATES) AppDestination.PROFILE else AppDestination.HOME
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    if (destination == AppDestination.CREATE_LISTING) {
                        IconButton(onClick = { destination = AppDestination.HOME }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                title = {
                    if (destination == AppDestination.HOME) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(R.drawable.nearshare_icon),
                                contentDescription = "NearShare logo",
                                modifier = Modifier.size(38.dp)
                            )
                            Column {
                                Text("NearShare", fontWeight = FontWeight.Bold)
                                Text(
                                    "Share more. Need less.",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        Text(destination.title, fontWeight = FontWeight.SemiBold)
                    }
                }
            )
        },
        bottomBar = {
            if (destination.showInNavigation) {
                NavigationBar {
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
                                        AppDestination.CREATE_LISTING -> Icons.Default.Add
                                    },
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (destination != AppDestination.CREATE_LISTING) {
                FloatingActionButton(
                    onClick = { destination = AppDestination.CREATE_LISTING },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create a listing")
                }
            }
        }
    ) { contentPadding ->
        val screenModifier = Modifier.padding(contentPadding)
        when (destination) {
            AppDestination.HOME -> HomeScreen(
                onExplore = { destination = AppDestination.EXPLORE },
                onCreateListing = { destination = AppDestination.CREATE_LISTING },
                modifier = screenModifier
            )
            AppDestination.EXPLORE -> ExploreScreen(modifier = screenModifier)
            AppDestination.SAVED -> SavedScreen(modifier = screenModifier)
            AppDestination.PROFILE -> ProfileScreen(modifier = screenModifier, onOpenUpdates = { destination = AppDestination.UPDATES })
            AppDestination.UPDATES -> UpdatesScreen(modifier = screenModifier)
            AppDestination.CREATE_LISTING -> CreateListingScreen(modifier = screenModifier)
        }
    }
}
