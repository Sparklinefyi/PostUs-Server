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
            route("auth"){
                get("instagram"){
                    val clientId = "486594670554364"
                    val clientSecret = "c4953d7d0d6771d0bace9d4d715647f2"
                    val redirectUri = "https://sparkline.fyi/login"
                    val code = call.parameters["code"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing code parameter")

                    val (longLivedToken, instagramBusinessAccountId) = SocialsController.getLongLivedAccessTokenAndInstagramBusinessAccountId(clientId, clientSecret, redirectUri, code)

                    if (longLivedToken == null || instagramBusinessAccountId == null) {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve tokens or Instagram Business Account ID")
                    } else {
                        call.respond(mapOf("long_lived_access_token" to longLivedToken, "instagram_business_account_id" to instagramBusinessAccountId))
                    }
                }
            }
        }
    }
}
