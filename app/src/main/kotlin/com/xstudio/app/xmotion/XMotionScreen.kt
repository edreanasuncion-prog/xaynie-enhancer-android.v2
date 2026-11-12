package com.xstudio.app.xmotion

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xstudio.app.core.designsystem.XBlack
import com.xstudio.app.core.designsystem.XDarkBg
import com.xstudio.app.core.designsystem.XGlassBorder
import com.xstudio.app.core.designsystem.XSurface
import com.xstudio.app.core.designsystem.XWhite
import com.xstudio.app.core.designsystem.XWhiteMuted
import com.xstudio.app.core.designsystem.liquidGlass
import com.xstudio.app.core.media.MediaStoreExporter
import java.io.File
import java.io.FileOutputStream
import java.io.StringReader
import java.net.URI
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import org.xml.sax.InputSource

private val AM_HOSTS = listOf("alight.link", "alightmotion.com", "alightcreative.com")

data class MotionKeyframe(
    val timeSec: Float,
    val offsetX: Float,
    val offsetY: Float,
    val scale: Float,
    val rotationDeg: Float
)

data class MotionLayer(
    val id: String,
    val label: String,
    val rounded: Boolean = false,
    val startMs: Int = 0,
    val endMs: Int = 2000,
    var offsetX: Float = 260f,
    var offsetY: Float = 260f,
    var scale: Float = 1f,
    var rotationDeg: Float = 0f,
    var opacity: Float = 1f,
    val fillColor: Color = Color(0xFF7DD3FC),
    val keyframes: MutableList<MotionKeyframe> = mutableListOf()
)

data class XMotionProject(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "New Project",
    val w: Int = 1080,
    val h: Int = 1920,
    val fps: Int = 60,
    val durationMs: Int = 10000,
    val xmlNotice: String? = null,
    /** Exact text of the imported Alight Motion XML, kept in memory for this session only. Null for native projects. */
    val sourceXml: String? = null,
    /** Element path -> how many times it appeared but was NOT imported (e.g. "effect" -> 3). */
    val unsupportedElements: Map<String, Int> = emptyMap(),
    val layers: MutableList<MotionLayer> = mutableListOf()
)

