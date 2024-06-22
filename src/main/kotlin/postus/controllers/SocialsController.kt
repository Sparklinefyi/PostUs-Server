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
import java.time.format.DateTimeFormatter

class SocialsController(client: OkHttpClient, userRepository: UserRepository, userController: UserController, val mediaController: MediaController) {
    val instagramController = InstagramController(client, userRepository, userController, mediaController)
    val youtubeController = YouTubeController(client, userRepository, userController, mediaController)
    val twitterController = TwitterController(client, userRepository, userController, mediaController)
    val linkedinController = LinkedInController(client, userRepository, userController, mediaController)
    val tiktokController = TikTokController(client, userRepository, userController, mediaController)

    fun schedulePost(userId: Int, schedulePostRequest: SchedulePostRequest): Boolean {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val postTimeLocalDateTime: LocalDateTime = LocalDateTime.parse(schedulePostRequest.postTime, dateTimeFormatter)
        val postTimeInstant: Instant = postTimeLocalDateTime.toInstant(ZoneOffset.UTC)

        val currentInstant: Instant = Instant.now()
        println("Current Instant: $currentInstant") // Debugging statement
        println("Post Time Instant: $postTimeInstant") // Debugging statement

        val delay = Duration.between(currentInstant, postTimeInstant).toHours()
        if (delay < 3) {
            val post = ScheduledPost(
                0,
                userId,
                schedulePostRequest.contentUrl,
                schedulePostRequest.postTime,
                schedulePostRequest.mediaType,
                schedulePostRequest,
                false
            )
            PostWorker(post, this).schedule()
            return true
        }

        return ScheduleRepository.addSchedule(
            userId,
            schedulePostRequest.contentUrl,
            schedulePostRequest.postTime,
            schedulePostRequest.mediaType,
            schedulePostRequest
        )
    }
}