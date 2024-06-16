package postus.endpoints

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import postus.controllers.SocialsController
import postus.controllers.UserController
import postus.models.SchedulePostRequest
import postus.models.auth.OAuthLoginRequest
import postus.models.instagram.InstagramPostRequest
import postus.models.youtube.YoutubePostRequest
import postus.models.youtube.YoutubeUploadRequest

fun Application.configureSocialsRouting(userService: UserController, socialController: SocialsController) {

    val instagramController = socialController.instagramController
    val youtubeController = socialController.youtubeController
    val twitterController = socialController.twitterController
    val linkedinController = socialController.linkedinController

    routing {
        route("socials") {
            route("publish") {
                route("image") {
                    post("tiktok") {

                    }
                    post("instagram") {
                        val token = call.parameters["token"] as String
                        val userInfo = userService.fetchUserDataByToken(token)!!
                        val imageUrl = call.parameters["imageUrl"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing image"
                        )
                        val caption = call.parameters["caption"]
                        try {
                            val result = instagramController.uploadPictureToInstagram(userInfo.id, imageUrl, caption)
                            call.respond(HttpStatusCode.OK, result)
                        } catch (e: Exception) {
                            call.respond(e)
                        }
                    }
                }
                route("video") {
                    post("tiktok") {

                    }
                    post("instagram") {
                        val request = call.receive<InstagramPostRequest>()
                        val userInfo = userService.fetchUserDataByToken(request.token)
                            ?: throw IllegalArgumentException("Invalid token")

                        try {
                            val result = instagramController.uploadVideoToInstagram(
                                userInfo.id,
                                request.videoUrl,
                                request.caption
                            )
                            call.respond(HttpStatusCode.OK, result)
                        } catch (e: Exception) {
                            call.respond(e)
                        }
                    }
                    post("youtube") {
                        val request = call.receive<YoutubePostRequest>()
                        val userInfo = userService.fetchUserDataByToken(request.token)
                            ?: throw IllegalArgumentException("Invalid token")

                        val uploadRequest = YoutubeUploadRequest(request.snippet, request.status)
                        try {
                            val result =
                                youtubeController.uploadYoutubeShort(uploadRequest, userInfo.id, request.videoUrl)
                            call.respond(HttpStatusCode.OK)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, e.message ?: "An error occurred")
                        }
                    }
                }
                post("twitter") {
                    val token = call.parameters["token"] as String
                    val userInfo = userService.fetchUserDataByToken(token)!!
                    val userId = userInfo.id.toString()
                    val imageUrl = call.parameters["imageUrl"]
                    val videoUrl = call.parameters["videoUrl"]
                    val text = call.parameters["text"]
                    val response = twitterController.postToTwitter(userId, text, imageUrl, videoUrl)
                    call.respond(HttpStatusCode.OK, response)
                }
                post("linkedin") {
                    val userId = call.parameters["userId"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing userId"
                    )
                    val content = call.parameters["content"] ?: return@post call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing content"
                    )
                    val mediaUrls = call.parameters["mediaUrls"]?.split(",") ?: emptyList()
                    val response = linkedinController.postToLinkedIn(userId.toInt(), content, mediaUrls)
                    call.respond(HttpStatusCode.OK, response)
                }
            }
            route("analyze") {
                get("youtube/page") {
                    val token = call.request.headers["Authorization"]
                        ?.removePrefix("Bearer ")
                        ?.trim('"')
                        ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing or invalid Authorization header"
                        )
                    val userInfo = userService.fetchUserDataByToken(token) ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid token"
                    )

                    val userId = userInfo.id.toString()
                    val analytics = youtubeController.getYouTubeChannelAnalytics(userId)
                    if (analytics == null) {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve YouTube Channel Analytics")
                    } else {
                        call.respond(HttpStatusCode.OK, analytics)
                    }
                }

                get("youtube/post") {
                    val token = call.request.headers["Authorization"]
                        ?.removePrefix("Bearer ")
                        ?.trim('"')
                        ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing or invalid Authorization header"
                        )
                    val userInfo = userService.fetchUserDataByToken(token) ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Invalid token"
                    )

                    val userId = userInfo.id.toString()
                    val videoIds = youtubeController.getLast10YouTubeVideos(userId) ?: return@get call.respond(
                        HttpStatusCode.InternalServerError,
                        "Error fetching videos"
                    )

                    val videoDetailsList = youtubeController.getYouTubeVideoDetails(videoIds)
                    if (videoDetailsList.any { it == null }) {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve some video details")
                    } else {
                        val videoDetails = videoDetailsList.filterNotNull().map { detail ->
                            Json.parseToJsonElement(detail).jsonObject["items"]?.jsonArray?.firstOrNull()?.jsonObject
                        }
                        call.respond(HttpStatusCode.OK, videoDetails)
                    }
                }

                get("instagram/page") {
                    val token = call.request.headers["Authorization"]
                        ?.removePrefix("Bearer ")
                        ?.trim('"')
                        ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing or invalid Authorization header"
                        )

                    val userInfo = userService.fetchUserDataByToken(token)!!
                    val userId = userInfo.id.toString()
                    val analytics = instagramController.getInstagramPageAnalytics(userId)
                    if (analytics == null) {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve Instagram Page Analytics")
                    } else {
                        call.respond(analytics)
                    }
                }
                get("instagram/post") {
                    val token = call.request.headers["Authorization"]
                        ?.removePrefix("Bearer ")
                        ?.trim('"')
                        ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing or invalid Authorization header"
                        )
                    val userInfo = userService.fetchUserDataByToken(token)!!
                    val userId = userInfo.id.toString()
                    val postId = call.parameters["postId"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing postId parameter"
                    )

                    val analytics = instagramController.getInstagramPostAnalytics(userId, postId)
                    if (analytics == null) {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve Instagram Post Analytics")
                    } else {
                        call.respond(analytics)
                    }
                }
            }
            route("retrieve") {
                get("instagram") {
                    val token = call.request.headers["Authorization"]
                        ?.removePrefix("Bearer ")
                        ?.trim('"')
                        ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing or invalid Authorization header"
                        )
                    val userInfo = userService.fetchUserDataByToken(token)!!
                    val userId = userInfo.id.toString()
                    val postId = call.parameters["postId"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing postId parameter"
                    )
                    val url = instagramController.getInstagramMediaDetails(userId, postId)
                    call.respond(url)
                }
            }
            route("auth") {
                get("instagram") {
                    try {
                        val parameters = call.request.queryParameters
                        val state = parameters["state"]
                        val code = parameters["code"]

                        if (state == null || code == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing required parameters")
                            return@get
                        }

                        val info = userService.fetchUserDataByTokenWithPlatform(state)
                        val platform = info.first
                        val user = info.second

                        instagramController.getLongLivedAccessTokenAndInstagramBusinessAccountId(user.id, code)

                        if (platform == "web") {
                            val frontendRedirect = System.getProperty("FRONTEND_REDIRECT") ?: "/"
                            call.respondRedirect(frontendRedirect)
                        } else if (platform == "ios") {
                            // For iOS, you might need to use a custom scheme to notify the app
                            call.respond(HttpStatusCode.OK, "You can now close this window and return to the app.")
                        }
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "An error occurred while processing the request"
                        )
                    }
                }
                get("youtube") {
                    try {
                        val parameters = call.request.queryParameters
                        val state = parameters["state"]
                        val code = parameters["code"]

                        if (state == null || code == null) {
                            call.respond(HttpStatusCode.BadRequest, "Missing required parameters")
                            return@get
                        }

                        val info = userService.fetchUserDataByTokenWithPlatform(state)
                        val platform = info.first
                        val user = info.second

                        val validated = youtubeController.fetchYouTubeAccessToken(user.id, code)
                        if (validated) {
                            if (platform == "web") {
                                call.respondRedirect(System.getProperty("FRONTEND_REDIRECT") ?: "/")
                            } else if (platform == "ios") {
                                call.respond(HttpStatusCode.OK, "You can now close this window and return to the app.")
                            } else {
                                call.respond(HttpStatusCode.BadRequest, "Unknown platform")
                            }
                        } else {
                            call.respond(HttpStatusCode.InternalServerError, "Failed to validate YouTube access token")
                        }
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "An error occurred while processing the request"
                        )
                    }
                }
                get("twitter") {
                    val token = call.parameters["token"] as String
                    val userInfo = userService.fetchUserDataByToken(token)!!
                    val userId = userInfo.id.toString()
                    val code = call.parameters["code"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing code parameter"
                    )
                    twitterController.fetchTwitterAccessToken(userId, code)

                    call.respond(200)
                }
                get("linkedin") {
                    val userId = call.parameters["userId"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing userId"
                    )
                    val code = call.parameters["code"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing code parameter"
                    )
                    val token = linkedinController.getLinkedInAccessToken(userId.toInt(), code)

                    call.respond(200)
                }
            }
            get("test") {
                val token = call.parameters["token"] as String
                val userInfo = userService.fetchUserDataByToken(token)!!
                val userId = userInfo.id.toString()
                youtubeController.testYoutube(userId)
                call.respond(200)
            }
            post("schedule") {
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
                if (json == null || fileBytes == null) {
                    return@post call.respond(HttpStatusCode.BadRequest, "Missing scheduledetails or media")
                }
                val userId = call.parameters["userId"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing accessToken parameter"
                )
                val postTime = call.parameters["postTime"] ?: return@post call.respond(
                    HttpStatusCode.BadRequest,
                    "Missing accessToken parameter"
                )
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
