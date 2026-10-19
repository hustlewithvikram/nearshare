package com.vikram.nearshare.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.vikram.nearshare.data.auth.AuthRepository
import com.vikram.nearshare.data.auth.AuthUser
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
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
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            Text("Checking your account…", style = MaterialTheme.typography.bodyMedium)
            return@Column
        }

        if (user != null) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text("Your profile", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Manage the details other NearShare members will see.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Account details", style = MaterialTheme.typography.titleMedium)
                    HorizontalDivider()
                    OutlinedTextField(
                        value = displayName,
                        onValueChange = { displayName = it; message = null },
                        label = { Text("Display name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = user!!.email,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                busy = true
                                message = null
                                try {
                                    user = repository.updateDisplayName(displayName)
                                    displayName = user?.displayName.orEmpty()
                                    message = "Profile saved."
                                    messageIsError = false
                                } catch (error: Exception) {
                                    message = error.message ?: "Could not save your profile."
                                    messageIsError = true
                                } finally {
                                    busy = false
                                }
                            }
                        },
                        enabled = !busy && displayName.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (busy) CircularProgressIndicator()
                        else Text("Save changes")
                    }
                }
            }
            StatusMessage(message, messageIsError)
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
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Sign out") }
        } else {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Text(
                if (isSignUp) "Create your account" else "Sign in to NearShare",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                if (isSignUp) "Join your neighborhood and keep your profile in sync."
                else "Sign in to manage your profile and access your account.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isSignUp) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it; message = null },
                    label = { Text("Display name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            OutlinedTextField(
                value = email,
                onValueChange = { email = it; message = null },
                label = { Text("Email address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; message = null },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        message = null
                        try {
                            val result = if (isSignUp) {
                                repository.signUp(email, password, displayName)
                            } else {
                                repository.signIn(email, password)
                            }
                            if (result != null) {
                                user = result
                                displayName = result.displayName
                                email = result.email
                                password = ""
                                message = "You are signed in."
                                messageIsError = false
                            } else {
                                message = "Account created. Check your email to confirm your account, then sign in."
                                messageIsError = false
                                isSignUp = false
                                password = ""
                            }
                        } catch (error: Exception) {
                            message = error.message ?: "Authentication failed. Please try again."
                            messageIsError = true
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy && email.contains("@") && password.length >= 6 &&
                    (!isSignUp || displayName.isNotBlank()),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (busy) CircularProgressIndicator()
                else Text(if (isSignUp) "Create account" else "Sign in")
            }
            StatusMessage(message, messageIsError)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (isSignUp) "Already have an account?" else "New to NearShare?",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = {
                    isSignUp = !isSignUp
                    message = null
                }) {
                    Text(if (isSignUp) "Sign in" else "Create account")
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Your account is managed securely by Supabase. Never share your password with anyone.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StatusMessage(message: String?, isError: Boolean) {
    if (message != null) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}
