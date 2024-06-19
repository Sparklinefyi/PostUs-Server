package postus.controllers

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import postus.models.twitter.TwitterAuthenticatedUserResponse
import postus.models.twitter.TwitterOAuthResponse
import postus.repositories.UserRepository

class TwitterController(
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
     * Fetch Twitter access token.
     * Sample Call:
     * `fetchTwitterAccessToken("1", "authCode")`
     */
    fun fetchTwitterAccessToken(userId: Int, code: String, codeVerifier: String): Boolean? {
        val clientId = System.getProperty("TWITTER_CLIENT_ID") ?: throw Error("Missing Twitter client ID")
        val clientSecret = System.getProperty("TWITTER_CLIENT_SECRET") ?: throw Error("Missing Twitter client secret")
        val redirectUri = System.getProperty("TWITTER_REDIRECT_URI") ?: throw Error("Missing Twitter redirect URI")

        val requestBody = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("client_id", clientId)
            .add("redirect_uri", redirectUri)
            .add("code", code)
            .add("code_verifier", codeVerifier)
            .build()

        println(requestBody)

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

        val twitterOAuthResponse =
            Json { ignoreUnknownKeys = true }.decodeFromString<TwitterOAuthResponse>(responseBody)
        val accountID = getAuthenticatedTwitterAccountId(twitterOAuthResponse.access_token)
            ?: throw Exception("Error getting Twitter account ID")
        userController.linkAccount(
            userId,
            "TWITTER",
            accountID,
            twitterOAuthResponse.access_token,
            twitterOAuthResponse.refresh_token
        )
        return true
    }

    /**
     * Refresh Twitter access token.
     * Sample Call:
     * `refreshTwitterAccessToken("1")`
     */
    fun refreshTwitterAccessToken(userId: String): String? {
        val user = userRepository.findById(userId.toInt())
            ?: throw Exception("User not found")
        val refreshToken = user.twitterRefresh
        val clientId = System.getProperty("TWITTER_CLIENT_ID") ?: throw Error("Missing Twitter client ID")
        val clientSecret = System.getProperty("TWITTER_CLIENT_SECRET") ?: throw Error("Missing Twitter client secret")
        val requestBody = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken!!)
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

        val twitterOAuthResponse =
            Json { ignoreUnknownKeys = true }.decodeFromString<TwitterOAuthResponse>(responseBody)
        userController.linkAccount(
            userId.toInt(),
            "TWITTER",
            null,
            twitterOAuthResponse.access_token,
            twitterOAuthResponse.refresh_token
        )
        return twitterOAuthResponse.access_token
    }

    /**
     * Retrieve authenticated Twitter account ID.
     * Sample Call:
     * `getAuthenticatedTwitterAccountId("accessToken")`
     */
    fun getAuthenticatedTwitterAccountId(accessToken: String): String? {
        val request = Request.Builder()
            .url("https://api.twitter.com/2/users/me")
            .header("Authorization", "Bearer $accessToken")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return null
        if (!response.isSuccessful) {
            println("Error: $responseBody")
            return null
        }

        val twitterUserResponse =
            Json { ignoreUnknownKeys = true }.decodeFromString<TwitterAuthenticatedUserResponse>(responseBody)
        return twitterUserResponse.data.id
    }

    /**
     * Post a tweet.
     * Sample Call:
     * `postToTwitter("1", "Sample tweet text", "path/to/image.jpg", "path/to/video.mp4")`
     */
    fun postToTwitter(
        userId: String,
        text: String? = null,
        imagePath: String? = null,
        videoPath: String? = null,
    ): String {
        val accessToken = refreshTwitterAccessToken(userId) ?: throw Exception("User not found")
        /*
        var mediaId: String? = null
        if (imagePath != null) {
            mediaId = uploadMedia(accessToken, imagePath, "IMAGE")
        } else if (videoPath != null) {
            mediaId = uploadMedia(accessToken, videoPath, "VIDEO")
        }

        val tweetData = mutableMapOf<String, Any?>("text" to text)
        if (mediaId != null) {
            tweetData["media"] = mapOf("media_ids" to listOf(mediaId))
        }
*/
        val tweetData = mutableMapOf<String, String?>("text" to text)
        val tweetJson = Json.encodeToString(tweetData)
        val tweetRequestBody = tweetJson.toRequestBody("application/json".toMediaTypeOrNull())
        val tweetRequest = Request.Builder()
            .url("https://api.twitter.com/2/tweets")
            .post(tweetRequestBody)
            .header("Authorization", "Bearer $accessToken")
            .build()

        val tweetResponse = client.newCall(tweetRequest).execute()
        val tweetResponseBody = tweetResponse.body?.string()
        if (!tweetResponse.isSuccessful) {
            throw Exception("Error posting tweet: $tweetResponseBody")
        }

        return tweetResponseBody ?: ""
    }
}
