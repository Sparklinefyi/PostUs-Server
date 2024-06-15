package postus.models.media

import kotlinx.serialization.Serializable

@Serializable
data class ResourceRequest(
    val token: String,
    val key: String
)