package postus.models.twitter

import kotlinx.serialization.Serializable

@Serializable
data class TwitterAuthenticatedUserResponse(
    val data: TwitterAuthenticatedUserData
)

@Serializable
data class TwitterAuthenticatedUserData(
    val id: String,
    val name: String,
    val username: String
)