package postus.utils

import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database as ExposedDatabase
import org.slf4j.LoggerFactory
import java.sql.Connection

object Database {
    private val logger = LoggerFactory.getLogger(ExposedDatabase::class.java)
    private val config = ConfigFactory.load().getConfig("database")
    private val hikariConfig = HikariConfig().apply {
        jdbcUrl = config.getString("url")
        username = config.getString("user")
        password = config.getString("password")
        driverClassName = config.getString("driver")
        maximumPoolSize = config.getInt("maximumPoolSize")
    }

    private val dataSource = HikariDataSource(hikariConfig)

    init {
        logger.info("Database initialized with URL: ${hikariConfig.jdbcUrl}")
        ExposedDatabase.connect(
            url = "jdbc:postgresql://sparkline-db.c1y0c8y882lf.us-east-1.rds.amazonaws.com:5432/postgres",
                    user = "sparklinefyi",
                    password = "SuperSecure052624!",
                    driver = "org.postgresql.Driver",
        )
    }

    fun getConnection(): Connection {
        logger.info("Getting database connection...")
        return dataSource.connection
    }
}
