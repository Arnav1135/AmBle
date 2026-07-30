package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class SupabaseAuthState {
    object Unauthenticated : SupabaseAuthState()
    data class Authenticated(val userId: String, val email: String, val accessToken: String) : SupabaseAuthState()
    data class Error(val message: String) : SupabaseAuthState()
}

data class SupabaseConfigStatus(
    val isConfigured: Boolean,
    val url: String,
    val isConnected: Boolean,
    val lastSyncTime: Long? = null,
    val errorMessage: String? = null
)

class SupabaseClient private constructor() {

    companion object {
        private const val TAG = "SupabaseClient"

        @Volatile
        private var instance: SupabaseClient? = null

        fun getInstance(): SupabaseClient {
            return instance ?: synchronized(this) {
                instance ?: SupabaseClient().also { instance = it }
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    // Configured via secrets or BuildConfig
    val supabaseUrl: String
        get() {
            val url = try { BuildConfig.SUPABASE_URL } catch (e: Throwable) { "" }
            return if (url.isNotBlank() && !url.contains("your-project-id")) url.trimEnd('/') else "https://your-project-id.supabase.co"
        }

    val supabaseAnonKey: String
        get() {
            val key = try { BuildConfig.SUPABASE_ANON_KEY } catch (e: Throwable) { "" }
            return if (key.isNotBlank() && !key.contains("your-supabase-anon-key")) key else "your-supabase-anon-key-placeholder"
        }

    val isConfigured: Boolean
        get() = supabaseUrl.contains("supabase.co") && !supabaseUrl.contains("your-project-id") && supabaseAnonKey.length > 20

    private val _authState = MutableStateFlow<SupabaseAuthState>(SupabaseAuthState.Unauthenticated)
    val authState: StateFlow<SupabaseAuthState> = _authState.asStateFlow()

    private val _configStatus = MutableStateFlow(
        SupabaseConfigStatus(
            isConfigured = isConfigured,
            url = supabaseUrl,
            isConnected = false
        )
    )
    val configStatus: StateFlow<SupabaseConfigStatus> = _configStatus.asStateFlow()

    private fun buildRequest(path: String, method: String = "GET", jsonBody: String? = null): Request {
        val fullUrl = if (path.startsWith("http")) path else "$supabaseUrl$path"
        val builder = Request.Builder()
            .url(fullUrl)
            .addHeader("apikey", supabaseAnonKey)
            .addHeader("Content-Type", "application/json")

        // Add Auth Token if available
        val currentAuth = authState.value
        if (currentAuth is SupabaseAuthState.Authenticated) {
            builder.addHeader("Authorization", "Bearer ${currentAuth.accessToken}")
        } else {
            builder.addHeader("Authorization", "Bearer $supabaseAnonKey")
        }

        when (method.uppercase()) {
            "POST" -> {
                val body = (jsonBody ?: "{}").toRequestBody("application/json; charset=utf-8".toMediaType())
                builder.post(body)
                builder.addHeader("Prefer", "return=representation")
            }
            "PATCH" -> {
                val body = (jsonBody ?: "{}").toRequestBody("application/json; charset=utf-8".toMediaType())
                builder.patch(body)
                builder.addHeader("Prefer", "return=representation")
            }
            "PUT" -> {
                val body = (jsonBody ?: "{}").toRequestBody("application/json; charset=utf-8".toMediaType())
                builder.put(body)
            }
            "DELETE" -> {
                builder.delete()
            }
            else -> builder.get()
        }

        return builder.build()
    }

    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            _configStatus.value = _configStatus.value.copy(
                isConfigured = false,
                isConnected = false,
                errorMessage = "Supabase URL/Key not configured in Secrets"
            )
            return@withContext false
        }

        try {
            val request = buildRequest("/rest/v1/")
            client.newCall(request).execute().use { response ->
                val success = response.isSuccessful || response.code == 404 || response.code == 401
                _configStatus.value = _configStatus.value.copy(
                    isConfigured = true,
                    isConnected = success,
                    lastSyncTime = System.currentTimeMillis(),
                    errorMessage = if (success) null else "Server returned code ${response.code}"
                )
                return@withContext success
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase connection failed", e)
            _configStatus.value = _configStatus.value.copy(
                isConfigured = true,
                isConnected = false,
                errorMessage = e.localizedMessage ?: "Network connection error"
            )
            return@withContext false
        }
    }

    // AUTHENTICATION
    suspend fun signUp(email: String, pass: String): Result<SupabaseAuthState.Authenticated> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.failure(Exception("Supabase credentials not configured. Please add SUPABASE_URL and SUPABASE_ANON_KEY to Secrets."))
        }

        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", pass)
            }
            val request = buildRequest("/auth/v1/signup", "POST", json.toString())
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonObj = JSONObject(bodyStr)
                    val userObj = jsonObj.optJSONObject("user") ?: jsonObj
                    val userId = userObj.optString("id", "user_${System.currentTimeMillis()}")
                    val token = jsonObj.optString("access_token", "anon_token")

