package postus.utils

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database as ExposedDatabase
import org.slf4j.LoggerFactory
import java.sql.Connection

object Database {
    private val logger = LoggerFactory.getLogger(ExposedDatabase::class.java)
    private val databaseConfig = ConfigFactory.load().getConfig("database")
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = databaseConfig.getString("url")
        username = databaseConfig.getString("user")
        password = databaseConfig.getString("password")
    }

    init {
        logger.info("Database initialized with URL: ${hikariConfig.jdbcUrl}")
        ExposedDatabase.connect(
            url = hikariConfig.jdbcUrl,
            user = hikariConfig.username,
            password = hikariConfig.password,
            driver = "org.postgresql.Driver",
        )
    }

}
