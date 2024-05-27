package postus.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import postus.controllers.MediaController

val MediaController = MediaController()

fun Application.configureMediaRouting() {
    routing {
        route("/socials") {

            route("/upload") {

                post("image") {
                    val userId = call.parameters["userId"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing userId"
                    )
                    val imageByteArray = call.receive<ByteArray>()
                    val response = MediaController.uploadImage(userId, imageByteArray)
                    call.respond(response)
                }

                post("video"){
                    val userId = call.parameters["userId"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing userId"
                    )
                    val videoByteArray = call.receive<ByteArray>()
                    val response = MediaController.uploadVideo(userId, videoByteArray)
                    call.respond(response)
                }
            }

            route("/list") {

                get("images") {
                    val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")
                    val response = MediaController.getImageList(userId)
                    call.respond(response)
                }

                get("videos"){
                    val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")
                    val response = MediaController.getVideoList(userId)
                    call.respond(response)
                }
            }

            route("/resource") {

                get("image"){
                    val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")
                    val key = call.parameters["key"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")
                    val response = MediaController.getImage(userId, key)
                    call.respond(response)
                }

                get("video"){
                    val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")
                    val key = call.parameters["key"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId")
                    val response = MediaController.getVideo(userId, key)
                    call.respond(response)
                }
            }
        }
    }
}
