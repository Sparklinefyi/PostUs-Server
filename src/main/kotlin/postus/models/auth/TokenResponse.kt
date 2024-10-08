package postus.models.auth

import kotlinx.serialization.Serializable

@Serializable
data class UpdateUserRequest(
    val token: String,
    val description: String?,
    val currentPassword: String?,
    val newPassword: String?
)