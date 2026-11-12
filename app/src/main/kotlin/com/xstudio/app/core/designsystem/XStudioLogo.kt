package com.xstudio.app.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

const val LOGO_D =
    "M34 25 19 42 9 62 4 86 5 122 11 151 24 188 70 278 74 296 74 318 68 342 26 421 7 480 4 501 5 535 11 557 26 581 47 598 79 609 133 607 161 600 190 589 241 564 298 528 366 475 410 435 451 392 464 384 479 382 493 387 506 400 535 460 543 485 547 512 546 526 541 538 530 545 516 547 486 543 443 528 389 500 338 542 366 560 410 583 442 596 476 606 494 609 535 609 559 602 581 588 600 564 606 549 609 535 609 493 602 459 587 419 545 339 541 324 540 299 547 270 583 204 598 167 606 139 609 121 609 78 600 50 589 34 575 21 552 9 529 4 492 5 456 13 412 30 364 55 337 72 389 114 436 89 481 72 518 67 533 70 541 76 547 96 545 119 535 154 524 180 507 212 499 222 483 231 468 231 451 222 391 161 320 102 260 61 211 34 158 13 114 4 86 4 69 7 52 13Z" +
    "M96 67 125 70 178 89 221 112 278 150 342 203 387 247 400 268 408 297 406 329 391 362 352 402 306 442 261 476 196 516 136 541 102 547 84 545 72 536 68 526 67 511 74 474 85 445 108 400 117 390 135 382 142 382 156 387 227 456 279 419 224 363 212 341 206 311 207 290 218 260 226 248 279 195 228 158 156 227 147 231 130 231 117 224 108 214 86 171 74 139 68 113 67 97 70 81 79 71Z"

@Composable
fun XStudioLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 32.dp,
    showWordmark: Boolean = true
) {
    val logoPath = remember { PathParser().parsePathString(LOGO_D).toPath() }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(iconSize)) {
            val scale = size.width / 614f
            withTransform({
                scale(scale, scale, Offset.Zero)
            }) {
                drawPath(path = logoPath, color = XWhite, style = Fill)
            }
        }
        if (showWordmark) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "STUDIO",
                color = XWhite,
                fontSize = 22.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
