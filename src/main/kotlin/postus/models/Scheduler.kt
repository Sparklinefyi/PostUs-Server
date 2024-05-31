package postus.models

import org.jetbrains.exposed.sql.Table
import java.time.LocalDateTime

object Schedules : Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val userId = varchar("user_id", 50)
    val s3Path = varchar("s3_path", 255)
    val postTime = varchar("post_time", 255)
    val mediaType = varchar("media_type", 20)
    val providers = varchar("providers", 255)  // JSON or comma-separated list of providers
}

data class ScheduledPost(
    val id: Int,
    val userId: String,
    val s3Path: String,
    val postTime: String,
    val mediaType: String,
    val providers: List<String>
)