package postus.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName


@Serializable
enum class YoutubePrivacyStatuses {
    @SerialName("public")
    PUBLIC,

    @SerialName("private")
    PRIVATE,

    @SerialName("unlisted")
    UNLISTED
}
@Serializable
data class YoutubePrivacyStatus(
    val privacyStatus: YoutubePrivacyStatuses
)
