package postus.endpoints

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import postus.controllers.SocialsController
import postus.controllers.UserController
import postus.models.SchedulePostRequest
import postus.models.YoutubePrivacyStatus
import postus.models.YoutubeShortSnippet
import postus.models.YoutubeUploadRequest

val socialController = SocialsController()

fun Application.configureSocialsRouting(userService: UserController) {
    routing {
        route("socials"){
            route("publish"){
                route("image"){
                    post("tiktok"){

                    }
                    post("instagram"){
                        val token = call.parameters["token"] as String
                        val userInfo = userService.fetchUserDataByToken(token)!!
                        val userId = userInfo.id.toString()
                        val imageUrl = call.parameters["imageUrl"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing image"
                        )
                        val caption = call.parameters["caption"]
                        try{
                            val result = socialController.uploadPictureToInstagram(userId, imageUrl, caption)
                            call.respond(HttpStatusCode.OK, result)
                        } catch (e : Exception){
                            call.respond(e)
                        }
                    }
                }
                route("video"){
                    post("tiktok"){

                    }
                    post("instagram"){
                        val token = call.parameters["token"] as String
                        val userInfo = userService.fetchUserDataByToken(token)!!
                        val userId = userInfo.id.toString()
                        val videoUrl = call.parameters["videoUrl"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing video"
                        )
                        val caption = call.parameters["caption"]
                        try{
                            val result = socialController.uploadVideoToInstagram(userId, videoUrl, caption)
                            call.respond(HttpStatusCode.OK, result)
                        } catch (e : Exception){
                            call.respond(e)
                        }
                    }
                    post("youtube") {
                        val multipart = call.receiveMultipart()
                        var token: String? = null
                        var videoUrl: String? = null
                        var snippet: YoutubeShortSnippet? = null
                        var status: YoutubePrivacyStatus? = null

                        multipart.forEachPart { part ->
                            when (part) {
                                is PartData.FormItem -> {
                                    when (part.name) {
                                        "token" -> token = part.value
                                        "videoUrl" -> videoUrl = part.value
                                        "snippet" -> snippet = Json.decodeFromString(part.value)
                                        "status" -> status = Json.decodeFromString(part.value)
                                    }
                                }
                                else -> Unit
                            }
                            part.dispose()
                        }

                        if (token == null || videoUrl == null || snippet == null || status == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing parameters")
                            return@post
                        }

                        val userInfo = userService.fetchUserDataByToken(token!!)!!
                        val userId = userInfo.id.toString()
                        val uploadRequest = YoutubeUploadRequest(snippet!!, status!!)

                        try {
                            val result = socialController.uploadYoutubeShort(uploadRequest, userId, videoUrl!!)
                            call.respond(HttpStatusCode.OK, result)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, e.message ?: "An error occurred")
                        }
                    }
                }
                post("twitter"){
                    val token = call.parameters["token"] as String
                    val userInfo = userService.fetchUserDataByToken(token)!!
                    val userId = userInfo.id.toString()
                    val imageUrl = call.parameters["imageUrl"]
                    val videoUrl = call.parameters["videoUrl"]
                    val text = call.parameters["text"]
                    val response = socialController.postToTwitter(userId, text, imageUrl, videoUrl)
                    call.respond(HttpStatusCode.OK, response)
                }
                post("linkedin"){
                    val userId = call.parameters["userId"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing userId"
                    )
                    val content = call.parameters["content"]?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing content"
                    )
                    val response = socialController.postToLinkedIn(userId.toInt(), content)
                    call.respond(HttpStatusCode.OK, response)
                }
            }
            route("analyze"){
                route("page"){
                    get("youtube"){
                        val token = call.parameters["token"] as String
                        val userInfo = userService.fetchUserDataByToken(token)!!
                        val userId = userInfo.id.toString()
                        val analytics = socialController.getYouTubeChannelAnalytics(userId)
                        if (analytics == null) {
                            call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve YouTube Channel Analytics")
                        } else {
                            call.respond(analytics)
                        }
                    }
                    get("instagram"){
                        val token = call.parameters["token"] as String
                        val userInfo = userService.fetchUserDataByToken(token)!!
                        val userId = userInfo.id.toString()
                        val analytics = socialController.getInstagramPageAnalytics(userId)
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

                        val analytics = socialController.getYouTubeVideoAnalytics(videoId)
                        if (analytics == null) {
                            call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve YouTube Video Analytics")
                        } else {
                            call.respond(analytics)
                        }
                    }
                    get("instagram"){
                        val token = call.parameters["token"] as String
                        val userInfo = userService.fetchUserDataByToken(token)!!
                        val userId = userInfo.id.toString()
                        val postId = call.parameters["postId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing postId parameter")

                        val analytics = socialController.getInstagramPostAnalytics(userId, postId)
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
                    val token = call.parameters["token"] as String
                    val userInfo = userService.fetchUserDataByToken(token)!!
                    val userId = userInfo.id.toString()
                    val postId = call.parameters["postId"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing postId parameter")
                    val url = socialController.getInstagramMediaDetails(userId, postId)
                    call.respond(url)
                }
            }
            route("auth"){
                get("instagram"){
                    val code = call.parameters["code"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing code parameter")
                    val state = call.parameters["state"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing state parameter")

                    val info = userService.fetchUserDataByTokenWithPlatform(state) ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid state parameter")
                    val platform = info.first;
                    val user = info.second

                    socialController.getLongLivedAccessTokenAndInstagramBusinessAccountId(user!!.id, code)

                    if (platform == "web") {
                        call.respondRedirect(System.getenv("FRONTEND_REDIRECT"))
                    } else if (platform == "ios") {
                        // For iOS, you might need to use a custom scheme to notify the app
                        call.respond(HttpStatusCode.OK, "You can now close this window and return to the app.")
                    }
                }
                get("youtube") {
                    try {
                        val code = call.parameters["code"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing code parameter")
                        val state = call.parameters["state"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing state parameter")

                        val info = userService.fetchUserDataByTokenWithPlatform(state) ?: return@get call.respond(HttpStatusCode.BadRequest, "Invalid state parameter")
                        val platform = info.first
                        val user = info.second

                        val validated = socialController.fetchYouTubeAccessToken(user!!.id, code)
                        if (validated) {
                            if (platform == "web") {
                                call.respondRedirect(System.getenv("FRONTEND_REDIRECT") ?: "/")
                            } else if (platform == "ios") {
                                call.respond(HttpStatusCode.OK, "You can now close this window and return to the app.")
                            } else {
                                call.respond(HttpStatusCode.BadRequest, "Unknown platform")
                            }
                        } else {
                            call.respond(HttpStatusCode.InternalServerError, "Failed to validate YouTube access token")
                        }
                    } catch (e: Exception) {
                        call.application.environment.log.error("Error processing YouTube callback", e)
                        call.respond(HttpStatusCode.InternalServerError, "An error occurred while processing the request")
                    }
                }
                get("twitter"){
                    val token = call.parameters["token"] as String
                    val userInfo = userService.fetchUserDataByToken(token)!!
                    val userId = userInfo.id.toString()
                    val code = call.parameters["code"] ?: return@get call.respond(HttpStatusCode.BadRequest, "Missing code parameter")
                    socialController.fetchTwitterAccessToken(userId, code)
                    call.respond(200)
                }
            }
            get("test"){
                val token = call.parameters["token"] as String
                val userInfo = userService.fetchUserDataByToken(token)!!
                val userId = userInfo.id.toString()
                socialController.testYoutube(userId)
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
                val posted = socialController.schedulePost(userId, postTime, mediaByteArray, schedulePostRequest)
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
