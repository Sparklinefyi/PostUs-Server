package postus.repositories
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import postus.models.auth.UserModel

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.*
import postus.models.auth.AccountInfoModel
import java.time.LocalDateTime.now

class UserRepository {

    fun findById(id: Int): UserModel? {
        return transaction {
            User.find(UserTable.id eq id).firstOrNull()?.toUserModel()
        }
    }
    fun update(updatedUser: UserModel) {
        return transaction {
            val user = User.findById(updatedUser.id) ?: throw IllegalArgumentException("User not found")
            user.email = updatedUser.email
            user.name = updatedUser.name
            user.password = updatedUser.password
            user.role = updatedUser.role
            user.createdAt = updatedUser.createdAt
            user.emailVerified = updatedUser.emailVerified
            user.image = updatedUser.image

            // Check if the updatedUser has accounts and update/save them
            updatedUser.accounts?.forEach { updatedAccount ->

                println( updatedAccount.accountId)
                val account = Account.find { AccountTable.accountId eq updatedAccount.accountId }.singleOrNull() ?: Account.new {
                    // If the account doesn't exist, create a new one
                    this.userId = user
                    this.type = updatedAccount.type
                    this.provider = updatedAccount.provider
                    this.accountId = updatedAccount.accountId
                    this.refreshToken = updatedAccount.refreshToken
                    this.accessToken = updatedAccount.accessToken
                    this.expiresAt = updatedAccount.expiresAt
                    this.tokenType = updatedAccount.tokenType
                    this.scope = updatedAccount.scope
                    this.idToken = updatedAccount.idToken
                    this.sessionState = updatedAccount.sessionState
                }
                // If the account exists, update its fields
                account.type = updatedAccount.type
                account.provider = updatedAccount.provider
                account.refreshToken = updatedAccount.refreshToken
                account.accessToken = updatedAccount.accessToken
                account.expiresAt = updatedAccount.expiresAt
                account.tokenType = updatedAccount.tokenType
                account.scope = updatedAccount.scope
                account.idToken = updatedAccount.idToken
                account.sessionState = updatedAccount.sessionState
            }
        }
    }

    fun findByEmail(email: String): UserModel? {
        return transaction {
            User.find { UserTable.email eq email }.firstOrNull()?.toUserModel()
        }
    }

    fun create(user: UserModel): Int {
        return transaction {
            User.new {
                email = user.email
                name = user.name
                password = user.password
                role = user.role
                createdAt = user.createdAt
                emailVerified = user.emailVerified
                image = user.image
            }.id.value
        }
    }
}

object UserTable : IntIdTable(
    "\"User\""
) {
    val name = varchar("name", 255).nullable()
    val email = varchar("email", 255).uniqueIndex().nullable()
    val emailVerified = datetime("emailVerified").nullable()
    val image = varchar("image", 255).nullable()
    val password = varchar("password", 255).nullable()

    val role = customEnumeration(
        "role", "UserRole",
        { value -> UserRole.valueOf(value as String) },
        { it.name }
    ).default(UserRole.USER)

    val createdAt = varchar("createdAt", 255).default(now().toString())

    init {
        uniqueIndex(email)
    }
}

object AccountTable : IntIdTable(
    "\"Account\""
) {

    val userId = reference("userId", UserTable.id, onDelete = ReferenceOption.CASCADE)
    val type = varchar("type", 255)
    val provider = varchar("provider", 255)
    val accountId = varchar("providerAccountId", 255)
    val refreshToken = text("refresh_token").nullable()
    val accessToken = text("access_token").nullable()
    val expiresAt = integer("expires_at").nullable()
    val tokenType = varchar("token_type", 255).nullable()
    val scope = varchar("scope", 255).nullable()
    val idToken = text("id_token").nullable()
    val sessionState = varchar("session_state", 255).nullable()

    init {
        uniqueIndex(provider, accountId)
    }
}

object VerificationTokenTable : IntIdTable(
    "\"VerificationToken\""
) {
    val email = varchar("email", 255)
    val token = varchar("token", 255).uniqueIndex()
    val expires = datetime("expires")

    init {
        uniqueIndex(email, token)
    }
}

object PasswordResetTokenTable : IntIdTable(
    "\"PasswordResetToken\""
) {
    val email = varchar("email", 255)
    val token = varchar("token", 255).uniqueIndex()
    val expires = datetime("expires")

    init {
        uniqueIndex(email, token)
    }
}

