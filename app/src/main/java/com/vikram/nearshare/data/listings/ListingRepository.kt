package com.vikram.nearshare.data.listings

import android.content.Context
import android.net.Uri
import com.vikram.nearshare.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class Listing(
    val id: String, val ownerId: String, val title: String, val description: String,
    val category: String, val listingType: String, val condition: String,
    val location: String, val imageUrl: String?, val createdAt: String, val latitude: Double?, val longitude: Double?
)

data class ListingRequest(val id: String, val listingId: String, val listingTitle: String, val requesterId: String, val ownerId: String, val message: String, val status: String, val createdAt: String = "")
data class ListingMessage(val id: String, val requestId: String, val senderId: String, val body: String, val createdAt: String)
data class AppNotification(val id: String, val title: String, val body: String, val createdAt: String, val readAt: String?)

class ListingRepository(private val context: Context) {
    suspend fun requestListing(listingId: String, message: String, token: String) = withContext(Dispatchers.IO) {
        val body = JSONObject().put("listing_id", listingId).put("message", message.take(500))
        request("/rest/v1/listing_requests?select=*", "POST", body, token, mapOf("Prefer" to "return=representation"))
    }
    suspend fun getRequests(token: String, incoming: Boolean): List<ListingRequest> = withContext(Dispatchers.IO) {
        val userId = currentUserId(token)
        val path = if (incoming) "/rest/v1/listing_requests?select=*,listings!inner(title,owner_id)&listings.owner_id=eq.$userId&order=created_at.desc" else "/rest/v1/listing_requests?select=*,listings!inner(title,owner_id)&requester_id=eq.$userId&order=created_at.desc"
        val array = JSONArray(request(path, "GET", token = token))
        (0 until array.length()).mapNotNull { i -> array.optJSONObject(i)?.let { row -> val item = row.optJSONObject("listings") ?: JSONObject(); ListingRequest(row.optString("id"), row.optString("listing_id"), item.optString("title"), row.optString("requester_id"), item.optString("owner_id"), row.optString("message"), row.optString("status"), row.optString("created_at")) } }
    }

    suspend fun getMessages(requestId: String, token: String): List<ListingMessage> = withContext(Dispatchers.IO) {
        val path = "/rest/v1/listing_messages?select=*&request_id=eq.${encode(requestId)}&order=created_at.asc&limit=300"
        val array = JSONArray(request(path, "GET", token = token))
        (0 until array.length()).mapNotNull { i -> array.optJSONObject(i)?.let { row ->
            ListingMessage(row.optString("id"), row.optString("request_id"), row.optString("sender_id"), row.optString("body"), row.optString("created_at"))
        } }
    }

    suspend fun sendMessage(requestId: String, body: String, token: String) = withContext(Dispatchers.IO) {
        val clean = body.trim()
        if (clean.isBlank() || clean.length > 2000) throw IOException("Messages must be between 1 and 2,000 characters.")
        request("/rest/v1/listing_messages", "POST", JSONObject().put("request_id", requestId).put("body", clean), token, mapOf("Prefer" to "return=minimal"))
    }

    suspend fun getNotifications(token: String): List<AppNotification> = withContext(Dispatchers.IO) {
        val userId = currentUserId(token)
        val path = "/rest/v1/app_notifications?select=*&recipient_id=eq.${encode(userId)}&order=created_at.desc&limit=100"
        val array = JSONArray(request(path, "GET", token = token))
        (0 until array.length()).mapNotNull { i -> array.optJSONObject(i)?.let { row ->
            AppNotification(row.optString("id"), row.optString("title"), row.optString("body"), row.optString("created_at"), row.optString("read_at").takeIf { it.isNotBlank() && it != "null" })
        } }
    }

    suspend fun markNotificationsRead(token: String) = withContext(Dispatchers.IO) {
        val userId = currentUserId(token)
        request("/rest/v1/app_notifications?recipient_id=eq.${encode(userId)}&read_at=is.null", "PATCH", JSONObject().put("read_at", java.time.Instant.now().toString()), token, mapOf("Prefer" to "return=minimal"))
    }

