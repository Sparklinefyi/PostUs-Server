package postus.endpoints

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import postus.controllers.SocialsController
import postus.models.SchedulePostRequest
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
                        val userId = call.parameters["userId"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing userId"
                        )
                        val imageUrl = call.parameters["imageUrl"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing image"
                        )
                        val caption = call.parameters["caption"]
                        try{
                            val result = SocialsController.uploadPictureToInstagram(userId, imageUrl, caption)
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
                        val userId = call.parameters["userId"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing userId"
                        )
                        val videoUrl = call.parameters["videoUrl"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing video"
                        )
                        val caption = call.parameters["caption"]
                        try{
                            val result = SocialsController.uploadVideoToInstagram(userId, videoUrl, caption)
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
            route("analyze"){
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
                        val userId = call.parameters["userId"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing userId"
                        )
                        val analytics = SocialsController.getInstagramPageAnalytics(userId)
                        if (analytics == null) {
                            call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve Instagram Page Analytics")
                        } else {
                            call.respond(analytics)
                        }
                    }
                }
                route("post"){
                    get("youtube"){
                        val videoId = call.parameters["videoId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing videoId parameter")

                        val analytics = SocialsController.getYouTubeVideoAnalytics(videoId)
                        if (analytics == null) {
                            call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve YouTube Video Analytics")
                        } else {
                            call.respond(analytics)
                        }
                    }
                    get("instagram"){
                        val userId = call.parameters["userId"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing userId"
                        )
                        val postId = call.parameters["postId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing postId parameter")

                        val analytics = SocialsController.getInstagramPostAnalytics(userId, postId)
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
                    val userId = call.parameters["userId"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing userId"
                    )
                    val postId = call.parameters["postId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing postId parameter")
                    val url = SocialsController.getInstagramMediaDetails(userId, postId)
                    call.respond(url)
                }
            }
            route("auth"){
                get("instagram"){
                    val code = call.parameters["code"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing code parameter")
                    val (longLivedToken, instagramBusinessAccountId) = SocialsController.getLongLivedAccessTokenAndInstagramBusinessAccountId(code)

                    if (longLivedToken == null || instagramBusinessAccountId == null) {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve tokens or Instagram Business Account ID")
                    } else {
                        call.respond(mapOf("long_lived_access_token" to longLivedToken, "instagram_business_account_id" to instagramBusinessAccountId))
                    }
                }
            }
            get("test"){
                val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId parameter")
                SocialsController.testYoutube(userId)
                call.respond(200)
            }
            post("schedule"){
                val userId = call.parameters["userId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing accessToken parameter")
                val postTime = call.parameters["postTime"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing accessToken parameter")
                val schedulePostRequest = call.receive<SchedulePostRequest>()
                val mediaByteArray = call.receive<ByteArray>()
                val posted = SocialsController.schedulePost(userId, postTime, mediaByteArray, schedulePostRequest)
                if (posted) {
                    call.respond(HttpStatusCode.OK, posted)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, "Post could not be scheduled")
                }
            }
        }
    }
}
