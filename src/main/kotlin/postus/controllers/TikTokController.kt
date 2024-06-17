package postus.controllers

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import postus.models.tiktok.TikTokRefreshTokenRequest
import postus.models.tiktok.TiktokAuthRequest
import postus.repositories.UserRepository
import java.io.IOException

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

    fun exchangeAuthorizationCodeForTokens(userId: Int, clientId: String, clientSecret: String, code: String, redirectUri: String): Boolean {
        val tokenUrl = "https://open-api.tiktok.com/oauth/access_token"
        val authRequest = TiktokAuthRequest(client_key = clientId, client_secret = clientSecret, code = code, redirect_uri = redirectUri)
        val requestBody = Json.encodeToString(authRequest)

        val request = Request.Builder()
            .url(tokenUrl)
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IOException("Empty response body")

        if (!response.isSuccessful) {
            println("Failed to exchange authorization code: $responseBody")
            return false
        }

        val jsonResponse = JSONObject(responseBody)
        val accessToken = jsonResponse.getString("access_token")
        val refreshToken = jsonResponse.getString("refresh_token")
        val accountId = getTikTokAccountId(accessToken)

        userController.linkAccount(
            userId,
            "TIKTOK",
            accountId,
            accessToken,
            refreshToken
        )
        return true
    }

    fun getTikTokAccountId(accessToken: String): String {
        val url = "https://open-api.tiktok.com/oauth/userinfo/"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IOException("Empty response body")

        return JSONObject(responseBody).getJSONObject("data").getString("open_id")
    }

    fun postToTikTok(userId: Int, videoPath: String, description: String): String {
        val user = userRepository.findById(userId)
        val accessToken = user?.tiktokAccessToken ?: throw Exception("User does not have a TikTok account linked")
        val uploadUrl = "https://open-api.tiktok.com/video/upload/"

        val videoFile = mediaController.downloadVideo(videoPath)
        val mediaType = "video/mp4".toMediaTypeOrNull()
        val videoRequestBody = videoFile.asRequestBody(mediaType)

        val multipartBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("video", videoFile.name, videoRequestBody)
            .addFormDataPart("description", description)
            .build()

        val request = Request.Builder()
            .url(uploadUrl)
            .post(multipartBody)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw IOException("Empty response body")

        return JSONObject(responseBody).getJSONObject("data").getString("video_id")
    }

    fun getTikTokChannelAnalytics(accessToken: String): String {
        val url = "https://open-api.tiktok.com/analytics/channels/"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: throw IOException("Empty response body")
    }

    fun getTikTokPostAnalytics(accessToken: String, videoId: String): String {
        val url = "https://open-api.tiktok.com/analytics/posts/$videoId"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = client.newCall(request).execute()
        return response.body?.string() ?: throw IOException("Empty response body")
    }


    fun refreshAccessToken(userId: Int, clientId: String, clientSecret: String, refreshToken: String): Boolean {
        val tokenUrl = "https://open.tiktokapis.com/v2/oauth/token/"
        val authRequest = TikTokRefreshTokenRequest(client_key = clientId, client_secret = clientSecret, refresh_token = refreshToken)
        val requestBody = Json.encodeToString(authRequest)

        val request = Request.Builder()
            .url(tokenUrl)
            .post(requestBody.toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        val response = client.newCall(request).execute()
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