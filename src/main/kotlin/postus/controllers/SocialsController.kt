package postus.controllers

import com.typesafe.config.ConfigFactory
import kotlinx.serialization.*
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
import postus.endpoints.MediaController
import postus.models.*
import postus.repositories.User
import postus.repositories.UserRepository
import postus.workers.PostWorker
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset


@Serializable
data class PlaylistItemsResponse(
    val kind: String,
    val etag: String,
    val items: List<PlaylistItem>,
    val pageInfo: PageInfo
)

@Serializable
data class PlaylistItem(
    val kind: String,
    val etag: String,
    val id: String,
    val snippet: Snippet
)

@Serializable
data class Snippet(
    val publishedAt: String,
    val channelId: String,
    val title: String,
    val description: String,
    val thumbnails: Thumbnails,
    val channelTitle: String,
    val playlistId: String,
    val position: Int,
    val resourceId: ResourceId,
    val videoOwnerChannelTitle: String,
    val videoOwnerChannelId: String
)

@Serializable
data class Thumbnails(
    val default: Thumbnail,
    val medium: Thumbnail,
    val high: Thumbnail,
    val standard: Thumbnail? = null,
    val maxres: Thumbnail? = null
)

@Serializable
data class Thumbnail(
    val url: String,
    val width: Int,
    val height: Int
)

@Serializable
data class ResourceId(
    val kind: String,
    val videoId: String
)

@Serializable
data class PageInfo(
    val totalResults: Int,
    val resultsPerPage: Int
)

class SocialsController{

    private val client = OkHttpClient()
    private val userRepository = UserRepository()
    private val userController = UserController(userRepository)
    private val youtubeConfig = ConfigFactory.load().getConfig("google")
    private val twitterConfig = ConfigFactory.load().getConfig("twitter")
    private val instagramConfig = ConfigFactory.load().getConfig("instagram")


    fun uploadVideoToInstagram(userId: String, videoUrl: String, caption: String? = "") : String {
        val user = userRepository.findById(userId.toInt())
        val accessToken = user?.instagramAccessToken ?: throw Exception("User not found")
        val instagramAccountId = user.instagramAccountId ?: throw Exception("Instagram access token or accountId not found")

        val containerUrl = "https://graph.facebook.com/v11.0/$instagramAccountId/media".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("media_type", "REELS")
            .addQueryParameter("video_url", videoUrl)
            .addQueryParameter("caption", caption)
            .addQueryParameter("access_token", accessToken)
            .build()
            .toString()

        val containerRequest = Request.Builder()
            .url(containerUrl)
            .post(RequestBody.create(null, ByteArray(0)))
            .build()

        val containerResponse = client.newCall(containerRequest).execute()
        if (!containerResponse.isSuccessful) {
            println("Container Response Body: ${containerResponse.body?.string()}")
        }

        val responseBody = containerResponse.body?.string()

        val containerId = Json.parseToJsonElement(responseBody ?: "").jsonObject["id"]?.jsonPrimitive?.content

        if (containerId == null) {
            return "Failed to get container ID"
        }

        val maxRetries = 10
        val retryDelayMillis = 3000L
        var mediaReady = false
        var retries = 0

        while (!mediaReady && retries < maxRetries) {
            Thread.sleep(retryDelayMillis)
            val statusUrl = "https://graph.facebook.com/v11.0/$containerId".toHttpUrlOrNull()!!.newBuilder()
                .addQueryParameter("fields", "status_code")
                .addQueryParameter("access_token", accessToken)
                .build()
                .toString()

            val statusRequest = Request.Builder()
                .url(statusUrl)
                .get()
                .build()

            val statusResponse = client.newCall(statusRequest).execute()

            val statusBody = statusResponse.body?.string()

            val statusCode = Json.parseToJsonElement(statusBody ?: "").jsonObject["status_code"]?.jsonPrimitive?.content

            if (statusCode == "FINISHED") {
                mediaReady = true
            } else {
                retries++
            }
        }

        val publishUrl = "https://graph.facebook.com/v11.0/$instagramAccountId/media_publish".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("creation_id", containerId)
            .addQueryParameter("access_token", accessToken)
            .build()
            .toString()

        val publishRequest = Request.Builder()
            .url(publishUrl)
            .post(RequestBody.create(null, ByteArray(0)))
            .build()

        val publishResponse = try {
            client.newCall(publishRequest).execute()
        } catch (e: Exception) {
            return "Publish Request Exception: ${e.message}"
        }
        if (!publishResponse.isSuccessful) {
            println("INSTAGRAM FAILURE ${publishResponse.body?.string()}" ?: "Publish Request Failed")
        }

        return "Publish Response Body: ${publishResponse.body?.string()}"
    }

