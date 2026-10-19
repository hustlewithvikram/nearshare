package com.vikram.nearshare.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.vikram.nearshare.data.listings.Listing
import com.vikram.nearshare.data.listings.ListingRepository
import kotlinx.coroutines.launch

@Composable
fun ExploreScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repository = remember(context) { ListingRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var listings by remember { mutableStateOf<List<Listing>>(emptyList()) }
    var search by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("All") }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    fun load(query: String = search) {
        scope.launch {
            loading = true; error = null
            try { listings = repository.getListings(query) }
            catch (e: Exception) { error = e.message ?: "Could not load listings." }
            finally { loading = false }
        }
    }
    LaunchedEffect(repository) { load("") }
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Search items") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("All", "Borrow", "Give away").forEach { option ->
                FilterChip(selected = selectedType == option, onClick = { selectedType = option }, label = { Text(option) })
            }
            OutlinedButton(onClick = { load() }, enabled = !loading) { Text("Search") }
        }
        when {
            loading -> CircularProgressIndicator()
            error != null -> Text(error!!, color = MaterialTheme.colorScheme.error)
            listings.none { selectedType == "All" || it.listingType == selectedType } -> Text(
                if (search.isBlank()) "No listings yet. Be the first to share something useful." else "No matching listings. Try a different search.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(listings.filter { selectedType == "All" || it.listingType == selectedType }, key = { it.id }) { listing ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listing.imageUrl?.let { AsyncImage(model = it, contentDescription = listing.title,
                                modifier = Modifier.fillMaxWidth().height(190.dp), contentScale = ContentScale.Crop) }
                            Text(listing.title, style = MaterialTheme.typography.titleLarge)
                            Text("${listing.listingType} · ${listing.category} · ${listing.condition}",
                                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(listing.description, style = MaterialTheme.typography.bodyMedium)
                            Text(listing.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
