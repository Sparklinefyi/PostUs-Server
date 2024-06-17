package postus.controllers

import okhttp3.OkHttpClient
import postus.models.SchedulePostRequest
import postus.models.ScheduledPost
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

import postus.workers.PostWorker
import postus.repositories.UserRepository

class SocialsController(client: OkHttpClient, userRepository: UserRepository, userController: UserController, val mediaController: MediaController) {
    val instagramController = InstagramController(client, userRepository, userController, mediaController)
    val youtubeController = YouTubeController(client, userRepository, userController, mediaController)
    val twitterController = TwitterController(client, userRepository, userController, mediaController)
    val linkedinController = LinkedInController(client, userRepository, userController, mediaController)
    val tiktokController = TikTokController(client, userRepository, userController, mediaController)

    fun schedulePost(userId: String, postTime: String, mediaUrl: ByteArray, schedulePostRequest: SchedulePostRequest): Boolean {
        val mediaType = schedulePostRequest.mediaType
        val s3Path: String
        when (mediaType) {
            "IMAGE" -> {
                s3Path = mediaController.uploadImage(userId, mediaUrl)
            }
            "VIDEO" -> {
                s3Path = mediaController.uploadVideo(userId, mediaUrl)
            }
            else -> {
                throw Exception("Not a supported media type (VIDEO or IMAGE)")
            }
        }
        val postTimeInstant: Instant = LocalDateTime.parse(postTime).toInstant(ZoneOffset.UTC)
        val delay = Duration.between(LocalDateTime.now().toInstant(ZoneOffset.UTC), postTimeInstant).toHours()
        if (delay < 3) {
            val post = ScheduledPost(
                0,
                userId.toInt(),
                s3Path,
                postTime,
                mediaType,
                schedulePostRequest,
                false
            )
            PostWorker(post, this).schedule()
            return true
        }
        val scheduled = ScheduleRepository.addSchedule(userId.toInt(), s3Path, postTime, mediaType, schedulePostRequest)
        return scheduled
    }
}