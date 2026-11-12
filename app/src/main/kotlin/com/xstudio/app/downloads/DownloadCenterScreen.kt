package com.xstudio.app.downloads

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xstudio.app.BuildConfig
import com.xstudio.app.core.designsystem.XBlack
import com.xstudio.app.core.designsystem.XGlassBorder
import com.xstudio.app.core.designsystem.XWhite
import com.xstudio.app.core.designsystem.XWhiteMuted
import com.xstudio.app.core.designsystem.liquidGlass
import com.xstudio.app.network.SupabaseModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable
data class CobaltResponse(
    val status: String = "",
    val url: String? = null,
    val filename: String? = null
)

data class DownloadRow(
    val sourceUrl: String,
    val status: String,
    val downloadLink: String? = null,
    val filename: String? = null,
    val error: String? = null
)

@Composable
fun DownloadCenterScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var urlsText by remember { mutableStateOf("") }
    var selectedQuality by remember { mutableStateOf("1080") }
    var isWorking by remember { mutableStateOf(false) }
    val results = remember { mutableStateListOf<DownloadRow>() }

    val qualities = listOf("720", "1080", "1440", "2160", "max")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(XBlack)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = XWhite)
            }
            Column {
                Text("DOWNLOAD CENTER", color = XWhite, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                Text("Save public media from YouTube, TikTok, Instagram, X, Reddit (up to 10 links)", color = XWhiteMuted, fontSize = 11.sp)
            }
        }

        OutlinedTextField(
            value = urlsText,
            onValueChange = { urlsText = it },
            placeholder = { Text("Paste public links, one per line (batch up to 10)") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = XWhite,
                unfocusedBorderColor = XGlassBorder,
                focusedTextColor = XWhite,
                unfocusedTextColor = XWhite
            )
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            qualities.forEach { q ->
                val active = q == selectedQuality
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) XWhite else XBlack)
                        .border(1.dp, XGlassBorder, RoundedCornerShape(10.dp))
                        .clickable { selectedQuality = q }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = if (q == "max") "Max" else "${q}p",
                        color = if (active) XBlack else XWhite,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Button(
            onClick = {
                val urls = urlsText.split(Regex("\\s+")).filter { it.isNotBlank() }.take(10)
                if (urls.isEmpty()) return@Button
                scope.launch {
                    isWorking = true
                    results.clear()
                    urls.forEach { link ->
                        val row = withContext(Dispatchers.IO) {
                            runCatching {
                                val payload = SupabaseModule.json.encodeToString(
                                    buildJsonObject {
                                        put("url", link)
                                        put("videoQuality", selectedQuality)
                                        put("filenameStyle", "pretty")
                                    }
                                )
                                val raw = if (BuildConfig.XSTUDIO_API_BASE_URL.isNotBlank()) {
                                    SupabaseModule.postBackendJson("/api/cobalt-download", payload).second
                                } else {
                                    val req = Request.Builder()
                                        .url(BuildConfig.DEFAULT_COBALT_URL)
                                        .addHeader("Accept", "application/json")
                                        .addHeader("Content-Type", "application/json")
                                        .post(payload.toRequestBody("application/json".toMediaType()))
                                        .build()
                                    SupabaseModule.httpClient.newCall(req).execute().use { it.body?.string().orEmpty() }
                                }
                                val parsed = SupabaseModule.json.decodeFromString<CobaltResponse>(raw)
                                if ((parsed.status == "tunnel" || parsed.status == "redirect") && !parsed.url.isNullOrBlank()) {
                                    DownloadRow(sourceUrl = link, status = "ok", downloadLink = parsed.url, filename = parsed.filename)
                                } else {
                                    DownloadRow(sourceUrl = link, status = "error", error = parsed.status.ifBlank { "unsupported_link" })
                                }
                            }.getOrElse { e ->
                                DownloadRow(sourceUrl = link, status = "error", error = e.localizedMessage ?: "request_failed")
                            }
                        }
                        results.add(row)
                    }
                    isWorking = false
                }
            },
            enabled = !isWorking && urlsText.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isWorking) "Working…" else "Get download", fontWeight = FontWeight.ExtraBold)
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(results) { r ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .liquidGlass(RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(r.sourceUrl, color = XWhiteMuted, fontSize = 12.sp)
                    if (r.status == "ok" && r.downloadLink != null) {
                        Button(
                            onClick = {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(r.downloadLink)))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = XWhite, contentColor = XBlack)
                        ) {
                            Text("Download ${r.filename.orEmpty()}", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Text("Error: ${r.error}", color = XWhite, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
