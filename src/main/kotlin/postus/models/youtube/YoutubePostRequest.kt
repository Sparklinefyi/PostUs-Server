package postus.models.youtube

import kotlinx.serialization.Serializable
import postus.models.TokenAndVideoUrl

@Serializable
data class YoutubePostRequest(
    val tokenAndVideoUrl: TokenAndVideoUrl?,
    val snippet: YoutubeShortSnippet,
    val status: YoutubePrivacyStatus
)
