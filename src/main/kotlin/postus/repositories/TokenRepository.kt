import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import com.fasterxml.uuid.Generators
import postus.repositories.VerificationTokenTable
import java.time.LocalDateTime
import java.util.UUID

data class VerificationToken(val id: Int, val email: String, val token: String, val expires: LocalDateTime)

object TokenService {

    fun generateVerificationToken(email: String): VerificationToken {
        return transaction {
            val token: UUID = Generators.timeBasedGenerator().generate()
            val expires = LocalDateTime.now().plusHours(1)

            // Delete existing token if present
            VerificationTokenTable.deleteWhere { VerificationTokenTable.email eq email }

            // Insert new token
            val id = VerificationTokenTable.insertAndGetId {
                it[VerificationTokenTable.email] = email
                it[VerificationTokenTable.token] = token.toString()
                it[VerificationTokenTable.expires] = expires
            }

            VerificationToken(
                id = id.value,
                email = email,
                token = token.toString(),
                expires = expires
            )
        }
    }
}
