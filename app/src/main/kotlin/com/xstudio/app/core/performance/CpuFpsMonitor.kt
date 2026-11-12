package com.xstudio.app.core.performance

import android.os.Process
import android.os.SystemClock
import android.view.Choreographer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xstudio.app.core.designsystem.XWhite
import com.xstudio.app.core.designsystem.liquidGlass
import kotlin.math.roundToInt

@Composable
fun CpuProfileOverlay(
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    if (!enabled) return

    var fps by remember { mutableIntStateOf(60) }
    var cpuPercent by remember { mutableIntStateOf(0) }
    var ramMb by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) {
        var frameCount = 0
        var lastWallTimeMs = SystemClock.elapsedRealtime()
        var lastCpuTimeMs = Process.getElapsedCpuTime()
        val cores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)

        val callback = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                frameCount++
                val nowWallMs = SystemClock.elapsedRealtime()
                val elapsedWallMs = nowWallMs - lastWallTimeMs

                if (elapsedWallMs >= 1000L) {
                    val nowCpuMs = Process.getElapsedCpuTime()
                    val elapsedCpuMs = nowCpuMs - lastCpuTimeMs

                    fps = ((frameCount * 1000f) / elapsedWallMs).roundToInt().coerceIn(0, 144)
                    cpuPercent = ((elapsedCpuMs.toFloat() / (elapsedWallMs * cores)) * 100f)
                        .roundToInt()
                        .coerceIn(1, 100)

                    val runtime = Runtime.getRuntime()
                    ramMb = ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)).toInt()

                    frameCount = 0
                    lastWallTimeMs = nowWallMs
                    lastCpuTimeMs = nowCpuMs
                }
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
        Choreographer.getInstance().postFrameCallback(callback)
        onDispose {
            Choreographer.getInstance().removeFrameCallback(callback)
        }
    }

    Row(
        modifier = modifier
            .padding(top = 42.dp, end = 16.dp)
            .liquidGlass(shape = RoundedCornerShape(50), alpha = 0.18f)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "FPS $fps",
            color = XWhite,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "CPU $cpuPercent%",
            color = XWhite,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "${ramMb}MB",
            color = XWhite,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
