package postus.utils

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import java.util.*

class JwtHandler {

    fun makeJwtVerifier(issuer: String): JWTVerifier {
        val algorithm = Algorithm.HMAC256("your_secret_here")
        return JWT.require(algorithm)
            .withIssuer(issuer)
            .build()
    }

    fun makeToken(userId: String): String {
        val algorithm = Algorithm.HMAC256("your_secret_here")
        return JWT.create()
            .withSubject(userId)
            .withIssuer("your_issuer_here")
            .withExpiresAt(Date(System.currentTimeMillis() + 1 * 1000 * 60 * 60 * 72)) // 3 days
            .sign(algorithm)
    }

    fun validateTokenAndGetUserId(token: String): String? {
        try {
            val verifier = makeJwtVerifier("your_issuer_here")
            val jwt = verifier.verify(token)
            return jwt.subject
        } catch (e: Exception) {
            return null
        }
    }


}