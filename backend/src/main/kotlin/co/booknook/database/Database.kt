package co.booknook.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    fun init() {
        val config = HikariConfig().apply {
            driverClassName = "org.sqlite.JDBC"
            jdbcUrl = System.getenv("JDBC_URL") ?: "jdbc:sqlite:/data/bookiba.sqlite"
            maximumPoolSize = 1
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
