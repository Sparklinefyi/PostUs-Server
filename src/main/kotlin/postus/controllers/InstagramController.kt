package postus.controllers

import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.jsonArray

import postus.repositories.UserRepository
class InstagramController(client: OkHttpClient, userRepository: UserRepository, userController: UserController, mediaController: MediaController) {

    val client = client
    val userRepository = userRepository
    val userController = userController
    val mediaController = mediaController

    /**
     * Upload a video to Instagram.
     * Sample Call:
     * `uploadVideoToInstagram("1", "https://example.com/video.mp4", "Sample Caption")`
     */
    fun uploadVideoToInstagram(userId: String, videoUrl: String, caption: String? = ""): String {
        val user = userRepository.findById(userId.toInt())
            ?: throw Exception("User not found")
        val accessToken = user.instagramAccessToken
            ?: throw Exception("Instagram access token not found")
        val instagramAccountId = user.instagramAccountId
            ?: throw Exception("Instagram account ID not found")

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
            return "Failed to create media container"
        }

        val containerId = Json.parseToJsonElement(containerResponse.body?.string() ?: "").jsonObject["id"]?.jsonPrimitive?.content
            ?: return "Failed to get container ID"

        // Wait for media to be ready
        var mediaReady = false
        val maxRetries = 10
        val retryDelayMillis = 3000L
        repeat(maxRetries) {
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
            val statusCode = Json.parseToJsonElement(statusResponse.body?.string() ?: "").jsonObject["status_code"]?.jsonPrimitive?.content
            if (statusCode == "FINISHED") {
                mediaReady = true
                return@repeat
            }
        }

        if (!mediaReady) return "Media was not ready in time"

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
        return if (publishResponse.isSuccessful) {
            "Publish Response Body: ${publishResponse.body?.string()}"
        } else {
            "INSTAGRAM FAILURE ${publishResponse.body?.string() ?: "Publish Request Failed"}"
        }
    }

    /**
     * Upload an image to Instagram.
     * Sample Call:
     * `uploadPictureToInstagram("1", "https://example.com/image.jpg", "Sample Caption")`
     */
    fun uploadPictureToInstagram(userId: String, imageUrl: String, caption: String? = ""): String {
        val user = userRepository.findById(userId.toInt())
        refreshInstagramAccessToken(userId.toInt())
        val accessToken = user?.instagramAccessToken ?: throw Exception("User not found")
        val instagramAccountId = user.instagramAccountId ?: throw Exception("Instagram account ID not found")

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

        val containerId = Json.parseToJsonElement(containerResponse.body?.string() ?: "").jsonObject["id"]?.jsonPrimitive?.content
            ?: return "Failed to get container ID"

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
        return if (publishResponse.isSuccessful) {
            "Publish Response Body: ${publishResponse.body?.string()}"
        } else {
            "Publish Response Body: ${publishResponse.body?.string()}"
        }
    }

    /**
     * Exchange authorization code for Instagram access token.
     * Sample Call:
     * `exchangeCodeForAccessToken("clientId", "clientSecret", "http://example.com/redirect", "authCode")`
     */
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

    /**
     * Exchange short-lived Instagram token for a long-lived token.
     * Sample Call:
     * `exchangeShortLivedTokenForLongLivedToken("clientId", "clientSecret", "shortLivedToken")`
     */
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

    /**
     * Refresh Instagram access token.
     * Sample Call:
     * `refreshInstagramAccessToken(1)`
     */
    fun refreshInstagramAccessToken(userId: Int) {
        val clientId = System.getProperty("INSTAGRAM_CLIENT_ID")
        val clientSecret = System.getProperty("INSTAGRAM_CLIENT_SECRET")
        val refreshToken = userRepository.findById(userId)?.instagramRefresh ?: throw Exception("User or refresh token not found")
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
        } catch (e: Exception) {
            println("Error refreshing Instagram access token: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Retrieve user's Facebook pages.
     * Sample Call:
     * `getUserPages("accessToken")`
     */
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

    /**
     * Retrieve Instagram Business Account ID for a given Facebook page.
     * Sample Call:
     * `getInstagramBusinessAccountId("pageId", "accessToken")`
     */
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

    /**
     * Retrieve long-lived Instagram access token and Business Account ID.
     * Sample Call:
     * `getLongLivedAccessTokenAndInstagramBusinessAccountId(1, "authCode")`
     */
    fun getLongLivedAccessTokenAndInstagramBusinessAccountId(userId: Int, code: String): Pair<String?, String?> {
        val clientId = System.getProperty("INSTAGRAM_CLIENT_ID")
        val clientSecret = System.getProperty("INSTAGRAM_CLIENT_SECRET")
        val redirectUri = System.getProperty("INSTAGRAM_REDIRECT_URI")
        val shortLivedToken = exchangeCodeForAccessToken(clientId!!, clientSecret!!, redirectUri!!, code)
            ?: return null to null

        val longLivedToken = exchangeShortLivedTokenForLongLivedToken(clientId, clientSecret, shortLivedToken)
            ?: return null to null
        val pages = getUserPages(longLivedToken) ?: return null to null

        val jsonElement = Json.parseToJsonElement(pages)
        val pageId = jsonElement.jsonObject["data"]?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content
            ?: return longLivedToken to null

        val instagramBusinessAccountId = getInstagramBusinessAccountId(pageId, longLivedToken)

        userController.linkAccount(userId, "INSTAGRAM", instagramBusinessAccountId, longLivedToken, longLivedToken)

        return longLivedToken to instagramBusinessAccountId
    }

    /**
     * Retrieve Instagram page analytics.
     * Sample Call:
     * `getInstagramPageAnalytics("1")`
     */
    fun getInstagramPageAnalytics(userId: String): String? {
        val user = userRepository.findById(userId.toInt())
            ?: throw Exception("User not found")
        val accessToken = user.instagramAccessToken
            ?: throw Exception("Instagram access token not found")
        val instagramAccountId = user.instagramAccountId
            ?: throw Exception("Instagram account ID not found")

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

    /**
     * Retrieve Instagram post analytics.
     * Sample Call:
     * `getInstagramPostAnalytics("1", "postId")`
     */
    fun getInstagramPostAnalytics(userId: String, postId: String): String? {
        val user = userRepository.findById(userId.toInt())
            ?: throw Exception("User not found")
        val accessToken = user.instagramAccessToken
            ?: throw Exception("Instagram access token not found")

        val url = "https://graph.facebook.com/v11.0/$postId/insights".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("metric", "plays,comments,likes,reach")
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

    /**
     * Retrieve Instagram media IDs.
     * Sample Call:
     * `getInstagramMediaIds("1")`
     */
    fun getInstagramMediaIds(userId: String): String? {
        val user = userRepository.findById(userId.toInt())
            ?: throw Exception("User not found")
        val accessToken = user.instagramAccessToken
            ?: throw Exception("Instagram access token not found")
        val instagramAccountId = user.instagramAccountId
            ?: throw Exception("Instagram account ID not found")

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
        // VIDEO TYPE is REELS, IMAGE and CAROUSEL_ALBUM are other 2 types
        return null
    }

    /**
     * Retrieve Instagram media details.
     * Sample Call:
     * `getInstagramMediaDetails("1", "postId")`
     */
    fun getInstagramMediaDetails(userId: String, postId: String): String {
        val user = userRepository.findById(userId.toInt())
            ?: throw Exception("User not found")
        val accessToken = user.instagramAccessToken
            ?: throw Exception("Instagram access token not found")

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
}
