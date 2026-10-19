package com.vikram.nearshare.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vikram.nearshare.ui.components.EmptyState

@Composable
fun SavedScreen(modifier: Modifier = Modifier) {
    EmptyState(
        title = "Nothing saved yet",
        description = "When you save a listing, you will find it here for easy access.",
        modifier = modifier.fillMaxSize(),
        icon = Icons.Outlined.FavoriteBorder
    )
}
