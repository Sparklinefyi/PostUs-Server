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
            route("analytics"){
                route("page"){
                    get("youtube"){
                        val apiKey = call.parameters["apiKey"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing apiKey parameter")
                        val channelId = call.parameters["channelId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing channelId parameter")

                        val analytics = SocialsController.getYouTubeChannelAnalytics(apiKey, channelId)
                        if (analytics == null) {
                            call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve YouTube Channel Analytics")
                        } else {
                            call.respond(analytics)
                        }
                    }
                    get("instagram"){
                        val accessToken = call.parameters["accessToken"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing accessToken parameter")
                        val instagramBusinessAccountId = call.parameters["accountId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing instagramBusinessAccountId parameter")

                        val analytics = SocialsController.getInstagramPageAnalytics(accessToken, instagramBusinessAccountId)
                        if (analytics == null) {
                            call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve Instagram Page Analytics")
                        } else {
                            call.respond(analytics)
                        }
                    }
                }
                route("post"){
                    get("youtube"){
                        val apiKey = call.parameters["apiKey"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing apiKey parameter")
                        val videoId = call.parameters["videoId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing videoId parameter")

                        val analytics = SocialsController.getYouTubeVideoAnalytics(apiKey, videoId)
                        if (analytics == null) {
                            call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve YouTube Video Analytics")
                        } else {
                            call.respond(analytics)
                        }
                    }
                    get("instagram"){
                        val accessToken = call.parameters["accessToken"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing accessToken parameter")
                        val postId = call.parameters["postId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing postId parameter")

                        val analytics = SocialsController.getInstagramPostAnalytics(accessToken, postId)
                        if (analytics == null) {
                            call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve Instagram Post Analytics")
                        } else {
                            call.respond(analytics)
                        }
                    }
                }
            }
            route("retrieve"){
                get("instagram"){
                    val accessToken = call.parameters["accessToken"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing accessToken parameter")
                    val postId = call.parameters["postId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing postId parameter")
                    val url = SocialsController.getInstagramMediaDetails(accessToken, postId)
                    call.respond(url)
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
            get("test"){
                val accessToken = call.parameters["accessToken"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing accessToken parameter")
                val apiKey = call.parameters["key"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing postId parameter")
                val query = call.parameters["query"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing accessToken parameter")
                SocialsController.testYoutube(accessToken, apiKey, query)
                call.respond(200)
            }
        }
    }
}
