package postus.models
import kotlinx.serialization.Serializable

@Serializable
data class TikTokSourceInfo(
    val source: String = "PULL_FROM_URL",
    val video_url: String
)