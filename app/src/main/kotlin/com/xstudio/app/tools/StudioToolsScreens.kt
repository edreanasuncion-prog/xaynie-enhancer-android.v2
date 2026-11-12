package com.xstudio.app.tools

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Presentation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import com.xstudio.app.core.designsystem.XBlack
import com.xstudio.app.core.designsystem.XGlassBorder
import com.xstudio.app.core.designsystem.XStudioLogo
import com.xstudio.app.core.designsystem.XSurface
import com.xstudio.app.core.designsystem.XWhite
import com.xstudio.app.core.designsystem.XWhiteMuted
import com.xstudio.app.core.designsystem.liquidGlass
import com.xstudio.app.core.media.MediaStoreExporter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URLEncoder
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/* ============================================================================
 * 1. SPLASH SCREEN & UPDATE NOTICE (ported from source/src/Splash.tsx & update.tsx)
 * ============================================================================ */

private const val UPDATE_ID = "2026-10-22"
private val UPDATE_FEATURES = listOf(
    "Account sign-up with email verification code",
    "New liquid glass design, smooth transitions and animations",
    "On-device software Photo Enhancer with before/after slider",
    "Video Enhancer with before/after slider",
    "Batch Enhance for up to 5 photos",
    "TikTok Video Exporter: FPS, resolution and quality",
    "Ask X: full-page AI chat",
    "Photo Lab and Quick Tools: compress, convert, color picker",
    "Download Center for public links",
    "AM Project Finder for Alight Motion links and XML",
    "Live CPU, FPS and status while enhancing",
    "Profile with free plan, picture, history and settings",
    "Compact default display, adjustable in Settings"
)

