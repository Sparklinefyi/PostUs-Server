package postus.models.youtube

import kotlinx.serialization.Serializable

@Serializable
data class YoutubeUploadRequest(
    val snippet: YoutubeShortSnippet,
    val status: YoutubePrivacyStatus
)

@Serializable
data class YoutubePostRequest(
    val token: String,
    val videoUrl: String,
    val snippet: YoutubeShortSnippet,
    val status: YoutubePrivacyStatus
)