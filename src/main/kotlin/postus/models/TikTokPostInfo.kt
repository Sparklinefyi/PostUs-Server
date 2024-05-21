package postus.models
import kotlinx.serialization.Serializable

@Serializable
data class TikTokPostInfo(
    val title: String,
    val privacy_level: String = "PUBLIC",
    val disable_duet: Boolean = false,
    val disable_comment: Boolean = false,
    val disable_stitch: Boolean = false,
    val video_cover_timestamp_ms: Int = 1000
)