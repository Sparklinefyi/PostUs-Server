package postus.controllers.Social

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import postus.controllers.MediaController
import postus.controllers.UserController
import postus.models.linkedin.*
import postus.repositories.UserRepository
import java.io.IOException

class LinkedInController(
    private val client: OkHttpClient,
    private val userRepository: UserRepository,
    private val userController: UserController,
    private val mediaController: MediaController
) {

    suspend fun fetchLinkedInAccessToken(userId: Int, authCode: String): Boolean? = withContext(Dispatchers.IO) {
        val clientId = System.getProperty("LINKEDIN_CLIENT_ID") ?: throw Exception("LinkedIn client ID not found")
        val clientSecret =
            System.getProperty("LINKEDIN_CLIENT_SECRET") ?: throw Exception("LinkedIn client secret not found")
        val redirectUri =
            System.getProperty("LINKEDIN_REDIRECT_URI") ?: throw Exception("LinkedIn redirect URI not found")
        val tokenUrl = System.getProperty("LINKEDIN_TOKEN_URL") ?: throw Exception("LinkedIn token URL not found")

        val requestBody = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", authCode)
            .add("redirect_uri", redirectUri)
            .add("client_id", clientId)
            .add("client_secret", clientSecret)
            .build()

        val request = Request.Builder()
            .url(tokenUrl)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext null
        if (!response.isSuccessful) return@withContext null

        val linkedInOAuthResponse =
            Json { ignoreUnknownKeys = true }.decodeFromString<LinkedinOAuthResponse>(responseBody)
        val accountId = linkedInAccountId(linkedInOAuthResponse.accessToken) ?: return@withContext null
        userController.linkAccount(
            userId,
            "LINKEDIN",
            accountId,
            linkedInOAuthResponse.accessToken,
            null
        )
        true
    }

    suspend fun linkedInAccountId(accessToken: String): String? = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.linkedin.com/v2/userinfo")
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext null
        if (!response.isSuccessful) return@withContext null

        val json = Json { ignoreUnknownKeys = true }
        val profileData = json.decodeFromString<LinkedinUserInfo>(responseBody)

        profileData.sub
    }

    suspend fun postToLinkedIn(userId: Int, content: String, mediaUrls: List<String> = emptyList()): Boolean = withContext(Dispatchers.IO) {
        val user = userRepository.findById(userId)
        val accessToken = user?.accounts?.find { it.provider == "LINKEDIN" }?.accessToken
        val accountId = user?.accounts?.find { it.provider == "LINKEDIN" }?.accountId
        val postUrl = System.getProperty("LINKEDIN_POST_URL") ?: throw Exception("LinkedIn post URL not found")

        val mediaURNs = mediaUrls.map { uploadLinkedInMedia(accessToken!!, accountId!!, it) }
        val postBody = createPostData(accountId!!, content, mediaURNs)

        val mediaTypeHeader = "application/json".toMediaTypeOrNull()
        val requestBody = RequestBody.create(mediaTypeHeader, postBody)

        val request = Request.Builder()
            .url(postUrl)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext false
        if (!response.isSuccessful) return@withContext false

        println("Post successful: $responseBody")
        true
    }

    suspend fun getLinkedInPostAnalytics(accessToken: String, postUrn: String): LinkedInAnalyticsResponse? = withContext(Dispatchers.IO) {
        val analyticsUrl =
            System.getProperty("LINKEDIN_ANALYTICS_URL") ?: throw Exception("LinkedIn analytics URL not found")

        val url = analyticsUrl.toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("q", "statistics")
            .addQueryParameter("shares", postUrn)
            .build()

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return@withContext null
        if (!response.isSuccessful) return@withContext null

        Json { ignoreUnknownKeys = true }.decodeFromString<LinkedInAnalyticsResponse>(responseBody)
    }

    fun createPostData(
        authorURN: String,
        text: String,
        mediaUrls: List<String> = emptyList(),
        mediaType: String = "IMAGE",
        visibility: String = "PUBLIC"
    ): String {
        val mediaObjects = mediaUrls.map { url ->
            Media(
                status = "READY",
                description = MediaDescription(text = "Media description"),
                media = url,
                title = MediaTitle(text = "Media title"),
                mediaType = mediaType
            )
        }

        val postData = LinkedInPostRequest(
            author = "urn:li:person:$authorURN",
            lifecycleState = "PUBLISHED",
            specificContent = SpecificContent(
                shareContent = ShareContent(
                    shareCommentary = ShareCommentary(text = text),
                    shareMediaCategory = if (mediaObjects.isEmpty()) "NONE" else "IMAGE",
                    media = mediaObjects
                )
            ),
            visibility = Visibility(
                memberNetworkVisibility = visibility
            )
        )

        return Json.encodeToString(postData)
    }

    suspend fun uploadLinkedInMedia(accessToken: String, accountId: String, mediaUrl: String): String = withContext(Dispatchers.IO) {
        val linkedInUploadUrl = "https://api.linkedin.com/v2/assets?action=registerUpload"

        val registrationRequestJson = """
        {
            "registerUploadRequest": {
                "recipes": ["urn:li:digitalmediaRecipe:feedshare-image"],
                "owner": "urn:li:person:$accountId",
                "serviceRelationships": [{
                    "relationshipType": "OWNER",
                    "identifier": "urn:li:userGeneratedContent"
                }]
            }
        }
    """.trimIndent()

        val requestBody = registrationRequestJson.toRequestBody("application/json".toMediaTypeOrNull())
        val registrationRequest = Request.Builder()
            .url(linkedInUploadUrl)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .build()

        val response = client.newCall(registrationRequest).execute()
        if (!response.isSuccessful) throw IOException("Failed to register upload")

        val responseBody = response.body?.string() ?: throw IOException("Empty response body")
        val jsonResponse = JSONObject(responseBody)
        val uploadUrl = jsonResponse.getJSONObject("value").getJSONObject("uploadMechanism")
            .getJSONObject("com.linkedin.digitalmedia.uploading.MediaUploadHttpRequest").getString("uploadUrl")
        val asset = jsonResponse.getJSONObject("value").getString("asset")

        val mediaRequestBody = mediaUrl.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val mediaRequest = Request.Builder()
            .url(uploadUrl)
            .put(mediaRequestBody)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "image/jpeg")
            .build()

        val mediaResponse = client.newCall(mediaRequest).execute()
        if (!mediaResponse.isSuccessful) throw IOException("Failed to upload media")

        asset
    }

    suspend fun getLinkedInPostDetails(accessToken: String, postId: String): String = withContext(Dispatchers.IO) {
        val postUrl = "https://api.linkedin.com/v2/ugcPosts/$postId"

        val request = Request.Builder()
            .url(postUrl)
            .get()
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Failed to fetch post details: ${response.message}")

        response.body?.string() ?: throw IOException("Empty response body")
    }
}
