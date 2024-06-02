package postus.utils

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database as ExposedDatabase
import org.slf4j.LoggerFactory
import java.sql.Connection

object Database {
    private val logger = LoggerFactory.getLogger(ExposedDatabase::class.java)

    init {
        ExposedDatabase.connect(
            url = "jdbc:postgresql://sparkline-db.c1y0c8y882lf.us-east-1.rds.amazonaws.com:5432/postgres",
                    user = "sparklinefyi",
                    password = "SuperSecure052624!",
                    driver = "org.postgresql.Driver",
        )
    }
}
