package postus.models
import kotlinx.serialization.Serializable

@Serializable
data class HashedPasswordRequest(
    var password: String
)
