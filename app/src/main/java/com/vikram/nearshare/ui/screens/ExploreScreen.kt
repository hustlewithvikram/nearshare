package com.vikram.nearshare.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import coil.compose.AsyncImage
import com.vikram.nearshare.data.listings.Listing
import com.vikram.nearshare.data.listings.ListingRepository
import com.vikram.nearshare.data.auth.AuthRepository
import com.vikram.nearshare.data.location.DeviceLocation
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember(context) { ListingRepository(context.applicationContext) }
    val auth = remember(context) { AuthRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("All") }
    var selectedCategory by remember { mutableStateOf("All categories") }
    var sortNearby by remember { mutableStateOf(false) }
    var userLatitude by remember { mutableStateOf<Double?>(null) }
    var userLongitude by remember { mutableStateOf<Double?>(null) }
    var locationMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val categories = listOf("All categories", "Home & DIY", "Electronics", "Books", "Sports & Outdoors", "Kitchen", "Clothing", "Toys & Games", "Other")
    fun load(query: String = search) {
        scope.launch {
            loading = true; error = null
            try { listings = repository.getListings(query) }
            catch (e: Exception) { error = e.message ?: "Could not load listings." }
            finally { loading = false }
        }
    }
    fun applyLocation() {
        val location = DeviceLocation.lastKnown(context)
        if (location == null) {
            locationMessage = "Location is unavailable. Turn on device location and try again."
            sortNearby = false
        } else {
            userLatitude = location.latitude
            userLongitude = location.longitude
            sortNearby = true
            locationMessage = "Showing items with saved coordinates first, sorted by distance."
        }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) applyLocation()
        else locationMessage = "Location permission was denied. Search and filters still work."
    }
    LaunchedEffect(repository) { load("") }
    val visibleListings = listings.filter { (selectedType == "All" || it.listingType == selectedType) &&
        (selectedCategory == "All categories" || it.category == selectedCategory) }
        .let { items -> if (sortNearby && userLatitude != null && userLongitude != null) items.sortedBy {
            if (it.latitude == null || it.longitude == null) Double.MAX_VALUE
            else DeviceLocation.distanceKm(userLatitude!!, userLongitude!!, it.latitude, it.longitude)
        } else items }
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            OutlinedTextField(value = search, onValueChange = { search = it }, placeholder = { Text("Search items") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, singleLine = true, modifier = Modifier.weight(1f))
            FilledIconButton(onClick = { load() }, enabled = !loading) { Icon(Icons.Default.Search, contentDescription = "Search") }
        }
        Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            listOf("All", "Borrow", "Give away").forEach { option ->
                FilterChip(selected = selectedType == option, onClick = { selectedType = option }, label = { Text(option) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }, modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = { categoryExpanded = true }, modifier = Modifier.menuAnchor().fillMaxWidth()) { Text(selectedCategory, maxLines = 1) }
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    categories.forEach { category -> DropdownMenuItem(text = { Text(category) }, onClick = { selectedCategory = category; categoryExpanded = false }) }
                }
            }
            FilledTonalIconButton(onClick = {
                if (DeviceLocation.hasPermission(context)) applyLocation()
                else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
            }) { Icon(Icons.Default.MyLocation, contentDescription = "Find nearby") }
        }
        locationMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            visibleListings.isEmpty() -> Text(
                if (search.isBlank()) "No listings match these filters yet. Be the first to share something useful." else "No matching listings. Try a different search.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(visibleListings, key = { it.id }) { listing ->
                    val distance = if (sortNearby && userLatitude != null && userLongitude != null && listing.latitude != null && listing.longitude != null)
                        DeviceLocation.distanceKm(userLatitude!!, userLongitude!!, listing.latitude, listing.longitude) else null
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listing.imageUrl?.let { AsyncImage(model = it, contentDescription = listing.title,
                                modifier = Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Crop) }
                            Text(listing.title, style = MaterialTheme.typography.titleLarge)
                            Text("${listing.listingType} · ${listing.category} · ${listing.condition}",
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(listing.description, style = MaterialTheme.typography.bodyMedium)
                            if (auth.currentUserId() != null && auth.currentUserId() != listing.ownerId) {
                                var reportDialog by remember(listing.id) { mutableStateOf(false) }
                                var reportNotice by remember(listing.id) { mutableStateOf<String?>(null) }
                                var reportBusy by remember(listing.id) { mutableStateOf(false) }
                                TextButton(onClick = { reportDialog = true }, enabled = !reportBusy) { Text("Report listing") }
                                reportNotice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                                if (reportDialog) {
                                    var reason by remember(listing.id) { mutableStateOf("Misleading or inaccurate") }
                                    var details by remember(listing.id) { mutableStateOf("") }
                                    var reportError by remember(listing.id) { mutableStateOf<String?>(null) }
                                    AlertDialog(onDismissRequest = { if (!reportBusy) reportDialog = false }, title = { Text("Report listing") },
                                        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text("Choose the reason that best describes this listing.")
                                            listOf("Misleading or inaccurate", "Prohibited item", "Suspicious or scam", "Inappropriate content", "Other").forEach { option ->
                                                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { RadioButton(selected = reason == option, onClick = { reason = option }); Text(option) }
                                            }
                                            OutlinedTextField(value = details, onValueChange = { details = it.take(1000) }, label = { Text("Additional details (optional)") }, minLines = 2)
                                            reportError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                                        } },
                                        confirmButton = { TextButton(onClick = { scope.launch { reportBusy = true; try { repository.reportListing(listing.id, auth.currentUserId() ?: error("Sign in again."), reason, details, auth.accessToken() ?: error("Sign in again.")); reportNotice = "Report submitted for review."; reportDialog = false } catch (e: Exception) { reportError = e.message ?: "Could not submit report." } finally { reportBusy = false } } }, enabled = !reportBusy) { Text(if (reportBusy) "Submitting…" else "Submit report") } },
                                        dismissButton = { TextButton(onClick = { reportDialog = false }, enabled = !reportBusy) { Text("Cancel") } })
                                }
                                var requestDialog by remember(listing.id) { mutableStateOf(false) }
                                OutlinedButton(onClick = { requestDialog = true }, modifier = Modifier.fillMaxWidth()) { Text("Request item") }
                                if (requestDialog) {
                                    var message by remember(listing.id) { mutableStateOf("Hi, is this item still available?") }
                                    var requestError by remember(listing.id) { mutableStateOf<String?>(null) }
                                    var sending by remember(listing.id) { mutableStateOf(false) }
                                    AlertDialog(onDismissRequest = { if (!sending) requestDialog = false }, title = { Text("Request item") },
                                        text = { Column { OutlinedTextField(value = message, onValueChange = { message = it.take(500) }, label = { Text("Message") }); requestError?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
                                        confirmButton = { TextButton(onClick = { scope.launch { sending = true; try { repository.requestListing(listing.id, message, auth.accessToken() ?: error("Sign in again.")); requestDialog = false } catch (e: Exception) { requestError = e.message ?: "Could not send request." } finally { sending = false } } }, enabled = !sending) { Text(if (sending) "Sending…" else "Send request") } },
                                        dismissButton = { TextButton(onClick = { requestDialog = false }, enabled = !sending) { Text("Cancel") } })
                                }
                            }
                            Text(buildString {
                                append(listing.location)
                                if (sortNearby) append(if (distance != null) " · ${String.format(Locale.getDefault(), "%.1f", distance)} km away" else " · distance unavailable")
                            }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
