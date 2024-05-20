package postus.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.time.Duration

fun Application.configureSocialRouting() {
    routing {
        route("/socials") {
            post("/upload") {
                val userId = call.parameters["userId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing userId")
                val fileName = call.parameters["fileName"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing fileName")
                val bucketName = "postus-user-media"
                val region = Region.US_EAST_1  // Replace with your bucket's region

                val presigner = S3Presigner.builder()
                    .region(region)
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build()

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

                call.respond(presignedUrl)
            }

            get("/list") {
                val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")
                val bucketName = "your-bucket-name"

                val s3Client = S3Client.builder()
                    .region(Region.US_WEST_2)
                    .credentialsProvider(DefaultCredentialsProvider.create())
                    .build()

                val listRequest = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .prefix("$userId/")
                    .build()

                val listResponse = s3Client.listObjectsV2(listRequest)
                val videoKeys = listResponse.contents().map { it.key() }

                call.respond(videoKeys)
            }

            get("/access") {
                val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")
                val fileName = call.parameters["fileName"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing fileName")
                val bucketName = "your-bucket-name"

                val presigner = S3Presigner.create()
                val objectKey = "$userId/$fileName"

                val getRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build()

                val presignRequest = GetObjectPresignRequest.builder()
                    .getObjectRequest(getRequest)
                    .signatureDuration(Duration.ofMinutes(10))
                    .build()

                val presignedRequest = presigner.presignGetObject(presignRequest)
                val presignedUrl = presignedRequest.url()

                call.respond(mapOf("url" to presignedUrl.toString()))
            }
        }
    }
}
