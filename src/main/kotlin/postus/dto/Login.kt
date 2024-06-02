package postus.dto

import kotlinx.serialization.Serializable
import postus.repositories.UserInfo

@Serializable
data class Login (
    val email: String,
    val password: String
)

@Serializable
data class LoginResponse (
    val id: Int,
    val email: String,
    val name: String,
    val role: String,
    val description: String,
)
