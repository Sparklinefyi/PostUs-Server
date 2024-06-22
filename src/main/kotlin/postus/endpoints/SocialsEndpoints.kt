package postus.endpoints

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import postus.controllers.SocialsController
import postus.controllers.UserController
import postus.models.SchedulePostRequest
import postus.models.auth.UserInfo
import postus.models.youtube.*
import postus.models.instagram.*

fun Application.configureSocialsRouting(userService: UserController, socialController: SocialsController) {

    val instagramController = socialController.instagramController
    val youtubeController = socialController.youtubeController
    val twitterController = socialController.twitterController
    val linkedinController = socialController.linkedinController
    val tiktokController = socialController.tiktokController

    routing {
        route("socials") {
            route("publish") {
                route("image") {
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
                        val userId = call.parameters["userId"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing userId"
                        )
                        val description = call.parameters["description"]
                        val videoUrl = call.parameters["videoUrl"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing videoUrl"
                        )
                        val response = tiktokController.postToTikTok(userId.toInt(), videoUrl, description)
                        call.respond(HttpStatusCode.OK, response)
                    }
                    post("instagram") {
                        val request = call.receive<InstagramPostRequest>()
                        val userInfo = userService.fetchUserDataByToken(request.postMetaData!!.token)
                            ?: throw IllegalArgumentException("Invalid token")

                        try {
                            val result = instagramController.uploadVideoToInstagram(
                                userInfo.id,
                                request.postMetaData!!.videoUrl,
                                request.caption
                            )
                            call.respond(HttpStatusCode.OK, result)
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, e.message ?: "An error occurred")
                        }
                    }
                    post("youtube") {
                        val request = call.receive<YoutubePostRequest>()
                        val userInfo = userService.fetchUserDataByToken(request.tokenAndVideoUrl!!.token)
                            ?: throw IllegalArgumentException("Invalid token")

                        try {
                            val result = youtubeController.uploadYoutubeShort(request, userInfo.id, request.tokenAndVideoUrl!!.videoUrl)

                            if (result!!.has("error")) {
                                val errorObject = result.getJSONObject("error")

                                // Check if the "reason" key exists in the "error" object
                                if (errorObject.has("reason") && errorObject.getString("reason") == "quotaExceeded") {
                                    call.respond(HttpStatusCode.Forbidden, mapOf("error" to "You have exceeded your YouTube API quota. Please try again later."))
                                } else {
                                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to errorObject.optString("message", "An error occurred")))
                                }
                            } else {
                                call.respond(HttpStatusCode.OK, mapOf("videoId" to "TempVideoId"))
                            }
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError, mapOf("error" to (e.message ?: "An error occurred")))
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
                    val videoIds = youtubeController.getLast50YoutubeVideos(userId) ?: return@get call.respond(
                        HttpStatusCode.InternalServerError,
                        "Error fetching videos"
                    )
                    val videoDetailsList = youtubeController.getYouTubeVideoDetails(videoIds)
                    if (videoDetailsList.any { it == null }) {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve some video details")
                    } else {
                        call.respond(HttpStatusCode.OK, videoDetailsList)
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
                get("tiktok/page"){
                    val token = call.request.headers["Authorization"]
                        ?.removePrefix("Bearer ")
                        ?.trim('"')
                        ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing or invalid Authorization header"
                        )
                    val userInfo = userService.fetchUserDataByToken(token)!!
                    val userId = userInfo.id.toString()
                    val analytics = tiktokController.getTikTokChannelAnalytics(userId.toInt())
                    if (analytics == null) {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve TikTok Channel Analytics")
                    } else {
                        call.respond(analytics)
                    }
                }
                get("tiktok/post"){
                    val token = call.request.headers["Authorization"]
                        ?.removePrefix("Bearer ")
                        ?.trim('"')
                        ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            "Missing or invalid Authorization header"
                        )
                    val userInfo = userService.fetchUserDataByToken(token)!!
                    val userId = userInfo.id.toString()
                    val videoId = call.parameters["videoId"] ?: return@get call.respond(
                        HttpStatusCode.BadRequest,
                        "Missing videoId parameter"
                    )
                    val analytics = tiktokController.getTikTokPostAnalytics(userId.toInt(), videoId)
                    if (analytics == null) {
                        call.respond(HttpStatusCode.InternalServerError, "Failed to retrieve TikTok Post Analytics")
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
                        val code = parameters["code"]

                        val info = processRequest(call, userService) ?: return@get
                        val validated = instagramController.getLongLivedAccessTokenAndInstagramBusinessAccountId(
                            info.second.id,
                            code!!
                        )
                        oAuthResponse(call, validated, info.first)
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
                        val code = parameters["code"]

                        val info = processRequest(call, userService) ?: return@get
                        val validated = youtubeController.fetchYouTubeAccessToken(info.second.id, code!!)
                        oAuthResponse(call, validated, info.first)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "An error occurred while processing the request"
                        )
                    }
                }
                get("twitter") {
                    try {
                        val parameters = call.request.queryParameters
                        val code = parameters["code"]

                        val info = processRequest(call, userService) ?: return@get
                        val validated = twitterController.fetchTwitterAccessToken(info.second.id, code!!, info.third!!)
                        oAuthResponse(call, validated, info.first)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "An error occurred while processing the request"
                        )
                    }
                }
                get("linkedin") {
                    try {
                        val parameters = call.request.queryParameters
                        val code = parameters["code"]

                        val info = processRequest(call, userService) ?: return@get
                        val validated = linkedinController.fetchLinkedInAccessToken(info.second.id, code!!)
                        oAuthResponse(call, validated, info.first)
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            "An error occurred while processing the request"
                        )
                    }
                }
            }
            post("schedule") {

                val gson: Gson = GsonBuilder().create()

                val jsonString = call.receiveText()
                val request = gson.fromJson(jsonString, SchedulePostRequest::class.java)

                val userInfo = userService.fetchUserDataByToken(request.token)
                    ?: throw IllegalArgumentException("Invalid token")

                val posted = socialController.schedulePost(userInfo.id, request)
                if (posted) {
                    call.respond(HttpStatusCode.OK, posted)
                } else {
                    call.respond(HttpStatusCode.InternalServerError, "Post could not be scheduled")
                }
                call.respond(request)
            }
        }
    }
}

suspend fun oAuthResponse(call: ApplicationCall, validated: Boolean?, platform: String?) {
    if (validated!!) {
        if (platform == "web") {
            call.respondRedirect(System.getProperty("FRONTEND_REDIRECT"))
        } else if (platform == "ios") {
            call.respond(HttpStatusCode.OK, "You can now close this window and return to the app.")
        } else {
            call.respond(HttpStatusCode.BadRequest, "Unknown platform")
        }
    } else {
        call.respond(HttpStatusCode.InternalServerError, "Failed to validate YouTube access token")
    }
}

suspend fun processRequest(call: ApplicationCall, userService: UserController): Triple<String, UserInfo, String?> {
    val parameters = call.request.queryParameters
    val state = parameters["state"]
    val code = parameters["code"]

    if (state == null || code == null) {
        call.respond(HttpStatusCode.BadRequest, "Missing required parameters")
        return Triple("", UserInfo(0, "", "", "", "", ""), "")
    }

    val info = userService.fetchUserDataByTokenWithPlatform(state)
    return info
}
