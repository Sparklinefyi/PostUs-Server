package postus.endpoints

import com.auth0.jwt.JWT
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import postus.models.media.*
import postus.models.auth.*
import postus.controllers.UserController
import postus.controllers.MediaController

fun Application.configureMediaRouting(userService: UserController, mediaController: MediaController) {
    routing {
        route("/socials") {

            // Upload image or video
            route("/upload") {
                post("image") {
                    var request = call.receive<UploadRequest>()
                    val userInfo = userService.fetchUserDataByToken(request.token) ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid token"
                    )

                    val response = mediaController.uploadImage(userInfo.id.toString(), request.byteArray)
                    call.respond(response)
                }
                post("video") {
                    val request = call.receive<UploadRequest>()
                    // Process the upload request
                    val userInfo = userService.fetchUserDataByToken(request.token) ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid token"
                    )

                    val response = mediaController.uploadVideo(userInfo.id.toString(), request.byteArray)
                    call.respond(response)

                }
            }

            // List images or videos
            route("/list") {
                get("images") {
                    val request = call.receive<TokenRequest>()
                    val userInfo = userService.fetchUserDataByToken(request.token) ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid token"
                    )

                    val response = mediaController.getImageList(userInfo.id.toString())
                    call.respond(response)
                }

                get("videos") {
                    val request = call.receive<TokenRequest>()
                    val userInfo = userService.fetchUserDataByToken(request.token) ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid token"
                    )
                    val response = mediaController.getVideoList(userInfo.id.toString())
                    call.respond(response)
                }
            }

            // Get image or video
            route("/resource") {
                get("image") {
                    val request = call.receive<ResourceRequest>()
                    val userInfo = userService.fetchUserDataByToken(request.token) ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid token"
                    )
                    val key = request.key
                    //val response = mediaController.getImage(userInfo.id.toString(), key)
                    //call.respond(response)
                }

                get("video") {
                    val request = call.receive<ResourceRequest>()
                    val userInfo = userService.fetchUserDataByToken(request.token) ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid token"
                    )
                    val key = request.key
                    //val response = mediaController.getVideo(userInfo.id.toString(), key)
                    //call.respond(response)
                }
                get("media") {
                    val path = call.parameters["path"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing path to media"
                    )
                    val response = mediaController.getPresignedUrlFromPath(path)
                    call.respond(response)
                }
            }
        }
    }
}
