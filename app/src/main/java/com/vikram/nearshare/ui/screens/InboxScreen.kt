package com.vikram.nearshare.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class InboxSection(val label: String) { REQUESTS("Requests"), MESSAGES("Messages"), ACTIVITY("Activity") }

@Composable
fun InboxScreen(modifier: Modifier = Modifier) {
    var selected by rememberSaveable { mutableIntStateOf(0) }
    Column(modifier.fillMaxSize().padding(top = 8.dp)) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Your exchanges", style = MaterialTheme.typography.headlineSmall)
            Text("Requests, conversations and important updates in one place.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InboxSection.entries.forEachIndexed { index, section ->
                FilterChip(
                    selected = selected == index,
                    onClick = { selected = index },
                    label = { Text(section.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = when (section) {
                                InboxSection.REQUESTS -> Icons.Default.SwapHoriz
                                InboxSection.MESSAGES -> Icons.Default.ChatBubbleOutline
                                InboxSection.ACTIVITY -> Icons.Default.Notifications
                            },
                            contentDescription = null
                        )
                    }
                )
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (selected) {
                0 -> RequestsScreen(Modifier.fillMaxSize())
                1 -> MessagesScreen(Modifier.fillMaxSize())
                else -> NotificationsScreen(Modifier.fillMaxSize())
            }
        }
    }
}
