package com.xstudio.app.videoenhancer

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.Contrast
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.Presentation
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.ui.PlayerView
import com.xstudio.app.core.designsystem.XBlack
import com.xstudio.app.core.designsystem.XGlassBorder
import com.xstudio.app.core.designsystem.XWhite
import com.xstudio.app.core.designsystem.XWhiteMuted
import com.xstudio.app.core.designsystem.liquidGlass
import com.xstudio.app.core.media.MediaStoreExporter
import com.xstudio.app.core.ui.EnhancerIntroCard
import com.xstudio.app.data.local.EnhancementHistoryEntity
import com.xstudio.app.data.local.XStudioDatabase
import com.xstudio.app.data.model.EnhancementQuality
import com.xstudio.app.data.model.VideoFpsOption
import java.io.File
import java.util.UUID
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

private const val MAX_CLIP_BYTES = 150L * 1024L * 1024L

/** What was actually measured from a video file (source clip or finished export). */
private data class VideoProbe(
    val width: Int,
    val height: Int,
    val fps: Float?,
    val durationMs: Long?
) {
    val sizeText: String get() = "${width}×${height}"
    val fpsText: String get() = fps?.let { "~" + (if (kotlin.math.abs(it - it.roundToInt()) < 0.05f) it.roundToInt().toString() else "%.2f".format(it)) + " fps" } ?: "frame rate unknown"
}

private fun probeVideo(context: Context, uri: Uri): VideoProbe? {
    val retriever = MediaMetadataRetriever()
    return try {
        retriever.setDataSource(context, uri)
        var w = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: return null
        var h = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: return null
        val rotation = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        if (rotation == 90 || rotation == 270) {
            val t = w; w = h; h = t
        }
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()

        // 1) measured: frame count / duration (API 28+)
        var fps: Float? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val frames = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_FRAME_COUNT)?.toIntOrNull()
            if (frames != null && frames > 1 && durationMs != null && durationMs > 0L) {
                fps = frames * 1000f / durationMs.toFloat()
            }
        }
        // 2) declared in the container
        if (fps == null) fps = declaredFrameRate(context, uri)
        VideoProbe(w, h, fps?.takeIf { it > 0f }, durationMs)
    } catch (e: Exception) {
        null
    } finally {
        runCatching { retriever.release() }
    }
}

private fun declaredFrameRate(context: Context, uri: Uri): Float? {
    val extractor = MediaExtractor()
    return try {
        extractor.setDataSource(context, uri, null)
        var result: Float? = null
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME).orEmpty()
            if (mime.startsWith("video/") && format.containsKey(MediaFormat.KEY_FRAME_RATE)) {
                result = runCatching { format.getInteger(MediaFormat.KEY_FRAME_RATE).toFloat() }
                    .getOrElse { runCatching { format.getFloat(MediaFormat.KEY_FRAME_RATE) }.getOrNull() }
                break
            }
        }
        result
    } catch (e: Exception) {
        null
    } finally {
        runCatching { extractor.release() }
    }
}

private fun querySizeBytes(context: Context, uri: Uri): Long? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
        if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
    }
}.getOrNull()

/** Resolution text for history/status that never claims a tier the file does not have. */
private fun describeResolution(probe: VideoProbe?, requested: EnhancementQuality): String {
    if (probe == null) return "${requested.label} (not verified)"
    val shortSide = min(probe.width, probe.height)
    val target = requested.targetShortSidePx
    return if (target == null || kotlin.math.abs(shortSide - target) <= 1) {
        "${probe.width}x${probe.height} (${requested.label})"
    } else {
        "${probe.width}x${probe.height} (requested ${requested.label})"
    }
}

@OptIn(UnstableApi::class)
private fun describeExportFailure(e: ExportException): String = when (e.errorCode) {
    ExportException.ERROR_CODE_ENCODER_INIT_FAILED,
    ExportException.ERROR_CODE_ENCODING_FORMAT_UNSUPPORTED ->
        "This device's video encoder could not start for these settings. Try a lower resolution or Original."
    ExportException.ERROR_CODE_DECODER_INIT_FAILED,
    ExportException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
        "This device cannot decode the selected video. Try a different clip."
    else -> "Export failed: ${e.localizedMessage ?: "unknown error"}. Try a lower resolution."
}

