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
import postus.controllers.MediaController
import postus.controllers.UserController

val mediaController = MediaController()

fun Application.configureMediaRouting(userService: UserController) {
    routing {
            route("/socials") {

                // Upload image or video
                route("/upload") {
                    post("image") {
                        val payload = call.receive<Map<String, Any>>()
                        val token = payload["token"] as String
                        val imageByteArray = (payload["file"] as List<*>).map { (it as Number).toByte() }.toByteArray()

                        val userInfo = userService.fetchUserDataByToken(token)!!
                        val response = mediaController.uploadImage(userInfo.id.toString(), imageByteArray)
                        call.respond(response)
                    }
                    post("video") {
                        val multipart = call.receiveMultipart()
                        var fileBytes: ByteArray? = null
                        var token: String? = null

                        multipart.forEachPart { part ->
                            when (part) {
                                is PartData.FormItem -> {
                                    if (part.name == "token") {
                                        token = part.value
                                    }
                                }
                                is PartData.FileItem -> {
                                    if (part.name == "file") {
                                        fileBytes = part.streamProvider().readBytes()
                                    }
                                }
                                else -> Unit
                            }
                            part.dispose()
                        }

                        if (fileBytes == null || token == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing file or token")
                            return@post
                        }

                        val userInfo = userService.fetchUserDataByToken(token!!) ?: return@post call.respond(HttpStatusCode.BadRequest, "Invalid token")
                        val response = mediaController.uploadVideo(userInfo.id.toString(), fileBytes!!)
                        call.respond(response)
                    }

                }

                // List images or videos
                route("/list") {
                    get("images") {
                        val token = call.parameters["token"] as String
                        val userInfo = userService.fetchUserDataByToken(token)!!
                        val response = mediaController.getImageList(userInfo.id.toString())
                        call.respond(response)
                    }

                    get("videos") {
                        val token = call.parameters["token"] as String
                        val userInfo = userService.fetchUserDataByToken(token)!!
                        val response = mediaController.getVideoList(userInfo.id.toString())
                        call.respond(response)
                    }
                }

                // Get image or video
                route("/resource") {
                    get("image") {
                        val token = call.parameters["token"] as String
                        val userInfo = userService.fetchUserDataByToken(token)!!
                        val key = call.parameters["key"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing key")
                        val response = mediaController.getImage(userInfo.id.toString(), key)
                        call.respond(response)
                    }

                    get("video") {
                        val token = call.parameters["token"] as String
                        val userInfo = userService.fetchUserDataByToken(token)!!
                        val key = call.parameters["key"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing key")
                        val response = mediaController.getVideo(userInfo.id.toString(), key)
                        call.respond(response)
                    }
                get("media"){
                    val path = call.parameters["path"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing path to media")
                    val response = MediaController.getPresignedUrlFromPath(path)
                    call.respond(response)
                }
            }
    }
}
