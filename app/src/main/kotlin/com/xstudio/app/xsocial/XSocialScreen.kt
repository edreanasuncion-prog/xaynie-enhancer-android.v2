package com.xstudio.app.xsocial

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddBox
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xstudio.app.core.designsystem.XBlack
import com.xstudio.app.core.designsystem.XGlassBorder
import com.xstudio.app.core.designsystem.XSurface
import com.xstudio.app.core.designsystem.XWhite
import com.xstudio.app.core.designsystem.XWhiteMuted
import com.xstudio.app.core.designsystem.liquidGlass
import com.xstudio.app.core.ui.DynamicIslandBadgePopup
import com.xstudio.app.core.ui.RoleBadgeChip
import com.xstudio.app.data.model.DirectMessage
import com.xstudio.app.data.model.SocialAccount
import com.xstudio.app.data.model.SocialAuthor
import com.xstudio.app.data.model.SocialConversation
import com.xstudio.app.data.model.SocialPost
import com.xstudio.app.data.model.UserProfile
import com.xstudio.app.data.model.UserRole
import com.xstudio.app.network.SupabaseModule
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun XSocialScreen(
    currentUser: UserProfile?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0=Feed, 1=Explore, 2=Messages, 3=Profile, 4=Admin
    var activeBadgePopup by remember { mutableStateOf<UserRole?>(null) }

    var account by remember { mutableStateOf<SocialAccount?>(null) }
    val feedPosts = remember { mutableStateListOf<SocialPost>() }
    val conversations = remember { mutableStateListOf<SocialConversation>() }
    val activeMessages = remember { mutableStateListOf<DirectMessage>() }
    val searchResults = remember { mutableStateListOf<SocialAuthor>() }
    val topics = remember { mutableStateListOf<String>() }

    var newPostCaption by remember { mutableStateOf("") }
    var exploreQuery by remember { mutableStateOf("") }
    var selectedTopic by remember { mutableStateOf<String?>(null) }
    var activeConvId by remember { mutableStateOf<String?>(null) }
    var messageBody by remember { mutableStateOf("") }
    var heldMessage by remember { mutableStateOf<DirectMessage?>(null) }
    var profilePostTab by remember { mutableIntStateOf(0) } // 0 = Photos/Videos, 1 = Text Posts
    var statusBanner by remember { mutableStateOf<String?>(null) }

    val myRole = account?.author?.parsedRole ?: currentUser?.parsedRole ?: UserRole.FREE

    suspend fun refreshSocialFeed() {
        runCatching {
            val rows = SupabaseModule.client.postgrest.rpc(
                function = "social_feed",
                parameters = buildJsonObject {
                    put("p_mode", "explore")
                    put("p_before_ts", JsonNull)
                    put("p_before_id", JsonNull)
                    put("p_limit", 20)
                }
            ).decodeList<SocialPost>()
            feedPosts.clear()
            feedPosts.addAll(rows)
        }.onFailure { e ->
            statusBanner = e.localizedMessage
        }
    }

    LaunchedEffect(Unit) {
        if (SupabaseModule.ensureInitialized() != null) {
            runCatching {
                var acc = SupabaseModule.client.postgrest.rpc("social_my_account").decodeSingle<SocialAccount>()
                if (acc.profile == null) {
                    SupabaseModule.client.postgrest.rpc("social_ensure_profile")
                    acc = SupabaseModule.client.postgrest.rpc("social_my_account").decodeSingle<SocialAccount>()
                }
                account = acc
            }
            refreshSocialFeed()
            runCatching {
                val tp = SupabaseModule.client.postgrest.rpc(
                    function = "social_explore_topics",
                    parameters = buildJsonObject { put("p_limit", 12) }
                ).decodeList<String>()
                topics.clear()
                topics.addAll(tp)
            }
            runCatching {
                val convs = SupabaseModule.client.postgrest.rpc(
                    function = "social_conversations_list",
                    parameters = buildJsonObject { put("p_limit", 60) }
                ).decodeList<SocialConversation>()
                conversations.clear()
                conversations.addAll(convs)
            }
        }
    }

    DynamicIslandBadgePopup(
        selectedRole = activeBadgePopup,
        onDismiss = { activeBadgePopup = null }
    )

    // Long-press Message Sheet (matches source/tests/tests.json requirement)
    heldMessage?.let { msg ->
        ModalBottomSheet(
            onDismissRequest = { heldMessage = null },
            containerColor = XSurface,
            contentColor = XWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Message", color = XWhite, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                Text(msg.body ?: "Message deleted", color = XWhite, fontSize = 15.sp)
                Text("Sent: ${msg.createdAt}", color = XWhiteMuted, fontSize = 12.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!msg.deleted && !msg.body.isNullOrBlank()) {
                        Button(
                            onClick = {
                                clipboard.setText(AnnotatedString(msg.body))
                                heldMessage = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack)
                        ) {
                            Text("Copy text", fontWeight = FontWeight.Bold)
                        }
                    }
                    if (msg.mine && !msg.deleted) {
                        Button(
                            onClick = {
                                scope.launch {
                                    runCatching {
                                        SupabaseModule.client.postgrest.rpc(
                                            function = "social_delete_message",
                                            parameters = buildJsonObject { put("p_id", msg.id) }
                                        )
                                        val idx = activeMessages.indexOfFirst { it.id == msg.id }
                                        if (idx >= 0) {
                                            activeMessages[idx] = activeMessages[idx].copy(body = null, deleted = true)
                                        }
                                    }
                                    heldMessage = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = XBlack, contentColor = XWhite),
                            modifier = Modifier.border(1.dp, XGlassBorder, RoundedCornerShape(12.dp))
                        ) {
                            Text("Delete message")
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XBlack)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = XWhite)
                }
                Text("X SOCIAL", color = XWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
            RoleBadgeChip(
                role = myRole,
                onClick = { activeBadgePopup = it }
            )
        }

        val navIcons = listOf(
            Icons.Default.Home to "Feed",
            Icons.Default.Explore to "Explore",
            Icons.Default.AddBox to "New post",
            Icons.Default.Notifications to "Activity",
            Icons.Default.ChatBubbleOutline to "Messages",
            Icons.Default.Person to "Profile"
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x1AFFFFFF))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(999.dp))
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navIcons.forEachIndexed { idx, (icon, desc) ->
                val active = when (idx) {
                    0 -> selectedTab == 0
                    1 -> selectedTab == 1
                    3 -> selectedTab == 4
                    4 -> selectedTab == 2
                    5 -> selectedTab == 3
                    else -> false
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (active) Color(0x33FFFFFF) else Color.Transparent)
                        .clickable {
                            selectedTab = when (idx) {
                                0, 2 -> 0
                                1 -> 1
                                3 -> 4
                                4 -> 2
                                5 -> 3
                                else -> 0
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = desc, tint = if (active) XWhite else XWhiteMuted)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        statusBanner?.let {
            Text(it, color = XWhiteMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        }

        when (selectedTab) {
            0 -> {
                // Feed + Compose via social_create_post RPC
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newPostCaption,
                        onValueChange = { newPostCaption = it },
                        placeholder = { Text("Write a caption…") },
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
                            if (newPostCaption.isBlank()) return@Button
                            scope.launch {
                                runCatching {
                                    SupabaseModule.client.postgrest.rpc(
                                        function = "social_create_post",
                                        parameters = buildJsonObject {
                                            put("p_caption", newPostCaption.trim())
                                            put("p_media", buildJsonArray {})
                                        }
                                    )
                                    newPostCaption = ""
                                    refreshSocialFeed()
                                }.onFailure { e ->
                                    statusBanner = e.localizedMessage
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Post", fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(feedPosts, key = { it.id }) { post ->
                        SocialPostCard(
                            post = post,
                            onBadgeClick = { activeBadgePopup = it },
                            onToggleLike = {
                                scope.launch {
                                    val next = !post.liked
                                    val idx = feedPosts.indexOfFirst { it.id == post.id }
                                    if (idx >= 0) {
                                        feedPosts[idx] = post.copy(
                                            liked = next,
                                            likeCount = (post.likeCount + if (next) 1 else -1).coerceAtLeast(0)
                                        )
                                    }
                                    runCatching {
                                        if (next) {
                                            SupabaseModule.client.postgrest["social_likes"].insert(
                                                buildJsonObject { put("post_id", post.id) }
                                            )
                                        } else {
                                            val uid = account?.userId ?: currentUser?.id.orEmpty()
                                            SupabaseModule.client.postgrest["social_likes"].delete {
                                                filter {
                                                    eq("post_id", post.id)
                                                    eq("user_id", uid)
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            onToggleSave = {
                                scope.launch {
                                    val next = !post.saved
                                    val idx = feedPosts.indexOfFirst { it.id == post.id }
                                    if (idx >= 0) {
                                        feedPosts[idx] = post.copy(saved = next)
                                    }
                                    runCatching {
                                        if (next) {
                                            SupabaseModule.client.postgrest["social_saved_posts"].insert(
                                                buildJsonObject { put("post_id", post.id) }
                                            )
                                        } else {
                                            val uid = account?.userId ?: currentUser?.id.orEmpty()
                                            SupabaseModule.client.postgrest["social_saved_posts"].delete {
                                                filter {
                                                    eq("post_id", post.id)
                                                    eq("user_id", uid)
                                                }
                                            }
                                        }
                                    }
                                }
                            },
                            onShare = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "@${post.author.username}: ${post.caption}")
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Post"))
                            }
                        )
                    }
                }
            }

            1 -> {
                // Explore with Search people, Saved shortcut, and For you topic chip (matches source/src/social/explore.tsx)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = exploreQuery,
                        onValueChange = { q ->
                            exploreQuery = q
                            scope.launch {
                                val clean = q.trim().removePrefix("@")
                                if (clean.isNotEmpty()) {
                                    runCatching {
                                        val found = SupabaseModule.client.postgrest.rpc(
                                            function = "social_search_users",
                                            parameters = buildJsonObject {
                                                put("p_q", clean)
                                                put("p_limit", 20)
                                            }
                                        ).decodeList<SocialAuthor>()
                                        searchResults.clear()
                                        searchResults.addAll(found)
                                    }
                                } else {
                                    searchResults.clear()
                                }
                            }
                        },
                        placeholder = { Text("Search people") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (exploreQuery.isBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val forYouActive = selectedTopic == null
                            Text(
                                text = "For you",
                                color = if (forYouActive) XBlack else XWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (forYouActive) XWhite else XBlack)
                                    .border(1.dp, XGlassBorder, RoundedCornerShape(50))
                                    .clickable { selectedTopic = null }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                            topics.forEach { t ->
                                val active = selectedTopic == t
                                Text(
                                    text = "#$t",
                                    color = if (active) XBlack else XWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (active) XWhite else XBlack)
                                        .border(1.dp, XGlassBorder, RoundedCornerShape(50))
                                        .clickable { selectedTopic = t }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    } else {
                        if (searchResults.isEmpty()) {
                            Text("No one found for “$exploreQuery”", color = XWhiteMuted, fontSize = 13.sp)
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(searchResults, key = { it.id }) { author ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .liquidGlass(RoundedCornerShape(14.dp))
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text("@${author.username}", color = XWhite, fontWeight = FontWeight.Bold)
                                            RoleBadgeChip(role = author.parsedRole, onClick = { activeBadgePopup = it })
                                        }
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    runCatching {
                                                        val convId = SupabaseModule.client.postgrest.rpc(
                                                            function = "social_start_conversation",
                                                            parameters = buildJsonObject { put("p_target", author.id) }
                                                        ).decodeSingle<String>()
                                                        activeConvId = convId
                                                        selectedTab = 2
                                                    }
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack)
                                        ) {
                                            Text("Message", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                // Direct Messages via social_conversations_list, social_messages_page, and social_messages insert
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (activeConvId == null) {
                        Text("CONVERSATIONS", color = XWhiteMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(conversations, key = { it.id }) { conv ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .liquidGlass(RoundedCornerShape(14.dp))
                                        .clickable {
                                            activeConvId = conv.id
                                            scope.launch {
                                                runCatching {
                                                    val rows = SupabaseModule.client.postgrest.rpc(
                                                        function = "social_messages_page",
                                                        parameters = buildJsonObject {
                                                            put("p_conv", conv.id)
                                                            putJsonNull("p_before_ts")
                                                            putJsonNull("p_before_id")
                                                            put("p_limit", 30)
                                                        }
                                                    ).decodeList<DirectMessage>()
                                                    activeMessages.clear()
                                                    activeMessages.addAll(rows.reversed())
                                                }
                                            }
                                        }
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("@${conv.other.username}", color = XWhite, fontWeight = FontWeight.Bold)
                                            RoleBadgeChip(role = conv.other.parsedRole, onClick = { activeBadgePopup = it })
                                        }
                                        Text(conv.last?.body ?: "Tap to open chat", color = XWhiteMuted, fontSize = 12.sp)
                                    }
                                    if (conv.unread > 0) {
                                        Text("${conv.unread} new", color = XWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        Button(
                            onClick = { activeConvId = null },
                            colors = ButtonDefaults.buttonColors(containerColor = XBlack, contentColor = XWhite)
                        ) {
                            Text("← All Conversations")
                        }
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(activeMessages, key = { it.id }) { msg ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .liquidGlass(RoundedCornerShape(14.dp))
                                        .pointerInput(msg.id) {
                                            detectTapGestures(
                                                onLongPress = { heldMessage = msg }
                                            )
                                        }
                                        .padding(12.dp),
                                    horizontalAlignment = if (msg.mine) Alignment.End else Alignment.Start
                                ) {
                                    Text(
                                        text = if (msg.deleted) "Message deleted" else (msg.body ?: "Photo"),
                                        color = XWhite,
                                        fontSize = 14.sp
                                    )
                                    Text(msg.createdAt.take(16), color = XWhiteMuted, fontSize = 10.sp)
                                }
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = messageBody,
                                onValueChange = { messageBody = it },
                                placeholder = { Text("Message") },
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    val cid = activeConvId ?: return@IconButton
                                    val body = messageBody.trim()
                                    if (body.isEmpty()) return@IconButton
                                    scope.launch {
                                        runCatching {
                                            SupabaseModule.client.postgrest["social_messages"].insert(
                                                buildJsonObject {
                                                    put("conversation_id", cid)
                                                    put("body", body)
                                                }
                                            )
                                            activeMessages.add(
                                                DirectMessage(
                                                    id = "local-${System.currentTimeMillis()}",
                                                    senderId = account?.userId.orEmpty(),
                                                    mine = true,
                                                    body = body,
                                                    createdAt = "Now"
                                                )
                                            )
                                            messageBody = ""
                                        }.onFailure { e ->
                                            statusBanner = e.localizedMessage
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = XWhite)
                            }
                        }
                    }
                }
            }

            3 -> {
                // Social Profile separating Photos/Videos tab from Text Posts tab (matches source/tests/tests.json)
                val myPosts = feedPosts.filter { it.author.id == (account?.userId ?: currentUser?.id) }
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { profilePostTab = 0 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (profilePostTab == 0) XWhite else XBlack,
                                contentColor = if (profilePostTab == 0) XBlack else XWhite
                            )
                        ) {
                            Text("Photos/Videos", fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { profilePostTab = 1 },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (profilePostTab == 1) XWhite else XBlack,
                                contentColor = if (profilePostTab == 1) XBlack else XWhite
                            )
                        ) {
                            Text("Text Posts", fontWeight = FontWeight.Bold)
                        }
                    }

                    val filtered = if (profilePostTab == 0) {
                        myPosts.filter { it.media.isNotEmpty() }
                    } else {
                        myPosts.filter { it.media.isEmpty() }
                    }

                    if (filtered.isEmpty()) {
                        Text(
                            text = if (profilePostTab == 0) "No photos or videos yet" else "No text posts yet",
                            color = XWhiteMuted,
                            fontSize = 14.sp
                        )
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(filtered, key = { it.id }) { p ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .liquidGlass(RoundedCornerShape(14.dp))
                                        .padding(14.dp)
                                ) {
                                    Text(p.caption, color = XWhite, fontSize = 14.sp)
                                    Text(p.createdAt.take(16), color = XWhiteMuted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            4 -> {
                // Staff / Owner Moderation Guard (social_moderate, social_set_rank, social_set_staff_role)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(RoundedCornerShape(20.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = XWhite)
                        Text(
                            text = " STAFF ADMIN (${myRole.displayName.uppercase()})",
                            color = XWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "• Enforced by Supabase RLS & 001_x_social.sql RPCs (social_moderate, social_set_rank, social_set_staff_role).\n" +
                            "• The Owner is protected and cannot be changed or moderated.\n" +
                            "• Developer (is_super) has full badge, rank, config, and audit access.\n" +
                            "• Admins can moderate non-staff users and reports.",
                        color = XWhiteMuted,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SocialPostCard(
    post: SocialPost,
    onBadgeClick: (UserRole) -> Unit,
    onToggleLike: () -> Unit,
    onToggleSave: () -> Unit,
    onShare: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .liquidGlass(RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "@${post.author.username}",
                    color = XWhite,
                    fontWeight = FontWeight.Bold
                )
                RoleBadgeChip(
                    role = post.author.parsedRole,
                    onClick = onBadgeClick
                )
            }
            Text(post.createdAt.take(10), color = XWhiteMuted, fontSize = 11.sp)
        }

        if (post.caption.isNotBlank()) {
            Text(text = post.caption, color = XWhite, fontSize = 14.sp)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onToggleLike() }
                ) {
                    Icon(
                        imageVector = if (post.liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Like",
                        tint = XWhite
                    )
                    Text(" ${post.likeCount}", color = XWhite, fontSize = 12.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ChatBubbleOutline, contentDescription = "Comments", tint = XWhite)
                    Text(" ${post.commentCount}", color = XWhite, fontSize = 12.sp)
                }
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = XWhite,
                    modifier = Modifier.clickable { onShare() }
                )
            }
            Icon(
                imageVector = if (post.saved) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = "Save",
                tint = XWhite,
                modifier = Modifier.clickable { onToggleSave() }
            )
        }
    }
}
