package postus.controllers.Social

import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import postus.controllers.MediaController
import postus.controllers.UserController
import postus.models.SchedulePostRequest
import postus.models.ScheduledPost
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import postus.workers.PostWorker
import postus.repositories.UserRepository

class SocialsController(
    client: OkHttpClient,
    userRepository: UserRepository,
    userController: UserController,
    val mediaController: MediaController
) {
    val instagramController = InstagramController(client, userRepository, userController, mediaController)
    val youtubeController = YouTubeController(client, userRepository, userController, mediaController)
    val twitterController = TwitterController(client, userRepository, userController, mediaController)
    val linkedinController = LinkedInController(client, userRepository, userController, mediaController)
    val tiktokController = TikTokController(client, userRepository, userController, mediaController)

    suspend fun schedulePost(userId: Int, schedulePostRequest: SchedulePostRequest): Boolean {
        return withContext(Dispatchers.IO) {
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
                PostWorker(post, this@SocialsController).schedule()
                true
            } else {
                ScheduleRepository.addSchedule(
                    userId,
                    schedulePostRequest.contentUrl,
                    schedulePostRequest.postTime,
                    schedulePostRequest.mediaType,
                    schedulePostRequest
                )
            }
        }
    }
}
