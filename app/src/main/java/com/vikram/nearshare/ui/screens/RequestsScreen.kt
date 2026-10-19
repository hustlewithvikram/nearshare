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
import com.vikram.nearshare.data.listings.ListingRepository
import com.vikram.nearshare.data.listings.ListingRequest
import kotlinx.coroutines.launch

@Composable
fun RequestsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val auth = remember(context) { AuthRepository(context.applicationContext) }
    val repository = remember(context) { ListingRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var incoming by remember { mutableStateOf(true) }
    var requests by remember { mutableStateOf<List<ListingRequest>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var refreshKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(incoming, refreshKey) {
        val token = auth.accessToken()
        if (token.isNullOrBlank()) { requests = emptyList(); error = "Sign in from Profile to view your requests."; loading = false }
        else {
            loading = true; error = null
            try { requests = repository.getRequests(token, incoming) }
            catch (e: Exception) { error = e.message ?: "Could not load requests. Check your Supabase setup." }
            finally { loading = false }
        }
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Keep every hand-off organised.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            FilterChip(selected = incoming, onClick = { incoming = true; notice = null }, label = { Text("Received") })
            FilterChip(selected = !incoming, onClick = { incoming = false; notice = null }, label = { Text("Sent") })
            TextButton(onClick = { refreshKey++ }, enabled = !loading) { Icon(Icons.Default.Refresh, contentDescription = "Refresh"); Text("Refresh") }
        }
        notice?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        if (loading) { CircularProgressIndicator(); Text("Loading requests…") }
        else if (error != null) { Text(error!!, color = MaterialTheme.colorScheme.error); OutlinedButton(onClick = { refreshKey++ }) { Text("Try again") } }
        else if (requests.isEmpty()) {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (incoming) "No requests received yet" else "No requests sent yet", style = MaterialTheme.typography.titleMedium)
                Text(if (incoming) "Requests for your listings will appear here." else "Explore listings and send a request to start sharing.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
        } else requests.forEach { item ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(item.listingTitle.ifBlank { "NearShare listing" }, style = MaterialTheme.typography.titleMedium)
                Text("Status: " + item.status.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.labelLarge)
                if (item.message.isNotBlank()) { Text("Message", style = MaterialTheme.typography.labelMedium); Text(item.message) }
                val changeStatus: (String) -> Unit = { status ->
                    val token = auth.accessToken()
                    if (token.isNullOrBlank()) error = "Your session expired. Sign in again."
                    else scope.launch {
                        busyId = item.id; error = null; notice = null
                        try { repository.setRequestStatus(item.id, status, token); notice = when (status) { "accepted" -> "Request accepted."; "declined" -> "Request declined."; "cancelled" -> "Request cancelled."; "completed" -> "Marked as completed."; else -> "Request updated." }; refreshKey++ }
                        catch (e: Exception) { error = e.message ?: "Could not update this request." }
                        finally { busyId = null }
                    }
                }
                when {
                    incoming && item.status == "pending" -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = { changeStatus("accepted") }, enabled = busyId != item.id) { Text("Accept") }; OutlinedButton(onClick = { changeStatus("declined") }, enabled = busyId != item.id) { Text("Decline") } }
                    incoming && item.status == "accepted" -> Button(onClick = { changeStatus("completed") }, enabled = busyId != item.id) { Text("Mark completed") }
                    !incoming && item.status == "pending" -> OutlinedButton(onClick = { changeStatus("cancelled") }, enabled = busyId != item.id) { Text("Cancel request") }
                }
                if (busyId == item.id) CircularProgressIndicator()
            } }
        }
    }
}
