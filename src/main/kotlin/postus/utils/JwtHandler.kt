package postus.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

class JwtHandler {

    fun makeJwtVerifier(issuer: String): JWTVerifier {
        val algorithm = Algorithm.HMAC256(System.getProperty("JWT_SECRET")!!)
        return JWT.require(algorithm)
            .withIssuer(issuer)
            .build()
    }

    fun makeToken(userId: Int): String {
        val algorithm = Algorithm.HMAC256(System.getProperty("JWT_SECRET")!!)
        return JWT.create()
            .withIssuer(System.getProperty("JWT_ISSUER")!!)
            .withSubject(userId.toString())
            .withExpiresAt(Date(System.currentTimeMillis() + 1 * 1000 * 60 * 60 * 72)) // 3 days
            .sign(algorithm)
    }

    fun validateTokenAndGetUserId(token: String): String? {
        return try {
            val verifier = makeJwtVerifier(System.getProperty("JWT_ISSUER")!!)
            val jwt = verifier.verify(token)

            val userId = jwt.getClaim("token").asString() ?: jwt.getClaim("user").asString()
            if (userId.isNullOrEmpty()) jwt.subject else userId
        } catch (e: Exception) {
            null
        }
    }



}