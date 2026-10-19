package com.vikram.nearshare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vikram.nearshare.data.auth.AuthRepository
import com.vikram.nearshare.data.listings.AppNotification
import com.vikram.nearshare.data.listings.ListingRepository

@Composable
fun NotificationsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val auth = remember(context) { AuthRepository(context.applicationContext) }
    val repo = remember(context) { ListingRepository(context.applicationContext) }
    var items by remember { mutableStateOf<List<AppNotification>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    LaunchedEffect(refresh) {
        loading = true; error = null
        try {
            val token = auth.accessToken() ?: throw IllegalStateException("Sign in from Profile to view notifications.")
            items = repo.getNotifications(token)
            repo.markNotificationsRead(token)
        } catch (e: Exception) { error = e.message ?: "Could not load notifications." }
        finally { loading = false }
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Updates about your requests and conversations.", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
            IconButton(onClick = { refresh++ }, enabled = !loading) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") }
        }
        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error); OutlinedButton(onClick = { refresh++ }) { Text("Try again") } }
        if (!loading && error == null && items.isEmpty()) Text("You're all caught up. Notifications will appear here.")
        items.forEach { item ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(item.body)
                Text(item.createdAt.replace("T", " ").take(16), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
        }
    }
}