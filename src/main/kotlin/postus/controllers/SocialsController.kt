package postus.controllers

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import postus.endpoints.MediaController
import postus.models.YoutubeUploadRequest
import java.io.IOException

class SocialsController{

    fun publishImageToInstagram(userId : String, videoUrl : String, caption : String){
        val client = OkHttpClient()

        val pageAccessToken = "YOUR_PAGE_ACCESS_TOKEN"
        val instagramAccountId = "YOUR_INSTAGRAM_ACCOUNT_ID"

        val containerUrl = "https://graph.facebook.com/v11.0/$instagramAccountId/media?image_url=$videoUrl&caption=$caption".toHttpUrlOrNull()!!.newBuilder()
            .build()
            .toString()

        val containerRequest = Request.Builder()
            .url(containerUrl)
            .post(okhttp3.internal.EMPTY_REQUEST)
            .addHeader("Authorization", "Bearer $pageAccessToken")
            .build()

        val containerResponse = client.newCall(containerRequest).execute()
        if (!containerResponse.isSuccessful) throw IOException("Unexpected code $containerResponse")

        val responseBody = containerResponse.body?.string()
        println("Container Response Body: $responseBody")

        val containerId = Json.parseToJsonElement(responseBody ?: "").jsonObject["id"]?.jsonPrimitive?.content
            ?: throw IOException("Container ID not found in response")

        println("Container ID: $containerId")

        val publishUrl = "https://graph.facebook.com/v11.0/$instagramAccountId/media_publish".toHttpUrlOrNull()!!.newBuilder()
            .addQueryParameter("creation_id", containerId)
            .addQueryParameter("access_token", pageAccessToken)
            .build()
            .toString()

        val publishRequest = Request.Builder()
            .url(publishUrl)
            .post(okhttp3.internal.EMPTY_REQUEST)
            .build()

        val publishResponse = client.newCall(publishRequest).execute()
        if (!publishResponse.isSuccessful) throw IOException("Unexpected code $publishResponse")

        println("Publish Response Status: ${publishResponse.code}")
        println("Publish Response Body: ${publishResponse.body?.string()}")
    }

    fun uploadYoutubeShort(uploadRequest: YoutubeUploadRequest, accessToken: String, videoUrl: String): String {
        val client = OkHttpClient()

        val videoFile = MediaController.downloadVideo(videoUrl)

        val json = Json { encodeDefaults = true }
        val snippetJson = json.encodeToString(uploadRequest.snippet)
        val statusJson = json.encodeToString(uploadRequest.status)

        val mediaType = "video/*".toMediaTypeOrNull()

        val url = "https://www.googleapis.com/upload/youtube/v3/videos?part=snippet,status"

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("snippet", null, snippetJson.toRequestBody("application/json".toMediaTypeOrNull()))
            .addFormDataPart("status", null, statusJson.toRequestBody("application/json".toMediaTypeOrNull()))
            .addFormDataPart("video", videoFile.name, videoFile.asRequestBody(mediaType))
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val responseBody = response.body?.string()
        return responseBody ?: ""
    }
}