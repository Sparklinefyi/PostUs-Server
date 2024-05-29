package postus.dto

data class RegistrationRequest(
    val email: String,
    val password: String,
    val name: String?
)

data class OAuthTokenRequest(
    val accessToken: String,
    val refreshToken: String?
)

data class SignInRequest(
    val email: String,
    val password: String,
    val provider: String?,
    val oauthToken: OAuthTokenRequest?
)

data class LinkAccountRequest(
    val provider: String,
    val providerUserId: String,
    val oauthToken: OAuthTokenRequest
)

data class UserInfo(
    val provider: String,
    val providerUserId: String,
    val email: String,
    val name: String?,
    val accessToken: String,
    val refreshToken: String?
)