fun parseAlightMotionXml(xmlText: String, fallbackName: String): XMotionProject {
    if (xmlText.contains("<!DOCTYPE", ignoreCase = true) || xmlText.contains("<!ENTITY", ignoreCase = true)) {
        error("XML with DTD or entity declarations is blocked for safety.")
    }
    // The DOCTYPE/ENTITY text check above is the guaranteed guard. These parser hardening flags are
    // best-effort: Android's built-in DocumentBuilderFactory rejects features it does not know and
    // would otherwise make EVERY import fail with ParserConfigurationException.
    val factory = DocumentBuilderFactory.newInstance().apply {
        runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
    }
    val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(xmlText)))
    val root = doc.documentElement
    if (root.tagName != "scene") {
        error("This is valid XML but not an Alight Motion project (no <scene> root element).")
    }
    val w = root.getAttribute("width").toIntOrNull()?.takeIf { it > 0 } ?: 1080
    val h = root.getAttribute("height").toIntOrNull()?.takeIf { it > 0 } ?: 1920
    val fps = root.getAttribute("fps").toIntOrNull()?.takeIf { it > 0 } ?: 60
    val title = root.getAttribute("title").ifBlank { fallbackName }

    val layers = mutableListOf<MotionLayer>()
    var maxEnd = 4000
    val unsupported = linkedMapOf<String, Int>()
    fun skipped(path: String) {
        unsupported[path] = (unsupported[path] ?: 0) + 1
    }

    val children = root.childNodes
    for (i in 0 until children.length) {
        val node = children.item(i) as? Element ?: continue
        when (node.tagName) {
            "shape" -> {
                val id = node.getAttribute("id").ifBlank { UUID.randomUUID().toString() }
                val label = node.getAttribute("label").ifBlank { "Shape ${layers.size + 1}" }
                val startMs = node.getAttribute("startTime").toIntOrNull() ?: 0
                val endMs = node.getAttribute("endTime").toIntOrNull() ?: 3000
                if (endMs > maxEnd) maxEnd = endMs
                val shapeType = node.getAttribute("s")
                val rounded = shapeType.contains("round", ignoreCase = true)

                var ox = 260f
                var oy = 260f
                var sc = 1f
                var rot = 0f
                var op = 1f
                var color = Color(0xFF7DD3FC)

                val sub = node.childNodes
                for (j in 0 until sub.length) {
                    val el = sub.item(j) as? Element ?: continue
                    if (el.tagName == "transform") {
                        val tKids = el.childNodes
                        for (k in 0 until tKids.length) {
                            val tk = tKids.item(k) as? Element ?: continue
                            val v = tk.getAttribute("value")
                            when (tk.tagName) {
                                "location" -> {
                                    val parts = v.split(",")
                                    if (parts.size >= 2) {
                                        ox = (parts[0].toFloatOrNull() ?: 540f) * (520f / w.toFloat())
                                        oy = (parts[1].toFloatOrNull() ?: 960f) * (520f / h.toFloat())
                                    }
                                }
                                "scale" -> {
                                    val parts = v.split(",")
                                    sc = (parts.firstOrNull()?.toFloatOrNull() ?: 1f).coerceIn(0.1f, 5f)
                                }
                                "rotation" -> rot = v.toFloatOrNull() ?: 0f
                                "opacity" -> op = (v.toFloatOrNull() ?: 1f).coerceIn(0f, 1f)
                                else -> skipped("shape/transform/${tk.tagName}")
                            }
                        }
                    } else if (el.tagName == "fillColor") {
                        val hex = el.getAttribute("value").removePrefix("#")
                        if (hex.length == 8) {
                            runCatching {
                                val r = hex.substring(0, 2).toInt(16)
                                val g = hex.substring(2, 4).toInt(16)
                                val b = hex.substring(4, 6).toInt(16)
                                val a = hex.substring(6, 8).toInt(16)
                                color = Color(r, g, b, a)
                            }
                        }
                    } else {
                        skipped("shape/${el.tagName}")
                    }
                }
                layers.add(
                    MotionLayer(
                        id = id,
                        label = label,
                        rounded = rounded,
                        startMs = startMs,
                        endMs = endMs,
                        offsetX = ox,
                        offsetY = oy,
                        scale = sc,
                        rotationDeg = rot,
                        opacity = op,
                        fillColor = color
                    )
                )
            }
            else -> skipped(node.tagName)   // media, bookmark, audio, effect, null objects, ...
        }
    }

    val notice = buildImportNotice(layers.size, unsupported)

    return XMotionProject(
        name = title,
        w = w,
        h = h,
        fps = fps,
        durationMs = maxEnd.coerceAtLeast(2000),
        xmlNotice = notice,
        sourceXml = xmlText,
        unsupportedElements = unsupported,
        layers = layers
    )
}

/**
 * Honest import summary. Only shape layers (location, scale, rotation, opacity, fill color, time range)
 * are read. Everything else is listed as NOT imported; the original XML text is kept in memory only.
 */
fun buildImportNotice(shapeLayerCount: Int, unsupported: Map<String, Int>): String {
    val head = "Imported $shapeLayerCount shape layer(s)."
    return if (unsupported.isEmpty()) {
        "$head No unsupported elements were found. Only shape position, scale, rotation, opacity, fill color and " +
            "timing are read; other attributes are ignored. The original XML text is kept in memory for this session only."
    } else {
        val list = unsupported.entries.joinToString(", ") { (tag, n) -> "$tag ×$n" }
        "$head NOT imported (not rendered, not exported): $list. " +
            "The original XML text is kept in memory for this session only; it is not saved or re-exported."
    }
}

/** Number of layers visible at [playheadMs]. */
fun visibleLayerCount(project: XMotionProject, playheadMs: Float): Int =
    project.layers.count { playheadMs >= it.startMs && playheadMs <= it.endMs }

/** True if any sampled pixel differs from the flat background, i.e. something was really drawn. */
fun bitmapHasDrawnContent(bmp: Bitmap, background: Int): Boolean {
    val stepX = (bmp.width / 96).coerceAtLeast(1)
    val stepY = (bmp.height / 96).coerceAtLeast(1)
    var y = 0
    while (y < bmp.height) {
        var x = 0
        while (x < bmp.width) {
            if (bmp.getPixel(x, y) != background) return true
            x += stepX
        }
        y += stepY
    }
    return false
}

