package com.kbridge.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kbridge.ai.ui.KBridgeApp
import com.kbridge.ai.ui.theme.KBridgeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KBridgeTheme {
                KBridgeApp()
            }
        }
    }
}
