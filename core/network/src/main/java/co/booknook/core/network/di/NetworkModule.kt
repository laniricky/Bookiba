package co.booknook.core.network.di

import co.booknook.core.network.api.BookibaApi
import co.booknook.core.network.auth.AuthInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Provides the base URL from BuildConfig so each build variant can point to
 * the correct environment (emulator dev / staging / production).
 *
 * The token is provided by [TokenProvider] which is bound to the DataStore
 * implementation in the :core:datastore module.
 */
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    /**
     * Simple interface so the network module does not have a direct
     * compile-time dependency on the datastore module.
     */
    interface TokenProvider {
        /** Returns the current auth token, or an empty string if not logged in. */
        fun getToken(): String
    }

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(tokenProvider: TokenProvider): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Re-read the token on every request so token changes take effect immediately
                val token = tokenProvider.getToken()
                val req = chain.request().newBuilder().apply {
                    if (token.isNotBlank()) header("Authorization", "Bearer $token")
                }.build()
                chain.proceed(req)
            }
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)

        // Only attach full-body logging in debug builds
        if (co.booknook.core.network.BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            builder.addInterceptor(logging)
        }

        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient, json: Json): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(co.booknook.core.network.BuildConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideBookibaApi(retrofit: Retrofit): BookibaApi =
        retrofit.create(BookibaApi::class.java)
}
