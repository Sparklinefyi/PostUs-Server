package postus.endpoints

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import postus.controllers.SocialsController
import postus.controllers.UserController
import postus.models.SchedulePostRequest
import postus.models.YoutubeUploadRequest
import io.github.cdimascio.dotenv.Dotenv

val SocialsController = SocialsController()

fun Application.configureSocialsRouting(userService: UserController, dotenv: Dotenv) {
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
                        val userId = call.parameters["userId"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing userId"
                        )
                        val videoUrl = call.parameters["videoUrl"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing video"
                        )
                        val uploadRequest = call.receive<YoutubeUploadRequest>()
                        try{
                            val result = SocialsController.uploadYoutubeShort(uploadRequest, userId, videoUrl)
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
                        val userId = call.parameters["userId"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing userId"
                        )
                        val analytics = SocialsController.getYouTubeChannelAnalytics(userId)
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
                    val state = call.parameters["state"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing state parameter")

                    val info = userService.fetchUserDataByTokenWithProvider(state) ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid state parameter")
                    val platform = info.first;
                    val user = info.second

                    SocialsController.getLongLivedAccessTokenAndInstagramBusinessAccountId(user!!.id, code)

                    if (platform == "web") {
                        call.respondRedirect(dotenv["FRONTEND_REDIRECT"]!!)
                    } else if (platform == "ios") {
                        // For iOS, you might need to use a custom scheme to notify the app
                        call.respond(HttpStatusCode.OK, "You can now close this window and return to the app.")
                    }
                }
                get("youtube"){
                    val code = call.parameters["code"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing code parameter")
                    val state = call.parameters["state"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing state parameter")

                    val info = userService.fetchUserDataByTokenWithProvider(state) ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid state parameter")
                    val platform = info.first;
                    val user = info.second

                    SocialsController.fetchYouTubeAccessToken(user!!.id, code)
                    if (platform == "web") {
                        call.respondRedirect(dotenv["FRONTEND_REDIRECT"]!!)
                    } else if (platform == "ios") {
                        // For iOS, you might need to use a custom scheme to notify the app
                        call.respond(HttpStatusCode.OK, "You can now close this window and return to the app.")
                    }
                }
            }
            get("test"){
                val userId = call.parameters["userId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing userId parameter")
                SocialsController.testYoutube(userId)
                call.respond(200)
            }
            post("schedule"){
                val multipart = call.receiveMultipart()
                var json: String? = null
                var fileBytes: ByteArray? = null

                multipart.forEachPart { part ->
                    when (part) {
                        is PartData.FormItem -> {
                            if (part.name == "json") {
                                json = part.value
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
                if (json == null || fileBytes == null){
                    return@post call.respond(HttpStatusCode.BadRequest, "Missing scheduledetails or media")
                }
                val userId = call.parameters["userId"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing accessToken parameter")
                val postTime = call.parameters["postTime"] ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing accessToken parameter")
                val schedulePostRequest = Gson().fromJson(json, SchedulePostRequest::class.java)
                val mediaByteArray = fileBytes!!
                val posted = SocialsController.schedulePost(userId, postTime, mediaByteArray, schedulePostRequest)
                if (posted) {
                    call.respond(HttpStatusCode.OK, posted)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, "Post could not be scheduled")
                }
                call.respond(schedulePostRequest)
            }
        }
    }
}
