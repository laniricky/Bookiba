package co.booknook.core.data.auth

import co.booknook.core.datastore.BookibaPreferencesDataSource
import co.booknook.core.network.di.NetworkModule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

class DataStoreTokenProvider @Inject constructor(
    private val preferencesDataSource: BookibaPreferencesDataSource
) : NetworkModule.TokenProvider {
    
    override fun getToken(): String {
        return runBlocking { 
            preferencesDataSource.authToken.first() ?: "" 
        }
    }
}
