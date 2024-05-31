import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import postus.models.ScheduledPost
import postus.models.Schedules
import java.time.LocalDateTime

object ScheduleRepository {

    private fun toScheduledPost(row: ResultRow): ScheduledPost {
        return ScheduledPost(
            id = row[Schedules.id],
            userId = row[Schedules.userId],
            s3Path = row[Schedules.s3Path],
            postTime = row[Schedules.postTime],
            mediaType = row[Schedules.mediaType],
            providers = row[Schedules.providers].split(",")
        )
    }

    fun addSchedule(
        userId: String,
        s3Path: String,
        postTime: String,
        mediaType: String,
        providers: List<String>
    ): Boolean {
        return transaction {
            Schedules.insert {
                it[Schedules.userId] = userId
                it[Schedules.s3Path] = s3Path
                it[Schedules.postTime] = postTime
                it[Schedules.mediaType] = mediaType
                it[Schedules.providers] = providers.joinToString(",")
            }
            true
        }
    }

    fun findById(id: Int): ScheduledPost? {
        return transaction {
            Schedules.select { Schedules.id eq id }
                .mapNotNull { toScheduledPost(it) }
                .singleOrNull()
        }
    }

    fun getPostsScheduledWithinNextHours(hours: Long): List<ScheduledPost> {
        val now = LocalDateTime.now()
        return transaction {
            Schedules.select {
                Schedules.postTime.greater(now) and
                        Schedules.postTime.less(now.plusHours(hours))
            }.map { toScheduledPost(it) }
        }
    }

    fun removePost(id: Int): Boolean {
        return transaction {
            Schedules.deleteWhere { Schedules.id eq id } > 0
        }
    }
}
