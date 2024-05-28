package postus.controllers

import postus.Database
import org.slf4j.LoggerFactory
import java.sql.Connection
import java.sql.Date
class UserController {
    private val logger = LoggerFactory.getLogger(UserController::class.java)

    fun addUser(id: String, email: String, first: String, last: String, picture: String, refreshToken: String, subExpire: Date) {
        logger.info("Adding user to database...")
        val connection: Connection = Database.getConnection()
        connection.use { conn ->
            val stmt = conn.prepareStatement("INSERT INTO users (id, email, first, last, picture, refresh_token, sub_expire) VALUES (?, ?, ?, ?, ?, ?, ?)")
            stmt.setString(1, id) // Convert id to Long
            stmt.setString(2, email)
            stmt.setString(3, first)
            stmt.setString(4, last)
            stmt.setString(5, picture)
            stmt.setString(6, refreshToken)
            stmt.setDouble(7, subExpire.time.toDouble())
            stmt.executeUpdate()

            logger.info("User added: id = $id, email = $email, first = $first, last = $last, picture = $picture, sub_expire = $subExpire")
        }
    }

    fun getAllUsers() {
        logger.info("Fetching all users from database...")
        val connection: Connection = Database.getConnection()
        connection.use { conn ->
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery("SELECT * FROM Users")
            while (rs.next()) {
                println("id = ${rs.getString("id")}, email = ${rs.getString("email")}, first = ${rs.getString("first")}, last = ${rs.getString("last")}, picture = ${rs.getString("picture")}, sub_expire = ${rs.getDate("sub_expire")}")
            }
        }
    }
}
