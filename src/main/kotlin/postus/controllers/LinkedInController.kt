package postus.controllers

import kotlinx.serialization.json.Json
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import postus.models.linkedin.*
import postus.repositories.UserRepository

class LinkedInController (client: OkHttpClient, userRepository: UserRepository, userController: UserController, mediaController: MediaController) {

    val client = client
    val userRepository = userRepository
    val userController = userController


    fun getLinkedInAccessToken(userId: Int, authCode: String): Boolean? {
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
    fun postToLinkedIn(userId: Int, content: String): Boolean {
        val user = userRepository.findById(userId)

        val accessToken = user?.linkedinAccessToken ?: throw Exception("User not found")
        val accountId = user.linkedinAccountId ?: throw Exception("LinkedIn account ID not found")

        val postUrl = System.getProperty("LINKEDIN_POST_URL") ?: throw Exception("LinkedIn post URL not found")

        val postRequest = LinkedInPostRequest(
            author = accountId,
            lifecycleState = "PUBLISHED",
            specificContent = SpecificContent(
                shareContent = ShareContent(
                    shareCommentary = ShareCommentary(text = content),
                    shareMediaCategory = "NONE"
                )
            ),
            visibility = Visibility(memberNetworkVisibility = "PUBLIC")
        )

        val postBody = Json.encodeToString(LinkedInPostRequest.serializer(), postRequest)

        val mediaType = "application/json".toMediaTypeOrNull()
        val requestBody = RequestBody.create(mediaType, postBody)

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
}
