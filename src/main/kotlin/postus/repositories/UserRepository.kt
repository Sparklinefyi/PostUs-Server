package postus.repositories
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import postus.models.Users
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import postus.models.Users.nullable
import postus.models.Users.varchar

@Serializable
data class UserInfo(
    val id: Int,
    val email: String,
    val name: String,
    val role: String,
    val createdAt: String,
    val description: String = "",
    val token: String = ""
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


class UserRepository {
    fun findByEmail(email: String): User? {
        return transaction {
            Users.selectAll().where { Users.email eq email }
                .map { toUser(it) }
                .singleOrNull()
        }
    }

    fun findById(id: Int): User? {
        return transaction {
            Users.selectAll().where { Users.id eq id }
                .map { toUser(it) }
                .singleOrNull()
        }
    }


    fun save(user: User): UserInfo {
        val savedUser = transaction {
            val existingUser = Users.selectAll().where { Users.email eq user.email }.singleOrNull()
            if (existingUser != null) {
                throw IllegalArgumentException("User with email ${user.email} already exists")
            }

            val id = Users.insertAndGetId {
                it[email] = user.email
                it[name] = user.name
                it[passwordHash] = user.passwordHash
                it[googleAccountId] = user.googleAccountId
                it[googleAccessToken] = user.googleAccessToken
                it[googleRefresh] = user.googleRefresh
                it[googleAccountId] = user.googleAccountId
                it[googleAccessToken] = user.googleAccessToken
                it[facebookRefresh] = user.facebookRefresh
                it[googleAccountId] = user.googleAccountId
                it[googleAccessToken] = user.googleAccessToken
                it[twitterRefresh] = user.twitterRefresh
                it[googleAccountId] = user.googleAccountId
                it[googleAccessToken] = user.googleAccessToken
                it[instagramRefresh] = user.instagramRefresh
            }
            user.copy(id = id.value)
        }

        return UserInfo(savedUser.id, savedUser.email, savedUser.name, savedUser.role, savedUser.description)
    }

    fun update(user: User) {
        transaction {
            Users.update({ Users.id eq user.id }) {
                it[email] = user.email
                it[name] = user.name
                it[passwordHash] = user.passwordHash
                it[googleAccountId] = user.googleAccountId
                it[googleAccessToken] = user.googleAccessToken
                it[googleRefresh] = user.googleRefresh
                it[facebookAccountId] = user.facebookAccountId
                it[facebookAccessToken] = user.facebookAccessToken
                it[facebookRefresh] = user.facebookRefresh
                it[twitterAccountId] = user.twitterAccountId
                it[twitterAccessToken] = user.twitterAccessToken
                it[twitterRefresh] = user.twitterRefresh
                it[instagramAccountId] = user.instagramAccountId
                it[instagramAccessToken] = user.instagramAccessToken
                it[instagramRefresh] = user.instagramRefresh
                it[linkedinAccountId] = user.linkedinAccountId
                it[linkedinAccessToken] = user.linkedinAccessToken
                it[linkedinRefresh] = user.linkedinRefresh
                it[tiktokAccountId] = user.tiktokAccountId
                it[tiktokAccessToken] = user.tiktokAccessToken
                it[tiktokRefresh] = user.tiktokRefresh
            }
        }
    }

    suspend fun updateUserDescription(userId: Int, description: String) {
        transaction {
            Users.update({ Users.id eq userId }) {
                it[Users.description] = description
            }
        }
    }

    private fun toUser(row: ResultRow): User {
        return User(
            id = row[Users.id].value,
            email = row[Users.email],
            name = row[Users.name],
            role = row[Users.role],
            description = row[Users.description],
            passwordHash = row[Users.passwordHash],
            createdAt = row[Users.timeCreated].toString(),
            updatedAt = row[Users.timeUpdated].toString(),
            googleAccountId = row[Users.googleAccountId],
            googleAccessToken = row[Users.googleAccessToken],
            googleRefresh = row[Users.googleRefresh],
            facebookAccountId = row[Users.facebookAccountId],
            facebookAccessToken = row[Users.facebookAccessToken],
            facebookRefresh = row[Users.facebookRefresh],
            twitterAccountId = row[Users.twitterAccountId],
            twitterAccessToken = row[Users.twitterAccessToken],
            twitterRefresh = row[Users.twitterRefresh],
            instagramAccountId = row[Users.instagramAccountId],
            instagramAccessToken = row[Users.instagramAccessToken],
            instagramRefresh = row[Users.instagramRefresh],
            linkedinAccountId = row[Users.linkedinAccountId],
            linkedinAccessToken = row[Users.linkedinAccessToken],
            linkedinRefresh = row[Users.linkedinRefresh],
            tiktokAccountId = row[Users.tiktokAccountId],
            tiktokAccessToken = row[Users.tiktokAccessToken],
            tiktokRefresh = row[Users.tiktokRefresh]
        )
    }
}