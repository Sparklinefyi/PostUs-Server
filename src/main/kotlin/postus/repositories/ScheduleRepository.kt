import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import postus.models.SchedulePostRequest
import postus.models.ScheduledPost
import postus.models.Schedules
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ScheduleRepository {

    private val jsonFormat = Json { encodeDefaults = true }

    private fun toScheduledPost(row: ResultRow): ScheduledPost {
        return ScheduledPost(
            id = row[Schedules.id].value,
            userId = row[Schedules.userId],
            s3Path = row[Schedules.s3Path],
            postTime = row[Schedules.postTime],
            mediaType = row[Schedules.mediaType],
            schedulePostRequest = jsonFormat.decodeFromString(row[Schedules.schedulePostRequest]),
            posted = row[Schedules.posted]
        )
    }

    fun addSchedule(
        userId: Int,
        s3Path: String,
        postTime: String,
        mediaType: String,
        schedulePostRequest: SchedulePostRequest
    ): Boolean {
        return transaction {
            Schedules.insert {
                it[Schedules.userId] = userId
                it[Schedules.s3Path] = s3Path
                it[Schedules.postTime] = postTime
                it[Schedules.mediaType] = mediaType
                it[Schedules.schedulePostRequest] = jsonFormat.encodeToString(schedulePostRequest)
                it[posted] = false
            }
            true
        }
    }

    fun findById(id: Int): ScheduledPost? {
        return transaction {
            Schedules.selectAll().where { Schedules.id eq id }
                .mapNotNull { toScheduledPost(it) }
                .singleOrNull()
        }
    }

    fun getPostsScheduledWithinNextHours(hours: Long): List<ScheduledPost> {
        val now = LocalDateTime.now()
        val formatter = DateTimeFormatter.ISO_DATE_TIME
        return transaction {
            Schedules.selectAll().where {
                not(Schedules.posted) and
                        Schedules.postTime.less(now.plusHours(hours).format(formatter))
            }.map { toScheduledPost(it) }
        }
    }

    fun removePost(id: Int): Boolean {
        transaction {
            Schedules.update({ Schedules.id eq id }) {
                it[posted] = true
            }
        }
        return true
    }
}
