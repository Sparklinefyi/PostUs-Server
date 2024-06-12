package postus.workers

import postus.endpoints.mediaController
import postus.endpoints.socialController
import postus.models.ScheduledPost
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class PostWorker(private val scheduledPost: ScheduledPost) {
    private val scheduler = Executors.newScheduledThreadPool(1)
    private var future: ScheduledFuture<*>? = null

    fun schedule() {
        val postTime: Instant = LocalDateTime.parse(scheduledPost.postTime).toInstant(ZoneOffset.UTC)
        val delay = Duration.between(LocalDateTime.now().toInstant(ZoneOffset.UTC), postTime).toMillis()
        future = scheduler.schedule({
            post()
        }, delay, TimeUnit.MILLISECONDS)
    }

    private fun post() {
        val userId = scheduledPost.userId.toString()
        val S3Path = scheduledPost.s3Path
        val mediaType = scheduledPost.mediaType
        for (provider in scheduledPost.schedulePostRequest.providers) {
            println("Posting to $provider with media at ${scheduledPost.s3Path}")
            when (provider) {
                "YOUTUBE" -> {
                    try {
                        val mediaUrl = MediaController.getPresignedUrlFromPath(S3Path)
                        SocialsController.uploadYoutubeShort(
                            scheduledPost.schedulePostRequest.youtubePostRequest!!,
                            userId,
                            mediaUrl
                        )
                    } catch (e: Exception) {
                        println("Failed  to post youtube video:  $e")
                    }
                }
                "INSTAGRAM" -> {
                    try {
                        val mediaUrl = MediaController.getPresignedUrlFromPath(S3Path)
                        if (mediaType == "IMAGE") {
                            socialController.uploadPictureToInstagram(
                                userId,
                                mediaUrl,
                                scheduledPost.schedulePostRequest.instagramPostRequest?.caption,
                            )
                        } else {
                            socialController.uploadVideoToInstagram(
                                userId,
                                mediaUrl,
                                scheduledPost.schedulePostRequest.instagramPostRequest?.caption,
                            )
                        }
                    } catch (e: Exception) {
                        println("Failed to post to instagram:  $e")
                    }
                }
            }
        }
        ScheduleRepository.removePost(scheduledPost.id)
    }

    fun cancel() {
        future?.cancel(false)
    }
}
