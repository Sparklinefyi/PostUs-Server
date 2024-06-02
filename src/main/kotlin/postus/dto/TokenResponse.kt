package postus.dto

import kotlinx.serialization.Serializable

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)

@Serializable
data class UpdateUserRequest(
    val token: String,
    val description: String
)

@Serializable
data class UserInfoRequest(
    val token: String
)
