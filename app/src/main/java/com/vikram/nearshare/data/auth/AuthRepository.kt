package com.vikram.nearshare.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.vikram.nearshare.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class AuthUser(
    val id: String,
    val email: String,
    val displayName: String
)

class AuthRepository(context: Context) {
    private val preferences = EncryptedSharedPreferences.create(
        context,
        "nearshare_auth",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val anonKey = BuildConfig.SUPABASE_ANON_KEY

    fun isConfigured(): Boolean = baseUrl.startsWith("https://") && anonKey.isNotBlank()

    suspend fun restoreSession(): AuthUser? = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext null
        val token = preferences.getString(KEY_ACCESS_TOKEN, null) ?: return@withContext null
        try {
            userRequest(token)
        } catch (error: IOException) {
            if (error.message?.contains("401") != true) throw error
            refreshSession()
        }
    }

    suspend fun signIn(email: String, password: String): AuthUser = withContext(Dispatchers.IO) {
        ensureConfigured()
        val response = request(
            path = "/auth/v1/token?grant_type=password",
            method = "POST",
            body = JSONObject().put("email", email.trim()).put("password", password)
        )
        saveSession(response)
        userFrom(response.optJSONObject("user") ?: response)
    }

    suspend fun signUp(email: String, password: String, displayName: String): AuthUser? =
        withContext(Dispatchers.IO) {
            ensureConfigured()
            val response = request(
                path = "/auth/v1/signup",
                method = "POST",
                body = JSONObject()
                    .put("email", email.trim())
                    .put("password", password)
                    .put("data", JSONObject().put("full_name", displayName.trim()))
            )
            val accessToken = response.optString("access_token")
            if (accessToken.isBlank()) return@withContext null
            saveSession(response)
            userFrom(response.optJSONObject("user") ?: response)
        }

    suspend fun updateDisplayName(displayName: String): AuthUser = withContext(Dispatchers.IO) {
        ensureConfigured()
        val token = preferences.getString(KEY_ACCESS_TOKEN, null)
            ?: throw IOException("Your session has expired. Please sign in again.")
        val response = request(
            path = "/auth/v1/user",
            method = "PUT",
            body = JSONObject().put("data", JSONObject().put("full_name", displayName.trim())),
            accessToken = token
        )
        userFrom(response)
    }

    suspend fun signOut() = withContext(Dispatchers.IO) {
        val token = preferences.getString(KEY_ACCESS_TOKEN, null)
        try {
            if (isConfigured() && !token.isNullOrBlank()) {
                request("/auth/v1/logout", "POST", JSONObject(), token)
            }
        } finally {
            clearSession()
        }
    }

    private fun refreshSession(): AuthUser? {
        val refreshToken = preferences.getString(KEY_REFRESH_TOKEN, null) ?: run {
            clearSession()
            return null
        }
        return try {
            val response = request(
                "/auth/v1/token?grant_type=refresh_token",
                "POST",
                JSONObject().put("refresh_token", refreshToken)
            )
            saveSession(response)
            userFrom(response.optJSONObject("user") ?: response)
        } catch (_: Exception) {
            clearSession()
            null
        }
    }

    private fun userRequest(token: String): AuthUser {
        val response = request("/auth/v1/user", "GET", null, token)
        return userFrom(response)
    }

    private fun userFrom(json: JSONObject): AuthUser {
        val metadata = json.optJSONObject("user_metadata") ?: JSONObject()
        return AuthUser(
            id = json.optString("id"),
            email = json.optString("email"),
            displayName = metadata.optString("full_name").takeIf { it.isNotBlank() }
                ?: json.optString("email").substringBefore("@").ifBlank { "NearShare member" }
        )
    }

    private fun saveSession(response: JSONObject) {
        val access = response.optString("access_token")
        val refresh = response.optString("refresh_token")
        if (access.isNotBlank()) preferences.edit().putString(KEY_ACCESS_TOKEN, access).apply()
        if (refresh.isNotBlank()) preferences.edit().putString(KEY_REFRESH_TOKEN, refresh).apply()
    }

    private fun clearSession() {
        preferences.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN).apply()
    }

    private fun ensureConfigured() {
        if (!isConfigured()) {
            throw IOException(
                "Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY to your local Gradle properties or GitHub Actions secrets."
            )
        }
    }

    private fun request(
        path: String,
        method: String,
        body: JSONObject?,
        accessToken: String? = null
    ): JSONObject {
        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 15_000
            setRequestProperty("apikey", anonKey)
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/json")
            if (!accessToken.isNullOrBlank()) {
                setRequestProperty("Authorization", "Bearer $accessToken")
            }
            if (body != null) doOutput = true
        }
        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            val json = runCatching { JSONObject(text) }.getOrElse { JSONObject() }
            if (status !in 200..299) {
                val message = json.optString("msg").ifBlank {
                    json.optString("message").ifBlank {
                        json.optString("error_description").ifBlank {
                            json.optString("error").ifBlank { "Request failed (HTTP $status)." }
                        }
                    }
                }
                throw IOException("$message (HTTP $status)")
            }
            return json
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
    }
}
