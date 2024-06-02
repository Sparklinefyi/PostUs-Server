package postus.repositories
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.*
import postus.models.Users
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime


@Serializable
data class User(
    val id: Int,
    val email: String,
    val name: String,
    val passwordHash: String,
    val googleRefresh: String?,
    val facebookRefresh: String?,
    val twitterRefresh: String?,
    val instagramRefresh: String?,
    val createdAt: String,
    val updatedAt: String,
    val role: String,
    val description: String
)

@Serializable
data class UserInfo(
    val id: Int,
    val email: String,
    val name: String,
    val role: String = "inactive",
    val description: String = "",
    val token: String = ""
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
                it[googleRefresh] = user.googleRefresh
                it[facebookRefresh] = user.facebookRefresh
                it[twitterRefresh] = user.twitterRefresh
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
                it[googleRefresh] = user.googleRefresh
                it[facebookRefresh] = user.facebookRefresh
                it[twitterRefresh] = user.twitterRefresh
                it[instagramRefresh] = user.instagramRefresh
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

            googleRefresh = row[Users.googleRefresh],
            facebookRefresh = row[Users.facebookRefresh],
            twitterRefresh = row[Users.twitterRefresh],
            instagramRefresh = row[Users.instagramRefresh],
        )
    }

    private fun userInfo(user: User): UserInfo {
        return UserInfo(
            id = user.id,
            email = user.email,
            name = user.name,
            role = user.role,
            description = user.description,
        )
    }
}