fun XMotionIcon(
    size: Dp = 24.dp,
    tint: Color = XWhite,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 24f
        val stroke = 2.3f * s
        val triangle = Path().apply {
            moveTo(11f * s, 5.5f * s)
            lineTo(11f * s, 18.5f * s)
            lineTo(20.5f * s, 12f * s)
            close()
        }
        drawPath(triangle, tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawLine(tint, Offset(3f * s, 9.5f * s), Offset(6.5f * s, 9.5f * s), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(tint, Offset(3f * s, 14.5f * s), Offset(6.5f * s, 14.5f * s), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
fun IcoPhoto(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 64f
        val silver = Brush.linearGradient(listOf(Color.White, Color(0xFFA9ADC0)))
        drawRoundRect(
            brush = Brush.linearGradient(listOf(Color(0x24FFFFFF), Color(0x0AFFFFFF))),
            topLeft = Offset(6f * s, 10f * s),
            size = Size(52f * s, 44f * s),
            cornerRadius = CornerRadius(10f * s, 10f * s)
        )
        drawRoundRect(
            brush = silver,
            topLeft = Offset(6f * s, 10f * s),
            size = Size(52f * s, 44f * s),
            cornerRadius = CornerRadius(10f * s, 10f * s),
            style = Stroke(width = 2.6f * s)
        )
        val mtn = Path().apply {
            moveTo(12f * s, 46f * s)
            lineTo(26f * s, 28f * s)
            lineTo(36f * s, 40f * s)
            lineTo(42f * s, 33f * s)
            lineTo(52f * s, 46f * s)
            close()
        }
        drawPath(mtn, brush = silver)
        drawCircle(brush = silver, radius = 5f * s, center = Offset(44f * s, 21f * s))
    }
}

@Composable
fun IcoVideo(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 64f
        val silver = Brush.linearGradient(listOf(Color.White, Color(0xFFA9ADC0)))
        drawRoundRect(
            brush = Brush.linearGradient(listOf(Color(0x24FFFFFF), Color(0x0AFFFFFF))),
            topLeft = Offset(6f * s, 12f * s),
            size = Size(46f * s, 40f * s),
            cornerRadius = CornerRadius(10f * s, 10f * s)
        )
        drawRoundRect(
            brush = silver,
            topLeft = Offset(6f * s, 12f * s),
            size = Size(46f * s, 40f * s),
            cornerRadius = CornerRadius(10f * s, 10f * s),
            style = Stroke(width = 2.6f * s)
        )
        val play = Path().apply {
            moveTo(26f * s, 23f * s)
            lineTo(41f * s, 32f * s)
            lineTo(26f * s, 41f * s)
            close()
        }
        drawPath(play, brush = silver)
        val star = Path().apply {
            moveTo(54f * s, 7f * s)
            lineTo(55.7f * s, 12.3f * s)
            lineTo(61f * s, 14f * s)
            lineTo(55.7f * s, 15.7f * s)
            lineTo(54f * s, 21f * s)
            lineTo(52.3f * s, 15.7f * s)
            lineTo(47f * s, 14f * s)
            lineTo(52.3f * s, 12.3f * s)
            close()
        }
        drawPath(star, brush = silver)
    }
}

@Composable
fun IcoChat(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 64f
        val silver = Brush.linearGradient(listOf(Color.White, Color(0xFFA9ADC0)))
        val bubble = Path().apply {
            moveTo(32f * s, 8f * s)
            cubicTo(18f * s, 8f * s, 8f * s, 17f * s, 8f * s, 29f * s)
            cubicTo(8f * s, 35f * s, 11f * s, 40f * s, 16f * s, 44f * s)
            lineTo(14f * s, 56f * s)
            lineTo(27f * s, 49f * s)
            cubicTo(29f * s, 49.5f * s, 30f * s, 49.7f * s, 32f * s, 49.7f * s)
            cubicTo(46f * s, 49.7f * s, 56f * s, 40.7f * s, 56f * s, 28.7f * s)
            cubicTo(56f * s, 17f * s, 46f * s, 8f * s, 32f * s, 8f * s)
            close()
        }
        drawPath(bubble, brush = silver)
        drawCircle(Color(0x990A0A0C), radius = 3.2f * s, center = Offset(22f * s, 29f * s))
        drawCircle(Color(0x990A0A0C), radius = 3.2f * s, center = Offset(32f * s, 29f * s))
        drawCircle(Color(0x990A0A0C), radius = 3.2f * s, center = Offset(42f * s, 29f * s))
    }
}

@Composable
fun IcoClap(modifier: Modifier = Modifier, size: Dp = 44.dp) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.width / 64f
        val silver = Brush.linearGradient(listOf(Color.White, Color(0xFFA9ADC0)))
        drawRoundRect(
            brush = Brush.linearGradient(listOf(Color(0x24FFFFFF), Color(0x0AFFFFFF))),
            topLeft = Offset(8f * s, 26f * s),
            size = Size(48f * s, 30f * s),
            cornerRadius = CornerRadius(7f * s, 7f * s)
        )
        drawRoundRect(
            brush = silver,
            topLeft = Offset(8f * s, 26f * s),
            size = Size(48f * s, 30f * s),
            cornerRadius = CornerRadius(7f * s, 7f * s),
            style = Stroke(width = 2.6f * s)
        )
        drawCircle(brush = silver, radius = 5f * s, center = Offset(32f * s, 42f * s))
        withTransform({
            rotate(-10f, pivot = Offset(8f * s, 24f * s))
        }) {
            drawRoundRect(
                brush = silver,
                topLeft = Offset(8f * s, 11f * s),
                size = Size(48f * s, 13f * s),
                cornerRadius = CornerRadius(4f * s, 4f * s)
            )
            drawLine(Color(0x8C0A0A0C), Offset(18f * s, 11f * s), Offset(25f * s, 24f * s), strokeWidth = 3f * s)
            drawLine(Color(0x8C0A0A0C), Offset(30f * s, 11f * s), Offset(37f * s, 24f * s), strokeWidth = 3f * s)
            drawLine(Color(0x8C0A0A0C), Offset(42f * s, 11f * s), Offset(49f * s, 24f * s), strokeWidth = 3f * s)
        }
    }
}

