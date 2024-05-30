package postus.dto

data class UserInfo(
    val id: Int?,
    val provider: String,
    val providerUserId: String,
    val email: String,
    val name: String?,
    val accessToken: String,
    val refreshToken: String?
)