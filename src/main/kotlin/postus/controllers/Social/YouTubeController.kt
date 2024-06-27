package postus.controllers.Social

import com.google.gson.Gson
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import postus.controllers.MediaController
import postus.controllers.UserController
import postus.models.youtube.*
import postus.repositories.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class YouTubeController(
    private val client: OkHttpClient,
    private val userRepository: UserRepository,
    private val userController: UserController,
    private val mediaController: MediaController
) {

    /**
     * Fetch YouTube access token.
     * Sample Call:
     * `fetchYouTubeAccessToken(1, "authCode")`
     */
    suspend fun fetchYouTubeAccessToken(userId: Int, code: String): Boolean = withContext(Dispatchers.IO) {
        val clientId = System.getProperty("GOOGLE_CLIENT_ID")
        val clientSecret = System.getProperty("GOOGLE_CLIENT_SECRET")
        val redirectUri = System.getProperty("GOOGLE_REDIRECT_URI")
        val requestBody = FormBody.Builder()
            .add("code", code)
            .add("client_id", clientId!!)
            .add("client_secret", clientSecret!!)
            .add("redirect_uri", redirectUri!!)
            .add("grant_type", "authorization_code")
            .build()

        val request = Request.Builder()
            .url(System.getProperty("GOOGLE_TOKEN_URL"))
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext false
        if (!response.isSuccessful) return@withContext false

        val youtubeTokens = Json { ignoreUnknownKeys = true }.decodeFromString<YoutubeOAuthResponse>(responseBody)
        val channelId = getAuthenticatedUserChannelId(youtubeTokens.access_token)
        userController.linkAccount(userId, "GOOGLE", channelId, youtubeTokens.access_token, youtubeTokens.refresh_token)
        true
    }

    /**
     * Refresh YouTube access token.
     * Sample Call:
     * `refreshYouTubeAccessToken("1")`
     */
    suspend fun refreshYouTubeAccessToken(userId: Int): String? = withContext(Dispatchers.IO) {
        val clientId = System.getProperty("GOOGLE_CLIENT_ID")
        val clientSecret = System.getProperty("GOOGLE_CLIENT_SECRET")
        val user = userRepository.findById(userId) ?: return@withContext null

        val refreshToken = user.accounts.find { it.provider == "GOOGLE" }?.refreshToken

        val requestBody = FormBody.Builder()
            .add("client_id", clientId!!)
            .add("client_secret", clientSecret!!)
            .add("refresh_token", refreshToken!!)
            .add("grant_type", "refresh_token")
            .build()

        val request = Request.Builder()
            .url(System.getProperty("GOOGLE_TOKEN_URL"))
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext null

        if (!response.isSuccessful) {
            println("Error: $responseBody")
            return@withContext null
        }

        val oauthResponse = Json { ignoreUnknownKeys = true }.decodeFromString<YoutubeRefreshResponse>(responseBody)
        userController.linkAccount(userId, "GOOGLE", null, oauthResponse.access_token, null)

        oauthResponse.access_token
    }

    /**
     * Upload YouTube Short.
     * Sample Call:
     * `uploadYoutubeShort(uploadRequest, "1", "videoUrl")`
     */
    suspend fun uploadYoutubeShort(uploadRequest: YoutubePostRequest, userId: Int, videoUrl: String): JSONObject? = withContext(Dispatchers.IO) {
        refreshYouTubeAccessToken(userId)
        val signedUrl = mediaController.getPresignedUrlFromPath(videoUrl)
        val videoFile = mediaController.downloadVideo(signedUrl)
        val user = userRepository.findById(userId) ?: throw Exception("User not found")
        val accessToken = user.accounts.find { it.provider == "GOOGLE" }?.accessToken

        val json = Json { encodeDefaults = true }
        val metadataJson = json.encodeToString(uploadRequest)

        val mediaType = "video/*".toMediaTypeOrNull()

        val url = "https://www.googleapis.com/upload/youtube/v3/videos?uploadType=multipart&part=snippet,status"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("metadata", null, metadataJson.toRequestBody("application/json".toMediaTypeOrNull()))
            .addFormDataPart("video", videoFile.name, videoFile.asRequestBody(mediaType))
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body ?: throw Exception("No response body")

        responseBody.use { body ->
            val jsonResponse = body.string()
            return@withContext JSONObject(jsonResponse)
        }
    }

    /**
     * Test YouTube functionality.
     * Sample Call:
     * `testYoutube("1")`
     */
    suspend fun testYoutube(userId: String) = withContext(Dispatchers.IO) {
        val user = userRepository.findById(userId.toInt()) ?: throw Exception("User not found")
        val accessToken = user.accounts.find { it.provider == "GOOGLE" }?.accessToken
        val channelId = getAuthenticatedUserChannelId(accessToken!!) ?: throw Exception("Error getting channel ID")
        val videoId = getLast50YoutubeVideos(channelId)?.first() ?: throw Exception("No videos found")
        println(getYouTubeVideoAnalytics(videoId))
    }

    /**
     * Retrieve YouTube uploads playlist ID.
     * Sample Call:
     * `getYouTubeUploadsPlaylistId("1")`
     */
    suspend fun getYouTubeUploadsPlaylistId(userId: String): String? = withContext(Dispatchers.IO) {
        val apiKey = System.getProperty("GOOGLE_API_KEY")
        val user = userRepository.findById(userId.toInt()) ?: throw Exception("YouTube Channel ID not found")
        val channelId = user.accounts.find { it.provider == "GOOGLE" }?.accountId

        val url = "https://www.googleapis.com/youtube/v3/channels".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("part", "contentDetails")
            .addQueryParameter("id", channelId)
            .addQueryParameter("key", apiKey)
            .build()
            .toString()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful) {
            println("Error: ${response.code}")
            println("Response Body: $responseBody")
            return@withContext null
        }

        val jsonElement = Json.parseToJsonElement(responseBody ?: "")
        return@withContext jsonElement.jsonObject["items"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("contentDetails")?.jsonObject
            ?.get("relatedPlaylists")?.jsonObject
            ?.get("uploads")?.jsonPrimitive?.content
    }

    /**
     * Retrieve last 50 YouTube videos.
     * Sample Call:
     * `getLast50YouTubeVideos("1")`
     */
    suspend fun getLast50YoutubeVideos(userId: String): List<String>? = withContext(Dispatchers.IO) {
        val apiKey = System.getProperty("GOOGLE_API_KEY")
        val uploadsPlaylistId = getYouTubeUploadsPlaylistId(userId) ?: return@withContext null

        val url = "https://www.googleapis.com/youtube/v3/playlistItems".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("part", "snippet")
            .addQueryParameter("playlistId", uploadsPlaylistId)
            .addQueryParameter("maxResults", "50")
            .addQueryParameter("key", apiKey)
            .build()
            .toString()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful) {
            println("Error: ${response.code}")
            println("Response Body: $responseBody")
            return@withContext null
        }

        val playlistItemsResponse = Json.decodeFromString<PlaylistItemsResponse>(responseBody ?: "")
        return@withContext playlistItemsResponse.items?.map { it.snippet!!.resourceId!!.videoId!! }
    }

    /**
     * Retrieve authenticated YouTube user's channel ID.
     * Sample Call:
     * `getAuthenticatedUserChannelId("accessToken")`
     */
    suspend fun getAuthenticatedUserChannelId(accessToken: String): String? = withContext(Dispatchers.IO) {
        val url = "https://www.googleapis.com/youtube/v3/channels".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("part", "id")
            .addQueryParameter("mine", "true")
            .build()
            .toString()

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful) {
            println("Error: ${response.code}")
            println("Response Body: $responseBody")
            return@withContext null
        }

        val jsonElement = Json.parseToJsonElement(responseBody ?: "")
        return@withContext jsonElement.jsonObject["items"]?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content
    }

    suspend fun getYouTubeChannelAnalytics(userId: String): String? = withContext(Dispatchers.IO) {
        val apiKey = System.getProperty("GOOGLE_API_KEY")
        val user = userRepository.findById(userId.toInt())
            ?: throw Exception("YouTube Channel ID not found")
        val channelId = user.accounts.find { it.provider == "GOOGLE" }?.accountId

        val url = "https://www.googleapis.com/youtube/v3/channels".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("part", "snippet,statistics")
            .addQueryParameter("id", channelId)
            .addQueryParameter("key", apiKey)
            .build()
            .toString()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return@withContext null

        return@withContext response.body?.string()
    }

    suspend fun getYouTubeVideoAnalytics(videoId: String): VideoItemResponse? = withContext(Dispatchers.IO) {
        val apiKey = System.getProperty("GOOGLE_API_KEY")
        val url = "https://www.googleapis.com/youtube/v3/videos".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("part", "snippet,statistics,contentDetails")
            .addQueryParameter("id", videoId)
            .addQueryParameter("key", apiKey)
            .build()
            .toString()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return@withContext null

        val body = response.body?.string() ?: return@withContext null

        val gson = Gson()
        return@withContext gson.fromJson(body, VideoItemResponse::class.java)
    }

    suspend fun getYouTubeVideoDetails(videoIds: List<String>): Array<VideoItemResponse?> = withContext(Dispatchers.IO) {
        videoIds.map { videoId ->
            getYouTubeVideoAnalytics(videoId)
        }.toTypedArray()
    }
}
