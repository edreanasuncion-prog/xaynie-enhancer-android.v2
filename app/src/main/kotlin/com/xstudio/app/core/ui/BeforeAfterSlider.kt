package com.xstudio.app.core.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xstudio.app.core.designsystem.XBlack
import com.xstudio.app.core.designsystem.XWhite
import kotlin.math.roundToInt

@Composable
fun BeforeAfterSlider(
    beforeBitmap: Bitmap,
    afterBitmap: Bitmap,
    modifier: Modifier = Modifier
) {
    var sliderFraction by remember { mutableFloatStateOf(0.5f) }
    var containerWidthPx by remember { mutableIntStateOf(1) }
    var containerHeightPx by remember { mutableIntStateOf(1) }

    val beforeImage = remember(beforeBitmap) { beforeBitmap.asImageBitmap() }
    val afterImage = remember(afterBitmap) { afterBitmap.asImageBitmap() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(XBlack)
            .onSizeChanged {
                containerWidthPx = it.width.coerceAtLeast(1)
                containerHeightPx = it.height.coerceAtLeast(1)
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    sliderFraction = (change.position.x / containerWidthPx.toFloat())
                        .coerceIn(0.02f, 0.98f)
                }
            }
    ) {
        val dividerX = containerWidthPx * sliderFraction

        Canvas(modifier = Modifier.fillMaxSize()) {
            val dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt())

            drawImage(
                image = afterImage,
                dstSize = dstSize
            )

            clipRect(
                left = 0f,
                top = 0f,
                right = dividerX,
                bottom = size.height
            ) {
                drawImage(
                    image = beforeImage,
                    dstSize = dstSize
                )
            }

            drawLine(
                color = XWhite,
                start = Offset(dividerX, 0f),
                end = Offset(dividerX, size.height),
                strokeWidth = 2.dp.toPx()
            )
        }

        val handleSizePx = with(LocalDensity.current) { 40.dp.toPx() }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (dividerX - handleSizePx / 2f).roundToInt(),
                        y = ((containerHeightPx - handleSizePx) / 2f).roundToInt()
                    )
                }
                .size(40.dp)
                .clip(CircleShape)
                .background(XBlack.copy(alpha = 0.85f))
                .border(2.dp, XWhite, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CompareArrows,
                contentDescription = "Drag Comparison Slider",
                tint = XWhite,
                modifier = Modifier.size(20.dp)
            )
        }

        Text(
            text = "BEFORE",
            color = XWhite,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(XBlack.copy(alpha = 0.65f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )

        Text(
            text = "ENHANCED",
            color = XBlack,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(XWhite)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
