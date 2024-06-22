package postus.workers

import ScheduleRepository
import postus.controllers.SocialsController
import postus.models.ScheduledPost
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class PostWorker(private val scheduledPost: ScheduledPost, private val socialController: SocialsController) {
    private val scheduler = Executors.newScheduledThreadPool(1)
    private var future: ScheduledFuture<*>? = null

    fun schedule() {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val postTimeLocalDateTime: LocalDateTime = LocalDateTime.parse(scheduledPost.postTime, dateTimeFormatter)
        val postTimeInstant: Instant = postTimeLocalDateTime.toInstant(ZoneOffset.UTC)

        val currentInstant: Instant = Instant.now()
        println("Current Instant: $currentInstant") // Debugging statement
        println("Post Time Instant: $postTimeInstant") // Debugging statement

        val delay = Duration.between(currentInstant, postTimeInstant).toHours()
        future = scheduler.schedule({
            post()
        }, delay, TimeUnit.MILLISECONDS)
    }

    private fun post() {
        val userId = scheduledPost.userId
        val S3Path = scheduledPost.s3Path
        val mediaType = scheduledPost.mediaType
        for (provider in scheduledPost.schedulePostRequest.providers) {
            println("Posting to $provider with media at ${scheduledPost.s3Path}")
            when (provider) {
                "YOUTUBE" -> {
                    try {
                        val mediaUrl = socialController.mediaController.getPresignedUrlFromPath(S3Path)
                        socialController.youtubeController.uploadYoutubeShort(
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
                        val mediaUrl = socialController.mediaController.getPresignedUrlFromPath(S3Path)
                        if (mediaType == "IMAGE") {
                            socialController.instagramController.uploadPictureToInstagram(
                                userId,
                                mediaUrl,
                                scheduledPost.schedulePostRequest.instagramPostRequest!!.caption,
                            )
                        } else {
                            socialController.instagramController.uploadVideoToInstagram(
                                userId,
                                mediaUrl,
                                scheduledPost.schedulePostRequest.instagramPostRequest!!.caption,
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
