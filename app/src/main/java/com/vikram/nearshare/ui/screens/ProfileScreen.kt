package com.vikram.nearshare.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vikram.nearshare.ui.components.EmptyState

@Composable
fun ProfileScreen(modifier: Modifier = Modifier) {
    EmptyState(
        title = "Your profile",
        description = "Your account details, listings and sharing history will live here. Account setup will be added in a later phase.",
        modifier = modifier.fillMaxSize(),
        icon = Icons.Default.Person
    )
}
