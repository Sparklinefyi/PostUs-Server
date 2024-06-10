package postus.models

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
    val shareMediaCategory: String
)

@Serializable
data class ShareCommentary(
    val text: String
)

@Serializable
data class Visibility(
    @SerialName("com.linkedin.ugc.MemberNetworkVisibility") val memberNetworkVisibility: String
)