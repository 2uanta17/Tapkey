package com.tapkey.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.tapkey.app.ui.MainViewModel
import com.tapkey.app.ui.SettingsScreen
import com.tapkey.app.ui.theme.TapkeyTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TapkeyTheme {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }
}
