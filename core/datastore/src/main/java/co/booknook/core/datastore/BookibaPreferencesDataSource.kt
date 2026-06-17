package co.booknook.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

class BookibaPreferencesDataSource @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {

    val authToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[AUTH_TOKEN]
    }

    val hasCompletedOnboarding: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[HAS_COMPLETED_ONBOARDING] ?: false
    }

    // Returns recent search terms as a list (most recent first, max 10)
    val searchHistory: Flow<List<String>> = dataStore.data.map { preferences ->
        preferences[SEARCH_HISTORY]?.split(DELIMITER)
            ?.filter { it.isNotBlank() }
            ?: emptyList()
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

    suspend fun addSearchHistory(term: String) {
        if (term.isBlank()) return
        dataStore.edit { preferences ->
            val existing = preferences[SEARCH_HISTORY]
                ?.split(DELIMITER)
                ?.filter { it.isNotBlank() && it != term }
                ?: emptyList()
            val updated = (listOf(term) + existing).take(MAX_HISTORY)
            preferences[SEARCH_HISTORY] = updated.joinToString(DELIMITER)
        }
    }

    suspend fun removeSearchHistory(term: String) {
        dataStore.edit { preferences ->
            val existing = preferences[SEARCH_HISTORY]
                ?.split(DELIMITER)
                ?.filter { it.isNotBlank() && it != term }
                ?: emptyList()
            preferences[SEARCH_HISTORY] = existing.joinToString(DELIMITER)
        }
    }

    suspend fun clearSearchHistory() {
        dataStore.edit { preferences ->
            preferences.remove(SEARCH_HISTORY)
        }
    }

    val isDarkMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_DARK_MODE] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_DARK_MODE] = enabled
        }
    }

    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[NOTIFICATIONS_ENABLED] ?: true
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    companion object {
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
        private val SEARCH_HISTORY = stringPreferencesKey("search_history")
        private val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private const val DELIMITER = "|"
        private const val MAX_HISTORY = 10
    }
}

