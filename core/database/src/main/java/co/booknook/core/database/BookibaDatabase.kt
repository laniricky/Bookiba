package co.booknook.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import co.booknook.core.database.dao.BookDao
import co.booknook.core.database.dao.CartDao
import co.booknook.core.database.dao.OrderDao
import co.booknook.core.database.model.BookEntity
import co.booknook.core.database.model.CartItemEntity
import co.booknook.core.database.model.OrderEntity
import co.booknook.core.database.model.OrderItemEntity

@Database(
    entities = [BookEntity::class, CartItemEntity::class, OrderEntity::class, OrderItemEntity::class],
    version = 3,
    exportSchema = true
)
abstract class BookibaDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
    abstract fun cartDao(): CartDao
    abstract fun orderDao(): OrderDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `cart_items` (`bookId` TEXT NOT NULL, `title` TEXT NOT NULL, `author` TEXT NOT NULL, `coverUrl` TEXT NOT NULL, `priceKsh` INTEGER NOT NULL, `quantity` INTEGER NOT NULL, PRIMARY KEY(`bookId`))"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `orders` (`id` TEXT NOT NULL, `dateMs` INTEGER NOT NULL, `totalAmount` INTEGER NOT NULL, `status` TEXT NOT NULL, PRIMARY KEY(`id`))"
                )
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `order_items` (`orderId` TEXT NOT NULL, `bookId` TEXT NOT NULL, `title` TEXT NOT NULL, `author` TEXT NOT NULL, `coverUrl` TEXT NOT NULL, `priceKsh` INTEGER NOT NULL, `quantity` INTEGER NOT NULL, PRIMARY KEY(`orderId`, `bookId`))"
                )
            }
        }
    }
}
