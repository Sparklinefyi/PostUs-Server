package postus.models.auth

import kotlinx.serialization.Contextual
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.exposed.sql.javatime.datetime
import postus.repositories.UserRole
import java.time.LocalDateTime


object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return LocalDateTime.parse(decoder.decodeString())
    }
}

object UserRoleSerializer : KSerializer<UserRole> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UserRole", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: UserRole) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): UserRole {
        return UserRole.valueOf(decoder.decodeString())
    }
}

@Serializable
data class UserInfo(
    val id: Int,
    val email: String?,
    val name: String?,
    val role: String,
    val createdAt: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val emailVerified: LocalDateTime? = null,
    val image: String? = null,
    val accounts: List<String> = emptyList(),
)

@Serializable
data class UserModel(
    val id: Int,
    val email: String?,
    val name: String?,
    val password: String?,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createdAt: LocalDateTime,
    val role: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val emailVerified: LocalDateTime? = null,
    val image: String? = null,
    val accounts: List<AccountInfoModel> = emptyList()
) {
    fun toUserInfo(): UserInfo {
        return UserInfo(
            id,
            email,
            name,
            role,
            createdAt.toString(),
            emailVerified,
            image,
            accounts.map { it.provider }
        )
    }
}

@Serializable
data class AccountInfoModel(
    val userId: Int,
    val type: String,
    val provider: String,
    val accountId: String,
    val refreshToken: String?,
    val accessToken: String?,
    val expiresAt: Int?,
    val tokenType: String?,
    val scope: String?,
    val idToken: String?,
    val sessionState: String?
)