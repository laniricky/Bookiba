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

    @Binds
    @Singleton
    abstract fun bindTokenProvider(
        dataStoreTokenProvider: co.booknook.core.data.auth.DataStoreTokenProvider
    ): co.booknook.core.network.di.NetworkModule.TokenProvider
}
