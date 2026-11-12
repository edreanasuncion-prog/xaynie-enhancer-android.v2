package com.xstudio.app.photoenhancer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xstudio.app.core.designsystem.XBlack
import com.xstudio.app.core.designsystem.XGlassBorder
import com.xstudio.app.core.designsystem.XWhite
import com.xstudio.app.core.designsystem.XWhiteMuted
import com.xstudio.app.core.designsystem.liquidGlass
import com.xstudio.app.core.ui.BeforeAfterSlider
import com.xstudio.app.core.media.MediaStoreExporter
import com.xstudio.app.core.ui.EnhancerIntroCard
import com.xstudio.app.data.local.EnhancementHistoryEntity
import com.xstudio.app.data.local.XStudioDatabase
import com.xstudio.app.data.model.EnhancementQuality
import com.xstudio.app.history.HistoryFiles
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

data class EnhanceOptions(
    val detail: Boolean = true,
    val repair: Boolean = true,
    val color: Boolean = false,
    val denoise: Boolean = false,
    val lowlight: Boolean = false
)

/** What one enhanced photo produced: the bitmap for preview, the saved JPEG and the real output size. */
data class EnhanceResult(val bitmap: Bitmap, val file: File, val plan: OutputPlan)

/**
 * On-device SOFTWARE photo enhancement: subsampled decode, memory-safe progressive scaling,
 * 5-tap unsharp mask and a luma-preserving contrast/saturation pass.
 * There is no neural network in this build; nothing here is "AI" and no model file is shipped.
 */
object PhotoEnhanceEngine {

    /**
     * - Preserves original aspect ratio
     * - Steps the resize in 2x stages for smooth interpolation
     * - Runs a 5-tap spatial sharpen + luminance-preserving color/contrast/low-light pass
     * The real output size is computed by [OutputPlan] and returned in [EnhanceResult.plan].
     */
    suspend fun enhanceImage(
        context: Context,
        sourceUri: Uri,
        quality: EnhancementQuality,
        strength: Int = 60,
        opt: EnhanceOptions = EnhanceOptions(),
        onProgress: (Float) -> Unit
    ): EnhanceResult = withContext(Dispatchers.Default) {
        onProgress(0.05f)

        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(sourceUri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOpts)
        }