@Composable
fun XStudioSplashOverlay(
    username: String?,
    onFinished: () -> Unit
) {
    val lines = remember { mutableStateListOf<String>() }

    LaunchedEffect(Unit) {
        lines.add("Creating Session....")
        delay(600L)
        lines.add("Created Session....")
        delay(600L)
        lines.add("Restoring Session...")
        delay(700L)
        val tag = if (!username.isNullOrBlank()) "@$username" else "@guest"
        lines.add("Successfully Restored, Welcome Back $tag!")
        delay(700L)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(XBlack)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            XStudioLogo(iconSize = 84.dp, showWordmark = false)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                lines.forEach { line ->
                    Text(
                        text = line,
                        color = XWhiteMuted,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun UpdateNoticeDialog() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("xstudio_prefs", Context.MODE_PRIVATE) }
    var visible by remember {
        mutableStateOf(prefs.getString("xe_update_seen", "") != UPDATE_ID)
    }

    if (!visible) return

    AlertDialog(
        onDismissRequest = { visible = false },
        containerColor = XSurface,
        titleContentColor = XWhite,
        textContentColor = XWhiteMuted,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                XStudioLogo(iconSize = 32.dp, showWordmark = false)
                Column {
                    Text("X Studio", color = XWhiteMuted, fontSize = 12.sp)
                    Text("NEW UPDATE!", color = XWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    "Major update with new tools, fixes, and overall performance improvements!",
                    color = XWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("Added Features:", color = XWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                UPDATE_FEATURES.forEach { feat ->
                    Text("• $feat", color = XWhiteMuted, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    prefs.edit().putString("xe_update_seen", UPDATE_ID).apply()
                    visible = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack)
            ) {
                Text("Don't Show Again", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = { visible = false }) {
                Text("Later", color = XWhite)
            }
        }
    )
}

/* ============================================================================
 * 2. PHOTO LAB (ported from source/src/lab.tsx)
 * ============================================================================ */

@Composable
fun PhotoLabScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var brightness by remember { mutableFloatStateOf(100f) } // 30..200
    var contrast by remember { mutableFloatStateOf(100f) }   // 30..200
    var saturation by remember { mutableFloatStateOf(100f) } // 0..250
    var grayscale by remember { mutableStateOf(false) }
    var sepia by remember { mutableStateOf(false) }
    var quarterRotations by remember { mutableIntStateOf(0) } // 0..3
    var flipHorizontal by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val bmp = context.contentResolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it)
                }
                sourceBitmap = bmp
            }
        }
    }

    LaunchedEffect(sourceBitmap, brightness, contrast, saturation, grayscale, sepia, quarterRotations, flipHorizontal) {
        val src = sourceBitmap ?: return@LaunchedEffect
        withContext(Dispatchers.Default) {
            previewBitmap = renderPhotoLabBitmap(
                src = src,
                maxSide = 900,
                bPct = brightness,
                cPct = contrast,
                sPct = saturation,
                gray = grayscale,
                sepia = sepia,
                rot = quarterRotations,
                flip = flipHorizontal
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = XWhite)
            }
            Text("PHOTO LAB", color = XWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }

        Button(
            onClick = {
                picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (sourceBitmap == null) "Choose a photo" else "Change photo", fontWeight = FontWeight.Bold)
        }

        previewBitmap?.let { bmp ->
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Photo Lab Preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .liquidGlass(RoundedCornerShape(20.dp))
                    .padding(8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Brightness: ${brightness.roundToInt()}%", color = XWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = brightness,
                    onValueChange = { brightness = it },
                    valueRange = 30f..200f,
                    colors = SliderDefaults.colors(thumbColor = XWhite, activeTrackColor = XWhite)
                )

                Text("Contrast: ${contrast.roundToInt()}%", color = XWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = contrast,
                    onValueChange = { contrast = it },
                    valueRange = 30f..200f,
                    colors = SliderDefaults.colors(thumbColor = XWhite, activeTrackColor = XWhite)
                )

                Text("Saturation: ${saturation.roundToInt()}%", color = XWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it },
                    valueRange = 0f..250f,
                    colors = SliderDefaults.colors(thumbColor = XWhite, activeTrackColor = XWhite)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { grayscale = !grayscale },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (grayscale) XWhite else XBlack,
                            contentColor = if (grayscale) XBlack else XWhite
                        ),
                        modifier = Modifier.weight(1f).border(1.dp, XGlassBorder, RoundedCornerShape(12.dp))
                    ) { Text("Grayscale") }

                    Button(
                        onClick = { sepia = !sepia },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (sepia) XWhite else XBlack,
                            contentColor = if (sepia) XBlack else XWhite
                        ),
                        modifier = Modifier.weight(1f).border(1.dp, XGlassBorder, RoundedCornerShape(12.dp))
                    ) { Text("Sepia") }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { quarterRotations = (quarterRotations + 1) % 4 },
                        colors = ButtonDefaults.buttonColors(containerColor = XBlack, contentColor = XWhite),
                        modifier = Modifier.weight(1f).border(1.dp, XGlassBorder, RoundedCornerShape(12.dp))
                    ) { Text("Rotate 90°") }

                    Button(
                        onClick = { flipHorizontal = !flipHorizontal },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (flipHorizontal) XWhite else XBlack,
                            contentColor = if (flipHorizontal) XBlack else XWhite
                        ),
                        modifier = Modifier.weight(1f).border(1.dp, XGlassBorder, RoundedCornerShape(12.dp))
                    ) { Text("Flip") }
                }

                Button(
                    onClick = {
                        val src = sourceBitmap ?: return@Button
                        scope.launch(Dispatchers.IO) {
                            val full = renderPhotoLabBitmap(
                                src = src,
                                maxSide = 4096,
                                bPct = brightness,
                                cPct = contrast,
                                sPct = saturation,
                                gray = grayscale,
                                sepia = sepia,
                                rot = quarterRotations,
                                flip = flipHorizontal
                            )
                            val f = File(context.cacheDir, "xaynie-lab-${System.currentTimeMillis()}.jpg")
                            FileOutputStream(f).use { full.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                            val ok = MediaStoreExporter.saveToGallery(context, f)
                            statusMsg = if (ok) "Exported xaynie-lab.jpg to Pictures/XStudio" else "Export failed"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export JPG", fontWeight = FontWeight.ExtraBold)
                }

                statusMsg?.let { Text(it, color = XWhiteMuted, fontSize = 12.sp) }
            }
        }
    }
}

