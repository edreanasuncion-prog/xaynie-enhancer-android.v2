package com.xstudio.app.navigation

import android.content.Context
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.AutoFixOff
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil3.compose.AsyncImage
import com.xstudio.app.authentication.AuthScreen
import com.xstudio.app.authentication.AuthViewModel
import com.xstudio.app.chatbot.AskXScreen
import com.xstudio.app.core.designsystem.HeroXArtwork
import com.xstudio.app.core.designsystem.IcoChat
import com.xstudio.app.core.designsystem.IcoClap
import com.xstudio.app.core.designsystem.IcoPhoto
import com.xstudio.app.core.designsystem.IcoVideo
import com.xstudio.app.core.designsystem.XDarkBg
import com.xstudio.app.core.designsystem.XGlassBorder
import com.xstudio.app.core.designsystem.XMotionIcon
import com.xstudio.app.core.designsystem.XStatusOnline
import com.xstudio.app.core.designsystem.XStudioLogo
import com.xstudio.app.core.designsystem.XWhite
import com.xstudio.app.core.designsystem.XWhiteMuted
import com.xstudio.app.core.designsystem.liquidGlass
import com.xstudio.app.core.designsystem.qaGlass
import com.xstudio.app.core.designsystem.squircleTile
import com.xstudio.app.data.model.SocialPost
import com.xstudio.app.data.model.UserProfile
import com.xstudio.app.downloads.DownloadCenterScreen
import com.xstudio.app.network.SupabaseModule
import com.xstudio.app.photoenhancer.PhotoEnhancerScreen
import com.xstudio.app.profile.HistoryListScreen
import com.xstudio.app.profile.ProfileScreen
import com.xstudio.app.tools.AmFinderScreen
import com.xstudio.app.tools.PhotoLabScreen
import com.xstudio.app.tools.QuickToolsScreen
import com.xstudio.app.tools.TikTokExporterScreen
import com.xstudio.app.tools.UpdateNoticeDialog
import com.xstudio.app.tools.XStudioSplashOverlay
import com.xstudio.app.tools.ZipToVideoScreen
import com.xstudio.app.videoenhancer.VideoEnhancerScreen
import com.xstudio.app.xmotion.XMotionScreen
import com.xstudio.app.xsocial.XSocialScreen
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonNull

data class XToolItem(
    val id: String,
    val route: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val soon: Boolean = false
)

val AllXStudioTools = listOf(
    XToolItem("enhance", "photo_enhancer", "Photo Enhance", "Upscale and sharpen on-device (8K when memory allows)", Icons.Default.AutoAwesome),
    XToolItem("lab", "photo_lab", "Photo Lab", "Adjust, rotate, flip, export", Icons.Default.Tune),
    XToolItem("download", "downloads", "Download Center", "Save media from public links", Icons.Default.Download),
    XToolItem("quick", "quick_tools", "Quick Tools", "Compress, convert, pick colors", Icons.Default.Bolt),
    XToolItem("am", "am_finder", "AM Project Finder", "Alight Motion links and XML", Icons.Default.Theaters),
    XToolItem("zipvideo", "zip_video", "ZIP to Video", "Inspect Lottie or image-sequence ZIPs (video rendering not available yet)", Icons.Default.FilePresent),
    XToolItem("tiktok", "tiktok_exporter", "TikTok Video Exporter", "Customize FPS, resolution, and quality before uploading to TikTok.", Icons.Default.Movie),
    XToolItem("batch", "photo_enhancer", "Batch Enhance", "Up to 5 photos, shared settings", Icons.Default.Layers),
    XToolItem("video", "video_enhancer", "Video Enhance", "Scale and adjust clips up to 150 MB, export MP4", Icons.Default.Videocam),
    XToolItem("bg", "search", "Remove Background", "Coming soon", Icons.Default.AutoFixOff, soon = true),
    XToolItem("vlab", "search", "Video Lab", "Coming soon", Icons.Default.Videocam, soon = true)
)

