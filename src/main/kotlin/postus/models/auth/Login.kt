package postus.models.auth

import kotlinx.serialization.Serializable
import postus.repositories.UserInfo

@Serializable
data class Login (
    val email: String,
    val password: String
)
