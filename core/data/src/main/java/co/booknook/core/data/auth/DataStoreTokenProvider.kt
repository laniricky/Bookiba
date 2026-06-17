package co.booknook.core.data.auth

import co.booknook.core.datastore.BookibaPreferencesDataSource
import co.booknook.core.network.di.NetworkModule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataStoreTokenProvider @Inject constructor(
    preferencesDataSource: BookibaPreferencesDataSource
) : NetworkModule.TokenProvider {
    
    @Volatile
    private var currentToken: String = ""

    init {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            preferencesDataSource.authToken.collect { token ->
                currentToken = token ?: ""
            }
        }
    }

    override fun getToken(): String {
        return currentToken
    }
}
