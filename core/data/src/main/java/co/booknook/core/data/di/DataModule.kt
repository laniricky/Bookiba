package co.booknook.core.data.di

import co.booknook.core.data.repository.OfflineFirstBookRepository
import co.booknook.core.domain.repository.BookRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindBookRepository(
        offlineFirstBookRepository: OfflineFirstBookRepository
    ): BookRepository
}
