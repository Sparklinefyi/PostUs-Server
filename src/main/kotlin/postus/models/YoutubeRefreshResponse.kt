package postus.models

import kotlinx.serialization.Serializable

@Serializable
data class YoutubeRefreshResponse(
    val access_token: String,
    val expires_in: Int,
    val id_token: String?,
    val scope: String,
    val token_type: String
)
