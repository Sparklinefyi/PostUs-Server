package postus.models.linkedin

import kotlinx.serialization.Serializable

@Serializable
data class LinkedInAnalyticsResponse(
    val elements: List<Element>
)

@Serializable
data class Element(
    val urn: String,
    val totalShareStatistics: TotalShareStatistics
)

@Serializable
data class TotalShareStatistics(
    val shareCount: Int,
    val commentCount: Int,
    val likeCount: Int
)
