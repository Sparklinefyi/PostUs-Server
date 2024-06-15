package postus.models.instagram

import kotlinx.serialization.Serializable

@Serializable
data class InstagramPostRequest(
    val token: String,
    val videoUrl: String,
    val caption: String
)