        val srcW = boundsOpts.outWidth.coerceAtLeast(1)
        val srcH = boundsOpts.outHeight.coerceAtLeast(1)
        val maxSafeDecodeEdge = 4096
        var sampleSize = 1
        while (max(srcW, srcH) / sampleSize > maxSafeDecodeEdge) {
            sampleSize *= 2
        }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val inputBitmap = context.contentResolver.openInputStream(sourceUri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: error("Could not read that image. Try a JPG or PNG.")

        onProgress(0.15f)

        val iw = inputBitmap.width
        val ih = inputBitmap.height

        val runtime = Runtime.getRuntime()
        val availableHeapBytes = runtime.maxMemory() - (runtime.totalMemory() - runtime.freeMemory())
        val plan = OutputPlan.plan(
            decodedW = iw,
            decodedH = ih,
            requestedLabel = quality.label,
            requestedShortSidePx = quality.targetShortSidePx,
            maxSafePixels = OutputPlan.safePixelBudget(availableHeapBytes)
        )
        val outW = plan.outW
        val outH = plan.outH

        // Step resize in 2x increments as in source/src/ai.ts so large upscales stay smooth
        var currentBmp = inputBitmap
        while (currentBmp.width * 2 < outW && currentBmp.height * 2 < outH) {
            val nextBmp = Bitmap.createScaledBitmap(currentBmp, currentBmp.width * 2, currentBmp.height * 2, true)
            if (currentBmp != inputBitmap && !currentBmp.isRecycled) {
                currentBmp.recycle()
            }
            currentBmp = nextBmp
        }

        val workingBitmap = if (currentBmp.width == outW && currentBmp.height == outH && currentBmp.isMutable) {
            currentBmp
        } else {
            val scaled = Bitmap.createScaledBitmap(currentBmp, outW, outH, true)
            if (currentBmp != inputBitmap && currentBmp != scaled && !currentBmp.isRecycled) {
                currentBmp.recycle()
            }
            if (scaled.isMutable) scaled else scaled.copy(Bitmap.Config.ARGB_8888, true)
        }

        onProgress(0.25f)

        // Exact 5-tap unsharp mask + luma-preserving contrast/saturation pass from source/src/fx.ts
        val baseSharp = if (opt.detail) 1.1f else 0.35f
        val k = (strength / 100f) * baseSharp - (if (opt.denoise) 0.25f else 0f)
        val bMul = if (opt.lowlight) 1.3f else 1.0f
        val cMul = (if (opt.lowlight) 1.08f else 1.0f) *
            (if (opt.repair) 1.06f else 1.0f) *
            (if (opt.color) 1.05f else 1.0f)
        val sMul = (if (opt.repair) 1.04f else 1.0f) *
            (if (opt.color) 1.20f else 1.0f)

        val plain = bMul == 1.0f && cMul == 1.0f && sMul == 1.0f
        if (!plain || abs(k) >= 0.02f) {
            val w = workingBitmap.width
            val h = workingBitmap.height
            val band = 128
            val prevRow = IntArray(w)
            val currRow = IntArray(w)
            val nextRow = IntArray(w)
            val outRow = IntArray(w)

            workingBitmap.getPixels(currRow, 0, w, 0, 0, w, 1)
            System.arraycopy(currRow, 0, prevRow, 0, w)

            for (y in 0 until h) {
                if (y + 1 < h) {
                    workingBitmap.getPixels(nextRow, 0, w, 0, y + 1, w, 1)
                } else {
                    System.arraycopy(currRow, 0, nextRow, 0, w)
                }

                for (x in 0 until w) {
                    val c = currRow[x]
                    val a = (c ushr 24) and 0xFF
                    var r = ((c ushr 16) and 0xFF).toFloat()
                    var g = ((c ushr 8) and 0xFF).toFloat()
                    var b = (c and 0xFF).toFloat()

                    if (abs(k) >= 0.02f) {
                        val lPx = currRow[if (x > 0) x - 1 else x]
                        val rPx = currRow[if (x < w - 1) x + 1 else x]
                        val uPx = prevRow[x]
                        val dPx = nextRow[x]

                        r += k * (4f * r - ((lPx ushr 16) and 0xFF) - ((rPx ushr 16) and 0xFF) - ((uPx ushr 16) and 0xFF) - ((dPx ushr 16) and 0xFF))
                        g += k * (4f * g - ((lPx ushr 8) and 0xFF) - ((rPx ushr 8) and 0xFF) - ((uPx ushr 8) and 0xFF) - ((dPx ushr 8) and 0xFF))
                        b += k * (4f * b - (lPx and 0xFF) - (rPx and 0xFF) - (uPx and 0xFF) - (dPx and 0xFF))
                    }

                    if (!plain) {
                        r = (r * bMul - 128f) * cMul + 128f
                        g = (g * bMul - 128f) * cMul + 128f
                        b = (b * bMul - 128f) * cMul + 128f
                        val lum = 0.299f * r + 0.587f * g + 0.114f * b
                        r = lum + (r - lum) * sMul
                        g = lum + (g - lum) * sMul
                        b = lum + (b - lum) * sMul
                    }

                    val ri = r.roundToInt().coerceIn(0, 255)
                    val gi = g.roundToInt().coerceIn(0, 255)
                    val bi = b.roundToInt().coerceIn(0, 255)
                    outRow[x] = (a shl 24) or (ri shl 16) or (gi shl 8) or bi
                }

                workingBitmap.setPixels(outRow, 0, w, 0, y, w, 1)
                System.arraycopy(currRow, 0, prevRow, 0, w)
                System.arraycopy(nextRow, 0, currRow, 0, w)

                if (y % band == 0) {
                    onProgress(0.25f + 0.65f * (y.toFloat() / h.toFloat()))
                    yield()
                }
            }
        }

        onProgress(0.92f)

        val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
        val outDir = File(baseDir, "enhanced_photos").apply { mkdirs() }
        val outFile = File(outDir, "xaynie-enhanced-${System.currentTimeMillis()}-${quality.label}.jpg")
        val written = try {
            FileOutputStream(outFile).use { fos ->
                workingBitmap.compress(Bitmap.CompressFormat.JPEG, 92, fos)
            }
        } catch (e: Exception) {
            false
        }
        if (!written || outFile.length() <= 0L) {
            runCatching { outFile.delete() }
            error("Could not save the enhanced photo to app storage.")
        }

        val now = System.currentTimeMillis()
        XStudioDatabase.getInstance(context).historyDao().insertHistory(
            EnhancementHistoryEntity(
                id = UUID.randomUUID().toString(),
                mediaType = "PHOTO",
                originalUri = sourceUri.toString(),
                enhancedFilePath = outFile.absolutePath,
                resolutionLabel = plan.historyLabel,
                createdAtEpochMs = now,
                expiresAtEpochMs = HistoryFiles.photoExpiryFor(now)
            )
        )

        onProgress(1.0f)
        EnhanceResult(workingBitmap, outFile, plan)
    }

