package com.vikram.nearshare.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vikram.nearshare.data.update.AppUpdate
import com.vikram.nearshare.data.update.UpdateRepository
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun UpdatesScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember(context) { UpdateRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    val installedCode = remember { repo.installedVersionCode() }
    val installedName = remember { repo.installedVersionName() }
    var latest by remember { mutableStateOf<AppUpdate?>(null) }
    var checking by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var message by remember { mutableStateOf<String?>(null) }
    var failed by remember { mutableStateOf(false) }
    var apk by remember { mutableStateOf<File?>(repo.downloadedApk().takeIf { it.exists() && it.length() > 0 }) }

    fun check() {
        scope.launch {
            checking = true; message = null; failed = false
            try {
                latest = repo.checkForUpdate()
                message = when {
                    latest!!.versionCode > installedCode -> "A newer debug build is available."
                    latest!!.versionCode == installedCode -> "You're using the latest debug build."
                    else -> "The installed version is newer than the published debug build."
                }
            } catch (e: Exception) { message = e.message ?: "Could not check for updates."; failed = true }
            finally { checking = false }
        }
    }
    LaunchedEffect(Unit) { check() }

    Column(modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.SystemUpdate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("App updates", style = MaterialTheme.typography.headlineSmall)
                Text("Get the latest signed NearShare debug build from GitHub.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Installed version", style = MaterialTheme.typography.titleMedium)
                Text(installedName, style = MaterialTheme.typography.bodyLarge)
                Text("Version code: $installedCode", style = MaterialTheme.typography.bodySmall)
                HorizontalDivider()
                Text("Latest debug release", style = MaterialTheme.typography.titleMedium)
                if (checking) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { CircularProgressIndicator(); Text("Checking GitHub Releases…") }
                else if (latest != null) {
                    Text(latest!!.versionName, style = MaterialTheme.typography.bodyLarge)
                    Text("Version code: ${latest!!.versionCode}", style = MaterialTheme.typography.bodySmall)
                    Text(when { latest!!.versionCode > installedCode -> "Update available"; latest!!.versionCode == installedCode -> "Up to date"; else -> "Installed version is newer" },
                        color = if (latest!!.versionCode > installedCode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                } else Text("No release information loaded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                message?.let { Text(it, color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        }
        if (downloading) {
            Text("Downloading update… $progress%")
            LinearProgressIndicator(progress = { progress / 100f }, modifier = Modifier.fillMaxWidth())
        }
        if (apk != null) {
            Button(onClick = {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        message = "Allow NearShare to install unknown apps, then return and tap Install."; failed = false
                    } else {
                        val uri = repo.uriFor(apk!!)
                        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK))
                    }
                } catch (e: ActivityNotFoundException) { message = "No package installer is available."; failed = true }
                catch (e: Exception) { message = e.message ?: "Could not open the installer."; failed = true }
            }, Modifier.fillMaxWidth()) { Text("Install downloaded update") }
            OutlinedButton(onClick = { apk?.delete(); apk = null; message = "Downloaded APK discarded."; failed = false }, Modifier.fillMaxWidth()) {
                Icon(Icons.Default.DeleteOutline, contentDescription = null); Text("Discard download")
            }
        } else {
            Button(onClick = {
                val found = latest ?: return@Button
                scope.launch {
                    downloading = true; progress = 0; message = null; failed = false
                    try { apk = repo.download(found) { progress = it }; message = "Download complete. Tap Install when ready." }
                    catch (e: Exception) { message = e.message ?: "Download failed."; failed = true }
                    finally { downloading = false }
                }
            }, enabled = !checking && !downloading && latest != null && latest!!.versionCode > installedCode, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.CloudDownload, contentDescription = null); Text("Download update")
            }
        }
        OutlinedButton(onClick = { check() }, enabled = !checking && !downloading, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Refresh, contentDescription = null); Text("Check again")
        }
        Text("Downloads come from the official NearShare GitHub debug release. Android will ask you to confirm installation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
