package postus.dto

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String,
)
