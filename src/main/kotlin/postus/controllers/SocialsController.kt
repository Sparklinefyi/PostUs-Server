package postus.controllers

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
import postus.models.YoutubeUploadRequest

class SocialsController{

    private val client = OkHttpClient()


    fun uploadVideoToInstagram(videoUrl: String, caption: String? = "", pageAccessToken: String, instagramAccountId: String) : String {
        val containerUrl = "https://graph.facebook.com/v11.0/$instagramAccountId/media".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("media_type", "REELS")
            .addQueryParameter("video_url", videoUrl)
            .addQueryParameter("caption", caption)
            .addQueryParameter("access_token", pageAccessToken)
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
                .addQueryParameter("access_token", pageAccessToken)
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
            .addQueryParameter("access_token", pageAccessToken)
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

    fun uploadPictureToInstagram(imageUrl: String, caption: String? = "", pageAccessToken: String, instagramAccountId: String) : String {
        val containerUrl = "https://graph.facebook.com/v11.0/$instagramAccountId/media".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("image_url", imageUrl)
            .addQueryParameter("caption", caption)
            .addQueryParameter("access_token", pageAccessToken)
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
            .addQueryParameter("access_token", pageAccessToken)
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

    fun uploadYoutubeShort(uploadRequest: YoutubeUploadRequest, accessToken: String, videoUrl: String): String {
        val videoFile = MediaController.downloadVideo(videoUrl)

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

    fun getLongLivedAccessTokenAndInstagramBusinessAccountId(clientId: String, clientSecret: String, redirectUri: String, code: String): Pair<String?, String?> {
        val shortLivedToken = exchangeCodeForAccessToken(clientId, clientSecret, redirectUri, code) ?: return null to null
        val longLivedToken = exchangeShortLivedTokenForLongLivedToken(clientId, clientSecret, shortLivedToken) ?: return null to null
        val pages = getUserPages(longLivedToken) ?: return null to null

        val jsonElement = Json.parseToJsonElement(pages)
        val pageId = jsonElement.jsonObject["data"]?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content ?: return longLivedToken to null

        val instagramBusinessAccountId = getInstagramBusinessAccountId(pageId, longLivedToken)
        return longLivedToken to instagramBusinessAccountId
    }

    fun getYouTubeChannelAnalytics(apiKey: String, channelId: String): String? {
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

    fun getYouTubeVideoAnalytics(apiKey: String, videoId: String): String? {
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

    fun getInstagramPageAnalytics(accessToken: String, instagramBusinessAccountId: String): String? {
        //periods allowed [day, week, days_28, month, lifetime]
        //metrics allowed [impressions, reach, profile_views, follower_count, website_clicks, email_contacts, get_directions_clicks]
        val url = "https://graph.facebook.com/v11.0/$instagramBusinessAccountId/insights".toHttpUrlOrNull()!!.newBuilder()
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
        getInstagramMediaIds(accessToken, instagramBusinessAccountId)

        return responseBody
    }

    fun getInstagramPostAnalytics(accessToken: String, postId: String): String? {
        // metrics allowed for media [impressions, reach, engagement, saved, video_views, comments, likes]
        //metrics allowed for shorts [plays, comments, likes, saves, shares, total_interactions, reach]
        val url = "https://graph.facebook.com/v11.0/$postId/insights".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("metric", "impressions,reach,engagement")
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

    fun getInstagramMediaIds(accessToken: String, instagramBusinessAccountId: String): String? {
        val url = "https://graph.facebook.com/v11.0/$instagramBusinessAccountId/media".toHttpUrlOrNull()!!.newBuilder()
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
        return null
    }

    fun verifyAccessTokenPermissions(accessToken: String, appId: String, appSecret: String): List<String> {
        val debugTokenUrl = "https://graph.facebook.com/debug_token".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("input_token", accessToken)
            .addQueryParameter("access_token", "$appId|$appSecret")
            .build()
            .toString()

        val request = Request.Builder()
            .url(debugTokenUrl)
            .get()
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string()

        if (!response.isSuccessful) {
            println("Error: ${response.code}")
            println("Response Body: $responseBody")
            return listOf("")
        }

        val jsonElement = Json.parseToJsonElement(responseBody ?: "").jsonObject
        val data = jsonElement["data"]?.jsonObject
        val scopes = data?.get("scopes")?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()

        return scopes
    }
}