    fun uploadPictureToInstagram(userId: String, imageUrl: String, caption: String? = "") : String {
        val user = userRepository.findById(userId.toInt())
        refreshInstagramAccessToken(userId.toInt())
        val accessToken = user?.instagramAccessToken ?: throw Exception("User not found")
        val instagramAccountId = user.instagramAccountId ?: throw Exception("Instagram access token or accountId not found")

        val containerUrl = "https://graph.facebook.com/v11.0/$instagramAccountId/media".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("image_url", imageUrl)
            .addQueryParameter("caption", caption)
            .addQueryParameter("access_token", accessToken)
            .build()
            .toString()

        val containerRequest = Request.Builder()
            .url(containerUrl)
            .post(RequestBody.create(null, ByteArray(0)))
            .build()

        val containerResponse = client.newCall(containerRequest).execute()
        if (!containerResponse.isSuccessful) {
            println("${containerResponse.body?.string()}")
        }

        val responseBody = containerResponse.body?.string()

        val containerId = Json.parseToJsonElement(responseBody ?: "").jsonObject["id"]?.jsonPrimitive?.content

        if (containerId == null) {
            return "Failed to get container ID"
        }

        val publishUrl = "https://graph.facebook.com/v11.0/$instagramAccountId/media_publish".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("creation_id", containerId)
            .addQueryParameter("access_token", accessToken)
            .build()
            .toString()

        val publishRequest = Request.Builder()
            .url(publishUrl)
            .post(RequestBody.create(null, ByteArray(0)))
            .build()

        val publishResponse = client.newCall(publishRequest).execute()
        if (!publishResponse.isSuccessful) {
            return "Publish Response Body: ${publishResponse.body?.string()}"
        }

        return "Publish Response Body: ${publishResponse.body?.string()}"
    }

    fun exchangeCodeForAccessToken(clientId: String, clientSecret: String, redirectUri: String, code: String): String? {
        val url = "https://graph.facebook.com/v11.0/oauth/access_token".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("client_id", clientId)
            .addQueryParameter("redirect_uri", redirectUri)
            .addQueryParameter("client_secret", clientSecret)
            .addQueryParameter("code", code)
            .build()
            .toString()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return null
        if (!response.isSuccessful) return null

        val jsonElement = Json.parseToJsonElement(responseBody)
        return jsonElement.jsonObject["access_token"]?.jsonPrimitive?.content
    }

    fun exchangeShortLivedTokenForLongLivedToken(clientId: String, clientSecret: String, shortLivedToken: String): String? {
        val url = "https://graph.facebook.com/v11.0/oauth/access_token".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("grant_type", "fb_exchange_token")
            .addQueryParameter("client_id", clientId)
            .addQueryParameter("client_secret", clientSecret)
            .addQueryParameter("fb_exchange_token", shortLivedToken)
            .build()
            .toString()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseBody = response.body?.string() ?: return null
        val jsonElement = Json.parseToJsonElement(responseBody)
        return jsonElement.jsonObject["access_token"]?.jsonPrimitive?.content
    }