fun renderProjectFrameBitmap(project: XMotionProject, playheadMs: Float, previewRefSize: Float = 520f): Bitmap {
    val bmp = Bitmap.createBitmap(project.w, project.h, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bmp)
    canvas.drawColor(XDarkBg.toArgb())

    val scaleX = project.w.toFloat() / previewRefSize
    val scaleY = project.h.toFloat() / previewRefSize
    val shapeScale = scaleX   // one factor for both axes so shapes are not stretched on tall/wide frames
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

    project.layers.forEach { layer ->
        if (playheadMs >= layer.startMs && playheadMs <= layer.endMs) {
            canvas.save()
            canvas.translate(layer.offsetX * scaleX, layer.offsetY * scaleY)
            canvas.rotate(layer.rotationDeg)
            canvas.scale(layer.scale * shapeScale, layer.scale * shapeScale)

            paint.color = layer.fillColor.copy(alpha = (layer.fillColor.alpha * layer.opacity).coerceIn(0f, 1f)).toArgb()
            val rect = RectF(-70f, -70f, 70f, 70f)
            if (layer.rounded) {
                canvas.drawRoundRect(rect, 28f, 28f, paint)
            } else {
                canvas.drawRect(rect, paint)
            }
            canvas.restore()
        }
    }
    return bmp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XMotionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val projects = remember { mutableStateListOf<XMotionProject>() }
    var activeProject by remember { mutableStateOf<XMotionProject?>(null) }
    var hubTab by remember { mutableStateOf("Projects") }
    var projectSearch by remember { mutableStateOf("") }
    var xmlBannerNote by remember { mutableStateOf<String?>(null) }

    var showNewDialog by remember { mutableStateOf(false) }
    var showMenuSheet by remember { mutableStateOf(false) }
    var showLinkSheet by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("New Project") }
    var shareLinkInput by remember { mutableStateOf("") }
    var shareLinkMessage by remember { mutableStateOf("") }

    val xmlPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                runCatching {
                    val xmlText = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                            ?: error("Could not read XML file.")
                    }
                    val parsed = parseAlightMotionXml(xmlText, "Imported XML")
                    projects.add(0, parsed)
                    xmlBannerNote = parsed.xmlNotice ?: "Imported ${parsed.name} (${parsed.layers.size} layers)"
                    activeProject = parsed
                }.onFailure { e ->
                    xmlBannerNote = e.localizedMessage ?: "Invalid XML file."
                }
            }
        }
    }

    val currentProj = activeProject
    if (currentProj == null) {
        if (showNewDialog) {
            ModalBottomSheet(
                onDismissRequest = { showNewDialog = false },
                containerColor = XSurface,
                contentColor = XWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("New project", color = XWhite, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    OutlinedTextField(
                        value = newProjectName,
                        onValueChange = { newProjectName = it },
                        label = { Text("Project name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Resolution · 1080 × 1920 · 60 fps", color = XWhiteMuted, fontSize = 12.sp)
                    Button(
                        onClick = {
                            val created = XMotionProject(
                                name = newProjectName.trim().ifBlank { "New Project" },
                                w = 1080,
                                h = 1920,
                                fps = 60
                            )
                            projects.add(0, created)
                            showNewDialog = false
                            activeProject = created
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("CREATE PROJECT", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        if (showMenuSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMenuSheet = false },
                containerColor = XSurface,
                contentColor = XWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Settings", color = XWhite, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Button(
                        onClick = {
                            showMenuSheet = false
                            showLinkSheet = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Import from link", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (showLinkSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLinkSheet = false },
                containerColor = XSurface,
                contentColor = XWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Import from link", color = XWhite, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    OutlinedTextField(
                        value = shareLinkInput,
                        onValueChange = { shareLinkInput = it },
                        placeholder = { Text("https://alightcreative.com/am/share/...") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val valid = runCatching {
                                val u = URI(shareLinkInput.trim())
                                val host = u.host?.lowercase().orEmpty()
                                u.scheme == "https" && AM_HOSTS.any { h -> host == h || host.endsWith(".$h") }
                            }.getOrDefault(false)
                            shareLinkMessage = if (valid) {
                                "This is an Alight Motion share page. It does not give the app the project data, so it cannot be imported from here. Open it in Alight Motion, export the project as XML, then use Upload XML file."
                            } else {
                                "That is not a valid Alight Motion share link (https required)."
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Check link", fontWeight = FontWeight.Bold)
                    }
                    if (shareLinkMessage.isNotBlank()) {
                        Text(shareLinkMessage, color = XWhiteMuted, fontSize = 12.sp)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0x14FFFFFF))
                            .border(1.dp, XGlassBorder, CircleShape)
                            .clickable { showMenuSheet = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = XWhite, modifier = Modifier.size(20.dp))
                    }
                    Text("X MOTION", color = XWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                        .border(1.dp, XGlassBorder, CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Home, contentDescription = "Home", tint = XWhite, modifier = Modifier.size(20.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .liquidGlass(RoundedCornerShape(28.dp))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Movie, contentDescription = null, tint = XWhite, modifier = Modifier.size(22.dp))
                    Text("Upload XML file", color = XWhite, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x14FFFFFF))))
                        .border(1.dp, Color(0x4DFFFFFF), RoundedCornerShape(999.dp))
                        .clickable { xmlPicker.launch(arrayOf("text/xml", "application/xml", "*/*")) }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text("Upload", color = XWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            xmlBannerNote?.let {
                Text(it, color = XWhiteMuted, fontSize = 12.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                listOf("Projects", "Your Templates", "Elements").forEach { tab ->
                    val active = hubTab == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (active) Color(0x24FFFFFF) else Color.Transparent)
                            .border(1.dp, if (active) Color(0x4DFFFFFF) else Color.Transparent, RoundedCornerShape(999.dp))
                            .clickable { hubTab = tab }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = tab,
                            color = if (active) XWhite else XWhiteMuted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            OutlinedTextField(
                value = projectSearch,
                onValueChange = { projectSearch = it },
                placeholder = { Text("Search projects", color = XWhiteMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = XWhiteMuted) },
                singleLine = true,
                shape = RoundedCornerShape(999.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = XWhite,
                    unfocusedBorderColor = XGlassBorder,
                    focusedTextColor = XWhite,
                    unfocusedTextColor = XWhite,
                    focusedContainerColor = Color(0x12FFFFFF),
                    unfocusedContainerColor = Color(0x0DFFFFFF)
                )
            )

            val visibleProjects = projects.filter {
                projectSearch.isBlank() || it.name.contains(projectSearch, ignoreCase = true)
            }

            if (visibleProjects.isEmpty()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("No projects... yet!", color = XWhite, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Unleash your imagination, create a project", color = XWhiteMuted, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(18.dp))
                    Canvas(modifier = Modifier.size(64.dp, 96.dp)) {
                        val sx = size.width / 60f
                        val sy = size.height / 110f
                        val arrowPath = Path().apply {
                            moveTo(30f * sx, 4f * sy)
                            cubicTo(56f * sx, 28f * sy, 6f * sx, 56f * sy, 28f * sx, 98f * sy)
                        }
                        drawPath(arrowPath, XWhite, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                        val tip = Path().apply {
                            moveTo(16f * sx, 86f * sy)
                            lineTo(28f * sx, 100f * sy)
                            lineTo(40f * sx, 85f * sy)
                        }
                        drawPath(tip, XWhite, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(visibleProjects, key = { it.id }) { proj ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(RoundedCornerShape(20.dp))
                                .clickable { activeProject = proj }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(proj.name, color = XWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${proj.w} × ${proj.h} · ${proj.fps} fps · ${proj.layers.size} layer(s)", color = XWhiteMuted, fontSize = 12.sp)
                            }
                            IconButton(onClick = { projects.remove(proj) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = XWhite)
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xB314141C))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(999.dp))
                        .padding(horizontal = 18.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onBack() }
                    ) {
                        Icon(Icons.Default.Home, contentDescription = "Home", tint = XWhiteMuted, modifier = Modifier.size(20.dp))
                        Text("Home", color = XWhiteMuted, fontSize = 10.sp)
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { hubTab = "Projects" }
                    ) {
                        Icon(Icons.Default.Layers, contentDescription = "Projects", tint = XWhite, modifier = Modifier.size(20.dp))
                        Text("Projects", color = XWhite, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(Color(0xFF7DD3FC), Color(0xFF818CF8))))
                            .clickable { showNewDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create Project", tint = XDarkBg, modifier = Modifier.size(24.dp))
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { hubTab = "Your Templates" }
                    ) {
                        Icon(Icons.Default.Dashboard, contentDescription = "Templates", tint = XWhiteMuted, modifier = Modifier.size(20.dp))
                        Text("Templates", color = XWhiteMuted, fontSize = 10.sp)
                    }
                }
            }
        }
        return
    }

    var editorTab by remember { mutableStateOf("shape") }
    var selectedLayerIndex by remember { mutableIntStateOf(0) }
    var playheadMs by remember { mutableFloatStateOf(0f) }
    var isPlaying by remember { mutableStateOf(false) }
    var exportNote by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(16L)
            playheadMs += 16f
            if (playheadMs >= currentProj.durationMs) {
                playheadMs = 0f
                isPlaying = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { activeProject = null }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to projects", tint = XWhite)
                }
                Text(currentProj.name, color = XWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Text("Session only", color = XWhiteMuted, fontSize = 12.sp)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .liquidGlass(RoundedCornerShape(20.dp))
                .pointerInput(selectedLayerIndex) {
                    detectTransformGestures { _, pan, zoom, rotation ->
                        val active = currentProj.layers.getOrNull(selectedLayerIndex) ?: return@detectTransformGestures
                        active.offsetX += pan.x
                        active.offsetY += pan.y
                        active.scale = (active.scale * zoom).coerceIn(0.25f, 4.0f)
                        active.rotationDeg += rotation
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                currentProj.layers.forEach { layer ->
                    if (playheadMs >= layer.startMs && playheadMs <= layer.endMs) {
                        withTransform({
                            translate(left = layer.offsetX, top = layer.offsetY)
                            rotate(degrees = layer.rotationDeg, pivot = Offset.Zero)
                            scale(scaleX = layer.scale, scaleY = layer.scale, pivot = Offset.Zero)
                        }) {
                            val drawColor = layer.fillColor.copy(alpha = (layer.fillColor.alpha * layer.opacity).coerceIn(0f, 1f))
                            if (layer.rounded) {
                                drawRoundRect(
                                    color = drawColor,
                                    topLeft = Offset(-70f, -70f),
                                    size = Size(140f, 140f),
                                    cornerRadius = CornerRadius(28f, 28f)
                                )
                            } else {
                                drawRect(
                                    color = drawColor,
                                    topLeft = Offset(-70f, -70f),
                                    size = Size(140f, 140f)
                                )
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { isPlaying = !isPlaying }) {
                Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play", tint = XWhite)
            }
            Text(
                text = String.format("%.2fs / %.2fs", playheadMs / 1000f, currentProj.durationMs / 1000f),
                color = XWhite,
                fontSize = 12.sp
            )
            Slider(
                value = playheadMs,
                onValueChange = { playheadMs = it },
                valueRange = 0f..currentProj.durationMs.toFloat(),
                colors = SliderDefaults.colors(thumbColor = XWhite, activeTrackColor = XWhite),
                modifier = Modifier.weight(1f)
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(currentProj.layers) { idx, layer ->
                val selected = idx == selectedLayerIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selected) XWhite.copy(alpha = 0.16f) else XSurface)
                        .border(1.dp, if (selected) XWhite else XGlassBorder, RoundedCornerShape(10.dp))
                        .clickable { selectedLayerIndex = idx }
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(layer.label, color = XWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("${layer.startMs / 1000f}s - ${layer.endMs / 1000f}s", color = XWhiteMuted, fontSize = 11.sp)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("shape" to "Shape", "layer" to "Layer", "report" to "Report", "export" to "Export").forEach { (id, label) ->
                val active = editorTab == id
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) XWhite else XBlack)
                        .border(1.dp, XGlassBorder, RoundedCornerShape(10.dp))
                        .clickable { editorTab = id }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(label, color = if (active) XBlack else XWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        when (editorTab) {
            "shape" -> {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val start = playheadMs.toInt().coerceAtMost((currentProj.durationMs - 2000).coerceAtLeast(0))
                            currentProj.layers.add(
                                MotionLayer(
                                    id = UUID.randomUUID().toString(),
                                    label = "Rectangle",
                                    rounded = false,
                                    startMs = start,
                                    endMs = (start + 2000).coerceAtMost(currentProj.durationMs)
                                )
                            )
                            selectedLayerIndex = currentProj.layers.lastIndex
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack)
                    ) {
                        Text("Rectangle", fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = {
                            val start = playheadMs.toInt().coerceAtMost((currentProj.durationMs - 2000).coerceAtLeast(0))
                            currentProj.layers.add(
                                MotionLayer(
                                    id = UUID.randomUUID().toString(),
                                    label = "Rounded rectangle",
                                    rounded = true,
                                    startMs = start,
                                    endMs = (start + 2000).coerceAtMost(currentProj.durationMs)
                                )
                            )
                            selectedLayerIndex = currentProj.layers.lastIndex
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack)
                    ) {
                        Text("Rounded rectangle", fontWeight = FontWeight.Bold)
                    }
                }
                Text("Adds a layer at the playhead. Text, drawing and audio tools are not available yet.", color = XWhiteMuted, fontSize = 11.sp)
            }

            "layer" -> {
                val cur = currentProj.layers.getOrNull(selectedLayerIndex)
                if (cur != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("${cur.label} · Position (${cur.offsetX.toInt()}, ${cur.offsetY.toInt()}) · Scale ${String.format("%.2f", cur.scale)}", color = XWhite, fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Opacity: ${(cur.opacity * 100).toInt()}%", color = XWhiteMuted, fontSize = 12.sp)
                            Slider(
                                value = cur.opacity,
                                onValueChange = { cur.opacity = it },
                                valueRange = 0f..1f,
                                colors = SliderDefaults.colors(thumbColor = XWhite, activeTrackColor = XWhite),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                } else {
                    Text("Tap a layer in the timeline to see its properties.", color = XWhiteMuted, fontSize = 12.sp)
                }
            }

            "report" -> {
                Text(
                    text = (currentProj.xmlNotice
                        ?: "Native project: only the shape layers made here are rendered. No Alight Motion data is attached.") +
                        "\nProjects live in memory only and are lost when you leave X Motion.",
                    color = XWhiteMuted,
                    fontSize = 12.sp
                )
            }

            "export" -> {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = {
                                exportNote = "MP4 video rendering is not available yet in v1.0. Use Current frame as PNG to export rendered frames."
                            },
                            enabled = false,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = XSurface,
                                contentColor = XWhiteMuted,
                                disabledContainerColor = XSurface,
                                disabledContentColor = XWhiteMuted
                            )
                        ) {
                            Text("Export MP4 (Unavailable in v1.0)", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                scope.launch {
                                    val t = playheadMs
                                    val timeText = String.format("%.2f", t / 1000f)
                                    if (visibleLayerCount(currentProj, t) == 0) {
                                        exportNote = "No layer is visible at ${timeText}s, so there is nothing to render. Move the playhead onto a layer."
                                        return@launch
                                    }
                                    val rendered = withContext(Dispatchers.Default) {
                                        try {
                                            val bmp = renderProjectFrameBitmap(currentProj, t)
                                            if (bitmapHasDrawnContent(bmp, XDarkBg.toArgb())) bmp else null
                                        } catch (oom: OutOfMemoryError) {
                                            null
                                        }
                                    }
                                    if (rendered == null) {
                                        exportNote = "Nothing was exported: the ${currentProj.w}×${currentProj.h} frame could not be rendered " +
                                            "(out of memory) or came out empty (layers fully transparent)."
                                        return@launch
                                    }
                                    val safeName = currentProj.name.replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "frame" }
                                    val f = File(context.cacheDir, "$safeName-frame-${System.currentTimeMillis()}.png")
                                    val written = withContext(Dispatchers.IO) {
                                        try {
                                            FileOutputStream(f).use { rendered.compress(Bitmap.CompressFormat.PNG, 100, it) } && f.length() > 0L
                                        } catch (e: Exception) {
                                            false
                                        }
                                    }
                                    if (!written) {
                                        runCatching { f.delete() }
                                        exportNote = "Could not encode the PNG frame. Nothing was saved."
                                        return@launch
                                    }
                                    exportNote = when (val outcome = MediaStoreExporter.export(context, f)) {
                                        is MediaStoreExporter.Outcome.Saved ->
                                            "Saved rendered ${currentProj.w}×${currentProj.h} PNG frame (at ${timeText}s) to Pictures/XStudio"
                                        is MediaStoreExporter.Outcome.Failed ->
                                            "Could not save PNG frame to gallery: ${outcome.reason}"
                                    }
                                    runCatching { f.delete() }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack)
                        ) {
                            Text("Current frame as PNG", fontWeight = FontWeight.Bold)
                        }
                    }
                    exportNote?.let { Text(it, color = XWhite, fontSize = 12.sp) }
                    Text(
                        text = "Not available yet: MP4 sequence encoding, project package, image sequence, GIF, XML.",
                        color = XWhiteMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
