package postus.controllers.Social

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
    client: OkHttpClient,
    userRepository: UserRepository,
    userController: UserController,
    mediaController: MediaController
) {

    val client = client
    val userRepository = userRepository
    val userController = userController


    fun fetchLinkedInAccessToken(userId: Int, authCode: String): Boolean? {
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
        val responseBody = response.body?.string() ?: return null
        if (!response.isSuccessful) return null

        val linkedInOAuthResponse =
            Json { ignoreUnknownKeys = true }.decodeFromString<LinkedinOAuthResponse>(responseBody)
        val accountId = linkedInAccountId(linkedInOAuthResponse.accessToken) ?: return null
        userController.linkAccount(
            userId,
            "LINKEDIN",
            accountId,
            linkedInOAuthResponse.accessToken,
            null
        )
        return true
    }

    /**
     * Get LinkedIn account ID.
     * Sample Call:
     * `linkedInAccountId("accessToken")`
     */
    fun linkedInAccountId(accessToken: String): String? {
        val request = Request.Builder()
            .url("https://api.linkedin.com/v2/userinfo")
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: return null
        if (!response.isSuccessful) return null

        val json = Json { ignoreUnknownKeys = true }
        val profileData = json.decodeFromString<LinkedinUserInfo>(responseBody)

        return profileData.sub
    }

    /**
     * Post to LinkedIn.
     * Sample Call:
     * `postToLinkedIn(1, "Sample LinkedIn post content")`
     */
    fun postToLinkedIn(userId: Int, content: String, mediaUrls: List<String> = emptyList()): Boolean {
        val user = userRepository.findById(userId)
        val accessToken = user?.accounts?.find { it.provider == "LINKEDIN" }?.accessToken
        val accountId = user?.accounts?.find { it.provider == "LINKEDIN" }?.accountId
        val postUrl = System.getProperty("LINKEDIN_POST_URL") ?: throw Exception("LinkedIn post URL not found")

        // Upload each media to LinkedIn and get the URNs
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
        val responseBody = response.body?.string() ?: return false
        if (!response.isSuccessful) return false

        println("Post successful: $responseBody")
        return true
    }


    /**
     * Get LinkedIn post analytics.
     * Sample Call:
     * `getLinkedInPostAnalytics("accessToken", "postUrn")`
     */
    fun getLinkedInPostAnalytics(accessToken: String, postUrn: String): LinkedInAnalyticsResponse? {

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
        val responseBody = response.body?.string() ?: return null
        if (!response.isSuccessful) return null

        return Json { ignoreUnknownKeys = true }.decodeFromString<LinkedInAnalyticsResponse>(responseBody)
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

    fun uploadLinkedInMedia(accessToken: String, accountId: String, mediaUrl: String): String {
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

        // Upload the media
        val mediaRequestBody = mediaUrl.toRequestBody("image/jpeg".toMediaTypeOrNull())
        val mediaRequest = Request.Builder()
            .url(uploadUrl)
            .put(mediaRequestBody)
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "image/jpeg")
            .build()

        val mediaResponse = client.newCall(mediaRequest).execute()
        if (!mediaResponse.isSuccessful) throw IOException("Failed to upload media")

        return asset
    }

    fun getLinkedInPostDetails(accessToken: String, postId: String): String {
        val postUrl = "https://api.linkedin.com/v2/ugcPosts/$postId"

        val request = Request.Builder()
            .url(postUrl)
            .get()
            .addHeader("Authorization", "Bearer $accessToken")
            .addHeader("Content-Type", "application/json")
            .build()

        val client = OkHttpClient()
        val response = client.newCall(request).execute()

        if (!response.isSuccessful) throw IOException("Failed to fetch post details: ${response.message}")

        return response.body?.string() ?: throw IOException("Empty response body")
    }
}