    /** Kept for existing callers; delegates to the shared, failure-safe exporter. */
    suspend fun saveToDeviceGallery(context: Context, file: File): Boolean =
        MediaStoreExporter.saveToGallery(context, file)
}

@Composable
fun PhotoEnhancerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val selectedUris = remember { mutableStateListOf<Uri>() }
    var activeIndex by remember { mutableIntStateOf(0) }
    var selectedQuality by remember { mutableStateOf(EnhancementQuality.P1080) }
    var strength by remember { mutableFloatStateOf(60f) }
    var opt by remember { mutableStateOf(EnhanceOptions()) }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var statusText by remember { mutableStateOf<String?>(null) }

    var beforeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var afterBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val batchOutputFiles = remember { mutableStateListOf<File>() }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedUris.clear()
            selectedUris.addAll(uris.take(5))
            activeIndex = 0
            batchOutputFiles.clear()
            afterBitmap = null
            scope.launch(Dispatchers.IO) {
                val bmp = context.contentResolver.openInputStream(selectedUris[0])?.use {
                    BitmapFactory.decodeStream(it)
                }
                beforeBitmap = bmp
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
            Column {
                Text("PHOTO ENHANCE", color = XWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    text = "Software upscale · 5-tap sharpen & luma recovery · Batch up to 5 photos",
                    color = XWhiteMuted,
                    fontSize = 11.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                
                .liquidGlass(RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            val currentBefore = beforeBitmap
            val currentAfter = afterBitmap
            if (currentBefore != null && currentAfter != null) {
                // The slider draws on a Canvas (no intrinsic size), so it needs an explicit aspect ratio
                // or it collapses to zero height inside the scrolling column.
                BeforeAfterSlider(
                    beforeBitmap = currentBefore,
                    afterBitmap = currentAfter,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(currentAfter.width.toFloat() / currentAfter.height.toFloat())
                )
            } else if (selectedUris.isNotEmpty()) {
                AsyncImage(
                    model = selectedUris[activeIndex],
                    contentDescription = "Selected Preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                )
            } else {
                EnhancerIntroCard(
                    title = "Enhancer",
                    description = "Upscale and sharpen images on your device. No AI model is used.",
                    note = "JPG, PNG or WEBP. Runs on your device (up to 5 photos).",
                    onPickClick = {
                        pickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                )
            }
        }

        if (selectedUris.isNotEmpty()) {
        if (selectedUris.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                selectedUris.forEachIndexed { index, uri ->
                    AsyncImage(
                        model = uri,
                        contentDescription = "Batch item ${index + 1}",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                width = if (index == activeIndex) 2.dp else 1.dp,
                                color = if (index == activeIndex) XWhite else XGlassBorder,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { activeIndex = index }
                    )
                }
            }
        }

        Text("RESOLUTION", color = XWhiteMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            EnhancementQuality.entries.forEach { option ->
                val selected = option == selectedQuality
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) XWhite else XBlack)
                        .border(1.dp, if (selected) XWhite else XGlassBorder, RoundedCornerShape(12.dp))
                        .clickable { selectedQuality = option }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = option.label,
                        color = if (selected) XBlack else XWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text(
            text = "Output is capped at 8192 px per side and a memory-safe pixel budget (about 24 MP at most), " +
                "so 8K is only reached for near-square images. The real size is shown after each photo.",
            color = XWhiteMuted,
            fontSize = 11.sp
        )

        Column {
            Text("Strength: ${strength.roundToInt()}", color = XWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = strength,
                onValueChange = { strength = it },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(thumbColor = XWhite, activeTrackColor = XWhite)
            )
        }

        // Exact 5 Toggles from source/src/enhance.tsx
        val toggleItems = listOf(
            Triple("Detail boost", opt.detail) { opt = opt.copy(detail = !opt.detail) },
            Triple("Auto repair", opt.repair) { opt = opt.copy(repair = !opt.repair) },
            Triple("Color correction", opt.color) { opt = opt.copy(color = !opt.color) },
            Triple("Denoise", opt.denoise) { opt = opt.copy(denoise = !opt.denoise) },
            Triple("Low-light repair", opt.lowlight) { opt = opt.copy(lowlight = !opt.lowlight) }
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            toggleItems.forEach { (label, active, onToggle) ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) XWhite else XBlack)
                        .border(1.dp, if (active) XWhite else XGlassBorder, RoundedCornerShape(12.dp))
                        .clickable { onToggle() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = label,
                        color = if (active) XBlack else XWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (isProcessing) {
            LinearProgressIndicator(
                progress = { progress },
                color = XWhite,
                trackColor = XGlassBorder,
                modifier = Modifier.fillMaxWidth()
            )
        }

        statusText?.let {
            Text(text = it, color = XWhiteMuted, fontSize = 12.sp)
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    pickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = XBlack, contentColor = XWhite),
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, XGlassBorder, RoundedCornerShape(14.dp))
            ) {
                Text("Select (${selectedUris.size}/5)")
            }

            Button(
                onClick = {
                    if (selectedUris.isEmpty()) return@Button
                    scope.launch {
                        isProcessing = true
                        batchOutputFiles.clear()
                        val sizeReports = mutableListOf<String>()
                        runCatching {
                            selectedUris.forEachIndexed { idx, uri ->
                                statusText = "Enhancing photo ${idx + 1} of ${selectedUris.size}..."
                                val result = PhotoEnhanceEngine.enhanceImage(
                                    context = context,
                                    sourceUri = uri,
                                    quality = selectedQuality,
                                    strength = strength.roundToInt(),
                                    opt = opt,
                                    onProgress = { p ->
                                        progress = ((idx + p) / selectedUris.size.toFloat()).coerceIn(0f, 1f)
                                    }
                                )
                                batchOutputFiles.add(result.file)
                                sizeReports.add("#${idx + 1}: ${result.plan.statusText}")
                                if (idx == activeIndex) {
                                    afterBitmap = result.bitmap
                                }
                            }
                            statusText = "Completed ${batchOutputFiles.size} photo(s). Drag slider to compare.\n" +
                                sizeReports.joinToString("\n")
                        }.onFailure { e ->
                            val done = if (sizeReports.isNotEmpty()) "\n" + sizeReports.joinToString("\n") else ""
                            statusText = (e.localizedMessage ?: "Enhancement failed.") + done
                        }
                        isProcessing = false
                    }
                },
                enabled = selectedUris.isNotEmpty() && !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
                modifier = Modifier.weight(1f)
            ) {
                Text(if (selectedUris.size > 1) "Enhance all" else "Enhance", fontWeight = FontWeight.Bold)
            }
        }

        if (batchOutputFiles.isNotEmpty()) {
            Button(
                onClick = {
                    scope.launch {
                        var savedCount = 0
                        var lastFailure: String? = null
                        batchOutputFiles.forEach { file ->
                            when (val outcome = MediaStoreExporter.export(context, file)) {
                                is MediaStoreExporter.Outcome.Saved -> savedCount++
                                is MediaStoreExporter.Outcome.Failed -> lastFailure = outcome.reason
                            }
                        }
                        statusText = if (savedCount == batchOutputFiles.size) {
                            "Saved $savedCount JPG(s) to Pictures/XStudio"
                        } else {
                            "Saved $savedCount of ${batchOutputFiles.size} JPG(s) to Pictures/XStudio. " +
                                "Failed: ${lastFailure ?: "unknown error"}"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Download, contentDescription = null, tint = XBlack)
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = if (batchOutputFiles.size > 1) "DOWNLOAD ALL (${batchOutputFiles.size})" else "DOWNLOAD JPG",
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        }
    }
}
