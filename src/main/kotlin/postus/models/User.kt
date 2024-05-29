package postus.models

import org.jetbrains.exposed.dao.id.IntIdTable

object Users : IntIdTable() {
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255).nullable()
    val passwordHash = varchar("password_hash", 255)
    val googleRefresh = varchar("google_refresh", 255).nullable()
    val facebookRefresh = varchar("facebook_refresh", 255).nullable()
    val twitterRefresh = varchar("twitter_refresh", 255).nullable()
    val instagramRefresh = varchar("instagram_refresh", 255).nullable()
}
