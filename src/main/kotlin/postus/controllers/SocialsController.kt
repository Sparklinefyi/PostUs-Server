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
import postus.models.SchedulePostRequest
import postus.models.YoutubeUploadRequest
import postus.repositories.UserRepository


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
    private val youtubeConfig = ConfigFactory.load().getConfig("google")
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
            return "Container Response Body: ${containerResponse.body?.string()}"
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
            return publishResponse.body?.toString() ?: "Publish Request Failed"
        }

        return "Publish Response Body: ${publishResponse.body?.string()}"
    }

    fun uploadPictureToInstagram(userId: String, imageUrl: String, caption: String? = "") : String {
        val user = userRepository.findById(userId.toInt())
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
            return "Container Response Body: ${containerResponse.body?.string()}"
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
        if (!response.isSuccessful) return null

        val responseBody = response.body?.string() ?: return null
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

    fun getLongLivedAccessTokenAndInstagramBusinessAccountId(code: String): Pair<String?, String?> {
        val clientId = instagramConfig.getString("clientID")
        val clientSecret = instagramConfig.getString("clientSecret")
        val redirectUri = instagramConfig.getString("redirectUri")
        val shortLivedToken = exchangeCodeForAccessToken(clientId, clientSecret, redirectUri, code) ?: return null to null
        val longLivedToken = exchangeShortLivedTokenForLongLivedToken(clientId, clientSecret, shortLivedToken) ?: return null to null
        val pages = getUserPages(longLivedToken) ?: return null to null

        val jsonElement = Json.parseToJsonElement(pages)
        val pageId = jsonElement.jsonObject["data"]?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content ?: return longLivedToken to null

        val instagramBusinessAccountId = getInstagramBusinessAccountId(pageId, longLivedToken)
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

    fun uploadYoutubeShort(uploadRequest: YoutubeUploadRequest, userId: String, videoUrl: String): String {
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
    fun getAuthenticatedUserChannelId(userId: String): String? {
        val user = userRepository.findById(userId.toInt())
        val accessToken = user?.googleAccessToken ?: throw Exception("User not found")
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

    fun getYouTubeChannelAnalytics(userId: String, channelId: String): String? {
        val apiKey = youtubeConfig.getString("apiKey")
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

    fun schedulePost(userId: String, postTime: String, mediaUrl: ByteArray, schedulePostRequest: SchedulePostRequest): Boolean {
        val mediaType = schedulePostRequest.mediaType
        val s3Path: String
        when (mediaType) {
            "IMAGE" -> s3Path = MediaController.uploadImage(userId, mediaUrl)
            "VIDEO" -> s3Path = MediaController.uploadVideo(userId, mediaUrl)
            else -> {
                throw Exception("Not a supported media type (VIDEO or IMAGE)")
            }
        }
        val scheduled = ScheduleRepository.addSchedule(userId, s3Path, postTime, mediaType, schedulePostRequest)
        return scheduled
    }
}