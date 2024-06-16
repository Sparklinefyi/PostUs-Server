package postus.models.linkedin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LinkedInPostRequest(
    val author: String,
    val lifecycleState: String,
    val specificContent: SpecificContent,
    val visibility: Visibility
)

@Serializable
data class SpecificContent(
    @SerialName("com.linkedin.ugc.ShareContent") val shareContent: ShareContent
)

@Serializable
data class ShareContent(
    val shareCommentary: ShareCommentary,
    val shareMediaCategory: String,
    val media: List<Media> = emptyList()
)

@Serializable
data class ShareCommentary(
    val text: String
)

@Serializable
data class Media(
    val status: String,
    val description: MediaDescription,
    val media: String,
    val title: MediaTitle,
    val mediaType: String
)

@Serializable
data class MediaDescription(
    val text: String
)

@Serializable
data class MediaTitle(
    val text: String
)

@Serializable
data class Visibility(
    @SerialName("com.linkedin.ugc.MemberNetworkVisibility")
    val memberNetworkVisibility: String
)
