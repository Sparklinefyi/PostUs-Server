package postus.models

import org.jetbrains.exposed.dao.id.IntIdTable
object LinkedAccounts : IntIdTable("linked_accounts") {
    val provider = varchar("provider", 50)
    val refreshToken = varchar("refresh_token", 255)
}

// account id ford user