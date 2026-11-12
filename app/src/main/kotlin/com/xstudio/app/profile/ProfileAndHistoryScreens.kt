package com.xstudio.app.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.xstudio.app.core.designsystem.XDarkBg
import com.xstudio.app.core.designsystem.XGlassBorder
import com.xstudio.app.core.designsystem.XStudioLogo
import com.xstudio.app.core.designsystem.XSurfaceElevated
import com.xstudio.app.core.designsystem.XWhite
import com.xstudio.app.core.designsystem.XWhiteMuted
import com.xstudio.app.core.designsystem.liquidGlass
import com.xstudio.app.core.ui.DynamicIslandBadgePopup
import com.xstudio.app.core.ui.RoleBadgeChip
import com.xstudio.app.data.local.XStudioDatabase
import com.xstudio.app.data.model.UserProfile
import com.xstudio.app.data.model.UserRole
import com.xstudio.app.core.media.MediaStoreExporter
import com.xstudio.app.history.HistoryFiles
import java.io.File
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    userProfile: UserProfile?,
    cpuOverlayEnabled: Boolean,
    onToggleCpuOverlay: (Boolean) -> Unit,
    pageScale: Float,
    onChangePageScale: (Float) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenEnhancer: () -> Unit = {},
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { XStudioDatabase.getInstance(context).historyDao() }
    val historyItems by dao.observeAllHistory().collectAsState(initial = emptyList())

    var avatarUri by remember { mutableStateOf<Uri?>(null) }
    var badgePopupRole by remember { mutableStateOf<UserRole?>(null) }
    var sheetTab by remember { mutableIntStateOf(0) }
    var learnMoreExpanded by remember { mutableStateOf(false) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) avatarUri = uri
    }

    DynamicIslandBadgePopup(
        selectedRole = badgePopupRole,
        onDismiss = { badgePopupRole = null }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { sheetTab = 1 }) {
                Icon(Icons.Default.Speed, contentDescription = "Performance", tint = XWhite)
            }
            IconButton(onClick = { sheetTab = 2 }) {
                Icon(Icons.Default.Settings, contentDescription = "Settings", tint = XWhite)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clickable {
                        avatarPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(Color(0x14FFFFFF))
                        .border(1.5.dp, Color(0x4DFFFFFF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    val model = avatarUri ?: userProfile?.avatarUrl
                    if (model != null) {
                        AsyncImage(
                            model = model,
                            contentDescription = "Profile Picture",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(Icons.Default.Person, contentDescription = null, tint = XWhite, modifier = Modifier.size(44.dp))
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(XWhite)
                        .border(2.dp, XDarkBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = XDarkBg, modifier = Modifier.size(14.dp))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val uname = userProfile?.username?.ifBlank { "xaynie" } ?: "xaynie"
                Text(
                    text = if (uname.startsWith("@")) uname else "@$uname",
                    color = XWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = userProfile?.email?.ifBlank { "Signed in locally" } ?: "Not signed in",
                    color = XWhiteMuted,
                    fontSize = 13.sp
                )
                val role = userProfile?.parsedRole ?: UserRole.FREE
                if (role != UserRole.FREE) {
                    RoleBadgeChip(role = role, onClick = { badgePopupRole = it })
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .liquidGlass(RoundedCornerShape(28.dp))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    XStudioLogo(iconSize = 26.dp, showWordmark = false)
                    Text("X Studio Free", color = XWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(XWhite)
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text("Free", color = XDarkBg, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Text(
                text = "Member since ${userProfile?.createdAt?.take(10)?.ifBlank { "October 30, 2026" } ?: "October 30, 2026"}",
                color = XWhiteMuted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    Icons.Default.Memory to "On-device\nProcessing",
                    Icons.Default.Layers to "Batch\nProcessing",
                    Icons.Default.CloudDownload to "Free\nDownloads"
                ).forEach { (icon, label) ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(0x14FFFFFF))
                                .border(1.dp, XGlassBorder, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = XWhite, modifier = Modifier.size(16.dp))
                        }
                        Text(label, color = XWhite, fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            HorizontalDivider(color = Color(0x1FFFFFFF))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { learnMoreExpanded = !learnMoreExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("X Studio makes every pixel count.", color = XWhiteMuted, fontSize = 12.sp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Learn more", color = XWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = XWhite, modifier = Modifier.size(16.dp))
                }
            }

            if (learnMoreExpanded) {
                Text(
                    text = "All photo upscaling, batch processing, and video frame pacing run locally on your Android hardware without cloud queues.",
                    color = XWhiteMuted,
                    fontSize = 12.sp
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .liquidGlass(RoundedCornerShape(20.dp))
                    .clickable { onOpenDownloads() }
                    .padding(horizontal = 14.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, tint = XWhite, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Download Center", color = XWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier
                    .weight(1f)
                    .liquidGlass(RoundedCornerShape(20.dp))
                    .clickable { onOpenEnhancer() }
                    .padding(horizontal = 14.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = XWhite, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quality Enhancer", color = XWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .background(XSurfaceElevated)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                listOf("History ${historyItems.size}", "Performance", "Settings").forEachIndexed { idx, label ->
                    val active = sheetTab == idx
                    Column(
                        modifier = Modifier.clickable { sheetTab = idx },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = label,
                            color = if (active) XWhite else XWhiteMuted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (active) XWhite else Color.Transparent)
                        )
                    }
                }
            }

            when (sheetTab) {
                0 -> {
                    if (historyItems.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Canvas(modifier = Modifier.size(96.dp)) {
                                withTransform({ rotate(10f, pivot = center) }) {
                                    drawRoundRect(
                                        color = Color(0xFF2C2C32),
                                        topLeft = Offset(size.width * 0.30f, size.height * 0.14f),
                                        size = Size(size.width * 0.46f, size.height * 0.56f),
                                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                                    )
                                }
                                withTransform({ rotate(-8f, pivot = center) }) {
                                    drawRoundRect(
                                        color = Color(0xFF383840),
                                        topLeft = Offset(size.width * 0.22f, size.height * 0.24f),
                                        size = Size(size.width * 0.46f, size.height * 0.56f),
                                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                                    )
                                }
                                val pencil = Path().apply {
                                    moveTo(size.width * 0.40f, size.height * 0.60f)
                                    lineTo(size.width * 0.52f, size.height * 0.46f)
                                    lineTo(size.width * 0.56f, size.height * 0.50f)
                                    lineTo(size.width * 0.44f, size.height * 0.64f)
                                    close()
                                }
                                drawPath(pencil, XWhiteMuted, style = Stroke(width = 2.dp.toPx()))
                            }
                            Text(
                                text = "Your enhancements will be saved here!\nEnhance a photo now!",
                                color = XWhiteMuted,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.SemiBold
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(Color(0x1AFFFFFF))
                                    .border(1.dp, Color(0x3DFFFFFF), RoundedCornerShape(999.dp))
                                    .clickable { onOpenEnhancer() }
                                    .padding(horizontal = 36.dp, vertical = 12.dp)
                            ) {
                                Text("Create", color = XWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Photo history is kept on this device for 3 days.", color = XWhiteMuted, fontSize = 11.sp)
                        }
                    } else {
                        historyItems.take(5).forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .liquidGlass(RoundedCornerShape(16.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AsyncImage(
                                    model = File(item.enhancedFilePath),
                                    contentDescription = item.resolutionLabel,
                                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("${item.mediaType} • ${item.resolutionLabel}", color = XWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(
                                        if (item.mediaType == "PHOTO") "Auto-expires after 3 days" else "Kept until you delete it",
                                        color = XWhiteMuted,
                                        fontSize = 11.sp
                                    )
                                }
                                IconButton(onClick = { scope.launch { exportHistoryItem(context, item.enhancedFilePath) } }) {
                                    Icon(Icons.Default.Download, contentDescription = "Download", tint = XWhite)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onOpenHistory,
                                colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XDarkBg),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("All History", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onOpenFavorites,
                                colors = ButtonDefaults.buttonColors(containerColor = XDarkBg, contentColor = XWhite),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Favorites", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                1 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(RoundedCornerShape(20.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Live Performance Overlay", color = XWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Show FPS, CPU usage & RAM monitor", color = XWhiteMuted, fontSize = 12.sp)
                            }
                            Switch(checked = cpuOverlayEnabled, onCheckedChange = onToggleCpuOverlay)
                        }
                    }
                }

                2 -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(RoundedCornerShape(20.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Page display: ${(pageScale * 100).toInt()}%", color = XWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Slider(
                                value = pageScale,
                                onValueChange = onChangePageScale,
                                valueRange = 0.80f..1.20f,
                                colors = SliderDefaults.colors(thumbColor = XWhite, activeTrackColor = XWhite)
                            )
                            Button(
                                onClick = { onChangePageScale(0.85f) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF), contentColor = XWhite),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Reset to default (85%)", fontWeight = FontWeight.Bold)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(RoundedCornerShape(20.dp))
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Account", color = XWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text(userProfile?.email.orEmpty(), color = XWhiteMuted, fontSize = 12.sp)
                            Button(
                                onClick = onLogout,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFFFFFF), contentColor = XWhite),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = XWhite)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign out", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HistoryListScreen(
    title: String,
    favoritesOnly: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val dao = remember { XStudioDatabase.getInstance(context).historyDao() }
    val items by (if (favoritesOnly) dao.observeFavorites() else dao.observeAllHistory())
        .collectAsState(initial = emptyList())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = XWhite)
            }
            Text(title.uppercase(), color = XWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items, key = { it.id }) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    AsyncImage(
                        model = File(item.enhancedFilePath),
                        contentDescription = item.resolutionLabel,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${item.mediaType} • ${item.resolutionLabel}", color = XWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        item.fpsLabel?.let { Text(it, color = XWhiteMuted, fontSize = 11.sp) }
                        if (item.mediaType == "PHOTO") {
                            Text("Auto-expires 3 days after creation", color = XWhiteMuted, fontSize = 10.sp)
                        }
                    }
                    IconButton(
                        onClick = {
                            scope.launch { dao.setFavorite(item.id, !item.isFavorite) }
                        }
                    ) {
                        Icon(
                            imageVector = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = XWhite
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch { exportHistoryItem(context, item.enhancedFilePath) }
                        }
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Download", tint = XWhite)
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                HistoryFiles.deleteOwnedFile(context, item.enhancedFilePath)
                                dao.deleteById(item.id)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = XWhite)
                    }
                }
            }
        }
    }
}

/** Copies a history file into the gallery and tells the user the real outcome. */
private suspend fun exportHistoryItem(context: android.content.Context, path: String) {
    val message = when (val outcome = MediaStoreExporter.export(context, File(path))) {
        is MediaStoreExporter.Outcome.Saved -> "Saved to gallery"
        is MediaStoreExporter.Outcome.Failed -> "Could not save: ${outcome.reason}"
    }
    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
}
