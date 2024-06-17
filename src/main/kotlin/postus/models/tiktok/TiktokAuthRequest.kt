package postus.models.tiktok

import kotlinx.serialization.Serializable

@Serializable
data class TiktokAuthRequest(
    val client_key: String,
    val client_secret: String,
    val code: String,
    val grant_type: String = "authorization_code",
    val redirect_uri: String
)

@Serializable
data class TikTokRefreshTokenRequest(
    val client_key: String,
    val client_secret: String,
    val refresh_token: String,
    val grant_type: String = "refresh_token"
)