    fun refreshInstagramAccessToken(userId: Int) {
        val clientId = instagramConfig.getString("clientID")
        val clientSecret = instagramConfig.getString("clientSecret")
        val refreshToken = userRepository.findById(userId)?.instagramRefresh ?: throw Exception("Useror refresh token not found")
        val url = "https://graph.instagram.com/refresh_access_token".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("grant_type", "ig_refresh_token")
            .addQueryParameter("client_id", clientId)
            .addQueryParameter("client_secret", clientSecret)
            .addQueryParameter("refresh_token", refreshToken)
            .build()
            .toString()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return
            if (!response.isSuccessful) return

            val jsonElement = Json.parseToJsonElement(responseBody)
            val newAccessToken = jsonElement.jsonObject["access_token"]?.jsonPrimitive?.content

            if (newAccessToken != null) {
                userController.linkAccount(userId, "INSTAGRAM", null, newAccessToken, newAccessToken)
            }

            newAccessToken
        } catch (e: Exception) {
            println("Error refreshing Instagram access token: ${e.message}")
            e.printStackTrace()
            null
        }
    }

    fun getUserPages(accessToken: String): String? {
        val url = "https://graph.facebook.com/v11.0/me/accounts".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("access_token", accessToken)
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

    fun getInstagramBusinessAccountId(pageId: String, accessToken: String): String? {
        val url = "https://graph.facebook.com/v11.0/$pageId".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("fields", "instagram_business_account")
            .addQueryParameter("access_token", accessToken)
            .build()
            .toString()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) return null

        val responseBody = response.body?.string() ?: return null
        val jsonElement = Json.parseToJsonElement(responseBody)
        return jsonElement.jsonObject["instagram_business_account"]?.jsonObject?.get("id")?.jsonPrimitive?.content
    }

    fun getLongLivedAccessTokenAndInstagramBusinessAccountId(userId: String, code: String): Pair<String?, String?> {
        val clientId = instagramConfig.getString("clientID")
        val clientSecret = instagramConfig.getString("clientSecret")
        val redirectUri = instagramConfig.getString("redirectUri")
        val shortLivedToken = exchangeCodeForAccessToken(clientId, clientSecret, redirectUri, code) ?: return null to null
        val longLivedToken = exchangeShortLivedTokenForLongLivedToken(clientId, clientSecret, shortLivedToken) ?: return null to null
        val pages = getUserPages(longLivedToken) ?: return null to null

        val jsonElement = Json.parseToJsonElement(pages)
        val pageId = jsonElement.jsonObject["data"]?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content ?: return longLivedToken to null

        val instagramBusinessAccountId = getInstagramBusinessAccountId(pageId, longLivedToken)
        userController.linkAccount(userId.toInt(), "INSTAGRAM", instagramBusinessAccountId, longLivedToken, longLivedToken )
        return longLivedToken to instagramBusinessAccountId
    }

    fun getInstagramPageAnalytics(userId: String): String? {
        //periods allowed [day, week, days_28, month, lifetime]
        //metrics allowed [impressions, reach, profile_views, follower_count, website_clicks, email_contacts, get_directions_clicks]
        val user = userRepository.findById(userId.toInt())
        val accessToken = user?.instagramAccessToken ?: throw Exception("User not found")
        val instagramAccountId = user.instagramAccountId ?: throw Exception("Instagram access token or accountId not found")

        val url = "https://graph.facebook.com/v11.0/$instagramAccountId/insights".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("metric", "impressions,reach,profile_views,follower_count")
            .addQueryParameter("period", "day")
            .addQueryParameter("access_token", accessToken)
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
        getInstagramMediaIds(userId)

        return responseBody
    }

    fun getInstagramPostAnalytics(userId: String, postId: String): String? {
        // metrics allowed for media [impressions, reach, engagement, saved, video_views, comments, likes]
        //metrics allowed for shorts [plays, comments, likes, saves, shares, total_interactions, reach]
        //
        val user = userRepository.findById(userId.toInt())
        val accessToken = user?.instagramAccessToken ?: throw Exception("User not found")
        val url = "https://graph.facebook.com/v11.0/$postId/insights".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("metric", "plays, comments, likes, reach")
            .addQueryParameter("period", "lifetime")
            .addQueryParameter("access_token", accessToken)
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

    fun getInstagramMediaIds(userId: String): String? {
        val user = userRepository.findById(userId.toInt())
        val accessToken = user?.instagramAccessToken ?: throw Exception("User not found")
        val instagramAccountId = user.instagramAccountId ?: throw Exception("Instagram access token or accountId not found")
        val url = "https://graph.facebook.com/v11.0/$instagramAccountId/media".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("fields", "id,media_type")
            .addQueryParameter("access_token", accessToken)
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
        println(responseBody)
        //VIDEO TYPE is REELS, IMAGE and CAROUSEL_ALBUM are other 2 types
        return null
    }

    fun getInstagramMediaDetails(userId: String, postId: String): String {
        val client = OkHttpClient()
        val user = userRepository.findById(userId.toInt())
        val accessToken = user?.instagramAccessToken ?: throw Exception("User not found")

        val url = "https://graph.facebook.com/v11.0/$postId".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("fields", "id,media_type,media_url,thumbnail_url")
            .addQueryParameter("access_token", accessToken)
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
            return ""
        }

        println("Response Body: $responseBody")
        val jsonResponse = Json.parseToJsonElement(responseBody ?: "").jsonObject
        return jsonResponse["media_url"]?.jsonPrimitive?.content ?: ""
    }

    fun fetchYouTubeAccessToken(userId: String, code: String): Boolean {
        val clientId = youtubeConfig.getString("clientID")
        val clientSecret = youtubeConfig.getString("clientSecret")
        val redirectUri = youtubeConfig.getString("redirectUri")
        val requestBody = FormBody.Builder()
            .add("code", code)
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("redirect_uri", redirectUri)
            .add("grant_type", "authorization_code")
            .build()

        val request = Request.Builder()
            .url("https://oauth2.googleapis.com/token")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return false
        if (!response.isSuccessful) return false

        val youtubeTokens = Json{ ignoreUnknownKeys = true }.decodeFromString<YoutubeOAuthResponse>(responseBody)
        val channelId = getAuthenticatedUserChannelId(youtubeTokens.access_token)
        userController.linkAccount(userId.toInt(), "GOOGLE", channelId, youtubeTokens.access_token, youtubeTokens.refresh_token)
        return true
    }

    fun refreshYouTubeAccessToken(userId: String): String? {
        val clientId = youtubeConfig.getString("clientID")
        val clientSecret = youtubeConfig.getString("clientSecret")
        val user = userRepository.findById(userId.toInt())
        val refreshToken = user?.googleRefresh ?: throw Exception("User not found")

        val requestBody = FormBody.Builder()
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .add("refresh_token", refreshToken)
            .add("grant_type", "refresh_token")
            .build()

        val request = Request.Builder()
            .url("https://oauth2.googleapis.com/token")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return null

        if (!response.isSuccessful) {
            println("Error: ${responseBody}")
            return null
        }

        val oauthResponse = Json { ignoreUnknownKeys = true }.decodeFromString<YoutubeRefreshResponse>(responseBody)
        userController.linkAccount(userId.toInt(), "GOOGLE", null, oauthResponse.access_token, null)

        return oauthResponse.access_token
    }

    fun uploadYoutubeShort(uploadRequest: YoutubeUploadRequest, userId: String, videoUrl: String): String {
        refreshYouTubeAccessToken(userId)
        val videoFile = MediaController.downloadVideo(videoUrl)
        val user = userRepository.findById(userId.toInt())
        val accessToken = user?.googleAccessToken ?: throw Exception("User not found")

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

        val responseBody = response.body?.string()
        return responseBody ?: ""
    }

    fun testYoutube(userId: String): Unit {
        val user = userRepository.findById(userId.toInt())
        val accessToken = user?.googleAccessToken ?: throw Exception("User not found")
        val channelId = getAuthenticatedUserChannelId(accessToken)!!
        val videoId = getLast10YouTubeVideos(channelId)?.get(0)!!
        println(getYouTubeVideoAnalytics(videoId))
    }

    fun getYouTubeUploadsPlaylistId(userId: String): String? {
        val apiKey = youtubeConfig.getString("apiKey")
        val user = userRepository.findById(userId.toInt())
        val channelId = user?.googleAccountId ?: throw Exception("Youtube ChannelId not found")
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
        val uploadsPlaylistId = jsonElement.jsonObject["items"]?.jsonArray?.firstOrNull()
            ?.jsonObject?.get("contentDetails")?.jsonObject
            ?.get("relatedPlaylists")?.jsonObject
            ?.get("uploads")?.jsonPrimitive?.content

        return uploadsPlaylistId
    }

    fun getLast10YouTubeVideos(userId: String): List<String>? {
        val user = userRepository.findById(userId.toInt())
        val channelId = user?.googleAccountId ?: throw Exception("Youtube ChannelId not found")
        val apiKey = youtubeConfig.getString("apiKey")
        val uploadsPlaylistId = getYouTubeUploadsPlaylistId(channelId) ?: return null

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
        val videoIds = playlistItemsResponse.items.map { it.snippet.resourceId.videoId }

        return videoIds
    }

    //Testing function
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
        val channelId = jsonElement.jsonObject["items"]?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content

        return channelId
    }

    fun getYouTubeChannelAnalytics(userId: String): String? {
        val apiKey = youtubeConfig.getString("apiKey")
        val user = userRepository.findById(userId.toInt())
        val channelId = user?.googleAccountId ?: throw Exception("Youtube ChannelId not found")
        val url = "https://www.googleapis.com/youtube/v3/channels".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("part", "statistics")
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

    fun getYouTubeVideoAnalytics(videoId: String): String? {
        val apiKey = youtubeConfig.getString("apiKey")
        val url = "https://www.googleapis.com/youtube/v3/videos".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("part", "statistics")
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

        return response.body?.string()
    }

    fun fetchTwitterAccessToken(userId: String, code: String): TwitterOAuthResponse? {
        val clientId = twitterConfig.getString("clientID")
        val clientSecret = twitterConfig.getString("clientSecret")
        val redirectUri = twitterConfig.getString("redirectUri")
        val requestBody = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", clientId)
            .add("redirect_uri", redirectUri)
            .add("code", code)
            .add("code_verifier", "YOUR_CODE_VERIFIER") // Use the actual code verifier here
            .build()

        val request = Request.Builder()
            .url("https://api.twitter.com/2/oauth2/token")
            .post(requestBody)
            .header("Authorization", Credentials.basic(clientId, clientSecret))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return null
        if (!response.isSuccessful) {
            println("Error: $responseBody")
            return null
        }

        val twitterOAuthResponse = Json { ignoreUnknownKeys = true }.decodeFromString<TwitterOAuthResponse>(responseBody)
        userController.linkAccount(userId.toInt(), "TWITTER", null, twitterOAuthResponse.access_token, twitterOAuthResponse.refresh_token)
    }

    fun schedulePost(userId: String, postTime: String, mediaUrl: ByteArray, schedulePostRequest: SchedulePostRequest): Boolean {
        val mediaType = schedulePostRequest.mediaType
        val s3Key: String
        when (mediaType) {
            "IMAGE" -> {
                val path = MediaController.uploadImage(userId, mediaUrl)
                s3Key= path.substring(path.indexOf("/", 10)+1)
            }
            "VIDEO" -> {
                val path = MediaController.uploadVideo(userId, mediaUrl)
                s3Key= path.substring(path.indexOf("/",10)+1)
            }
            else -> {
                throw Exception("Not a supported media type (VIDEO or IMAGE)")
            }
        }
        val postTimeInstant: Instant = LocalDateTime.parse(postTime).toInstant(ZoneOffset.UTC)
        val delay = Duration.between(LocalDateTime.now().toInstant(ZoneOffset.UTC), postTimeInstant).toHours()
        if (delay < 3){
            val post = ScheduledPost(
                0,
                userId.toInt(),
                s3Key,
                postTime,
                mediaType,
                schedulePostRequest,
                false
            )
            PostWorker(post).schedule()
            return true
        }
        val scheduled = ScheduleRepository.addSchedule(userId.toInt(), s3Key, postTime, mediaType, schedulePostRequest)
        return scheduled
    }
}