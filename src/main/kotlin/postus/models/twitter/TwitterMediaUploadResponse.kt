package postus.models.twitter
import kotlinx.serialization.Serializable

@Serializable
data class TwitterMediaUploadResponse(
    val media_id_string: String,
    val expires_after_secs: Int,
    val media_key: String,
    val media_category: String
)

@Serializable
data class TwitterTweetResponse(
    val data: TweetData
)

@Serializable
data class TweetData(
    val id: String,
    val text: String
)

