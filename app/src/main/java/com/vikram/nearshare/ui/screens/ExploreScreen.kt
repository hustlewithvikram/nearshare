package com.vikram.nearshare.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
import com.vikram.nearshare.data.location.DeviceLocation
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember(context) { ListingRepository(context.applicationContext) }
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
        OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Search items") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            listOf("All", "Borrow", "Give away").forEach { option ->
                FilterChip(selected = selectedType == option, onClick = { selectedType = option }, label = { Text(option) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            var categoryExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }, modifier = Modifier.weight(1f)) {
                OutlinedButton(onClick = { categoryExpanded = true }, modifier = Modifier.menuAnchor().fillMaxWidth()) {
                    Text(selectedCategory, maxLines = 1)
                }
                ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                    categories.forEach { category -> DropdownMenuItem(text = { Text(category) }, onClick = { selectedCategory = category; categoryExpanded = false }) }
                }
            }
            OutlinedButton(onClick = { load() }, enabled = !loading) { Text("Search") }
        }
        OutlinedButton(onClick = {
            if (DeviceLocation.hasPermission(context)) applyLocation()
            else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
        }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.MyLocation, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (sortNearby) "Refresh nearby sorting" else "Find nearby")
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
