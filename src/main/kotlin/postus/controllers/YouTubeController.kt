package postus.controllers


import com.google.gson.Gson
import com.google.gson.JsonObject
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
import postus.models.youtube.*
import postus.repositories.UserRepository


class YouTubeController(
    client: OkHttpClient,
    userRepository: UserRepository,
    userController: UserController,
    mediaController: MediaController
) {

    val client = client
    val userRepository = userRepository
    val userController = userController
    val mediaController = mediaController

    /**
     * Fetch YouTube access token.
     * Sample Call:
     * `fetchYouTubeAccessToken(1, "authCode")`
     */
    fun fetchYouTubeAccessToken(userId: Int, code: String): Boolean {
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
        val responseBody = response.body?.string() ?: return false
        if (!response.isSuccessful) return false

        val youtubeTokens = Json { ignoreUnknownKeys = true }.decodeFromString<YoutubeOAuthResponse>(responseBody)
        val channelId = getAuthenticatedUserChannelId(youtubeTokens.access_token)
        userController.linkAccount(userId, "GOOGLE", channelId, youtubeTokens.access_token, youtubeTokens.refresh_token)
        return true
    }

    /**
     * Refresh YouTube access token.
     * Sample Call:
     * `refreshYouTubeAccessToken("1")`
     */
    fun refreshYouTubeAccessToken(userId: Int): String? {
        val clientId = System.getProperty("GOOGLE_CLIENT_ID")
        val clientSecret = System.getProperty("GOOGLE_CLIENT_SECRET")
        val user = userRepository.findById(userId)
            ?: throw Exception("User not found")
        val refreshToken = user.googleRefresh

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
        val responseBody = response.body?.string() ?: return null

        if (!response.isSuccessful) {
            println("Error: $responseBody")
            return null
        }

        val oauthResponse = Json { ignoreUnknownKeys = true }.decodeFromString<YoutubeRefreshResponse>(responseBody)
        userController.linkAccount(userId.toInt(), "GOOGLE", null, oauthResponse.access_token, null)

        return oauthResponse.access_token
    }

    /**
     * Upload YouTube Short.
     * Sample Call:
     * `uploadYoutubeShort(uploadRequest, "1", "videoUrl")`
     */
    fun uploadYoutubeShort(uploadRequest: YoutubeUploadRequest, userId: Int, videoUrl: String): ResponseBody? {
        refreshYouTubeAccessToken(userId)
        val signedUrl = mediaController.getPresignedUrlFromPath(videoUrl)
        val videoFile = mediaController.downloadVideo(signedUrl)
        val user = userRepository.findById(userId.toInt())
            ?: throw Exception("User not found")
        val accessToken = user.googleAccessToken

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

        return responseBody
    }

    /**
     * Test YouTube functionality.
     * Sample Call:
     * `testYoutube("1")`
     */
    fun testYoutube(userId: String) {
        val user = userRepository.findById(userId.toInt())
            ?: throw Exception("User not found")
        val accessToken = user.googleAccessToken
        val channelId = getAuthenticatedUserChannelId(accessToken!!)
            ?: throw Exception("Error getting channel ID")
        val videoId = getLast10YouTubeVideos(channelId)?.first()
            ?: throw Exception("No videos found")
        println(getYouTubeVideoAnalytics(videoId))
    }

    /**
     * Retrieve YouTube uploads playlist ID.
     * Sample Call:
     * `getYouTubeUploadsPlaylistId("1")`
     */
    fun getYouTubeUploadsPlaylistId(userId: String): String? {
        val apiKey = System.getProperty("GOOGLE_API_KEY")
        val user = userRepository.findById(userId.toInt())
            ?: throw Exception("YouTube Channel ID not found")
        val channelId = user.googleAccountId

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
            return null
        }

        val jsonElement = Json.parseToJsonElement(responseBody ?: "")
        return jsonElement.jsonObject["items"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("contentDetails")?.jsonObject
            ?.get("relatedPlaylists")?.jsonObject
            ?.get("uploads")?.jsonPrimitive?.content
    }

    /**
     * Retrieve last 10 YouTube videos.
     * Sample Call:
     * `getLast10YouTubeVideos("1")`
     */
    fun getLast10YouTubeVideos(userId: String): List<String>? {
        val user = userRepository.findById(userId.toInt())
            ?: throw Exception("YouTube Channel ID not found")
        val channelId = user.googleAccountId
        val apiKey = System.getProperty("GOOGLE_API_KEY")
        val uploadsPlaylistId = getYouTubeUploadsPlaylistId(userId) ?: return null

        val url = "https://www.googleapis.com/youtube/v3/playlistItems".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("part", "snippet")
            .addQueryParameter("playlistId", uploadsPlaylistId)
            .addQueryParameter("maxResults", "10")
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
            return null
        }

        val playlistItemsResponse = Json.decodeFromString<PlaylistItemsResponse>(responseBody ?: "")
        return playlistItemsResponse.items?.map { it.snippet!!.resourceId!!.videoId!! }
    }

    /**
     * Retrieve authenticated YouTube user's channel ID.
     * Sample Call:
     * `getAuthenticatedUserChannelId("accessToken")`
     */
    fun getAuthenticatedUserChannelId(accessToken: String): String? {
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
            return null
        }

        val jsonElement = Json.parseToJsonElement(responseBody ?: "")
        return jsonElement.jsonObject["items"]?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content
    }

    fun getYouTubeChannelAnalytics(userId: String): String? {
        val apiKey = System.getProperty("GOOGLE_API_KEY")
        val user = userRepository.findById(userId.toInt())
            ?: throw Exception("YouTube Channel ID not found")
        val channelId = user.googleAccountId

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
        if (!response.isSuccessful) return null

        return response.body?.string()
    }

    fun getYouTubeVideoAnalytics(videoId: String): VideoItemResponse? {
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
        if (!response.isSuccessful) return null

        val body = response.body?.string() ?: return null

        val gson = Gson()
        return gson.fromJson(body, VideoItemResponse::class.java)
    }

    fun getYouTubeVideoDetails(videoIds: List<String>): Array<VideoItemResponse?> {
        return videoIds.map { videoId ->
            getYouTubeVideoAnalytics(videoId)
        }.toTypedArray()
    }
}
