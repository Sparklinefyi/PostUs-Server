package postus.controllers

import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
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
data class VideoListResponse(
    val videoList: List<String>
)

@Serializable
data class ImageListResponse(
    val imageList: List<String>
)

class MediaController {
    fun uploadImage(userId: String, image : ByteArray) : String {
        val bucketName = "postus-user-media"
        val region = Region.US_EAST_1
        val production = System.getProperty("production")

        val credentials = if (production == "true") {
            EnvironmentVariableCredentialsProvider.create()
        } else {
            DefaultCredentialsProvider.create()
        }

        val presigner = S3Presigner.builder()
            .region(region)
            .credentialsProvider(credentials)
            .build()

        val fileName = generateFileName()

        val objectKey = "$userId/images/$fileName"

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

        val production = System.getProperty("production")

        val credentials = if (production == "true") {
            EnvironmentVariableCredentialsProvider.create()
        } else {
            DefaultCredentialsProvider.create()
        }
        val presigner = S3Presigner.builder()
            .region(region)
            .credentialsProvider(credentials)
            .build()

        val fileName = generateFileName() + ".mp4"
        val objectKey = "$userId/videos/$fileName"

        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(objectKey)
            .build()

        val presignRequest = PutObjectPresignRequest.builder()
            .putObjectRequest(request)
            .signatureDuration(Duration.ofMinutes(10))
            .build()

        val presignedRequest = presigner.presignPutObject(presignRequest)
        val presignedUrl = presignedRequest.url().toString()

        val videoUrl = uploadFileToS3(presignedUrl, video, "video/mp4")
        return videoUrl
    }

    fun getPresignedUrlFromKey(key: String): String {
        val bucketName = "postus-user-media"
        val region = Region.US_EAST_1
        val production = System.getProperty("production")

        val credentials = if (production == "true") {
            EnvironmentVariableCredentialsProvider.create()
        } else {
            DefaultCredentialsProvider.create()
        }

        val presigner = S3Presigner.builder()
            .region(region)
            .credentialsProvider(credentials)
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

    fun getPresignedUrlFromPath(path: String): String {
        val s3Key = path.substring(path.indexOf("/", 10) + 1)
        return getPresignedUrlFromKey(s3Key)
    }

    fun getImageList(userId: String) : ImageListResponse {
        val bucketName = "postus-user-media"

        val production = System.getProperty("production")

        val credentials = if (production == "true") {
            EnvironmentVariableCredentialsProvider.create()
        } else {
            DefaultCredentialsProvider.create()
        }

        val s3Client = S3Client.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(credentials)
            .build()

        val listRequest = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix("$userId/images")
            .build()

        val listResponse = s3Client.listObjectsV2(listRequest)
        val videoKeys = listResponse.contents().map { getPresignedUrlFromKey(it.key()) }
        return ImageListResponse(videoKeys)
    }

    fun getVideoList(userId: String) : VideoListResponse {
        val bucketName = "postus-user-media"

        val production = System.getProperty("production")

        val credentials = if (production == "true") {
            EnvironmentVariableCredentialsProvider.create()
        } else {
            DefaultCredentialsProvider.create()
        }

        val s3Client = S3Client.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(credentials)
            .build()

        val listRequest = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix("$userId/videos")
            .build()

        val listResponse = s3Client.listObjectsV2(listRequest)
        val videoKeys = listResponse.contents().map { getPresignedUrlFromKey(it.key()) }
        return VideoListResponse(videoKeys)
    }

    private fun generateFileName(): String {
        val uuid = UUID.randomUUID()
        return uuid.toString().replace("-", "")
    }

    fun downloadVideo(videoUrl: String): File {

        val client = OkHttpClient()
        val request = Request.Builder().url(videoUrl).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful)
                throw IOException("Failed to download video: $response")

            val tempFile = File.createTempFile("video", ".mp4")
            tempFile.outputStream().use { fileOut ->
                response.body?.byteStream()?.copyTo(fileOut)
            }

            return tempFile
        }
    }

    fun downloadImage(imageUrl: String): File {
        val client = OkHttpClient()
        val request = Request.Builder().url(imageUrl).build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Failed to download image: $response")

            // Create a temporary file with a unique name and appropriate file extension
            val tempFile = File.createTempFile("image", ".jpg")
            tempFile.outputStream().use { fileOut ->
                response.body?.byteStream()?.copyTo(fileOut)
            }

            return tempFile
        }
    }

    private fun uploadFileToS3(presignedUrl: String, fileByteArray: ByteArray, mediaType: String): String {
        val client = OkHttpClient()
        val requestBody = fileByteArray.toRequestBody(mediaType.toMediaTypeOrNull())
        val request = Request.Builder()
            .url(presignedUrl)
            .put(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            return presignedUrl.split("?")[0] // Return the URL without query parameters
        }
    }
}
