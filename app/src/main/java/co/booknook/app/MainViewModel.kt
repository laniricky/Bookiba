package co.booknook.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.booknook.core.domain.repository.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

import co.booknook.core.network.api.BookibaApi
import co.booknook.core.network.model.FcmTokenRequest
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    cartRepository: CartRepository,
    preferencesDataSource: co.booknook.core.datastore.BookibaPreferencesDataSource,
    private val api: BookibaApi
) : ViewModel() {

    init {
        viewModelScope.launch {
            preferencesDataSource.authToken.collect { token ->
                if (!token.isNullOrEmpty()) {
                    try {
                        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val fcmToken = task.result
                                viewModelScope.launch {
                                    try {
                                        api.uploadFcmToken(FcmTokenRequest(fcmToken))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    val cartCount: StateFlow<Int> = cartRepository.getCartCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val isDarkMode: StateFlow<Boolean> = preferencesDataSource.isDarkMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )
}
