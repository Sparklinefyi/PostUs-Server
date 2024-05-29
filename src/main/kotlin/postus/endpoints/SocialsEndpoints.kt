package postus.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import postus.controllers.SocialsController
import postus.models.YoutubeUploadRequest

val SocialsController = SocialsController()

fun Application.configureSocialsRouting() {
    routing {
        route("socials"){
            route("publish"){
                route("image"){
                    post("tiktok"){

                    }
                    post("instagram"){
                        val accessToken = call.parameters["accessToken"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing accessToken"
                        )
                        val videoUrl = call.parameters["videoUrl"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing video"
                        )
                        val instagramAccountId = call.parameters["accountId"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing video"
                        )
                        val caption = call.parameters["caption"]
                        try{
                            val result = SocialsController.uploadPictureToInstagram(videoUrl, caption, accessToken, instagramAccountId)
                            call.respond(HttpStatusCode.OK, result)
                        } catch (e : Exception){
                            call.respond(e)
                        }
                    }
                    post("youtube"){

                    }
                }
                route("video"){
                    post("tiktok"){

                    }
                    post("instagram"){
                        val accessToken = call.parameters["accessToken"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing accessToken"
                        )
                        val videoUrl = call.parameters["videoUrl"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing video"
                        )
                        val instagramAccountId = call.parameters["accountId"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing video"
                        )
                        val caption = call.parameters["caption"]
                        try{
                            val result = SocialsController.uploadVideoToInstagram(videoUrl, caption, accessToken, instagramAccountId)
                            call.respond(HttpStatusCode.OK, result)
                        } catch (e : Exception){
                            call.respond(e)
                        }
                    }
                    post("youtube"){
                        val accessToken = call.parameters["accessToken"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing accessToken"
                        )
                        val videoUrl = call.parameters["videoUrl"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing video"
                        )
                        val uploadRequest = call.receive<YoutubeUploadRequest>()
                        try{
                            val result = SocialsController.uploadYoutubeShort(uploadRequest, accessToken, videoUrl)
                            call.respond(HttpStatusCode.OK, result)
                        } catch (e : Exception){
                            call.respond(e)
                        }
                    }
                }
            }
        }
    }
}
