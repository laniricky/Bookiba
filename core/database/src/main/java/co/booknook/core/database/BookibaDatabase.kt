package co.booknook.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import co.booknook.core.database.dao.BookDao
import co.booknook.core.database.model.BookEntity

@Database(
    entities = [BookEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BookibaDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao
}
