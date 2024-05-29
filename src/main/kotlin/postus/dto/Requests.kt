package postus.dto
import kotlinx.serialization.Serializable

@Serializable
data class RegistrationRequest(
    val email: String,
    val password: String,
    val name: String?
)

@Serializable
data class OAuthTokenRequest(
    val accessToken: String,
    val refreshToken: String?
)


@Serializable
data class SignInRequest(
    val email: String,
    val password: String,
    val provider: String?,
    val refreshToken: String?
)

@Serializable
data class LinkAccountRequest(
    val code: String,
    val provider: String,
)

data class UserInfo(
    val id: Int?,
    val provider: String,
    val providerUserId: String,
    val email: String,
    val name: String?,
    val accessToken: String,
    val refreshToken: String?
)