@OptIn(UnstableApi::class)
@Composable
fun VideoEnhancerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var outputVideoFile by remember { mutableStateOf<File?>(null) }
    var selectedResolution by remember { mutableStateOf(EnhancementQuality.P1080) }
    var selectedFps by remember { mutableStateOf(VideoFpsOption.SOURCE) }
    var sourceProbe by remember { mutableStateOf<VideoProbe?>(null) }
    var pendingOutFile by remember { mutableStateOf<File?>(null) }
    var strength by remember { mutableFloatStateOf(60f) }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var activeTransformer by remember { mutableStateOf<Transformer?>(null) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    DisposableEffect(Unit) {
        onDispose {
            activeTransformer?.cancel()
            pendingOutFile?.let { runCatching { it.delete() } }
            exoPlayer.release()
        }
    }

    // Detect the clip's real resolution and frame rate whenever a new video is chosen.
    LaunchedEffect(selectedVideoUri) {
        val uri = selectedVideoUri
        if (uri == null) {
            sourceProbe = null
        } else {
            val probe = withContext(Dispatchers.IO) { probeVideo(context, uri) }
            sourceProbe = probe
            selectedFps = VideoFpsPolicy.sanitize(probe?.fps, selectedFps)
        }
    }

    LaunchedEffect(isProcessing, activeTransformer) {
        val holder = ProgressHolder()
        while (isProcessing && activeTransformer != null) {
            val state = activeTransformer?.getProgress(holder)
            if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                progress = (holder.progress / 100f).coerceIn(0.01f, 0.99f)
            }
            delay(200L)
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val sizeBytes = querySizeBytes(context, uri)
            if (sizeBytes != null && sizeBytes > MAX_CLIP_BYTES) {
                statusMessage = "That clip is ${sizeBytes / (1024 * 1024)} MB. The limit is 150 MB per clip."
            } else {
                selectedVideoUri = uri
                outputVideoFile = null
                sourceProbe = null
                selectedFps = VideoFpsOption.SOURCE
                statusMessage = null
                exoPlayer.setMediaItem(MediaItem.fromUri(uri))
                exoPlayer.prepare()
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
                Text("VIDEO ENHANCE & TIKTOK EXPORTER", color = XWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    text = "Hardware Media3 GPU Pipeline · Max 150 MB clip · FPS can be lowered, not raised",
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
            if (selectedVideoUri != null) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            player = exoPlayer
                            useController = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )
            } else {
                EnhancerIntroCard(
                    title = "Video Enhancer",
                    description = "Scale videos up to 4K and adjust contrast on your device GPU.",
                    note = "Max 150 MB per clip",
                    onPickClick = {
                        videoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }
                )
            }
        }

        if (selectedVideoUri != null) {
        Text("RESOLUTION", color = XWhiteMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            listOf(
                EnhancementQuality.ORIGINAL,
                EnhancementQuality.P720,
                EnhancementQuality.P1080,
                EnhancementQuality.K2,
                EnhancementQuality.K4
            ).forEach { res ->
                val selected = res == selectedResolution
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) XWhite else XBlack)
                        .border(1.dp, if (selected) XWhite else XGlassBorder, RoundedCornerShape(12.dp))
                        .clickable { selectedResolution = res }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = res.label,
                        color = if (selected) XBlack else XWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Text("FRAME RATE (FPS)", color = XWhiteMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        val fpsChoices = VideoFpsPolicy.choices(sourceProbe?.fps)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.horizontalScroll(rememberScrollState())
        ) {
            fpsChoices.forEach { choice ->
                val selected = choice.option == selectedFps
                val alphaValue = if (choice.enabled) 1f else 0.35f
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) XWhite else XBlack)
                        .border(1.dp, if (selected) XWhite else XGlassBorder, RoundedCornerShape(12.dp))
                        .then(if (choice.enabled) Modifier.clickable { selectedFps = choice.option } else Modifier)
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = choice.option.label,
                        color = (if (selected) XBlack else XWhite).copy(alpha = alphaValue),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Column {
            Text("Strength: ${strength.roundToInt()}", color = XWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Slider(
                value = strength,
                onValueChange = { strength = it },
                valueRange = 0f..100f,
                colors = SliderDefaults.colors(thumbColor = XWhite, activeTrackColor = XWhite)
            )
        }

        val probeNow = sourceProbe
        val fpsPlanNow = VideoFpsPolicy.plan(probeNow?.fps, selectedFps)
        Text(
            text = buildString {
                append("Runs on your device GPU via AndroidX Media3. ")
                if (probeNow != null) {
                    append("Source: ${probeNow.sizeText} · ${probeNow.fpsText}. ")
                } else {
                    append("Reading the clip's size and frame rate... ")
                }
                append(fpsPlanNow?.description ?: "That frame rate is not available for this clip.")
                append(" Higher frame rates need new frames, which this app does not generate.")
            },
            color = XWhiteMuted,
            fontSize = 11.sp
        )

        if (isProcessing) {
            LinearProgressIndicator(
                progress = { progress },
                color = XWhite,
                trackColor = XGlassBorder,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    activeTransformer?.cancel()
                    activeTransformer = null
                    isProcessing = false
                    pendingOutFile?.let { runCatching { it.delete() } }
                    pendingOutFile = null
                    statusMessage = "Video export cancelled. Nothing was saved."
                },
                colors = ButtonDefaults.buttonColors(containerColor = XBlack, contentColor = XWhite),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, XGlassBorder, RoundedCornerShape(14.dp))
            ) {
                Text("Cancel")
            }
        }

        statusMessage?.let {
            Text(text = it, color = XWhiteMuted, fontSize = 12.sp)
        }

        Button(
            onClick = {
                val inputUri = selectedVideoUri ?: return@Button
                val fpsPlan = VideoFpsPolicy.plan(sourceProbe?.fps, selectedFps)
                if (fpsPlan == null) {
                    statusMessage = "That frame rate is not available for this clip. Choose Source FPS or a lower rate."
                    return@Button
                }
                val baseDir = context.getExternalFilesDir(null) ?: context.filesDir
                val outDir = File(baseDir, "enhanced_videos").apply { mkdirs() }
                val fpsTag = if (fpsPlan.dropToFps != null) "${selectedFps.fps}fps" else "srcfps"
                val outFile = File(
                    outDir,
                    "xaynie-video-${System.currentTimeMillis()}-${selectedResolution.label}-$fpsTag.mp4"
                )
                val requestedResolution = selectedResolution
                isProcessing = true
                progress = 0.02f
                statusMessage = "Enhancing video on GPU..."
                pendingOutFile = outFile

                val effectsList = buildList<Effect> {
                    requestedResolution.targetShortSidePx?.let { shortSide ->
                        add(Presentation.createForShortSide(shortSide))
                    }
                    // FrameDropEffect can only REMOVE frames. It is added only for a real reduction
                    // (source rate above the chosen rate); it never creates or interpolates frames.
                    fpsPlan.dropToFps?.let { target ->
                        add(FrameDropEffect.createDefaultFrameDropEffect(target))
                    }
                    val contrastDelta = ((strength - 50f) / 250f).coerceIn(-0.15f, 0.25f)
                    if (abs(contrastDelta) >= 0.01f) {
                        add(Contrast(contrastDelta))
                    }
                }

                fun failExport(message: String) {
                    isProcessing = false
                    activeTransformer = null
                    pendingOutFile = null
                    runCatching { outFile.delete() }   // never keep a partial file
                    statusMessage = message
                }

                val transformer = Transformer.Builder(context)
                    .setVideoMimeType(MimeTypes.VIDEO_H264)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                            if (!outFile.isFile || outFile.length() <= 0L) {
                                failExport("Export finished but produced no video file. Nothing was saved to history.")
                                return
                            }
                            isProcessing = false
                            progress = 1.0f
                            activeTransformer = null
                            pendingOutFile = null
                            outputVideoFile = outFile
                            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.fromFile(outFile)))
                            exoPlayer.prepare()
                            scope.launch {
                                val outProbe = withContext(Dispatchers.IO) { probeVideo(context, Uri.fromFile(outFile)) }
                                val actual = outProbe?.let { "${it.sizeText} · ${it.fpsText}" } ?: "size and frame rate could not be verified"
                                statusMessage = "Enhanced. Output: $actual. ${fpsPlan.description} Previewing result."
                                val fpsLabel = when {
                                    outProbe?.fps != null && fpsPlan.dropToFps != null -> "${outProbe.fpsText} (frames dropped, none created)"
                                    outProbe?.fps != null -> "${outProbe.fpsText} (source rate kept)"
                                    else -> "source rate kept"
                                }
                                XStudioDatabase.getInstance(context).historyDao().insertHistory(
                                    EnhancementHistoryEntity(
                                        id = UUID.randomUUID().toString(),
                                        mediaType = "VIDEO",
                                        originalUri = inputUri.toString(),
                                        enhancedFilePath = outFile.absolutePath,
                                        resolutionLabel = describeResolution(outProbe, requestedResolution),
                                        fpsLabel = fpsLabel,
                                        createdAtEpochMs = System.currentTimeMillis(),
                                        expiresAtEpochMs = null
                                    )
                                )
                            }
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException
                        ) {
                            // Failed exports are never added to history and leave no partial file behind.
                            failExport(describeExportFailure(exportException))
                        }
                    })
                    .build()

                activeTransformer = transformer
                try {
                    val editedItem = EditedMediaItem.Builder(MediaItem.fromUri(inputUri))
                        .setEffects(Effects(emptyList(), effectsList))
                        .build()
                    transformer.start(editedItem, outFile.absolutePath)
                } catch (e: Exception) {
                    failExport("Could not start the export: ${e.localizedMessage ?: "unknown error"}")
                }
            },
            enabled = selectedVideoUri != null && !isProcessing && VideoFpsPolicy.plan(sourceProbe?.fps, selectedFps) != null,
            colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ENHANCE VIDEO", fontWeight = FontWeight.ExtraBold)
        }

        outputVideoFile?.let { file ->
            Button(
                onClick = {
                    scope.launch {
                        statusMessage = when (val outcome = MediaStoreExporter.export(context, file)) {
                            is MediaStoreExporter.Outcome.Saved -> "Saved to Movies/XStudio/${file.name}"
                            is MediaStoreExporter.Outcome.Failed -> "Could not save video: ${outcome.reason}"
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("DOWNLOAD MP4 TO DEVICE", fontWeight = FontWeight.Bold)
            }
        }
        }
    }
}
