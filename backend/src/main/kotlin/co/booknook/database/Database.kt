package co.booknook.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val dbUrl = System.getenv("DATABASE_URL") ?: System.getenv("JDBC_URL")

        val config = HikariConfig().apply {
            if (dbUrl != null && dbUrl.startsWith("postgres")) {
                driverClassName = "org.postgresql.Driver"
                // Convert postgresql:// to jdbc:postgresql://
                jdbcUrl = if (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://")) {
                    "jdbc:" + dbUrl
                } else {
                    dbUrl
                }
            } else {
                driverClassName = "org.sqlite.JDBC"
                jdbcUrl = dbUrl ?: "jdbc:sqlite:/data/bookiba.sqlite"
            }
            maximumPoolSize = 10 // Increased for postgres
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_SERIALIZABLE"
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
