package postus.models

import kotlinx.serialization.Serializable

@Serializable
data class InstagramPostRequest(
    val caption: String
)