private fun renderPhotoLabBitmap(
    src: Bitmap,
    maxSide: Int,
    bPct: Float,
    cPct: Float,
    sPct: Float,
    gray: Boolean,
    sepia: Boolean,
    rot: Int,
    flip: Boolean
): Bitmap {
    val k = min(1f, maxSide.toFloat() / max(src.width, src.height).toFloat())
    val w = (src.width * k).roundToInt().coerceAtLeast(1)
    val h = (src.height * k).roundToInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(src, w, h, true)

    val matrix = Matrix().apply {
        postTranslate(-w / 2f, -h / 2f)
        if (flip) postScale(-1f, 1f)
        postRotate(rot * 90f)
        val outW = if (rot % 2 == 1) h else w
        val outH = if (rot % 2 == 1) w else h
        postTranslate(outW / 2f, outH / 2f)
    }

    val outW = if (rot % 2 == 1) h else w
    val outH = if (rot % 2 == 1) w else h
    val outBmp = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(outBmp)

    val satMatrix = ColorMatrix().apply { setSaturation(if (gray) 0f else (sPct / 100f)) }
    val b = bPct / 100f
    val c = cPct / 100f
    val offset = (-0.5f * c + 0.5f) * 255f
    val bcMatrix = ColorMatrix(
        floatArrayOf(
            b * c, 0f, 0f, 0f, offset,
            0f, b * c, 0f, 0f, offset,
            0f, 0f, b * c, 0f, offset,
            0f, 0f, 0f, 1f, 0f
        )
    )
    satMatrix.postConcat(bcMatrix)

    if (sepia) {
        val sepiaMatrix = ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        satMatrix.postConcat(sepiaMatrix)
    }

    val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
        colorFilter = ColorMatrixColorFilter(satMatrix)
    }
    canvas.drawBitmap(scaled, matrix, paint)
    return outBmp
}

/* ============================================================================
 * 3. QUICK TOOLS: COMPRESS / CONVERT & COLOR PICKER (source/src/tools.tsx)
 * ============================================================================ */

