package postus.models.tiktok
import kotlinx.serialization.Serializable

@Serializable
data class TikTokPostRequest(
    val post_info: TikTokPostInfo,
    val source_info: TikTokSourceInfo
)