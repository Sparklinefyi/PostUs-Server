package postus.repositories;

import postus.models.LinkedAccounts;
import org.jetbrains.exposed.sql.ResultRow;
import org.jetbrains.exposed.sql.*;
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction;

data class LinkedAccount(
        val id: Int,
        val userId: Int,
        val provider: String,
        val providerUserId: String,
        val accessToken: String,
        val refreshToken: String?
)

class LinkedAccountRepository {
    fun existsByProviderAndProviderUserId(provider: String, providerUserId: String): Boolean {
        return transaction {
            LinkedAccounts.selectAll()
                .where { (LinkedAccounts.provider eq provider) and (LinkedAccounts.providerUserId eq providerUserId) }
                .count() > 0
        }
    }

    fun findByProviderAndProviderUserId(provider: String, providerUserId: String): LinkedAccount? {
        return transaction {
            LinkedAccounts.selectAll()
                .where { (LinkedAccounts.provider eq provider) and (LinkedAccounts.providerUserId eq providerUserId) }
                .map { toLinkedAccount(it) }
                .singleOrNull()
        }
    }

    fun save(linkedAccount: LinkedAccount): LinkedAccount {
        return transaction {
            val id = LinkedAccounts.insertAndGetId {
                it[userId] = linkedAccount.userId
                it[provider] = linkedAccount.provider
                it[providerUserId] = linkedAccount.providerUserId
                it[accessToken] = linkedAccount.accessToken
                it[refreshToken] = linkedAccount.refreshToken
            }
            linkedAccount.copy(id = id.value)
        }
    }

    private fun toLinkedAccount(row: ResultRow): LinkedAccount {
        return LinkedAccount(
                id = row[LinkedAccounts.id].value,
                userId = row[LinkedAccounts.userId].value,
                provider = row[LinkedAccounts.provider],
                providerUserId = row[LinkedAccounts.providerUserId],
                accessToken = row[LinkedAccounts.accessToken],
                refreshToken = row[LinkedAccounts.refreshToken]
        )
    }
}
