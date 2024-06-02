package postus.models

import kotlinx.serialization.Serializable

@Serializable
data class YoutubeOAuthResponse(
    val access_token: String,
    val expires_in: Int,
    val refresh_token: String?,
    val scope: String,
    val token_type: String
)
