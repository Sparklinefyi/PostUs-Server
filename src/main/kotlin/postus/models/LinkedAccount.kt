package postus.models

import org.jetbrains.exposed.dao.id.IntIdTable
object LinkedAccounts : IntIdTable() {
    val userId = reference("user_id", Users) //.onDeleteCascade()
    val provider = varchar("provider", 50)
    val providerUserId = varchar("provider_user_id", 255)
    val accessToken = varchar("access_token", 255)
    val refreshToken = varchar("refresh_token", 255).nullable()

    init {
        uniqueIndex(provider, providerUserId)
    }
}

// account id for user