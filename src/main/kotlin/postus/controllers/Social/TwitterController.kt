package postus.controllers.Social

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import postus.controllers.MediaController
import postus.controllers.UserController
import postus.models.twitter.TwitterAuthenticatedUserResponse
import postus.models.twitter.TwitterOAuthResponse
import postus.repositories.UserRepository
import java.io.IOException

class TwitterController(
    private val client: OkHttpClient,
    private val userRepository: UserRepository,
    private val userController: UserController,
    private val mediaController: MediaController
) {

    /**
     * Fetch Twitter access token.
     * Sample Call:
     * `fetchTwitterAccessToken("1", "authCode")`
     */
    suspend fun fetchTwitterAccessToken(userId: Int, code: String, codeVerifier: String): Boolean? {
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

        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://api.twitter.com/2/oauth2/token")
                .post(requestBody)
                .header("Authorization", Credentials.basic(clientId, clientSecret))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null
            if (!response.isSuccessful) {
                println("Error: $responseBody")
                return@withContext null
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
            true
        }
    }

    /**
     * Refresh Twitter access token.
     * Sample Call:
     * `refreshTwitterAccessToken("1")`
     */
    suspend fun refreshTwitterAccessToken(userId: String): String? {
        val user = userRepository.findById(userId.toInt())
            ?: throw Exception("User not found")
        val refreshToken = user.accounts.find { it.provider == "TWITTER" }?.refreshToken
            ?: throw Exception("User not linked with Twitter")

        val clientId = System.getProperty("TWITTER_CLIENT_ID") ?: throw Error("Missing Twitter client ID")
        val clientSecret = System.getProperty("TWITTER_CLIENT_SECRET") ?: throw Error("Missing Twitter client secret")
        val requestBody = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken!!)
            .build()

        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://api.twitter.com/2/oauth2/token")
                .post(requestBody)
                .header("Authorization", Credentials.basic(clientId, clientSecret))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null
            if (!response.isSuccessful) {
                println("Error: $responseBody")
                return@withContext null
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
            twitterOAuthResponse.access_token
        }
    }

    /**
     * Retrieve authenticated Twitter account ID.
     * Sample Call:
     * `getAuthenticatedTwitterAccountId("accessToken")`
     */
    suspend fun getAuthenticatedTwitterAccountId(accessToken: String): String? {
        return withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("https://api.twitter.com/2/users/me")
                .header("Authorization", "Bearer $accessToken")
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: return@withContext null
            if (!response.isSuccessful) {
                println("Error: $responseBody")
                return@withContext null
            }

            val twitterUserResponse =
                Json { ignoreUnknownKeys = true }.decodeFromString<TwitterAuthenticatedUserResponse>(responseBody)
            twitterUserResponse.data.id
        }
    }

    /**
     * Post a tweet.
     * Sample Call:
     * `postToTwitter("1", "Sample tweet text", "path/to/image.jpg", "path/to/video.mp4")`
     */
    suspend fun postToTwitter(
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

        return withContext(Dispatchers.IO) {
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

            tweetResponseBody ?: ""
        }
    }
}