    suspend fun reportListing(listingId: String, reporterId: String, reason: String, details: String, token: String) = withContext(Dispatchers.IO) {
        val cleanReason = reason.trim()
        if (cleanReason.isBlank()) throw IOException("Choose a reason for the report.")
        request("/rest/v1/listing_reports", "POST", JSONObject().put("listing_id", listingId).put("reporter_id", reporterId).put("reason", cleanReason).put("details", details.trim().take(1000)), token, mapOf("Prefer" to "return=minimal"))
    }
    suspend fun setRequestStatus(id: String, status: String, token: String) = withContext(Dispatchers.IO) {
        if (status !in setOf("accepted", "declined", "cancelled", "completed")) throw IOException("Invalid request status.")
        request("/rest/v1/listing_requests?id=eq.$id", "PATCH", JSONObject().put("status", status), token, mapOf("Prefer" to "return=minimal"))
    }
    private fun currentUserId(token: String): String {
        val payload = token.split(".").getOrNull(1) ?: throw IOException("Invalid session. Sign in again.")
        val bytes = android.util.Base64.decode(payload, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING)
        return JSONObject(String(bytes, Charsets.UTF_8)).optString("sub")
    }
    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val apiKey = BuildConfig.SUPABASE_ANON_KEY

    suspend fun getListings(search: String = ""): List<Listing> = withContext(Dispatchers.IO) {
        val filter = if (search.isBlank()) "" else "&title=ilike.*${encode(search)}*"
        val response = request("/rest/v1/listings?select=*&status=eq.active$filter&order=created_at.desc&limit=100", "GET")
        val array = JSONArray(response)
        (0 until array.length()).mapNotNull { array.optJSONObject(it)?.toListing() }
    }

    suspend fun createListing(ownerId: String, token: String, title: String, description: String,
        category: String, listingType: String, condition: String, location: String, imageUri: Uri?, latitude: Double? = null, longitude: Double? = null): Listing =
        withContext(Dispatchers.IO) {
            val imageUrl = imageUri?.let { uploadImage(ownerId, token, it) }
            val payload = JSONObject().put("owner_id", ownerId).put("title", title.trim())
                .put("description", description.trim()).put("category", category)
                .put("listing_type", listingType).put("item_condition", condition)
                .put("location_text", location.trim()).put("image_url", imageUrl ?: JSONObject.NULL)
                .put("status", "active").put("latitude", latitude ?: JSONObject.NULL).put("longitude", longitude ?: JSONObject.NULL)
            val result = request("/rest/v1/listings?select=*", "POST", payload, token, mapOf("Prefer" to "return=representation"))
            JSONArray(result).optJSONObject(0)?.toListing()
                ?: throw IOException("Listing was saved, but the server returned no listing.")
        }

    private fun uploadImage(ownerId: String, token: String, uri: Uri): String {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val ext = when (mime.lowercase()) { "image/png" -> "png"; "image/webp" -> "webp"; else -> "jpg" }
        val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
            val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192); var total = 0
            while (true) { val n = input.read(buffer); if (n < 0) break; total += n
                if (total > 8 * 1024 * 1024) throw IOException("Choose an image smaller than 8 MB.")
                output.write(buffer, 0, n) }
            output.toByteArray()
        } ?: throw IOException("Could not read the selected image.")
        val path = "$ownerId/${UUID.randomUUID()}.$ext"
        val connection = (URL("$baseUrl/storage/v1/object/nearshare-listing-images/$path").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"; connectTimeout = 20000; readTimeout = 30000; doOutput = true
            setRequestProperty("apikey", apiKey); setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Content-Type", mime); setRequestProperty("x-upsert", "false")
        }
        try {
            connection.outputStream.use { it.write(bytes) }
            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) throw IOException("Image upload failed (HTTP $status): " +
                runCatching { JSONObject(body).optString("message") }.getOrDefault(""))
        } finally { connection.disconnect() }
        return "$baseUrl/storage/v1/object/public/nearshare-listing-images/$path"
    }

    private fun request(path: String, method: String, body: JSONObject? = null, token: String? = null,
        headers: Map<String, String> = emptyMap()): String {
        if (!baseUrl.startsWith("https://") || apiKey.isBlank()) throw IOException("Supabase is not configured. Add SUPABASE_URL and SUPABASE_ANON_KEY.")
        val connection = (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 15000; readTimeout = 20000
            setRequestProperty("apikey", apiKey)
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
            if (body != null) { setRequestProperty("Content-Type", "application/json"); doOutput = true }
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { val json = JSONObject(text); json.optString("message").ifBlank { json.optString("details").ifBlank { json.optString("error") } } }.getOrDefault("")
                throw IOException(message.ifBlank { "Supabase request failed (HTTP $status)." })
            }
            return text
        } finally { connection.disconnect() }
    }
    private fun JSONObject.toListing() = Listing(optString("id"), optString("owner_id"), optString("title"),
        optString("description"), optString("category"), optString("listing_type"), optString("item_condition"),
        optString("location_text"), optString("image_url").takeIf { it.isNotBlank() && it != "null" }, optString("created_at"),
        if (isNull("latitude")) null else optDouble("latitude"), if (isNull("longitude")) null else optDouble("longitude"))
    private fun encode(value: String) = java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
