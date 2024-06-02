package postus.models

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime.now

object Users : IntIdTable() {

    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255)
    val passwordHash = varchar("password_hash", 255)
    val googleRefresh = varchar("google_refresh", 255).nullable()
    val facebookRefresh = varchar("facebook_refresh", 255).nullable()
    val twitterRefresh = varchar("twitter_refresh", 255).nullable()
    val instagramRefresh = varchar("instagram_refresh", 255).nullable()
    val timeCreated = datetime("time_created").default(now())
    val timeUpdated = datetime("time_updated").default(now())
    val role: Column<String> = varchar("role", 255).default("inactive")
    val description: Column<String> = text("description").default("")
}
