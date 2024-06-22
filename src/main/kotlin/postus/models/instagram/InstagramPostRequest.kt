package postus.models.instagram

import kotlinx.serialization.Serializable
import postus.models.TokenAndVideoUrl

@Serializable
data class InstagramPostRequest(
    val tokenAndVideoUrl: TokenAndVideoUrl?,
    val caption: String?
)
