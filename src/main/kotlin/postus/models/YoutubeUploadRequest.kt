package postus.models

import kotlinx.serialization.Serializable

@Serializable
data class YoutubeUploadRequest(
    val snippet: YoutubeShortSnippet,
    val status: YoutubePrivacyStatus
)
