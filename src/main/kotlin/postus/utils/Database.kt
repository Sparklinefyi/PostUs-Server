package postus.utils
import io.github.cdimascio.dotenv.Dotenv;
import org.jetbrains.exposed.sql.Database as ExposedDatabase

object Database {

    private val dotenv: Dotenv? = if (System.getenv("ENVIRONMENT") == "prod") null else loadDotenv()

    val dbUrl: String = dotenv?.get("DB_URL") ?: System.getenv("DB_URL")
    val dbUser: String = dotenv?.get("DB_USER") ?: System.getenv("DB_USER")
    val dbPassword: String = dotenv?.get("DB_PASSWORD") ?: System.getenv("DB_PASSWORD")
    val dbDriver: String = dotenv?.get("DB_DRIVER") ?: System.getenv("DB_DRIVER")

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
