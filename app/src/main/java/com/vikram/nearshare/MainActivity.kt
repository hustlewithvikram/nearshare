package com.vikram.nearshare

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.vikram.nearshare.ui.NearShareApp
import com.vikram.nearshare.ui.theme.NearShareTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NearShareTheme { NearShareApp() } }
    }
}
