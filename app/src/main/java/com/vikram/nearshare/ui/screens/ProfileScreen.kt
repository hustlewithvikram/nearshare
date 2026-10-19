package com.vikram.nearshare.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.vikram.nearshare.data.auth.AuthRepository
import com.vikram.nearshare.data.auth.AuthUser
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    onOpenUpdates: () -> Unit = {},
    onOpenRequests: () -> Unit = {},
    onOpenMessages: () -> Unit = {},
    onOpenNotifications: () -> Unit = {}
) {
    val context = LocalContext.current
    val repository = remember(context) { AuthRepository(context.applicationContext) }
    val scope = rememberCoroutineScope()
    var user by remember { mutableStateOf<AuthUser?>(null) }
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSignUp by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var messageIsError by remember { mutableStateOf(false) }

    LaunchedEffect(repository) {
        try {
            user = repository.restoreSession()
            displayName = user?.displayName.orEmpty()
            email = user?.email.orEmpty()
        } catch (error: Exception) {
            message = error.message ?: "Could not restore your session."
            messageIsError = true
        } finally {
            loading = false
        }
    }

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (loading) {
            Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        if (user != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)) {
                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.padding(10.dp).height(44.dp), tint = MaterialTheme.colorScheme.primary)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(displayName.ifBlank { "NearShare member" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(user!!.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Community member", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it; message = null },
                            label = { Text("Display name") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(value = user!!.email, onValueChange = {}, readOnly = true, label = { Text("Email address") }, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                scope.launch {
                                    busy = true; message = null
                                    try {
                                        user = repository.updateDisplayName(displayName)
                                        displayName = user?.displayName.orEmpty()
                                        message = "Profile saved."
                                        messageIsError = false
                                    } catch (error: Exception) {
                                        message = error.message ?: "Could not save your profile."
                                        messageIsError = true
                                    } finally { busy = false }
                                }
                            },
                            enabled = !busy && displayName.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (busy) CircularProgressIndicator() else Text("Save profile")
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Your NearShare", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        ProfileAction(Icons.Default.FavoriteBorder, "Requests and exchanges", "Track hand-offs and status", onOpenRequests)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileAction(Icons.Default.ChatBubbleOutline, "Messages", "Continue an accepted exchange", onOpenMessages)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        ProfileAction(Icons.Default.Notifications, "Notifications", "Updates about your activity", onOpenNotifications)
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("App", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        ProfileAction(Icons.Default.SystemUpdate, "App updates", "Check for the latest build", onOpenUpdates)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text("Account security", style = MaterialTheme.typography.bodyLarge)
                                Text("Authentication secured by Supabase", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        busy = true
                        try {
                            repository.signOut()
                            user = null
                            password = ""
                            message = "You have been signed out."
                            messageIsError = false
                        } catch (error: Exception) {
                            message = error.message ?: "Could not sign out."
                            messageIsError = true
                        } finally { busy = false }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Sign out") }
        } else {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(if (isSignUp) "Join your neighborhood" else "Welcome to NearShare", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        if (isSignUp) "Create an account to request items and coordinate exchanges."
                        else "Sign in to manage your profile and keep your exchanges together.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            if (isSignUp) {
                OutlinedTextField(displayName, { displayName = it; message = null }, label = { Text("Display name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
            OutlinedTextField(email, { email = it; message = null }, label = { Text("Email address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(password, { password = it; message = null }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    scope.launch {
                        busy = true; message = null
                        try {
                            val result = if (isSignUp) repository.signUp(email, password, displayName) else repository.signIn(email, password)
                            if (result != null) {
                                user = result; displayName = result.displayName; email = result.email; password = ""
                                message = "You are signed in."; messageIsError = false
                            } else {
                                message = "Account created. Check your email to confirm your account, then sign in."
                                messageIsError = false; isSignUp = false; password = ""
                            }
                        } catch (error: Exception) {
                            message = error.message ?: "Authentication failed. Please try again."
                            messageIsError = true
                        } finally { busy = false }
                    }
                },
                enabled = !busy && email.contains("@") && password.length >= 6 && (!isSignUp || displayName.isNotBlank()),
                modifier = Modifier.fillMaxWidth()
            ) { if (busy) CircularProgressIndicator() else Text(if (isSignUp) "Create account" else "Sign in") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isSignUp) "Already have an account?" else "New to NearShare?", style = MaterialTheme.typography.bodyMedium)
                TextButton(onClick = { isSignUp = !isSignUp; message = null }) { Text(if (isSignUp) "Sign in" else "Create account") }
            }
            Text("Your account is managed securely by Supabase.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        message?.let {
            Text(it, color = if (messageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ProfileAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(10.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.padding(end = 4.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
