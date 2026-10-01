package com.android.moderntiles

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.android.moderntiles.ui.MainViewModel
import com.android.moderntiles.ui.SettingsScreen
import com.android.moderntiles.ui.theme.ModernTilesTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ModernTilesTheme {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}