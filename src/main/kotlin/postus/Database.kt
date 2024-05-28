package postus

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import org.slf4j.LoggerFactory

object Database {
    private val logger = LoggerFactory.getLogger(Database::class.java)
    private val config = ConfigFactory.load().getConfig("database")
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = config.getString("url")
        username = config.getString("user")
        password = config.getString("password")
        driverClassName = config.getString("driver")
        maximumPoolSize = config.getInt("maximumPoolSize")
        println("URL: ${config.getString("url")}")
    }

    private val dataSource = HikariDataSource(hikariConfig)

    init {
        logger.info("Database initialized with URL: ${hikariConfig.jdbcUrl}")
    }

    fun getConnection(): Connection {
        logger.info("Getting database connection...")
        return dataSource.connection
    }
}
