package com.xstudio.app.network

import com.xstudio.app.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** Result of trying to bring the Supabase client up. Never silently "succeeds" without a real client. */
sealed interface SupabaseInitResult {
    data object Ready : SupabaseInitResult
    data class Failed(val reason: String) : SupabaseInitResult
}

object SupabaseModule {
    const val BUCKET_PUBLIC = "social-public"
    const val BUCKET_MEDIA = "social-media"
    const val BUCKET_DM = "social-dm"

    val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(45, TimeUnit.SECONDS)
            .build()
    }

    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private val initMutex = Mutex()

    @Volatile
    private var resolvedUrl: String = BuildConfig.SUPABASE_URL.trim().trimEnd('/')

    @Volatile
    private var resolvedAnonKey: String = BuildConfig.SUPABASE_ANON_KEY.trim()

    @Volatile
    private var cachedClient: SupabaseClient? = null

    /** True once credentials are known (build-time or fetched from /api/config). */
    val isConfigured: Boolean
        get() = resolvedUrl.isNotBlank() && resolvedAnonKey.isNotBlank()

    /** True only after a real [SupabaseClient] has been created. */
    val isInitialized: Boolean
        get() = cachedClient != null

    val supabaseUrl: String get() = resolvedUrl

    /**
     * Brings Supabase up. Order of credential sources:
     *  1. SUPABASE_URL / SUPABASE_ANON_KEY compiled in from local.properties / environment.
     *  2. GET {XSTUDIO_API_BASE_URL}/api/config when (1) is incomplete.
     * A failure is returned (and not cached) so the caller can show it and retry.
     */
    suspend fun initialize(): SupabaseInitResult = withContext(Dispatchers.IO) {
        initMutex.withLock {
            if (cachedClient != null) return@withLock SupabaseInitResult.Ready

            if (!isConfigured) {
                val base = BuildConfig.XSTUDIO_API_BASE_URL.trim().trimEnd('/')
                if (base.isBlank()) {
                    return@withLock SupabaseInitResult.Failed(
                        "Supabase is not configured. Build with SUPABASE_URL and SUPABASE_ANON_KEY " +
                            "(or XSTUDIO_API_BASE_URL so the app can read /api/config)."
                    )
                }
                val fetched = fetchRemoteConfig(base)
                    ?: return@withLock SupabaseInitResult.Failed(lastRemoteConfigError)
                resolvedUrl = fetched.first
                resolvedAnonKey = fetched.second
            }

            if (resolvedUrl.toHttpUrlOrNull() == null) {
                return@withLock SupabaseInitResult.Failed(
                    "The configured Supabase URL is not a valid http(s) URL."
                )
            }

            try {
                cachedClient = createSupabaseClient(
                    supabaseUrl = resolvedUrl,
                    supabaseKey = resolvedAnonKey
                ) {
                    install(Auth) {
                        alwaysAutoRefresh = true
                        autoLoadFromStorage = true
                    }
                    install(Postgrest)
                    install(Realtime)
                    install(Storage)
                    install(Functions)
                }
                SupabaseInitResult.Ready
            } catch (e: Exception) {
                SupabaseInitResult.Failed("Could not create the Supabase client: ${e.localizedMessage ?: e.javaClass.simpleName}")
            }
        }
    }

    /** Compatibility helper for call sites that only need "client or null". */
    suspend fun ensureInitialized(): SupabaseClient? =
        if (initialize() is SupabaseInitResult.Ready) cachedClient else null

    /**
     * The live client. Throws if [initialize] has not succeeded: there is deliberately no
     * placeholder client, so an unconfigured build can never appear to work.
     */
    val client: SupabaseClient
        get() = cachedClient
            ?: throw IllegalStateException("Supabase is not initialized. Call SupabaseModule.initialize() first.")

    @Volatile
    private var lastRemoteConfigError: String = ""

    private fun fetchRemoteConfig(base: String): Pair<String, String>? {
        return try {
            val req = Request.Builder().url("$base/api/config").get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    lastRemoteConfigError = "Could not load Supabase settings from $base/api/config (HTTP ${resp.code})."
                    return null
                }
                val body = resp.body?.string().orEmpty()
                val obj: JsonObject = json.parseToJsonElement(body).jsonObject
                fun pick(vararg keys: String): String =
                    keys.firstNotNullOfOrNull { k -> (obj[k] as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim() }
                        .orEmpty()
                val url = pick("supabaseUrl", "supabase_url", "SUPABASE_URL").trimEnd('/')
                val key = pick("supabaseAnonKey", "supabase_anon_key", "supabaseKey", "SUPABASE_ANON_KEY")
                if (url.isBlank() || key.isBlank()) {
                    lastRemoteConfigError = "$base/api/config responded but did not include a Supabase URL and anon key."
                    return null
                }
                url to key
            }
        } catch (e: Exception) {
            lastRemoteConfigError = "Could not reach $base/api/config: ${e.localizedMessage ?: e.javaClass.simpleName}"
            null
        }
    }

    fun publicStorageUrl(bucket: String, path: String?): String? {
        if (path.isNullOrBlank() || !isConfigured) return null
        val encoded = path.split("/").joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
        return "$resolvedUrl/storage/v1/object/public/$bucket/$encoded"
    }

    fun publicStorageUrl(path: String?): String? = publicStorageUrl(BUCKET_PUBLIC, path)

    suspend fun postBackendJson(endpointPath: String, payloadJson: String): Pair<Int, String> =
        withContext(Dispatchers.IO) {
            val base = BuildConfig.XSTUDIO_API_BASE_URL.trim().trimEnd('/')
            require(base.isNotBlank()) { "XSTUDIO_API_BASE_URL is not configured in local.properties" }
            val req = Request.Builder()
                .url("$base$endpointPath")
                .post(payloadJson.toRequestBody("application/json".toMediaType()))
                .build()
            httpClient.newCall(req).execute().use { resp ->
                resp.code to (resp.body?.string().orEmpty())
            }
        }
}
