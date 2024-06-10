package postus.utils
import io.github.cdimascio.dotenv.Dotenv;
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database as ExposedDatabase
import org.slf4j.LoggerFactory

object Database {

    private val logger = LoggerFactory.getLogger(ExposedDatabase::class.java)
    private val dotenv = Dotenv.load()
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = dotenv["DB_URL"]
        username = dotenv["DB_USER"]
        password = dotenv["DB_PASSWORD"]
        driverClassName = dotenv["DB_DRIVER"]
        maximumPoolSize = dotenv["DB_MAX_POOL_SIZE"]?.toInt() ?: 10
    }

    init {
        logger.info("Database initialized with URL: ${hikariConfig.jdbcUrl}")
        ExposedDatabase.connect(
            url = hikariConfig.jdbcUrl,
            driver = hikariConfig.driverClassName,
            user = hikariConfig.username,
            password = hikariConfig.password
        )
    }

}
