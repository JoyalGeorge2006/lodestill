package dev.joyal.lodestill

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.joyal.lodestill.ui.nav.HfNav
import dev.joyal.lodestill.ui.theme.LodestillTheme

class MainActivity : ComponentActivity() {
    private var vm: AppViewModel? = null
    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        enableEdgeToEdge()
        setContent {
            val v: AppViewModel = viewModel(); vm = v
            val settings by v.settings.collectAsState()
            val dark = when (settings.theme) {
                "light" -> false; "dark" -> true; else -> isSystemInDarkTheme()
            }
            androidx.compose.runtime.LaunchedEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = if (dark) androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        else androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
                    navigationBarStyle = if (dark) androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                        else androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT))
            }
            LodestillTheme(dark = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { HfNav(v) }
            }
        }
    }
    override fun onStop() { super.onStop(); vm?.onBackground() }
    override fun onStart() { super.onStart(); vm?.onForeground() }
}
