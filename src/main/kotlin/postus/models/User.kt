package postus.models

import org.jetbrains.exposed.dao.id.IntIdTable

object Users : IntIdTable() {
    val email = varchar("email", 255).uniqueIndex()
    val name = varchar("name", 255).nullable()
    val passwordHash = varchar("password_hash", 255)
}
