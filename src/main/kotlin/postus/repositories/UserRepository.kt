package postus.repositories
import org.jetbrains.exposed.sql.*
import postus.models.Users
import org.jetbrains.exposed.sql.transactions.transaction
import postus.models.Users.nullable
import postus.models.Users.varchar

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
    val instagramRefresh: String?
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


    fun save(user: User): User {
        return transaction {
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
        }
    }

    private fun toUser(row: ResultRow): User {
        return User(
            id = row[Users.id].value,
            email = row[Users.email],
            name = row[Users.name],
            passwordHash = row[Users.passwordHash],
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
            instagramRefresh = row[Users.instagramRefresh]
        )
    }
}