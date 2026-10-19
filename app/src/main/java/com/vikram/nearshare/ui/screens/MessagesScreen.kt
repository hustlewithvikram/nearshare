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
import com.vikram.nearshare.data.listings.ListingMessage
import com.vikram.nearshare.data.listings.ListingRepository
import com.vikram.nearshare.data.listings.ListingRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessagesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val auth = remember(context) { AuthRepository(context.applicationContext) }
    val repo = remember(context) { ListingRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var requests by remember { mutableStateOf<List<ListingRequest>>(emptyList()) }
    var selectedId by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf<List<ListingMessage>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var sending by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    val selected = requests.firstOrNull { it.id == selectedId }
    LaunchedEffect(refresh) {
        loading = true; error = null
        try {
            val token = auth.accessToken() ?: throw IllegalStateException("Sign in from Profile to view messages.")
            val all = repo.getRequests(token, true) + repo.getRequests(token, false)
            requests = all.filter { it.status == "accepted" || it.status == "completed" }.distinctBy { it.id }
            if (selectedId.isBlank() || requests.none { it.id == selectedId }) selectedId = requests.firstOrNull()?.id.orEmpty()
        } catch (e: Exception) { error = e.message ?: "Could not load conversations." }
        finally { loading = false }
    }
    LaunchedEffect(selectedId, refresh) {
        if (selectedId.isNotBlank()) {
            try { val token = auth.accessToken() ?: return@LaunchedEffect; messages = repo.getMessages(selectedId, token) }
            catch (e: Exception) { error = e.message ?: "Could not load messages." }
        } else messages = emptyList()
    }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Private messages are available for accepted or completed requests.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (requests.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(value = selected?.listingTitle ?: "Choose conversation", onValueChange = {}, readOnly = true, label = { Text("Conversation") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        requests.forEach { item -> DropdownMenuItem(text = { Text(item.listingTitle.ifBlank { "NearShare listing" }) }, onClick = { selectedId = item.id; expanded = false }) }
                    }
                }
            }
            IconButton(onClick = { refresh++ }, enabled = !loading) { Icon(Icons.Default.Refresh, contentDescription = "Refresh") }
        }
        if (loading) CircularProgressIndicator()
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (!loading && requests.isEmpty() && error == null) Text("No conversations yet. Accept a request to start messaging.")
        messages.forEach { message ->
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(12.dp)) {
                Text(if (message.senderId == auth.currentUserId()) "You" else "Other member", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(message.body)
                Text(message.createdAt.replace("T", " ").take(16), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } }
        }
        if (selected != null) {
            OutlinedTextField(value = draft, onValueChange = { if (it.length <= 2000) draft = it }, label = { Text("Write a message") }, supportingText = { Text("${draft.length}/2000") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
            Button(onClick = {
                scope.launch {
                    sending = true; error = null
                    try { val token = auth.accessToken() ?: throw IllegalStateException("Sign in again."); repo.sendMessage(selected.id, draft, token); draft = ""; messages = repo.getMessages(selected.id, token); refresh++ }
                    catch (e: Exception) { error = e.message ?: "Could not send message." }
                    finally { sending = false }
                }
            }, enabled = !sending && draft.isNotBlank()) { if (sending) CircularProgressIndicator() else Text("Send message") }
        }
    }
}