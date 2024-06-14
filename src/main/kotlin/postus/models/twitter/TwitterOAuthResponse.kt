package postus.models.twitter

import kotlinx.serialization.Serializable

@Serializable
data class TwitterOAuthResponse(
    val access_token: String,
    val expires_in: Int? = null,
    val token_type: String? = null,
    val refresh_token: String
)