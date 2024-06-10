package postus.utils
import io.github.cdimascio.dotenv.Dotenv;
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database as ExposedDatabase
import org.slf4j.LoggerFactory

object Database {

    private val dotenv: Dotenv? = if (System.getenv("ENVIRONMENT") == "production") null else loadDotenv()

    val dbUrl: String = dotenv?.get("DB_URL") ?: System.getenv("DB_URL")
    val dbUser: String = dotenv?.get("DB_USER") ?: System.getenv("DB_USER")
    val dbPassword: String = dotenv?.get("DB_PASSWORD") ?: System.getenv("DB_PASSWORD")
    val dbDriver: String = dotenv?.get("DB_DRIVER") ?: System.getenv("DB_DRIVER")
    val dbMaxPoolSize: Int = dotenv?.get("DB_MAX_POOL_SIZE")?.toInt() ?: System.getenv("DB_MAX_POOL_SIZE")?.toInt() ?: 10

    private fun loadDotenv(): Dotenv? {
        return try {
            Dotenv.load()
        } catch (e: Exception) {
            println("Warning: .env file not found, falling back to system environment variables")
            null
        }
    }
    init {
        ExposedDatabase.connect(
            url = dbUrl,
            driver = dbDriver,
            user = dbUser,
            password = dbPassword
        )
    }

}
