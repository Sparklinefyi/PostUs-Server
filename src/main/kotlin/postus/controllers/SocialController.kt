package postus.controllers

import kotlinx.serialization.Serializable
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
import postus.models.YoutubeUploadRequest
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.io.File
import java.io.IOException
import java.time.Duration
import java.util.*

@Serializable
data class ListResponse(
    val videoList: List<String>
)

class SocialController {
    fun uploadImage(userId: String, image : ByteArray) : String {
        val bucketName = "postus-user-media"
        val region = Region.US_EAST_1

        val presigner = S3Presigner.builder()
            .region(region)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build()

        val fileName = generateFileName()

        val objectKey = "$userId/$fileName"

        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(objectKey)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .putObjectRequest(request)
            .signatureDuration(Duration.ofMinutes(10))
            .build()

        val presignedRequest = presigner.presignPutObject(presignRequest)
        val presignedUrl = presignedRequest.url()

        val imageUrl = uploadFileToS3(presignedUrl.toString(), image, "image/jpeg")
        return imageUrl
    }

    fun uploadVideo(userId: String, video: ByteArray): String {
        val bucketName = "postus-user-media"
        val region = Region.US_EAST_1

        val presigner = S3Presigner.builder()
            .region(region)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build()

        val fileName = generateFileName() + ".mp4"

        val objectKey = "$userId/$fileName"

        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(objectKey)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .putObjectRequest(request)
            .signatureDuration(Duration.ofMinutes(10))
            .build()

        val presignedRequest = presigner.presignPutObject(presignRequest)
        val presignedUrl = presignedRequest.url()

        val videoUrl = uploadFileToS3(presignedUrl.toString(), video, "video/mp4")
        return videoUrl
    }

    fun getPresignedUrl(key: String): String {
        val bucketName = "postus-user-media"
        val region = Region.US_EAST_1

        val presigner = S3Presigner.builder()
            .region(region)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build()

        val getObjectRequest = GetObjectPresignRequest.builder()
            .getObjectRequest { builder ->
                builder.bucket(bucketName).key(key)
            }
            .signatureDuration(Duration.ofMinutes(10))
            .build()

        val presignedRequest = presigner.presignGetObject(getObjectRequest)
        return presignedRequest.url().toString()
    }

    fun getImageList(userId: String) : ListResponse {
        val bucketName = "postus-user-media"

        val s3Client = S3Client.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build()

        val listRequest = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix("$userId/")
            .build()

        val listResponse = s3Client.listObjectsV2(listRequest)
        val videoKeys = listResponse.contents().map { getPresignedUrl(it.key()) }
        return ListResponse(videoKeys)
    }

    fun getFirstImage(userId: String) : String {
        val bucketName = "postus-user-media"

        val s3Client = S3Client.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build()

        val listRequest = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix("$userId/")
            .build()

        val listResponse = s3Client.listObjectsV2(listRequest)
        val videoKey = listResponse.contents().map { it.key() }[0]
        return getPresignedUrl(videoKey)
    }
}


fun uploadFileToS3(presignedUrl: String, fileByteArray: ByteArray, mediaType: String): String {
    val client = OkHttpClient()
    val requestBody = fileByteArray.toRequestBody(mediaType.toMediaTypeOrNull())
    val request = Request.Builder()
        .url(presignedUrl)
        .put(requestBody)
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
        val uncutURL = response.networkResponse?.request?.url.toString()
        val fileUrl = uncutURL.removeRange(uncutURL.indexOf("?"), uncutURL.length)
        return fileUrl
    }
}

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

    val videoFile = downloadVideo(videoUrl)

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

fun generateFileName(): String {
    val uuid = UUID.randomUUID()
    return uuid.toString().replace("-", "")
}

fun downloadVideo(videoUrl: String): File {
    val client = OkHttpClient()
    val request = Request.Builder().url(videoUrl).build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Failed to download video: $response")

        val tempFile = File.createTempFile("video", ".mp4")
        tempFile.outputStream().use { fileOut ->
            response.body?.byteStream()?.copyTo(fileOut)
        }

        return tempFile
    }
}
