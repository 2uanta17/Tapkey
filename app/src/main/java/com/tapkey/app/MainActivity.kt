package com.tapkey.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.tapkey.app.ui.MainViewModel
import com.tapkey.app.ui.SettingsScreen
import com.tapkey.app.ui.theme.TapkeyTheme

class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_OPEN_WRITE_SECURE_SETTINGS_SHEET = "extra_open_write_secure_settings_sheet"
    }

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleLaunchIntent(intent)
        enableEdgeToEdge()
        setContent {
            TapkeyTheme {
                SettingsScreen(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    private fun handleLaunchIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_WRITE_SECURE_SETTINGS_SHEET, false) == true) {
            viewModel.openWriteSecureSettingsBottomSheet()
            intent.removeExtra(EXTRA_OPEN_WRITE_SECURE_SETTINGS_SHEET)
        }
    }
}
