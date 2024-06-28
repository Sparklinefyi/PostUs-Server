package postus.controllers


import com.auth0.jwt.interfaces.Claim
import com.password4j.Password
import postus.models.auth.AccountInfoModel
import postus.models.auth.RegistrationRequest
import postus.models.auth.UserModel
import postus.models.auth.UserInfo
import postus.repositories.UserRepository
import postus.repositories.UserRole
import postus.utils.JwtHandler
import java.time.LocalDateTime.now


class UserController(
    private val userRepository: UserRepository,
) {

    suspend fun registerUser(request: RegistrationRequest): UserInfo {
        val hashedPassword = Password.hash(request.password).addRandomSalt(10).withBcrypt().result
        val user = UserModel(
            id = 0,
            email = request.email,
            name = request.name,
            password = hashedPassword.toString(),
            role = UserRole.USER,
            createdAt = now().toString(),
            emailVerified = null,
            image = null
        )

        val userId = userRepository.create(user)
        return user.toUserInfo()!!.copy(id = userId)
    }

    suspend fun authenticateWithEmailPassword(email: String, password: String): UserModel? {
        val user = userRepository.findByEmail(email) ?: return null
        return if (Password.check(password, user.password!!).withBcrypt()) user else null
    }

    suspend fun fetchUserDataByToken(token: String): UserModel? {
        val userId = JwtHandler().validateTokenAndGetUserId(token) ?: return null
        return userRepository.findById(userId.toInt())
    }

    suspend fun fetchUserDataByTokenWithPlatform(token: String): Triple<String, UserInfo, String?> {
        try {
            val verifier = JwtHandler().makeJwtVerifier(System.getProperty("JWT_ISSUER")!!)
            val decodedJWT = verifier.verify(token)

            val userClaim: Claim = decodedJWT.getClaim("userId")
            val userId = if (!userClaim.isNull) {
                userClaim.asString().removeSurrounding("\"").toInt()
            } else {
                0
            }

            // Safely access "platform" claim
            val platformClaim: Claim = decodedJWT.getClaim("platform")
            val platform = if (!platformClaim.isNull) {
                platformClaim.asString().removeSurrounding("\"")
            } else {
                ""
            }

            // Access other claims as needed
            val codeVerifierClaim: Claim = decodedJWT.getClaim("code_verifier")
            val codeVerifier = if (!codeVerifierClaim.isNull) {
               codeVerifierClaim.asString().removeSurrounding("\"")
            } else {
                ""
            }

            val user = userRepository.findById(userId)!!.toUserInfo()
            return Triple(platform, user, codeVerifier)
        } catch (e: Exception) {
            println("Error verifying token: ${e.message}")
            return Triple("", UserInfo(0, "", "", UserRole.USER, now().toString(), now(), ""), "")
        }
    }

    suspend fun linkAccount(userId: Int, provider: String, accountId: String?, accessToken: String?, refreshToken: String?) {
        val user = userRepository.findById(userId)
        val currentAccount = user!!.accounts.find { it.provider == provider }
        val updatedUser = user.copy(
            accounts = user.accounts + AccountInfoModel(
                userId = userId,
                type = "oauth",
                provider = provider,
                accountId = accountId ?: currentAccount?.accountId ?: "",
                refreshToken = refreshToken ?: currentAccount?.accountId ?: "",
                accessToken = accessToken,
                expiresAt = null,
                tokenType = null,
                scope = null,
                idToken = null,
                sessionState = null
            )
        )

        if (updatedUser == user) {
            throw Exception("must update some user information")
        }

        userRepository.update(updatedUser)
    }

    suspend fun updateUser(id: Int, description: String?, currentPassword: String?, newPassword: String?) {
        val user = userRepository.findById(id) ?: throw IllegalArgumentException("User not found")
        if (description != null) {
            user.copy(name = description)
        }

        if (currentPassword != null && newPassword != null) {
            if (Password.check(currentPassword, user.password!!).withBcrypt()) {
                val hashedPassword = Password.hash(newPassword).addRandomSalt(10).withBcrypt().result
                user.copy(password = hashedPassword.toString())
            } else {
                throw IllegalArgumentException("Invalid password")
            }

        }

        userRepository.update(user)
    }
}
