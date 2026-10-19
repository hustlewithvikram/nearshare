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
    val location: String, val imageUrl: String?, val createdAt: String
)

class ListingRepository(private val context: Context) {
    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/')
    private val apiKey = BuildConfig.SUPABASE_ANON_KEY

    suspend fun getListings(search: String = ""): List<Listing> = withContext(Dispatchers.IO) {
        val filter = if (search.isBlank()) "" else "&title=ilike.*${encode(search)}*"
        val response = request("/rest/v1/listings?select=*&status=eq.active$filter&order=created_at.desc&limit=100", "GET")
        val array = JSONArray(response)
        (0 until array.length()).mapNotNull { array.optJSONObject(it)?.toListing() }
    }

    suspend fun createListing(ownerId: String, token: String, title: String, description: String,
        category: String, listingType: String, condition: String, location: String, imageUri: Uri?): Listing =
        withContext(Dispatchers.IO) {
            val imageUrl = imageUri?.let { uploadImage(ownerId, token, it) }
            val payload = JSONObject().put("owner_id", ownerId).put("title", title.trim())
                .put("description", description.trim()).put("category", category)
                .put("listing_type", listingType).put("item_condition", condition)
                .put("location_text", location.trim()).put("image_url", imageUrl ?: JSONObject.NULL)
                .put("status", "active")
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
        optString("location_text"), optString("image_url").takeIf { it.isNotBlank() && it != "null" }, optString("created_at"))
    private fun encode(value: String) = java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
