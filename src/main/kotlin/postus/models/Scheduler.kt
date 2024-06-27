package postus.models

import kotlinx.serialization.Serializable
import org.jetbrains.exposed.dao.id.IntIdTable

object Schedules : IntIdTable(
    "\"ScheduledPosts\""
) {
    val userId = integer("userId")
    val s3Path = varchar("s3Path", 255)
    val postTime = varchar("postTime", 255)
    val mediaType = varchar("mediaType", 20)
    val schedulePostRequest = text("schedulePostRequest")
    val posted = bool("posted")
}

data class ScheduledPost(
    val id: Int,
    val userId: Int,
    val s3Path: String,
    val postTime: String,
    val mediaType: String,
    val schedulePostRequest: SchedulePostRequest,
    val posted: Boolean
)

@Serializable
data class TokenAndVideoUrl(
    val token: String,
    val videoUrl: String
)