@Composable
fun XStudioFloatingDock(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedIndex = when {
        currentRoute == "search" -> 1
        currentRoute == "xsocial" -> 2
        currentRoute == "xmotion" -> 3
        currentRoute in setOf("profile", "history", "favorites") -> 4
        else -> 0
    }
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "dock_pill"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 540.dp)
                .fillMaxWidth()
                .border(5.dp, Color(0x0AFFFFFF), RoundedCornerShape(999.dp))
                .padding(5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Brush.verticalGradient(listOf(Color(0x2BFFFFFF), Color(0x0DFFFFFF))))
                .background(Color(0x8C0E0E14))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(999.dp))
                .padding(7.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val slotWidth = maxWidth / 5
                Box(
                    modifier = Modifier
                        .offset(x = slotWidth * animatedIndex)
                        .width(slotWidth)
                        .height(58.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(Brush.radialGradient(listOf(Color(0x47FFFFFF), Color(0x14FFFFFF))))
                        .border(1.dp, Color(0x73FFFFFF), RoundedCornerShape(999.dp))
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    val items = listOf("home" to "Home", "search" to "Search", "xsocial" to "Social", "xmotion" to "Motion", "profile" to "Profile")
                    items.forEachIndexed { idx, (route, label) ->
                        val active = idx == selectedIndex
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .height(58.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { onNavigate(route) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            when (idx) {
                                0 -> Icon(Icons.Default.Home, contentDescription = label, tint = if (active) XWhite else XWhiteMuted, modifier = Modifier.size(22.dp))
                                1 -> Icon(Icons.Default.Search, contentDescription = label, tint = if (active) XWhite else XWhiteMuted, modifier = Modifier.size(22.dp))
                                2 -> XStudioLogo(iconSize = 26.dp, showWordmark = false)
                                3 -> XMotionIcon(size = 22.dp, tint = if (active) XWhite else XWhiteMuted)
                                4 -> Icon(Icons.Default.Person, contentDescription = label, tint = if (active) XWhite else XWhiteMuted, modifier = Modifier.size(22.dp))
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = label,
                                color = if (active) XWhite else XWhiteMuted,
                                fontSize = 11.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun XStudioRoot(
    authViewModel: AuthViewModel,
    cpuOverlayEnabled: Boolean,
    onToggleCpuOverlay: (Boolean) -> Unit,
    pageScale: Float,
    onChangePageScale: (Float) -> Unit
) {
    val authState by authViewModel.uiState.collectAsState()
    var showSplash by rememberSaveable { mutableStateOf(true) }

    // Authentication is never bypassed: if Supabase could not be initialized, AuthScreen
    // shows a configuration error (with Retry) instead of letting the user into the app.
    if (!authState.isAuthenticated) {
        AuthScreen(viewModel = authViewModel)
        return
    }

    if (showSplash) {
        XStudioSplashOverlay(
            username = authState.userProfile?.username,
            onFinished = { showSplash = false }
        )
        return
    }

    UpdateNoticeDialog()

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"
    val showMainDock = currentRoute !in setOf("xmotion", "ask_x")

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (showMainDock) {
                XStudioFloatingDock(
                    currentRoute = currentRoute,
                    onNavigate = { target ->
                        navController.navigate(target) {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = if (showMainDock) innerPadding.calculateBottomPadding() else 0.dp)
        ) {
            composable("home") {
                StudioHomeScreen(
                    userProfile = authState.userProfile,
                    onOpenTool = { route -> navController.navigate(route) }
                )
            }
            composable("search") {
                SearchScreen(onOpenTool = { route -> navController.navigate(route) })
            }
            composable("profile") {
                ProfileScreen(
                    userProfile = authState.userProfile,
                    cpuOverlayEnabled = cpuOverlayEnabled,
                    onToggleCpuOverlay = onToggleCpuOverlay,
                    pageScale = pageScale,
                    onChangePageScale = onChangePageScale,
                    onOpenHistory = { navController.navigate("history") },
                    onOpenFavorites = { navController.navigate("favorites") },
                    onOpenDownloads = { navController.navigate("downloads") },
                    onOpenEnhancer = { navController.navigate("photo_enhancer") },
                    onLogout = { authViewModel.signOut() }
                )
            }
            composable("photo_enhancer") {
                PhotoEnhancerScreen(onBack = { navController.popBackStack() })
            }
            composable("video_enhancer") {
                VideoEnhancerScreen(onBack = { navController.popBackStack() })
            }
            composable("photo_lab") {
                PhotoLabScreen(onBack = { navController.popBackStack() })
            }
            composable("quick_tools") {
                QuickToolsScreen(onBack = { navController.popBackStack() })
            }
            composable("am_finder") {
                AmFinderScreen(onBack = { navController.popBackStack() })
            }
            composable("tiktok_exporter") {
                TikTokExporterScreen(onBack = { navController.popBackStack() })
            }
            composable("zip_video") {
                ZipToVideoScreen(onBack = { navController.popBackStack() })
            }
            composable("xsocial") {
                XSocialScreen(
                    currentUser = authState.userProfile,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("ask_x") {
                AskXScreen(onBack = { navController.popBackStack() })
            }
            composable("xmotion") {
                XMotionScreen(onBack = { navController.popBackStack() })
            }
            composable("history") {
                HistoryListScreen(
                    title = "Enhancement History",
                    favoritesOnly = false,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("favorites") {
                HistoryListScreen(
                    title = "Favorite History",
                    favoritesOnly = true,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("downloads") {
                DownloadCenterScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
fun StudioHomeScreen(
    userProfile: UserProfile?,
    onOpenTool: (String) -> Unit
) {
    val trendingPosts = remember { mutableStateListOf<SocialPost>() }
    var trendingLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (SupabaseModule.ensureInitialized() != null) {
            runCatching {
                val rows = SupabaseModule.client.postgrest.rpc(
                    function = "social_explore_media",
                    parameters = buildJsonObject {
                        putJsonNull("p_topic")
                        put("p_offset", 0)
                        put("p_limit", 6)
                    }
                ).decodeList<SocialPost>()
                trendingPosts.clear()
                trendingPosts.addAll(rows.filter { !it.removed && it.media.isNotEmpty() }.take(6))
            }.onFailure {
                runCatching {
                    val fallback = SupabaseModule.client.postgrest.rpc(
                        function = "social_feed",
                        parameters = buildJsonObject {
                            put("p_mode", "explore")
                            putJsonNull("p_before_ts")
                            putJsonNull("p_before_id")
                            put("p_limit", 12)
                        }
                    ).decodeList<SocialPost>()
                    trendingPosts.clear()
                    trendingPosts.addAll(fallback.filter { !it.removed && it.media.isNotEmpty() }.take(6))
                }
            }
        }
        trendingLoaded = true
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                XStudioLogo(iconSize = 38.dp, showWordmark = true)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x0DFFFFFF))))
                            .border(1.dp, Color(0x3DFFFFFF), CircleShape)
                            .clickable { onOpenTool("search") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = XWhite, modifier = Modifier.size(20.dp))
                    }
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clickable { onOpenTool("profile") }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color(0x14FFFFFF))
                                .border(1.dp, Color(0x4DFFFFFF), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!userProfile?.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = userProfile?.avatarUrl,
                                    contentDescription = "Profile",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Default.Person, contentDescription = "Profile", tint = XWhite, modifier = Modifier.size(20.dp))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(13.dp)
                                .clip(CircleShape)
                                .background(XStatusOnline)
                                .border(2.dp, XDarkBg, CircleShape)
                        )
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1.08f)) {
                    val silverBrush = Brush.horizontalGradient(listOf(Color.White, Color(0xFFAEB3C9)))
                    Text(
                        text = "Create.",
                        color = XWhite,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 40.sp,
                        letterSpacing = (-1.5).sp
                    )
                    Text(
                        text = "Enhance.",
                        style = TextStyle(
                            brush = silverBrush,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 40.sp,
                            letterSpacing = (-1.5).sp
                        )
                    )
                    Text(
                        text = "Explore.",
                        style = TextStyle(
                            brush = silverBrush,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 40.sp,
                            letterSpacing = (-1.5).sp
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Your all-in-one AI studio\nfor photos, videos and more.",
                        color = Color(0xADFFFFFF),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier
                            .height(50.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFDFE2EE))))
                            .clickable { onOpenTool("photo_enhancer") }
                            .padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Get Started", color = XDarkBg, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = XDarkBg, modifier = Modifier.size(18.dp))
                    }
                }
                Box(modifier = Modifier.weight(0.92f), contentAlignment = Alignment.Center) {
                    HeroXArtwork()
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .qaGlass()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = XWhite, modifier = Modifier.size(20.dp))
                        Text("Quick Access", color = XWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.clickable { onOpenTool("search") },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("See all", color = Color(0x9EFFFFFF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0x9EFFFFFF), modifier = Modifier.size(15.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val quickTiles = listOf(
                        Triple("Photo\nEnhancer", "photo_enhancer", 0),
                        Triple("Video\nEnhancer", "video_enhancer", 1),
                        Triple("Ask X", "ask_x", 2),
                        Triple("X Motion", "xmotion", 3)
                    )
                    quickTiles.forEach { (title, route, iconIdx) ->
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(5f / 6f)
                                .clip(RoundedCornerShape(24.dp))
                                .background(Brush.verticalGradient(listOf(Color(0x1CFFFFFF), Color(0x07FFFFFF))))
                                .border(1.dp, Color(0x21FFFFFF), RoundedCornerShape(24.dp))
                                .clickable { onOpenTool(route) }
                                .padding(6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            when (iconIdx) {
                                0 -> IcoPhoto(size = 40.dp)
                                1 -> IcoVideo(size = 40.dp)
                                2 -> IcoChat(size = 40.dp)
                                3 -> IcoClap(size = 40.dp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = title,
                                color = Color(0xE6FFFFFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = XWhite, modifier = Modifier.size(22.dp))
                        Text("Trending", color = XWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.clickable { onOpenTool("xsocial") },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("See all", color = Color(0x9EFFFFFF), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color(0x9EFFFFFF), modifier = Modifier.size(15.dp))
                    }
                }

                if (trendingPosts.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        trendingPosts.forEach { post ->
                            val firstMedia = post.media.firstOrNull()
                            val thumbUrl = firstMedia?.let { SupabaseModule.publicStorageUrl("social-media", it.path) }
                            Box(
                                modifier = Modifier
                                    .width(164.dp)
                                    .aspectRatio(3f / 2f)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(Color(0x0FFFFFFF))
                                    .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(24.dp))
                                    .clickable { onOpenTool("xsocial") }
                            ) {
                                if (thumbUrl != null) {
                                    AsyncImage(
                                        model = thumbUrl,
                                        contentDescription = post.caption,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(10.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(Color(0x8C0A0A0E))
                                        .border(1.dp, Color(0x24FFFFFF), RoundedCornerShape(999.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (firstMedia?.kind == "video") Icons.Default.PlayArrow else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = XWhite,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text("${post.views}", color = XWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.FavoriteBorder, contentDescription = null, tint = XWhite, modifier = Modifier.size(16.dp))
                                    Text("${post.likeCount}", color = XWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .liquidGlass(RoundedCornerShape(24.dp))
                            .clickable { onOpenTool("xsocial") }
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (trendingLoaded) "Tap to load trending" else "Loading trending posts...",
                            color = XWhite,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Shows the top posts from X Social.",
                            color = XWhiteMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(onOpenTool: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("xstudio_prefs", Context.MODE_PRIVATE) }
    var query by remember { mutableStateOf("") }
    val favIds = remember {
        mutableStateListOf<String>().apply {
            addAll(prefs.getStringSet("xe_favs", emptySet()).orEmpty())
        }
    }

    val filtered = remember(query) {
        if (query.isBlank()) AllXStudioTools
        else AllXStudioTools.filter {
            it.title.contains(query, ignoreCase = true) ||
                it.subtitle.contains(query, ignoreCase = true)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text("Search", color = XWhite, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search features", color = XWhiteMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = XWhite) },
            singleLine = true,
            shape = RoundedCornerShape(999.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = XWhite,
                unfocusedBorderColor = XGlassBorder,
                focusedTextColor = XWhite,
                unfocusedTextColor = XWhite,
                focusedContainerColor = Color(0x14FFFFFF),
                unfocusedContainerColor = Color(0x0FFFFFFF)
            )
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            items(filtered, key = { it.id }) { tool ->
                val isFav = favIds.contains(tool.id)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(RoundedCornerShape(28.dp))
                        .alpha(if (tool.soon) 0.52f else 1f)
                        .clickable(enabled = !tool.soon) { onOpenTool(tool.route) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .squircleTile(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(tool.icon, contentDescription = tool.title, tint = XWhite, modifier = Modifier.size(22.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tool.title, color = XWhite, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                        Text(tool.subtitle, color = XWhiteMuted, fontSize = 12.sp)
                    }
                    IconButton(
                        onClick = {
                            if (isFav) favIds.remove(tool.id) else favIds.add(tool.id)
                            prefs.edit().putStringSet("xe_favs", favIds.toSet()).apply()
                        }
                    ) {
                        Icon(
                            imageVector = if (isFav) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorite",
                            tint = XWhite
                        )
                    }
                }
            }
        }
    }
}
