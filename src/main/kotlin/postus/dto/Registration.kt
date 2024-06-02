package postus.dto

import kotlinx.serialization.Serializable

@Serializable
data class Registration(
    val email: String,
    val password: String,
    val name: String
)

@Serializable
data class RegistrationResponse(
    val id: Int,
    val email: String,
    val name: String
)
