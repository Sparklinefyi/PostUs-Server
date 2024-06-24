package postus.models.auth

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import org.jetbrains.exposed.sql.javatime.datetime
import postus.repositories.UserRole
import java.time.LocalDateTime

@Serializable
data class UserInfo(
    val id: Int,
    val email: String?,
    val name: String?,
    val role: UserRole,
    val createdAt: String,
    @Contextual
    val emailVerified: LocalDateTime? = null,
    val image: String? = null,
    val accounts: List<String> = emptyList(),
)

@Serializable
data class UserModel(
    val id: Int,
    val email: String?,
    val name: String?,
    val password: String?,
    val createdAt: String,
    val role: UserRole,
    @Contextual
    val emailVerified: LocalDateTime? = null,
    val image: String? = null,
    val accounts: List<AccountInfoModel> = emptyList()
) {
    fun toUserInfo(): UserInfo? {
        return UserInfo(
            id,
            email,
            name,
            role,
            createdAt,
            emailVerified,
            image,
            accounts.map { it.provider }
        )
    }
}

@Serializable
data class AccountInfoModel(
    val userId: Int,
    val type: String,
    val provider: String,
    val accountId: String,
    val refreshToken: String?,
    val accessToken: String?,
    val expiresAt: Int?,
    val tokenType: String?,
    val scope: String?,
    val idToken: String?,
    val sessionState: String?
)