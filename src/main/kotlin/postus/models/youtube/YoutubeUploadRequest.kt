package postus.models.youtube

import kotlinx.serialization.Serializable

@Serializable
data class YoutubeUploadRequest(
    val snippet: YoutubeShortSnippet,
    val status: YoutubePrivacyStatus
)
