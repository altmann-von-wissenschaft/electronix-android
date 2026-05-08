package com.pnzgu.electronix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pnzgu.electronix.ui.AppRoot
import com.pnzgu.electronix.ui.theme.ElectronixAndroidTheme
import com.pnzgu.electronix.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as ElectronixApplication
        setContent {
            val themeMode by app.container.preferences.themeModeFlow.collectAsStateWithLifecycle(
                initialValue = ThemeMode.System,
            )
            ElectronixAndroidTheme(themeMode = themeMode) {
                AppRoot(container = app.container)
            }
        }
    }
}
