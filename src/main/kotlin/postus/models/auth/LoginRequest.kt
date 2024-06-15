package postus.models.auth

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class TokenRequest(
    val token: String
)

@Serializable
data class OAuthLoginRequest(
    val state: String,
    val code: String
)
