package postus.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import java.time.Duration
import postus.controllers.SocialController

val SocialController = SocialController()

fun Application.configureSocialRouting() {
    routing {
        route("/socials") {

            route("/upload") {

                post("image") {
                    val userId = call.parameters["userId"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing userId"
                    )
                    val imageByteArray = call.receive<ByteArray>()
                    val response = SocialController.uploadImage(userId, imageByteArray)
                    call.respond(response)
                }

                post("video"){
                    val userId = call.parameters["userId"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing userId"
                    )
                    val videoByteArray = call.receive<ByteArray>()
                    val response = SocialController.uploadVideo(userId, videoByteArray)
                    call.respond(response)
                }
            }

            get("/list") {
                val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")
                val response = SocialController.getImageList(userId)
                call.respond(response)
            }

            get("/image") {
                val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")
                val response = SocialController.getFirstImage(userId)
                call.respond(response)
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
