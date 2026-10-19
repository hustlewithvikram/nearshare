package com.vikram.nearshare.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vikram.nearshare.data.auth.AuthRepository
import com.vikram.nearshare.data.listings.ListingRepository
import com.vikram.nearshare.data.location.DeviceLocation
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import java.io.IOException

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateListingScreen(modifier: Modifier = Modifier, onCreated: () -> Unit = {}) {
    val context = LocalContext.current
    val auth = remember(context) { AuthRepository(context.applicationContext) }
    val repo = remember(context) { ListingRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Home & DIY") }
    var type by remember { mutableStateOf("Borrow") }
    var condition by remember { mutableStateOf("Good") }
    var location by remember { mutableStateOf("") }
    var image by remember { mutableStateOf<Uri?>(null) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var locationStatus by remember { mutableStateOf("Optional: attach coordinates to help neighbors find this item.") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var conditionExpanded by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val signedIn = auth.accessToken() != null && auth.currentUserId() != null
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { image = it }
    fun attachLocation() {
        val current = DeviceLocation.lastKnown(context)
        if (current == null) locationStatus = "Location unavailable. Turn on device location and try again."
        else { latitude = current.latitude; longitude = current.longitude; locationStatus = "Approximate device coordinates attached." }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants.values.any { it }) attachLocation() else locationStatus = "Permission denied. You can still publish using your neighborhood or city."
    }
    val categories = listOf("Home & DIY", "Electronics", "Books", "Sports & Outdoors", "Kitchen", "Clothing", "Toys & Games", "Other")
    val conditions = listOf("New", "Like new", "Good", "Fair", "For parts")

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Share something useful", style = MaterialTheme.typography.headlineSmall)
        Text("Add an item for your community to borrow or keep.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!signedIn) {
            Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Sign in to create a listing", style = MaterialTheme.typography.titleMedium)
                Text("Open Profile and sign in to your NearShare account before publishing.")
            } }
        }
        OutlinedTextField(title, { title = it.take(80); errorMessage = null }, label = { Text("Item title") },
            supportingText = { Text("${title.length}/80") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(description, { description = it.take(1000); errorMessage = null }, label = { Text("Description") },
            minLines = 3, modifier = Modifier.fillMaxWidth())
        ExposedDropdownMenuBox(expanded = categoryExpanded, onExpandedChange = { categoryExpanded = it }) {
            OutlinedTextField(category, {}, readOnly = true, label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(categoryExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }) {
                categories.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { category = item; categoryExpanded = false }) }
            }
        }
        Text("How are you sharing it?", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Borrow", "Give away").forEach { option ->
                if (type == option) Button(onClick = { type = option }, modifier = Modifier.weight(1f)) { Text(option) }
                else OutlinedButton(onClick = { type = option }, modifier = Modifier.weight(1f)) { Text(option) }
            }
        }
        ExposedDropdownMenuBox(expanded = conditionExpanded, onExpandedChange = { conditionExpanded = it }) {
            OutlinedTextField(condition, {}, readOnly = true, label = { Text("Item condition") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(conditionExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth())
            ExposedDropdownMenu(expanded = conditionExpanded, onDismissRequest = { conditionExpanded = false }) {
                conditions.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { condition = item; conditionExpanded = false }) }
            }
        }
        OutlinedTextField(location, { location = it.take(120); errorMessage = null }, label = { Text("Neighborhood or city") },
            placeholder = { Text("e.g. Baner, Pune") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedButton(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
            Icon(if (image == null) Icons.Default.AddPhotoAlternate else Icons.Default.Image, contentDescription = null)
            Text(if (image == null) "Choose item photo (optional)" else "Change selected photo")
        }
        if (image != null) Text("Photo selected. Images are limited to 8 MB.", style = MaterialTheme.typography.bodySmall)
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = {
            scope.launch {
                busy = true; errorMessage = null
                try {
                    val token = auth.accessToken() ?: throw IOException("Please sign in from Profile before publishing.")
                    val userId = auth.currentUserId() ?: throw IOException("Could not identify your account. Sign in again.")
                    repo.createListing(userId, token, title, description, category, type, condition, location, image, latitude, longitude)
                    onCreated()
                } catch (e: Exception) { errorMessage = e.message ?: "Could not publish listing." }
                finally { busy = false }
            }
        }, enabled = signedIn && !busy && title.isNotBlank() && description.isNotBlank() && location.isNotBlank(),
            modifier = Modifier.fillMaxWidth()) {
            if (busy) CircularProgressIndicator() else Text("Publish listing")
        }
    }
}
