package postus.repositories;

import postus.models.LinkedAccounts;
import org.jetbrains.exposed.sql.ResultRow;
import org.jetbrains.exposed.sql.*;
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.statements.InsertStatement
import org.jetbrains.exposed.sql.transactions.transaction;

data class LinkedAccount(
        val provider: String,
        val refreshToken: String?
) {
    fun LinkedAccount.toAccount(): LinkedAccount {
        return LinkedAccount(provider, refreshToken)
    }
}


class LinkedAccountRepository {

}
