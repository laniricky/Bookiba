package co.booknook.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BookibaPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val authToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[AUTH_TOKEN]
    }

    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HAS_COMPLETED_ONBOARDING] ?: false
    }

    suspend fun saveAuthToken(token: String) {
        dataStore.edit { preferences ->
            preferences[AUTH_TOKEN] = token
        }
    }

    suspend fun clearAuthToken() {
        dataStore.edit { preferences ->
            preferences.remove(AUTH_TOKEN)
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[HAS_COMPLETED_ONBOARDING] = completed
        }
    }

    companion object {
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    }
}
