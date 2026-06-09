package co.booknook.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.booknook.core.datastore.BookibaPreferencesDataSource
import co.booknook.core.network.api.BookibaApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val api: BookibaApi,
    private val dataStore: BookibaPreferencesDataSource
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.authToken.collect { token ->
                val loggedIn = token != null
                _state.update { it.copy(isLoggedIn = loggedIn) }
                if (loggedIn) {
                    fetchProfile()
                } else {
                    _state.update { it.copy(name = "", bio = "", ordersCount = 0, wishlistCount = 0, reviewsCount = 0) }
                }
            }
        }
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val response = api.getUserProfile()
                if (response.error == null) {
                    val orders = response.ordersCount ?: 0
                    val wishlist = response.wishlistCount ?: 0
                    val reviews = response.reviewsCount ?: 0
                    _state.update {
                        it.copy(
                            isLoading = false,
                            name = response.name ?: "",
                            bio = response.email ?: "",
                            ordersCount = orders,
                            wishlistCount = wishlist,
                            reviewsCount = reviews
                        )
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = response.error) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            dataStore.clearAuthToken()
        }
    }
}