                    val auth = SupabaseAuthState.Authenticated(userId, email, token)
                    _authState.value = auth
                    Result.success(auth)
                } else {
                    val errMsg = try { JSONObject(bodyStr).optString("msg", "Sign up failed") } catch (e: Exception) { "Sign up failed (${response.code})" }
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, pass: String): Result<SupabaseAuthState.Authenticated> = withContext(Dispatchers.IO) {
        if (!isConfigured) {
            return@withContext Result.failure(Exception("Supabase credentials not configured."))
        }

        try {
            val json = JSONObject().apply {
                put("email", email)
                put("password", pass)
            }
            val request = buildRequest("/auth/v1/token?grant_type=password", "POST", json.toString())
            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (response.isSuccessful) {
                    val jsonObj = JSONObject(bodyStr)
                    val token = jsonObj.getString("access_token")
                    val userObj = jsonObj.getJSONObject("user")
                    val userId = userObj.getString("id")

                    val auth = SupabaseAuthState.Authenticated(userId, email, token)
                    _authState.value = auth
                    Result.success(auth)
                } else {
                    val errMsg = try { JSONObject(bodyStr).optString("error_description", "Invalid login credentials") } catch (e: Exception) { "Sign in failed" }
                    Result.failure(Exception(errMsg))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // DATABASE REST API
    suspend fun syncUserToSupabase(user: UserEntity): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext false

        try {
            val json = JSONObject().apply {
                put("uid", user.uid)
                put("name", user.name)
                put("email", user.email)
                put("phone_number", user.phoneNumber)
                put("photo_url", user.photoUrl)
                put("status", user.status)
                put("is_online", user.isOnline)
                put("last_seen", user.lastSeen)
                put("updated_at", System.currentTimeMillis())
            }

            val request = buildRequest("/rest/v1/users", "POST", json.toString())
            client.newCall(request).execute().use { response ->
                Log.d(TAG, "Sync user to Supabase response: ${response.code}")
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to sync user to Supabase", e)
            return@withContext false
        }
    }

    suspend fun fetchRemoteUsers(): List<UserEntity> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext emptyList()

        val list = mutableListOf<UserEntity>()
        try {
            val request = buildRequest("/rest/v1/users?select=*")
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(bodyStr)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            UserEntity(
                                uid = obj.optString("uid", obj.optString("id", "")),
                                name = obj.optString("name", "AmBle Contact"),
                                email = obj.optString("email", ""),
                                photoUrl = obj.optString("photo_url", ""),
                                status = obj.optString("status", "Hey there! I am using AmBle"),
                                isOnline = obj.optBoolean("is_online", false),
                                lastSeen = obj.optLong("last_seen", System.currentTimeMillis()),
                                isMe = false,
                                phoneNumber = obj.optString("phone_number", "")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch remote users from Supabase", e)
        }
        return@withContext list
    }

    suspend fun sendMessageToSupabase(msg: MessageEntity): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext false

        try {
            val json = JSONObject().apply {
                put("id", msg.messageId)
                put("chat_id", msg.chatId)
                put("sender_id", msg.senderId)
                put("content", msg.text)
                put("media_type", msg.type)
                put("media_url", msg.mediaUrl)
                put("status", msg.status)
                put("timestamp", msg.timestamp)
            }

            val request = buildRequest("/rest/v1/messages", "POST", json.toString())
            client.newCall(request).execute().use { response ->
                Log.d(TAG, "Sent message to Supabase: ${response.code}")
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send message to Supabase", e)
            return@withContext false
        }
    }

    suspend fun fetchMessagesFromSupabase(chatId: String): List<MessageEntity> = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext emptyList()

        val list = mutableListOf<MessageEntity>()
        try {
            val request = buildRequest("/rest/v1/messages?chat_id=eq.$chatId&order=timestamp.asc")
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: "[]"
                    val jsonArray = JSONArray(bodyStr)
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        list.add(
                            MessageEntity(
                                messageId = obj.optString("id", obj.optString("message_id", "")),
                                chatId = obj.optString("chat_id"),
                                senderId = obj.optString("sender_id"),
                                text = obj.optString("content", obj.optString("text", "")),
                                type = obj.optString("media_type", obj.optString("type", "text")),
                                mediaUrl = obj.optString("media_url", null),
                                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                                status = obj.optString("status", "sent")
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch messages from Supabase", e)
        }
        return@withContext list
    }

    suspend fun saveBackupToSupabase(userId: String, backupDataJson: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured) return@withContext false

        try {
            val json = JSONObject().apply {
                put("user_id", userId)
                put("backup_payload", backupDataJson)
                put("created_at", System.currentTimeMillis())
            }

            val request = buildRequest("/rest/v1/backups", "POST", json.toString())
            client.newCall(request).execute().use { response ->
                return@withContext response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save backup to Supabase", e)
            return@withContext false
        }
    }
}
