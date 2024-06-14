package postus.models.instagram

import kotlinx.serialization.Serializable

@Serializable
data class InstagramPostRequest(
    val caption: String
)
