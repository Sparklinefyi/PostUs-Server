package postus.repositories
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import postus.models.auth.UserModel

import org.jetbrains.exposed.dao.*
import org.jetbrains.exposed.dao.id.*
import org.jetbrains.exposed.sql.javatime.*
import postus.models.auth.AccountInfoModel

class UserRepository {

    fun findById(id: Int): UserModel? {
        return transaction {
            User.findById(id)?.toUserModel()
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
        }
    }
}

// Enums
enum class UserRole {
    USER, ADMIN
}

// Tables
object Users : IntIdTable() {
    val name = varchar("name", 255).nullable()
    val email = varchar("email", 255).uniqueIndex().nullable()
    val emailVerified = varchar("emailVerified", 255).nullable()
    val image = varchar("image", 255).nullable()
    val password = varchar("password", 255).nullable()
    val role = enumeration("role", UserRole::class).default(UserRole.USER)
    val createdAt = varchar("createdAt", 255).default(CurrentDateTime.toString())

    init {
        uniqueIndex(email)
    }
}

object Accounts : IntIdTable() {
    val userId = reference("id", Users)
    val type = varchar("type", 255)
    val provider = varchar("provider", 255)
    val accountId = varchar("account_id", 255)
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

object VerificationTokens : IntIdTable() {
    val email = varchar("email", 255)
    val token = varchar("token", 255).uniqueIndex()
    val expires = datetime("expires")

    init {
        uniqueIndex(email, token)
    }
}

object PasswordResetTokens : IntIdTable() {
    val email = varchar("email", 255)
    val token = varchar("token", 255).uniqueIndex()
    val expires = datetime("expires")

    init {
        uniqueIndex(email, token)
    }
}

object UserSubscriptions : IntIdTable() {
    val userId = reference("userId", Users).uniqueIndex()
    val stripeCustomerId = varchar("stripe_customer_id", 255).uniqueIndex().nullable()
    val stripeSubscriptionId = varchar("stripe_subscription_id", 255).uniqueIndex().nullable()
    val stripePriceId = varchar("stripe_price_id", 255).uniqueIndex().nullable()
    val stripeCurrentPeriodEnd = datetime("stripe_current_period_end").uniqueIndex().nullable()
}

object Purchases : IntIdTable() {
    val userId = reference("userId", Users)
    val amount = float("amount")
    val createdAt = datetime("createdAt").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updatedAt").defaultExpression(CurrentDateTime)
}

object StripeCustomers : IntIdTable() {
    val userId = reference("userId", Users).uniqueIndex()
    val stripeCustomerId = varchar("stripeCustomerId", 255).uniqueIndex()
    val createdAt = datetime("createdAt").defaultExpression(CurrentDateTime)
    val updatedAt = datetime("updatedAt").defaultExpression(CurrentDateTime)
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

    companion object : IntEntityClass<User>(Users)
    var name by Users.name
    var email by Users.email
    var emailVerified by Users.emailVerified
    var image by Users.image
    var password by Users.password
    var role by Users.role
    var createdAt by Users.createdAt
    val accounts by Account referrersOn Accounts.userId
    val subscriptions by UserSubscription referrersOn UserSubscriptions.userId
    val purchases by Purchase referrersOn Purchases.userId
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

    companion object : IntEntityClass<Account>(Accounts)

    val userId by User referrersOn Accounts.userId
    var type by Accounts.type
    var provider by Accounts.provider
    var accountId by Accounts.accountId
    var refreshToken by Accounts.refreshToken
    var accessToken by Accounts.accessToken
    var expiresAt by Accounts.expiresAt
    var tokenType by Accounts.tokenType
    var scope by Accounts.scope
    var idToken by Accounts.idToken
    var sessionState by Accounts.sessionState
}

class VerificationToken(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<VerificationToken>(VerificationTokens)

    var email by VerificationTokens.email
    var token by VerificationTokens.token
    var expires by VerificationTokens.expires
}

class PasswordResetToken(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<PasswordResetToken>(PasswordResetTokens)

    var email by PasswordResetTokens.email
    var token by PasswordResetTokens.token
    var expires by PasswordResetTokens.expires
}

class UserSubscription(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<UserSubscription>(UserSubscriptions)

    val userId by User referrersOn UserSubscriptions.userId
    var stripeCustomerId by UserSubscriptions.stripeCustomerId
    var stripeSubscriptionId by UserSubscriptions.stripeSubscriptionId
    var stripePriceId by UserSubscriptions.stripePriceId
    var stripeCurrentPeriodEnd by UserSubscriptions.stripeCurrentPeriodEnd
}

class Purchase(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Purchase>(Purchases)

    var userId by User referencedOn Purchases.userId
    var amount by Purchases.amount
    var createdAt by Purchases.createdAt
    var updatedAt by Purchases.updatedAt
}

class StripeCustomer(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<StripeCustomer>(StripeCustomers)

    var userId by User referencedOn StripeCustomers.userId
    var stripeCustomerId by StripeCustomers.stripeCustomerId
    var createdAt by StripeCustomers.createdAt
    var updatedAt by StripeCustomers.updatedAt
}