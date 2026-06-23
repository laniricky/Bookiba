package co.booknook.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.booknook.core.datastore.BookibaPreferencesDataSource
import co.booknook.core.network.api.BookibaApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val isLoggedIn: Boolean = false,
    val isDarkMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val isLoading: Boolean = false,
    val deleteAccountSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesDataSource: BookibaPreferencesDataSource,
    private val bookibaApi: BookibaApi
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            launch {
                preferencesDataSource.authToken.collect { token ->
                    _state.update { it.copy(isLoggedIn = token != null) }
                }
            }
            launch {
                preferencesDataSource.isDarkMode.collect { isDark ->
                    _state.update { it.copy(isDarkMode = isDark) }
                }
            }
            launch {
                preferencesDataSource.notificationsEnabled.collect { enabled ->
                    _state.update { it.copy(notificationsEnabled = enabled) }
                }
            }
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setDarkMode(enabled)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferencesDataSource.setNotificationsEnabled(enabled)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val token = preferencesDataSource.authToken.firstOrNull()
                if (token.isNullOrEmpty()) {
                    _state.update { it.copy(isLoading = false, error = "Not logged in") }
                    return@launch
                }
                
                // Call API to delete account
                bookibaApi.deleteAccount("Bearer $token")
                
                // Clear local token
                preferencesDataSource.clearAuthToken()
                
                _state.update { it.copy(isLoading = false, deleteAccountSuccess = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Failed to delete account") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun logout() {
        viewModelScope.launch {
            preferencesDataSource.clearAuthToken()
        }
    }
}
