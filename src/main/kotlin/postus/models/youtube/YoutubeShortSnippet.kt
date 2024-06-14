package postus.models.youtube

import kotlinx.serialization.Serializable

@Serializable
data class YoutubeShortSnippet(
    val title: String,
    val description: String,
    val tags: List<String>,
    val categoryId: String,
)
