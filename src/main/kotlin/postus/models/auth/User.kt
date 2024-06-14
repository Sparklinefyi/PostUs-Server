package postus.models.auth

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime.now

object Users : IntIdTable() {
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255)
    val passwordHash = varchar("password_hash", 255)
    val googleAccountId = varchar("google_account_id", 255).nullable()
    val googleAccessToken = varchar("google_access_token", 255).nullable()
    val googleRefresh = varchar("google_refresh", 255).nullable()
    val facebookAccountId = varchar("facebook_account_id", 255).nullable()
    val facebookAccessToken = varchar("facebook_access_token", 255).nullable()
    val facebookRefresh = varchar("facebook_refresh", 255).nullable()
    val twitterAccountId = varchar("twitter_account_id", 255).nullable()
    val twitterAccessToken = varchar("twitter_access_token", 255).nullable()
    val twitterRefresh = varchar("twitter_refresh", 255).nullable()
    val instagramAccountId = varchar("instagram_account_id", 255).nullable()
    val instagramAccessToken = varchar("instagram_access_token", 255).nullable()
    val instagramRefresh = varchar("instagram_refresh", 255).nullable()
    val timeCreated = datetime("time_created").default(now())
    val timeUpdated = datetime("time_updated").default(now())
    val role: Column<String> = varchar("role", 255).default("inactive")
    val description: Column<String> = text("description").default("")
}