@Composable
fun QuickToolsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var selectedFormat by remember { mutableStateOf("JPG") } // JPG, PNG, WEBP
    var quality by remember { mutableFloatStateOf(80f) }
    var convertInfo by remember { mutableStateOf<String?>(null) }

    var colorBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var viewSize by remember { mutableStateOf(IntSize(1, 1)) }
    var pickedHex by remember { mutableStateOf<String?>(null) }
    var pickedRgb by remember { mutableStateOf<String?>(null) }
    var pickedColorInt by remember { mutableIntStateOf(android.graphics.Color.WHITE) }

    val convertPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val inputBytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                val bmp = BitmapFactory.decodeByteArray(inputBytes, 0, inputBytes.size) ?: return@launch
                val baos = ByteArrayOutputStream()
                val fmt = when (selectedFormat) {
                    "PNG" -> Bitmap.CompressFormat.PNG
                    "WEBP" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
                    else -> Bitmap.CompressFormat.JPEG
                }
                bmp.compress(fmt, quality.roundToInt(), baos)
                val outBytes = baos.toByteArray()
                val ext = selectedFormat.lowercase()
                val outFile = File(context.cacheDir, "xaynie.$ext")
                outFile.writeBytes(outBytes)
                val saved = MediaStoreExporter.saveToGallery(context, outFile)
                convertInfo = "${inputBytes.size / 1024} KB → ${outBytes.size / 1024} KB " +
                    if (saved) "(Saved xaynie.$ext to Pictures/XStudio)" else "(Converted, but saving to the gallery failed)"
            }
        }
    }

    val colorSamplePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) }
                if (bmp != null) {
                    val scale = min(1f, 900f / max(bmp.width, bmp.height).toFloat())
                    colorBitmap = Bitmap.createScaledBitmap(
                        bmp,
                        (bmp.width * scale).roundToInt().coerceAtLeast(1),
                        (bmp.height * scale).roundToInt().coerceAtLeast(1),
                        true
                    )
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = XWhite)
            }
            Text("QUICK TOOLS", color = XWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }

        // Compress / Convert Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Compress / convert image", color = XWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                listOf("JPG", "PNG", "WEBP").forEach { fmt ->
                    val active = selectedFormat == fmt
                    Button(
                        onClick = { selectedFormat = fmt },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (active) XWhite else XBlack,
                            contentColor = if (active) XBlack else XWhite
                        ),
                        modifier = Modifier.weight(1f).border(1.dp, XGlassBorder, RoundedCornerShape(12.dp))
                    ) {
                        Text(fmt, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text("Quality: ${quality.roundToInt()}", color = XWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = quality,
                onValueChange = { quality = it },
                valueRange = 10f..100f,
                colors = SliderDefaults.colors(thumbColor = XWhite, activeTrackColor = XWhite)
            )
            Button(
                onClick = {
                    convertPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose image and convert", fontWeight = FontWeight.Bold)
            }
            convertInfo?.let { Text(it, color = XWhite, fontSize = 12.sp) }
        }

        // Color Picker Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Color picker", color = XWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Button(
                onClick = {
                    colorSamplePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Choose image", fontWeight = FontWeight.Bold)
            }

            colorBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Tap to pick color",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .onSizeChanged { viewSize = it }
                        .pointerInput(bmp) {
                            detectTapGestures { offset ->
                                val px = ((offset.x / viewSize.width.coerceAtLeast(1)) * bmp.width)
                                    .toInt()
                                    .coerceIn(0, bmp.width - 1)
                                val py = ((offset.y / viewSize.height.coerceAtLeast(1)) * bmp.height)
                                    .toInt()
                                    .coerceIn(0, bmp.height - 1)
                                val pixel = bmp.getPixel(px, py)
                                val r = android.graphics.Color.red(pixel)
                                val g = android.graphics.Color.green(pixel)
                                val b = android.graphics.Color.blue(pixel)
                                pickedColorInt = pixel
                                pickedHex = String.format("#%02X%02X%02X", r, g, b)
                                pickedRgb = "rgb($r, $g, $b)"
                            }
                        }
                )
            }

            if (pickedHex != null && pickedRgb != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(pickedColorInt))
                            .border(1.dp, XWhite, CircleShape)
                    )
                    Button(
                        onClick = { clipboard.setText(AnnotatedString(pickedHex.orEmpty())) },
                        colors = ButtonDefaults.buttonColors(containerColor = XBlack, contentColor = XWhite),
                        modifier = Modifier.border(1.dp, XGlassBorder, RoundedCornerShape(12.dp))
                    ) {
                        Text(pickedHex.orEmpty())
                    }
                    Button(
                        onClick = { clipboard.setText(AnnotatedString(pickedRgb.orEmpty())) },
                        colors = ButtonDefaults.buttonColors(containerColor = XBlack, contentColor = XWhite),
                        modifier = Modifier.border(1.dp, XGlassBorder, RoundedCornerShape(12.dp))
                    ) {
                        Text(pickedRgb.orEmpty())
                    }
                }
            }
        }
    }
}

/* ============================================================================
 * 4. AM PROJECT FINDER (ported from source/src/tools.tsx)
 * ============================================================================ */

private val AM_ALLOWED_HOSTS = listOf("alight.link", "alightmotion.com", "alightcreative.com")

