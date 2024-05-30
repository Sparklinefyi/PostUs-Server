package postus.repositories
import org.jetbrains.exposed.sql.*
import postus.models.Users
import org.jetbrains.exposed.sql.transactions.transaction

data class User(
    val id: Int,
    val email: String,
    val name: String,
    val passwordHash: String,
    val googleRefresh: String?,
    val facebookRefresh: String?,
    val twitterRefresh: String?,
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
                it[googleRefresh] = user.googleRefresh
                it[facebookRefresh] = user.facebookRefresh
                it[twitterRefresh] = user.twitterRefresh
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
            passwordHash = row[Users.passwordHash],
            googleRefresh = row[Users.googleRefresh],
            facebookRefresh = row[Users.facebookRefresh],
            twitterRefresh = row[Users.twitterRefresh],
            instagramRefresh = row[Users.instagramRefresh]
        )
    }
}