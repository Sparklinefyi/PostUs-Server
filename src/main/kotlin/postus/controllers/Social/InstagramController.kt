package postus.controllers.Social

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.json.JSONObject
import postus.controllers.MediaController
import postus.controllers.UserController
import postus.repositories.UserRepository

class InstagramController(
    private val client: OkHttpClient,
    private val userRepository: UserRepository,
    private val userController: UserController,
    private val mediaController: MediaController
) {

    /**
     * Upload a video to Instagram.
     * Sample Call:
     * `uploadVideoToInstagram("1", "https://example.com/video.mp4", "Sample Caption")`
     */
    suspend fun uploadVideoToInstagram(userId: Int, videoUrl: String, caption: String? = ""): JSONObject? = withContext(Dispatchers.IO) {
        val user = userRepository.findById(userId)
            ?: throw Exception("User not found")
        val accessToken = user.accounts.find { it.type == "INSTAGRAM" }?.accessToken
            ?: throw Exception("Instagram access token not found")
        val accountId = user.accounts.find { it.type == "INSTAGRAM" }?.accountId
            ?: throw Exception("Instagram account ID not found")

        val signedUrl = mediaController.getPresignedUrlFromPath(videoUrl)
        val containerUrl = "https://graph.facebook.com/v11.0/$accountId/media".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("media_type", "REELS")
            .addQueryParameter("video_url", signedUrl)
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
            return@withContext JSONObject("{'error': 'Failed to get container ID'}")
        }

        val containerId =
            Json.parseToJsonElement(containerResponse.body?.string() ?: "").jsonObject["id"]?.jsonPrimitive?.content
                ?: return@withContext JSONObject("{'error': 'Failed to get container ID'}")

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
            val statusCode = Json.parseToJsonElement(
                statusResponse.body?.string() ?: ""
            ).jsonObject["status_code"]?.jsonPrimitive?.content
            if (statusCode == "FINISHED") {
                mediaReady = true
                return@repeat
            }
        }

        if (!mediaReady) {
            return@withContext JSONObject("{'error': 'Media not ready'}")
        }

        val publishUrl =
            "https://graph.facebook.com/v11.0/$accountId/media_publish".toHttpUrlOrNull()!!.newBuilder()
                .addQueryParameter("creation_id", containerId)
                .addQueryParameter("access_token", accessToken)
                .build()
                .toString()

        val publishRequest = Request.Builder()
            .url(publishUrl)
            .post(RequestBody.create(null, ByteArray(0)))
            .build()

        val publishResponse = client.newCall(publishRequest).execute()
        return@withContext if (publishResponse.isSuccessful) {
            JSONObject("{'success': 'Publish Request Successful'}")
        } else {
            JSONObject("{'error': 'Publish Request Failed'}")
        }
    }

    /**
     * Upload an image to Instagram.
     * Sample Call:
     * `uploadPictureToInstagram("1", "https://example.com/image.jpg", "Sample Caption")`
     */
    suspend fun uploadPictureToInstagram(userId: Int, imageUrl: String, caption: String? = ""): String = withContext(Dispatchers.IO) {
        val user = userRepository.findById(userId)
        refreshInstagramAccessToken(userId)

        val accessToken = user?.accounts?.find { it.type == "INSTAGRAM" }?.accessToken
            ?: throw Exception("Instagram access token not found")

        val accountId = user?.accounts?.find { it.type == "INSTAGRAM" }?.accountId
            ?: throw Exception("Instagram access token not found")

        val containerUrl = "https://graph.facebook.com/v11.0/$accountId/media".toHttpUrlOrNull()!!.newBuilder()
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

        val containerId =
            Json.parseToJsonElement(containerResponse.body?.string() ?: "").jsonObject["id"]?.jsonPrimitive?.content
                ?: return@withContext "Failed to get container ID"

        val publishUrl =
            "https://graph.facebook.com/v11.0/$accountId/media_publish".toHttpUrlOrNull()!!.newBuilder()
                .addQueryParameter("creation_id", containerId)
                .addQueryParameter("access_token", accessToken)
                .build()
                .toString()

        val publishRequest = Request.Builder()
            .url(publishUrl)
            .post(RequestBody.create(null, ByteArray(0)))
            .build()

        val publishResponse = client.newCall(publishRequest).execute()
        return@withContext if (publishResponse.isSuccessful) {
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
    suspend fun exchangeCodeForAccessToken(clientId: String, clientSecret: String, redirectUri: String, code: String): String? = withContext(Dispatchers.IO) {
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
        val responseBody = response.body?.string() ?: return@withContext null
        if (!response.isSuccessful) return@withContext null

        val jsonElement = Json.parseToJsonElement(responseBody)
        return@withContext jsonElement.jsonObject["access_token"]?.jsonPrimitive?.content
    }

    /**
     * Exchange short-lived Instagram token for a long-lived token.
     * Sample Call:
     * `exchangeShortLivedTokenForLongLivedToken("clientId", "clientSecret", "shortLivedToken")`
     */
    suspend fun exchangeShortLivedTokenForLongLivedToken(
        clientId: String,
        clientSecret: String,
        shortLivedToken: String
    ): String? = withContext(Dispatchers.IO) {
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
        if (!response.isSuccessful) return@withContext null

        val responseBody = response.body?.string() ?: return@withContext null
        val jsonElement = Json.parseToJsonElement(responseBody)
        return@withContext jsonElement.jsonObject["access_token"]?.jsonPrimitive?.content
    }

    suspend fun refreshInstagramAccessToken(userId: Int) = withContext(Dispatchers.IO) {
        val clientId = System.getProperty("INSTAGRAM_CLIENT_ID")
        val clientSecret = System.getProperty("INSTAGRAM_CLIENT_SECRET")
        val refreshToken =  userRepository.findById(userId)?.accounts?.find { it.type == "INSTAGRAM" }?.refreshToken
            ?: throw Exception("Instagram access token not found")

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
            val responseBody = response.body?.string() ?: return@withContext
            if (!response.isSuccessful) return@withContext

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
    suspend fun getUserPages(accessToken: String): String? = withContext(Dispatchers.IO) {
        val url = "https://graph.facebook.com/v11.0/me/accounts".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("access_token", accessToken)
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

    /**
     * Retrieve Instagram Business Account ID for a given Facebook page.
     * Sample Call:
     * `getInstagramBusinessAccountId("pageId", "accessToken")`
     */
    suspend fun getInstagramBusinessAccountId(pageId: String, accessToken: String): String? = withContext(Dispatchers.IO) {
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
        if (!response.isSuccessful) return@withContext null

        val responseBody = response.body?.string() ?: return@withContext null
        val jsonElement = Json.parseToJsonElement(responseBody)
        return@withContext jsonElement.jsonObject["instagram_business_account"]?.jsonObject?.get("id")?.jsonPrimitive?.content
    }

    /**
     * Retrieve long-lived Instagram access token and Business Account ID.
     * Sample Call:
     * `getLongLivedAccessTokenAndInstagramBusinessAccountId(1, "authCode")`
     */
    suspend fun getLongLivedAccessTokenAndInstagramBusinessAccountId(userId: Int, code: String): Boolean = withContext(Dispatchers.IO) {
        val clientId = System.getProperty("INSTAGRAM_CLIENT_ID")
        val clientSecret = System.getProperty("INSTAGRAM_CLIENT_SECRET")
        val redirectUri = System.getProperty("INSTAGRAM_REDIRECT_URI")
        val shortLivedToken = exchangeCodeForAccessToken(clientId!!, clientSecret!!, redirectUri!!, code)
            ?: return@withContext false

        val longLivedToken = exchangeShortLivedTokenForLongLivedToken(clientId, clientSecret, shortLivedToken)
            ?: return@withContext false
        val pages = getUserPages(longLivedToken) ?: return@withContext false

        val jsonElement = Json.parseToJsonElement(pages)
        val pageId =
            jsonElement.jsonObject["data"]?.jsonArray?.firstOrNull()?.jsonObject?.get("id")?.jsonPrimitive?.content
                ?: return@withContext false

        val instagramBusinessAccountId = getInstagramBusinessAccountId(pageId, longLivedToken)

        userController.linkAccount(userId, "INSTAGRAM", instagramBusinessAccountId, longLivedToken, longLivedToken)

        return@withContext true
    }

    /**
     * Retrieve Instagram page analytics.
     * Sample Call:
     * `getInstagramPageAnalytics("1")`
     */
    suspend fun getInstagramPageAnalytics(userId: String): String? = withContext(Dispatchers.IO) {
        val user = userRepository.findById(userId.toInt())
            ?: throw Exception("User not found")
        val accessToken = user.accounts.find { it.type == "INSTAGRAM" }?.accessToken
            ?: throw Exception("Instagram access token not found")
        val instagramAccountId = user.accounts.find { it.type == "INSTAGRAM" }?.accountId
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
            return@withContext null
        }
        getInstagramMediaIds(userId)

        return@withContext responseBody
    }

    /**
     * Retrieve Instagram post analytics.
     * Sample Call:
     * `getInstagramPostAnalytics("1", "postId")`
     */
    suspend fun getInstagramPostAnalytics(userId: String, postId: String): String? = withContext(Dispatchers.IO) {
        val user = userRepository.findById(userId.toInt())
            ?: throw Exception("User not found")
        val accessToken = user.accounts.find { it.type == "INSTAGRAM" }?.accessToken
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
        if (!response.isSuccessful) return@withContext null

        return@withContext response.body?.string()
    }

    /**
     * Retrieve Instagram media IDs.
     * Sample Call:
     * `getInstagramMediaIds("1")`
     */
    suspend fun getInstagramMediaIds(userId: String): String? = withContext(Dispatchers.IO) {
        val user = userRepository.findById(userId.toInt())
            ?: throw Exception("User not found")
        val accessToken = user.accounts.find { it.type == "INSTAGRAM" }?.accessToken
            ?: throw Exception("Instagram access token not found")
        val accountId = user.accounts.find { it.type == "INSTAGRAM" }?.accountId
            ?: throw Exception("Instagram account ID not found")

        val url = "https://graph.facebook.com/v11.0/$accountId/media".toHttpUrlOrNull()!!.newBuilder()
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
            return@withContext null
        }
        println(responseBody)
        // VIDEO TYPE is REELS, IMAGE and CAROUSEL_ALBUM are other 2 types
        return@withContext responseBody
    }

    /**
     * Retrieve Instagram media details.
     * Sample Call:
     * `getInstagramMediaDetails("1", "postId")`
     */
    suspend fun getInstagramMediaDetails(userId: String, postId: String): String = withContext(Dispatchers.IO) {
        val user = userRepository.findById(userId.toInt())
            ?: throw Exception("User not found")
        val accessToken = user.accounts.find { it.type == "INSTAGRAM" }?.accessToken
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
            return@withContext ""
        }

        println("Response Body: $responseBody")
        val jsonResponse = Json.parseToJsonElement(responseBody ?: "").jsonObject
        return@withContext jsonResponse["media_url"]?.jsonPrimitive?.content ?: ""
    }
}