@Composable
fun AmFinderScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var amLink by remember { mutableStateOf("") }
    var tiktokLink by remember { mutableStateOf("") }
    var statusMsg by remember { mutableStateOf("") }
    var xmlContent by remember { mutableStateOf("") }

    val xmlPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                    if (bytes.size > 10 * 1024 * 1024) {
                        statusMsg = "That file is over 10 MB."
                        return@launch
                    }
                    val text = bytes.decodeToString()
                    if (Regex("<!DOCTYPE|<!ENTITY", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
                        statusMsg = "XML with DTD or entity declarations is blocked for safety."
                        return@launch
                    }
                    val dbf = DocumentBuilderFactory.newInstance()
                    dbf.newDocumentBuilder().parse(text.byteInputStream())
                    xmlContent = text
                    statusMsg = "XML is valid."
                }.onFailure { e ->
                    statusMsg = "XML syntax error: ${e.localizedMessage?.take(160)}"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = XWhite)
            }
            Text("AM PROJECT FINDER", color = XWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = amLink,
                onValueChange = { amLink = it },
                placeholder = { Text("Alight Motion share link") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = XWhite,
                    unfocusedBorderColor = XGlassBorder,
                    focusedTextColor = XWhite,
                    unfocusedTextColor = XWhite
                )
            )
            Button(
                onClick = {
                    val valid = runCatching {
                        val u = URI(amLink.trim())
                        val h = u.host?.lowercase().orEmpty()
                        u.scheme == "https" && AM_ALLOWED_HOSTS.any { h == it || h.endsWith(".$it") }
                    }.getOrDefault(false)
                    if (valid) {
                        statusMsg = "Valid Alight Motion link. Opening it."
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(amLink.trim())))
                    } else {
                        statusMsg = "Not a valid Alight Motion share link (https required)."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Validate and open", fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = tiktokLink,
                onValueChange = { tiktokLink = it },
                placeholder = { Text("TikTok URL to search") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = XWhite,
                    unfocusedBorderColor = XGlassBorder,
                    focusedTextColor = XWhite,
                    unfocusedTextColor = XWhite
                )
            )
            Button(
                onClick = {
                    val valid = runCatching {
                        val u = URI(tiktokLink.trim())
                        u.host?.lowercase()?.endsWith("tiktok.com") == true
                    }.getOrDefault(false)
                    if (valid) {
                        val q = URLEncoder.encode("alight motion project xml ${tiktokLink.trim()}", "UTF-8")
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=$q")))
                        statusMsg = "Opened a web search for that TikTok."
                    } else {
                        statusMsg = "Enter a valid TikTok URL."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = XBlack, contentColor = XWhite),
                modifier = Modifier.fillMaxWidth().border(1.dp, XGlassBorder, RoundedCornerShape(12.dp))
            ) {
                Text("Search the web")
            }

            Button(
                onClick = { xmlPicker.launch("text/xml") },
                colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open local XML (max 10 MB)", fontWeight = FontWeight.Bold)
            }

            Text("Private links and logins are never bypassed.", color = XWhiteMuted, fontSize = 11.sp)
            if (statusMsg.isNotBlank()) {
                Text(statusMsg, color = XWhite, fontSize = 12.sp)
            }
        }

        if (xmlContent.isNotBlank()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = xmlContent.take(20000),
                    color = XWhiteMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

/* ============================================================================
 * 5. TIKTOK VIDEO EXPORTER (ported from source/src/tiktok.tsx)
 * ============================================================================ */

private const val SMOOTH_FPS_MSG =
    "Smooth 60 FPS needs a frame-interpolation engine, and one is not enabled for this preset. Ordinary frame duplication would not be real interpolation, so it is not used for this option. Use 60 FPS Standard instead."

@OptIn(UnstableApi::class)
@Composable
fun TikTokExporterScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var metaText by remember { mutableStateOf("") }
    var selectedRes by remember { mutableIntStateOf(1080) } // 0=Original, 720, 1080, 1440, 2160
    var selectedFpsId by remember { mutableStateOf("orig") } // orig, 30, 60, 60s, 90, 120
    var selectedQual by remember { mutableFloatStateOf(1.0f) } // 0.6, 1.0, 1.6
    var busy by remember { mutableStateOf(false) }
    var errText by remember { mutableStateOf("") }
    var outInfo by remember { mutableStateOf("") }

    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(Unit) { onDispose { exoPlayer.release() } }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            videoUri = uri
            errText = ""
            outInfo = ""
            exoPlayer.setMediaItem(MediaItem.fromUri(uri))
            exoPlayer.prepare()
            runCatching {
                val mmr = MediaMetadataRetriever()
                mmr.setDataSource(context, uri)
                val w = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
                val h = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
                val durMs = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val hasAudio = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) != null
                mmr.release()
                val orient = if (h > w) "portrait" else "landscape"
                metaText = "${w} x ${h} ($orient) · ${durMs / 1000}s · Audio: ${if (hasAudio) "detected" else "none"}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = XWhite)
            }
            Text("TIKTOK VIDEO EXPORTER", color = XWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }

        Button(
            onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)) },
            colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (videoUri == null) "Choose a video" else "Replace video", fontWeight = FontWeight.Bold)
        }

        if (videoUri != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .liquidGlass(RoundedCornerShape(20.dp))
            ) {
                AndroidView(
                    factory = { ctx -> PlayerView(ctx).apply { player = exoPlayer } },
                    modifier = Modifier.fillMaxSize()
                )
            }
            Text(metaText, color = XWhiteMuted, fontSize = 12.sp)

            Text("Resolution", color = XWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf("Original" to 0, "720p" to 720, "1080p" to 1080, "2K" to 1440, "4K" to 2160).forEach { (label, v) ->
                    val active = selectedRes == v
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) XWhite else XBlack)
                            .border(1.dp, XGlassBorder, RoundedCornerShape(10.dp))
                            .clickable { selectedRes = v }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(label, color = if (active) XBlack else XWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text("Frame rate", color = XWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                listOf(
                    "orig" to "Original FPS",
                    "30" to "30 FPS",
                    "60" to "60 FPS Standard",
                    "60s" to "60 FPS Smooth",
                    "90" to "90 FPS",
                    "120" to "120 FPS"
                ).forEach { (id, label) ->
                    val active = selectedFpsId == id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) XWhite else XBlack)
                            .border(1.dp, XGlassBorder, RoundedCornerShape(10.dp))
                            .clickable {
                                selectedFpsId = id
                                errText = if (id == "60s") SMOOTH_FPS_MSG else ""
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(label, color = if (active) XBlack else XWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Text("Quality", color = XWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Standard" to 0.6f, "High" to 1.0f, "Max" to 1.6f).forEach { (label, q) ->
                    val active = selectedQual == q
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) XWhite else XBlack)
                            .border(1.dp, XGlassBorder, RoundedCornerShape(10.dp))
                            .clickable { selectedQual = q }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(label, color = if (active) XBlack else XWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (errText.isNotBlank()) {
                Text(errText, color = XWhite, fontSize = 12.sp)
            }
            if (outInfo.isNotBlank()) {
                Text(outInfo, color = XWhiteMuted, fontSize = 12.sp)
            }

            Button(
                onClick = {
                    val uri = videoUri ?: return@Button
                    if (selectedFpsId == "60s") {
                        errText = SMOOTH_FPS_MSG
                        return@Button
                    }
                    busy = true
                    errText = ""
                    val outDir = File(context.getExternalFilesDir(null), "tiktok_exports").apply { mkdirs() }
                    val outFile = File(outDir, "xaynie-tiktok-${System.currentTimeMillis()}.mp4")
                    val effects = buildList<Effect> {
                        if (selectedRes > 0) add(Presentation.createForShortSide(selectedRes))
                    }
                    val transformer = Transformer.Builder(context)
                        .setVideoMimeType(MimeTypes.VIDEO_H264)
                        .addListener(object : Transformer.Listener {
                            override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                                busy = false
                                if (!outFile.isFile || outFile.length() <= 0L) {
                                    errText = "Export finished but produced no file."
                                } else {
                                    outInfo = "Export complete: ${outFile.name} (${outFile.length() / 1024} KB). " +
                                        "Only the resolution choice was applied; the frame rate and quality selections do not change the output in this version."
                                }
                            }
                            override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                                busy = false
                                runCatching { outFile.delete() }
                                errText = exportException.localizedMessage ?: "Export failed."
                            }
                        })
                        .build()
                    transformer.start(
                        EditedMediaItem.Builder(MediaItem.fromUri(uri))
                            .setEffects(Effects(emptyList(), effects))
                            .build(),
                        outFile.absolutePath
                    )
                },
                enabled = !busy && selectedFpsId != "60s",
                colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (busy) "Exporting your video..." else "Export video", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

/* ============================================================================
 * 6. ZIP TO VIDEO RENDERER (ported from source/src/ziprender.tsx)
 * ============================================================================ */

@Composable
fun ZipToVideoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var summaryText by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var isScanning by remember { mutableStateOf(false) }

    val zipPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                isScanning = true
                errorText = ""
                summaryText = ""
                runCatching {
                    val names = mutableListOf<String>()
                    var lottieSummary: String? = null
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        ZipInputStream(stream).use { zis ->
                            var entry = zis.nextEntry
                            while (entry != null) {
                                if (!entry.isDirectory && !entry.name.startsWith("__MACOSX")) {
                                    names.add(entry.name)
                                    if (entry.name.endsWith(".json", ignoreCase = true) && lottieSummary == null) {
                                        val txt = zis.readBytes().decodeToString()
                                        runCatching {
                                            val obj = JSONObject(txt)
                                            if (obj.has("layers") && obj.optInt("w") > 0 && obj.optInt("h") > 0) {
                                                val w = obj.getInt("w")
                                                val h = obj.getInt("h")
                                                val fr = obj.optInt("fr", 30)
                                                val frames = obj.optInt("op", 60) - obj.optInt("ip", 0)
                                                lottieSummary = "Lottie animation (${entry.name}) · ${w}x${h} · $fr FPS · $frames frames"
                                            }
                                        }
                                    }
                                }
                                entry = zis.nextEntry
                            }
                        }
                    }
                    if (names.isEmpty()) error("The ZIP is empty.")
                    val imgFrames = names.filter { it.lowercase().let { n -> n.endsWith(".png") || n.endsWith(".jpg") || n.endsWith(".jpeg") || n.endsWith(".webp") } }
                    when {
                        lottieSummary != null -> summaryText = lottieSummary!! + ". Video rendering is not available in v1.0."
                        imgFrames.size >= 2 -> summaryText = "Image sequence detected (${imgFrames.size} frames). Video rendering is not available in v1.0."
                        else -> {
                            val kind = when {
                                names.any { it.endsWith(".html", true) || it.endsWith(".js", true) } -> "a web animation (HTML, JS or CSS)"
                                names.any { it.endsWith(".riv", true) } -> "a Rive file"
                                names.any { it.endsWith(".aep", true) } -> "an After Effects project"
                                else -> "files this renderer does not understand"
                            }
                            error("This ZIP contains $kind, which cannot be rendered here. Supported: Lottie animations (JSON or .lottie) and image-sequence ZIPs (PNG, JPG or WebP frames).")
                        }
                    }
                }.onFailure { e ->
                    errorText = e.localizedMessage ?: "This file is not a valid ZIP archive."
                }
                isScanning = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XBlack)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = XWhite)
            }
            Text("ZIP TO VIDEO", color = XWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(RoundedCornerShape(20.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "Inspect an animation ZIP on your device (nothing is uploaded). Rendering to video is not available in v1.0; this tool only reads the ZIP and reports what it contains.",
                color = XWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Supported: Lottie animations (JSON or .lottie) and image-sequence ZIPs (PNG, JPG or WebP frames).",
                color = XWhiteMuted,
                fontSize = 12.sp
            )
            Button(
                onClick = { zipPicker.launch("application/zip") },
                colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isScanning) "Reading ZIP..." else "Choose a ZIP file", fontWeight = FontWeight.Bold)
            }

            if (summaryText.isNotBlank()) {
                Text(summaryText, color = XWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            if (errorText.isNotBlank()) {
                Text(errorText, color = XWhiteMuted, fontSize = 12.sp)
            }
        }
    }
}
