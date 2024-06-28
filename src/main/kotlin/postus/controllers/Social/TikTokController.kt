package postus.controllers.Social

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import postus.controllers.MediaController
import postus.controllers.UserController
import postus.models.tiktok.TikTokRefreshTokenRequest
import postus.models.tiktok.TiktokAuthRequest
import postus.repositories.UserRepository
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody

class TikTokController(
    client: OkHttpClient,
    userRepository: UserRepository,
    userController: UserController,
    mediaController: MediaController
) {

    val client = client
    val userRepository = userRepository
    val userController = userController
    val mediaController = mediaController

    suspend fun exchangeAuthorizationCodeForTokens(userId: Int, code: String): Boolean {
        val clientId = System.getProperty("TIKTOK_CLIENT_ID") ?: throw Error("Missing TikTok client ID")
        val clientSecret = System.getProperty("TIKTOK_CLIENT_SECRET") ?: throw Error("Missing TikTok client secret")
        val redirectUri = System.getProperty("TIKTOK_REDIRECT_URI") ?: throw Error("Missing TikTok redirect URI")
        val tokenUrl = "https://open.tiktokapis.com/v2/oauth/token/"
        val formBody = FormBody.Builder()
            .add("client_key", clientId)
            .add("client_secret", clientSecret)
            .add("code", code)
            .add("grant_type", "authorization_code")
            .add("redirect_uri", redirectUri)
            .build()

        val request = Request.Builder()
            .url(tokenUrl)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Cache-Control", "no-cache")
            .post(formBody)
            .build()

        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        val responseBody = response.body?.string() ?: throw IOException("Empty response body")

        if (!response.isSuccessful) {
            println("Failed to exchange authorization code: $responseBody")
            return false
        }

        val jsonResponse = JSONObject(responseBody)
        val accessToken = jsonResponse.getString("access_token")
        val refreshToken = jsonResponse.getString("refresh_token")
        val accountId = jsonResponse.getString("open_id")

        userController.linkAccount(
            userId,
            "TIKTOK",
            accountId,
            accessToken,
            refreshToken
        )
        return true
    }


    suspend fun postToTikTok(userId: Int, videoPath: String, description: String?): String {
        val user = userRepository.findById(userId)
        val accessToken = user?.accounts?.find { it.provider == "TIKTOK" }?.accessToken

        val uploadUrl = "https://open-api.tiktok.com/video/upload/"

        val videoFile = withContext(Dispatchers.IO) { mediaController.downloadVideo(videoPath) }
        val mediaType = "video/mp4".toMediaTypeOrNull()
        val videoRequestBody = videoFile.asRequestBody(mediaType)

        val description = description ?: ""

        val multipartBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("video", videoFile.name, videoRequestBody)
            .addFormDataPart("description", description)
            .build()

        val request = Request.Builder()
            .url(uploadUrl)
            .post(multipartBody)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        val responseBody = response.body?.string() ?: throw IOException("Empty response body")

        return JSONObject(responseBody).getJSONObject("data").getString("video_id")
    }

    suspend fun getTikTokChannelAnalytics(userId: Int): String {
        val url = "https://open-api.tiktok.com/analytics/channels/"

        val user = userRepository.findById(userId)
        val accessToken = user?.accounts?.find { it.provider == "TIKTOK" }?.accessToken

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        return response.body?.string() ?: throw IOException("Empty response body")
    }

    suspend fun getTikTokPostAnalytics(userId: Int, videoId: String): String {
        val url = "https://open-api.tiktok.com/analytics/posts/$videoId"

        val user = userRepository.findById(userId)
        val accessToken = user?.accounts?.find { it.provider == "TIKTOK" }?.accessToken

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        return response.body?.string() ?: throw IOException("Empty response body")
    }

    suspend fun refreshAccessToken(userId: Int, refreshToken: String): Boolean {
        val clientId = System.getProperty("TIKTOK_CLIENT_ID") ?: throw Error("Missing TikTok client ID")
        val clientSecret = System.getProperty("TIKTOK_CLIENT_SECRET") ?: throw Error("Missing TikTok client secret")
        val tokenUrl = "https://open.tiktokapis.com/v2/oauth/token/"
        val formBody = FormBody.Builder()
            .add("client_key", clientId)
            .add("client_secret", clientSecret)
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build()

        val request = Request.Builder()
            .url(tokenUrl)
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Cache-Control", "no-cache")
            .post(formBody)
            .build()

        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        val responseBody = response.body?.string() ?: throw IOException("Empty response body")
        if (!response.isSuccessful) {
            println("Failed to refresh access token: $responseBody")
            return false
        }

        val jsonResponse = JSONObject(responseBody)
        val accessToken = jsonResponse.getString("access_token")
        val newRefreshToken = jsonResponse.getString("refresh_token")

        userController.linkAccount(
            userId,
            "TIKTOK",
            null,
            accessToken,
            newRefreshToken
        )
        return true
    }
}
