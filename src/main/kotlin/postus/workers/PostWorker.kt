package postus.workers

import postus.endpoints.MediaController
import postus.endpoints.SocialsController
import postus.models.ScheduledPost
import postus.repositories.UserRepository
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
        val userId = scheduledPost.userId
        val S3Url = scheduledPost.s3Path
        val mediaType = scheduledPost.mediaType
        val user = UserRepository().findById(userId.toInt()) ?: throw Exception("User not found while trying to upload scheduled post")
        val mediaUrl = if(mediaType == "IMAGE") {
            MediaController.getImage(userId, S3Url)
        } else {
            MediaController.getVideo(userId, S3Url)
        }
        for (provider in scheduledPost.schedulePostRequest.providers) {
            println("Posting to $provider with media at ${scheduledPost.s3Path}")
            when (provider) {
                "YOUTUBE" -> {
                    val accessToken = user.googleAccessToken!!
                    try {
                        SocialsController.uploadYoutubeShort(
                            scheduledPost.schedulePostRequest.youtubePostRequest!!,
                            accessToken,
                            mediaUrl
                        )
                    } catch (e: Exception) {
                        println("Failed  to post youtube video:  $e")
                    }
                }
                "INSTAGRAM" -> {
                    val accessToken = user.instagramAccessToken!!
                    val accountId = user.instagramAccountId!!
                    try {
                        if (mediaType == "IMAGE") {
                            SocialsController.uploadPictureToInstagram(
                                userId,
                                mediaUrl,
                                scheduledPost.schedulePostRequest.instagramPostRequest?.caption,
                            )
                        } else {
                            SocialsController.uploadVideoToInstagram(
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
        // Remove from the repository after posting
        ScheduleRepository.removePost(scheduledPost.id)
    }

    fun cancel() {
        future?.cancel(false)
    }
}
