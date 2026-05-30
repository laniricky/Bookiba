package co.booknook.core.database.di

import android.content.Context
import androidx.room.Room
import co.booknook.core.database.BookibaDatabase
import co.booknook.core.database.dao.BookDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideBookibaDatabase(
        @ApplicationContext context: Context
    ): BookibaDatabase {
        return Room.databaseBuilder(
            context,
            BookibaDatabase::class.java,
            "bookiba_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideBookDao(
        database: BookibaDatabase
    ): BookDao {
        return database.bookDao()
    }
}
