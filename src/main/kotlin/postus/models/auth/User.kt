package postus.models.auth

import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime.now
import kotlinx.serialization.Serializable

object Users : IntIdTable() {
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255)
    val passwordHash = varchar("password_hash", 255)
    val role: Column<String> = varchar("role", 255).default("inactive")
    val description: Column<String> = text("description").default("")
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
    val linkedinAccountId = varchar("linkedin_account_id", 255).nullable()
    val linkedinAccessToken = varchar("linkedin_access_token", 355).nullable()
    val linkedinRefresh = varchar("linkedin_refresh", 255).nullable()
    val tiktokAccountId = varchar("tiktok_account_id", 255).nullable()
    val tiktokAccessToken = varchar("tiktok_access_token", 255).nullable()
    val tiktokRefresh = varchar("tiktok_refresh", 255).nullable()
    val timeCreated = datetime("time_created").default(now())
    val timeUpdated = datetime("time_updated").default(now())
}

@Serializable
data class UserInfo(
    val id: Int,
    val email: String,
    val name: String,
    val role: String,
    val createdAt: String,
    val description: String = "",
    val token: String = "",
    val googleAccountId: String = "",
    val facebookAccountId: String = "",
    val twitterAccountId: String = "",
    val instagramAccountId: String = "",
    val linkedInAccountId: String = "",
)

@Serializable
data class User(
    val id: Int,
    val email: String,
    val name: String,
    val passwordHash: String,
    val googleAccountId: String?,
    val googleAccessToken: String?,
    val googleRefresh: String?,
    val facebookAccountId: String?,
    val facebookAccessToken: String?,
    val facebookRefresh: String?,
    val twitterAccountId: String?,
    val twitterAccessToken: String?,
    val twitterRefresh: String?,
    val instagramAccountId: String?,
    val instagramAccessToken: String?,
    val instagramRefresh: String?,
    val linkedinAccountId: String?,
    val linkedinAccessToken: String?,
    val linkedinRefresh: String?,
    val tiktokAccountId: String?,
    val tiktokAccessToken: String?,
    val tiktokRefresh: String?,
    val createdAt: String,
    val updatedAt: String,
    val role: String,
    val description: String
)