object UserSubscriptionTable : IntIdTable(
    "\"UserSubscription\""
) {
    val userId = reference("userId", UserTable).uniqueIndex()
    val stripeCustomerId = varchar("stripe_customer_id", 255).uniqueIndex().nullable()
    val stripeSubscriptionId = varchar("stripe_subscription_id", 255).uniqueIndex().nullable()
    val stripePriceId = varchar("stripe_price_id", 255).uniqueIndex().nullable()
    val stripeCurrentPeriodEnd = datetime("stripe_current_period_end").uniqueIndex().nullable()
}

object PurchaseTable : IntIdTable(
    "\"Purchase\""
) {
    val userId = reference("userId", UserTable)
    val amount = float("amount")
    val createdAt = datetime("createdAt").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updatedAt").defaultExpression(CurrentDateTime)
}

object StripeCustomerTable : IntIdTable(
    name = "StripeCustomer"
) {
    val userId = reference("userId", UserTable).uniqueIndex()
    val stripeCustomerId = varchar("stripeCustomerId", 255).uniqueIndex()
    val createdAt = datetime("createdAt").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updatedAt").defaultExpression(CurrentDateTime)
}

// Enums
enum class UserRole {
    USER, ADMIN
}

// Entity classes
class User(id: EntityID<Int>) : IntEntity(id) {
    fun toUserModel(): UserModel {
        return UserModel(
            id.value,
            email,
            name,
            password,
            createdAt,
            role,
            emailVerified,
            image,
            accounts.map { it.toAccountInfoModel() }
        )
    }

    companion object : IntEntityClass<User>(UserTable)
    var name by UserTable.name
    var email by UserTable.email
    var emailVerified by UserTable.emailVerified
    var image by UserTable.image
    var password by UserTable.password
    var role by UserTable.role
    var createdAt by UserTable.createdAt
    val accounts by Account referrersOn AccountTable.userId
    val subscriptions by UserSubscription referrersOn UserSubscriptionTable.userId
    val purchases by Purchase referrersOn PurchaseTable.userId
}

class Account(id: EntityID<Int>) : IntEntity(id) {
    fun toAccountInfoModel(): AccountInfoModel {
        return AccountInfoModel(
            id.value,
            type,
            provider,
            accountId,
            refreshToken,
            accessToken,
            expiresAt,
            tokenType,
            scope,
            idToken,
            sessionState
        )
    }

    companion object : IntEntityClass<Account>(AccountTable)

    var userId by User referencedOn AccountTable.userId
    var type by AccountTable.type
    var provider by AccountTable.provider
    var accountId by AccountTable.accountId
    var refreshToken by AccountTable.refreshToken
    var accessToken by AccountTable.accessToken
    var expiresAt by AccountTable.expiresAt
    var tokenType by AccountTable.tokenType
    var scope by AccountTable.scope
    var idToken by AccountTable.idToken
    var sessionState by AccountTable.sessionState
}

class VerificationToken(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<VerificationToken>(VerificationTokenTable)

    var email by VerificationTokenTable.email
    var token by VerificationTokenTable.token
    var expires by VerificationTokenTable.expires
}

class PasswordResetToken(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PasswordResetToken>(PasswordResetTokenTable)

    var email by PasswordResetTokenTable.email
    var token by PasswordResetTokenTable.token
    var expires by PasswordResetTokenTable.expires
}

class UserSubscription(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserSubscription>(UserSubscriptionTable)

    val userId by User referrersOn UserSubscriptionTable.userId
    var stripeCustomerId by UserSubscriptionTable.stripeCustomerId
    var stripeSubscriptionId by UserSubscriptionTable.stripeSubscriptionId
    var stripePriceId by UserSubscriptionTable.stripePriceId
    var stripeCurrentPeriodEnd by UserSubscriptionTable.stripeCurrentPeriodEnd
}

class Purchase(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Purchase>(PurchaseTable)

    var userId by User referencedOn PurchaseTable.userId
    var amount by PurchaseTable.amount
    var createdAt by PurchaseTable.createdAt
    var updatedAt by PurchaseTable.updatedAt
}

class StripeCustomer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<StripeCustomer>(StripeCustomerTable)

    var userId by User referencedOn StripeCustomerTable.userId
    var stripeCustomerId by StripeCustomerTable.stripeCustomerId
    var createdAt by StripeCustomerTable.createdAt
    var updatedAt by StripeCustomerTable.updatedAt
}
