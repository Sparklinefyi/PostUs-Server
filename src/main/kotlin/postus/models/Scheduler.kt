package postus.models

import org.jetbrains.exposed.dao.id.IntIdTable

object Schedules : IntIdTable(
    name = "scheduled_posts"
) {
    val userId = integer("user_id")
    val s3Path = varchar("s3_path", 255)
    val postTime = varchar("post_time", 255)
    val mediaType = varchar("media_type", 20)
    val schedulePostRequest = text("schedule_post_request")
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