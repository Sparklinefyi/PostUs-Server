package postus.models.auth

import kotlinx.serialization.Serializable
import postus.repositories.UserRole

@Serializable
data class UserInfo(
    val id: Int,
    val email: String?,
    val name: String?,
    val role: UserRole,
    val createdAt: String,
    val emailVerified: String? = null,
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
    val emailVerified: String? = null,
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