package postus.models.linkedin

import kotlinx.serialization.Serializable

@Serializable
data class LinkedinUserInfo(
    val sub: String
)