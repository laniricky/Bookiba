package co.booknook.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val config = HikariConfig().apply {
            driverClassName = "org.postgresql.Driver"
            jdbcUrl = System.getenv("JDBC_URL") ?: "jdbc:postgresql://localhost:5432/bookiba"
            username = System.getenv("DB_USER") ?: "postgres"
            password = System.getenv("DB_PASSWORD") ?: "postgres"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        val dataSource = HikariDataSource(config)
        Database.connect(dataSource)
        transaction {
            SchemaUtils.create(
                co.booknook.database.models.Users,
                co.booknook.database.models.Books,
                co.booknook.database.models.Orders,
                co.booknook.database.models.OrderItems,
                co.booknook.database.models.Wishlists
            )
        }
    }
}
