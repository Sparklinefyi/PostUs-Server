package postus.repositories
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import postus.models.Users
import org.jetbrains.exposed.sql.transactions.transaction

data class User(val id: Int, val email: String, val name: String?, val passwordHash: String)

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
            val id = Users.insertAndGetId {
                it[email] = user.email
                it[name] = user.name
                it[passwordHash] = user.passwordHash
            }
            user.copy(id = id.value)
        }
    }

    private fun toUser(row: ResultRow): User {
        return User(
            id = row[Users.id].value,
            email = row[Users.email],
            name = row[Users.name],
            passwordHash = row[Users.passwordHash]
        )
    }
}