@Composable
fun HeroXArtwork(modifier: Modifier = Modifier) {
    val logoPath = remember { PathParser().parsePathString(LOGO_D).toPath() }
    val transition = rememberInfiniteTransition(label = "herox")
    val floatOffset by transition.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    val sparkleOpacity by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "twinkle"
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .graphicsLayer { translationY = floatOffset }
    ) {
        val s = (size.width / 740f).coerceAtMost(size.height / 700f)
        val cx = size.width * 0.52f
        val cy = size.height * 0.50f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0x3DFFFFFF), Color.Transparent),
                center = Offset(cx, cy),
                radius = 340f * s
            )
        )

        withTransform({
            translate(cx, cy)
            rotate(-24f)
        }) {
            drawOval(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0x1AFFFFFF), Color(0xE6FFFFFF), Color(0x33FFFFFF))
                ),
                topLeft = Offset(-420f * s, -132f * s),
                size = Size(840f * s, 264f * s),
                style = Stroke(width = 3.5f * s)
            )
        }

        withTransform({
            translate(cx - 307f * s + 16f * s, cy - 307f * s + 22f * s)
            scale(s, s, Offset.Zero)
        }) {
            drawPath(
                path = logoPath,
                brush = Brush.linearGradient(listOf(Color(0xFF7A7E92), Color(0xFF1D1E27))),
                style = Fill
            )
        }

        withTransform({
            translate(cx - 307f * s + 8f * s, cy - 307f * s + 11f * s)
            scale(s, s, Offset.Zero)
        }) {
            drawPath(path = logoPath, color = Color(0xFF5A5E72), style = Fill)
        }

        withTransform({
            translate(cx - 307f * s, cy - 307f * s)
            scale(s, s, Offset.Zero)
        }) {
            drawPath(
                path = logoPath,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White, Color(0xFFD5D8E4), Color(0xFF8F93A6), Color(0xFFE9EBF3)),
                    start = Offset(60f, 0f),
                    end = Offset(550f, 614f)
                ),
                style = Fill
            )
            drawPath(
                path = logoPath,
                color = Color(0xDDFFFFFF),
                style = Stroke(width = 3.5f, join = StrokeJoin.Round)
            )
        }

        val starPaint = Color.White.copy(alpha = sparkleOpacity)
        drawStar4(cx - 180f * s, cy - 170f * s, 16f * s, starPaint)
        drawStar4(cx + 190f * s, cy + 180f * s, 13f * s, starPaint)
        drawCircle(starPaint, radius = 2.5f * s, center = Offset(cx - 240f * s, cy - 80f * s))
        drawCircle(starPaint, radius = 2.2f * s, center = Offset(cx + 120f * s, cy - 200f * s))
        drawCircle(starPaint, radius = 2.2f * s, center = Offset(cx + 250f * s, cy + 20f * s))
    }
}

private fun DrawScope.drawStar4(cx: Float, cy: Float, size: Float, color: Color) {
    val p = Path().apply {
        moveTo(cx, cy - size)
        quadraticBezierTo(cx, cy, cx + size, cy)
        quadraticBezierTo(cx, cy, cx, cy + size)
        quadraticBezierTo(cx, cy, cx - size, cy)
        quadraticBezierTo(cx, cy, cx, cy - size)
        close()
    }
    drawPath(p, color = color, style = Fill)
}
