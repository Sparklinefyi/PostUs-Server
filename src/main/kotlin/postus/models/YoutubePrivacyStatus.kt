package postus.models

import kotlinx.serialization.Serializable

enum class YoutubePrivacyStatuses{
    PRIVATE,
    PUBLIC,
}
@Serializable
data class YoutubePrivacyStatus(
    val privacyStatus: YoutubePrivacyStatuses
)
