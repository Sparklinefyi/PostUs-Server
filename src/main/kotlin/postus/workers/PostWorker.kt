package postus.workers

import postus.endpoints.MediaController
import postus.endpoints.SocialsController
import postus.models.ScheduledPost
import postus.repositories.UserRepository
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class PostWorker(private val scheduledPost: ScheduledPost) {
    private val scheduler = Executors.newScheduledThreadPool(1)
    private var future: ScheduledFuture<*>? = null

    fun schedule() {
        val delay = Duration.between(LocalDateTime.now(), scheduledPost.postTime).toMillis()
        future = scheduler.schedule({
            post()
        }, delay, TimeUnit.MILLISECONDS)
    }

    private fun post() {
        val userId = scheduledPost.userId
        val S3Url = scheduledPost.s3Path
        val user = UserRepository().findById(userId.toInt())
        var mediaUrl : String
        if(scheduledPost.mediaType == "IMAGE") {
            mediaUrl = MediaController.getImage(userId, S3Url)
        } else {
            mediaUrl = MediaController.getVideo(userId, S3Url)
        }
        for (provider in scheduledPost.providers) {
            println("Posting to $provider with media at ${scheduledPost.s3Path}")
            when (provider) {
                "YOUTUBE" -> {
                    val accessToken = user.youtubeAccessToken
                    SocialsController.uploadYoutubeShort(scheduledPost.schedulePostRequest.youtubePostRequest!!, accessToken, mediaUrl)
                }
                "INSTAGRAM" -> {

                }
            }
        }
        // Remove from the repository after posting
        ScheduleRepository.removePost(scheduledPost.id)
    }

    fun cancel() {
        future?.cancel(false)
    }
}
