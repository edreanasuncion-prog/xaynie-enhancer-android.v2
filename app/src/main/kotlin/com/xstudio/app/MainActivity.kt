package com.xstudio.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.xstudio.app.authentication.AuthViewModel
import com.xstudio.app.core.designsystem.XStudioTheme
import com.xstudio.app.core.performance.CpuProfileOverlay
import com.xstudio.app.navigation.XStudioRoot

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var cpuOverlayEnabled by rememberSaveable { mutableStateOf(false) }
            var pageScale by rememberSaveable { mutableFloatStateOf(1.0f) }

            XStudioTheme(pageScale = pageScale) {
                Box(modifier = Modifier.fillMaxSize()) {
                    XStudioRoot(
                        authViewModel = authViewModel,
                        cpuOverlayEnabled = cpuOverlayEnabled,
                        onToggleCpuOverlay = { cpuOverlayEnabled = it },
                        pageScale = pageScale,
                        onChangePageScale = { pageScale = it }
                    )

                    CpuProfileOverlay(
                        enabled = cpuOverlayEnabled,
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
                }
            }
        }
    }
}
