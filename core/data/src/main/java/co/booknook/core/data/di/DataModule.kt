package co.booknook.core.data.di

import co.booknook.core.data.repository.LocalCartRepository
import co.booknook.core.data.repository.OfflineFirstBookRepository
import co.booknook.core.domain.repository.BookRepository
import co.booknook.core.domain.repository.CartRepository
import co.booknook.core.domain.repository.OrderRepository
import co.booknook.core.data.repository.LocalOrderRepository
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
    abstract fun bindCartRepository(
        localCartRepository: LocalCartRepository
    ): CartRepository

    @Binds
    @Singleton
    abstract fun bindOrderRepository(
        localOrderRepository: LocalOrderRepository
    ): OrderRepository

    @Binds
    @Singleton
    abstract fun bindTokenProvider(
        dataStoreTokenProvider: co.booknook.core.data.auth.DataStoreTokenProvider
    ): co.booknook.core.network.di.NetworkModule.TokenProvider
}
