package postus.controllers

import io.ktor.http.*
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.io.IOException
import java.time.Duration
import java.util.*

@Serializable
data class ListResponse(
    val videoList: List<String>
)

class SocialController {
    fun uploadImage(userId: String, image : ByteArray) : HttpStatusCode {
        val bucketName = "postus-user-media"
        val region = Region.US_EAST_1  // Replace with your bucket's region

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

        val isSuccess = uploadImageToS3(presignedUrl.toString(), image)
        if (isSuccess) {
            return HttpStatusCode.OK
        } else {
            return HttpStatusCode.InternalServerError
        }
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
        val videoKeys = listResponse.contents().map { it.key() }
        return ListResponse(videoKeys)
    }
}


fun uploadImageToS3(presignedUrl: String, imageByteArray: ByteArray): Boolean {
    val client = OkHttpClient()
    val requestBody = imageByteArray.toRequestBody("image/jpeg".toMediaTypeOrNull())
    val request = Request.Builder()
        .url(presignedUrl)
        .put(requestBody)
        .build()

    client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")
        return response.isSuccessful
    }
}

fun generateFileName(): String {
    val uuid = UUID.randomUUID()
    return uuid.toString().replace("-", "")
}
