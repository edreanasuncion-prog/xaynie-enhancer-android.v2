package com.xstudio.app.chatbot

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xstudio.app.core.designsystem.XBlack
import com.xstudio.app.core.designsystem.XGlassBorder
import com.xstudio.app.core.designsystem.XStudioLogo
import com.xstudio.app.core.designsystem.XWhite
import com.xstudio.app.core.designsystem.XWhiteMuted
import com.xstudio.app.core.designsystem.liquidGlass
import com.xstudio.app.network.SupabaseModule
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class AskModelOption(
    val id: String,
    val name: String,
    val level: String,
    val blurb: String
)

val ASK_X_MODELS = listOf(
    AskModelOption("x-ai-3-1", "X AI 3.1", "Fast", "Quick answers"),
    AskModelOption("x-ai-4-5", "X AI 4.5", "Medium", "Balanced speed and depth"),
    AskModelOption("xaynie-ai-5-5-astral", "Xaynie AI 5.5 Astral", "High", "Deepest reasoning")
)

private val ASK_ERRORS = mapOf(
    "sign_in_required" to "Please sign in again to use Ask X.",
    "gemini_key_missing" to "Ask X is not set up yet: the Gemini API key is missing on the server.",
    "model_unavailable" to "This model is not available from the Gemini API right now. It was not swapped for another one.",
    "gemini_error" to "The Gemini API returned an error.",
    "blocked" to "That message was blocked by the model's safety filters.",
    "empty_response" to "The model returned no text. Try rephrasing your message.",
    "invalid_messages" to "That message could not be sent. Messages must be under 8,000 characters.",
    "unknown_model" to "Unknown model selected.",
    "timeout" to "This model took too long to reply (the server allows about 25 seconds). Try a shorter question, or pick a faster model.",
    "auth_unreachable" to "Could not verify your sign-in right now. Please try again.",
    "network" to "Could not reach the server. Check your connection and try again."
)

@Serializable
data class AskApiError(val code: String? = null, val message: String? = null)

@Serializable
data class AskApiResponse(
    val ok: Boolean = false,
    val text: String? = null,
    val error: AskApiError? = null
)

data class ChatTurn(val role: String, val text: String, val isError: Boolean = false)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AskXScreen(onBack: () -> Unit) {
    var showModelSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val history = remember { mutableStateListOf<ChatTurn>() }
    var prompt by remember { mutableStateOf("") }
    var selectedModel by remember { mutableStateOf(ASK_X_MODELS[0]) }
    var isStreaming by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XBlack)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = XWhite)
                }
                Column {
                    Text("Ask X", color = XWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    Text("${selectedModel.name} · Thinking: ${selectedModel.level}", color = XWhiteMuted, fontSize = 11.sp)
                }
            }
            IconButton(
                enabled = !isStreaming,
                onClick = {
                    history.clear()
                    prompt = ""
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "New chat", tint = XWhite)
            }
        }

        if (showModelSheet) {
            ModalBottomSheet(
                onDismissRequest = { showModelSheet = false },
                containerColor = Color(0xFF141418),
                contentColor = XWhite
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Select model", color = XWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    ASK_X_MODELS.forEach { m ->
                        val active = m.id == selectedModel.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .liquidGlass(RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedModel = m
                                    showModelSheet = false
                                }
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("\${m.name}  ·  Thinking: \${m.level}", color = XWhite, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text(m.blurb, color = XWhiteMuted, fontSize = 12.sp)
                            }
                            if (active) {
                                Icon(Icons.Default.Check, contentDescription = "Selected", tint = XWhite)
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (history.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        XStudioLogo(iconSize = 56.dp, showWordmark = false)
                        Text("How can I help?", color = XWhite, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            items(history) { turn ->
                val isUser = turn.role == "user"
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
                ) {
                    Column(
                        modifier = Modifier
                            .liquidGlass(RoundedCornerShape(18.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = if (isUser) "YOU" else selectedModel.name,
                            color = XWhiteMuted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(text = turn.text, color = XWhite, fontSize = 14.sp)
                    }
                }
            }
        }

        if (isStreaming) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                CircularProgressIndicator(color = XWhite, strokeWidth = 2.dp)
                Text("Thinking...", color = XWhiteMuted, fontSize = 12.sp)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .liquidGlass(RoundedCornerShape(24.dp))
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = prompt,
                onValueChange = { if (it.length <= 8000) prompt = it },
                placeholder = { Text("Ask X anything…") },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = XWhite,
                    unfocusedBorderColor = XGlassBorder,
                    focusedTextColor = XWhite,
                    unfocusedTextColor = XWhite
                )
            )
            IconButton(
                enabled = prompt.isNotBlank() && !isStreaming,
                onClick = {
                    val userPrompt = prompt.trim()
                    prompt = ""
                    history.add(ChatTurn(role = "user", text = userPrompt))
                    scope.launch {
                        isStreaming = true
                        val replyTurn = runCatching {
                            SupabaseModule.ensureInitialized()
                            val accessToken = SupabaseModule.client.auth.currentSessionOrNull()?.accessToken.orEmpty()
                            val payload = buildJsonObject {
                                put("model", selectedModel.id)
                                put("token", accessToken)
                                put("messages", buildJsonArray {
                                    history.filter { !it.isError }.forEach { msg ->
                                        add(buildJsonObject {
                                            put("role", msg.role)
                                            put("text", msg.text)
                                        })
                                    }
                                })
                            }
                            val (_, rawBody) = SupabaseModule.postBackendJson(
                                endpointPath = "/api/ask",
                                payloadJson = SupabaseModule.json.encodeToString(payload)
                            )
                            val parsed = SupabaseModule.json.decodeFromString<AskApiResponse>(rawBody)
                            if (parsed.ok && !parsed.text.isNullOrBlank()) {
                                ChatTurn(role = "model", text = parsed.text)
                            } else {
                                val code = parsed.error?.code.orEmpty()
                                val detail = parsed.error?.message
                                val baseMsg = ASK_ERRORS[code] ?: "Something went wrong. Please try again."
                                val fullMsg = if (!detail.isNullOrBlank()) "$baseMsg ($detail)" else baseMsg
                                ChatTurn(role = "model", text = fullMsg, isError = true)
                            }
                        }.getOrElse { e ->
                            ChatTurn(
                                role = "model",
                                text = ASK_ERRORS["network"] + " (${e.localizedMessage ?: "Configure XSTUDIO_API_BASE_URL"})",
                                isError = true
                            )
                        }
                        history.add(replyTurn)
                        isStreaming = false
                    }
                }
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = XWhite)
            }
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0x1AFFFFFF))
                .border(1.dp, XGlassBorder, RoundedCornerShape(999.dp))
                .clickable { showModelSheet = true }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("\${selectedModel.name} \${selectedModel.level}", color = XWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Select model", tint = XWhite)
        }
        }
    }
}
