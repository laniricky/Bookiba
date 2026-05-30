package co.booknook.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val name: String = "",
    val otpCode: String = "",
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

sealed interface AuthEvent {
    data class LoginSubmit(val email: String, val password: String) : AuthEvent
    data class SignUpSubmit(val name: String, val email: String, val password: String) : AuthEvent
    data class ForgotPasswordSubmit(val email: String) : AuthEvent
    data class OtpSubmit(val code: String) : AuthEvent
    data object ClearError : AuthEvent
}

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.LoginSubmit -> login(event.email, event.password)
            is AuthEvent.SignUpSubmit -> signUp(event.name, event.email, event.password)
            is AuthEvent.ForgotPasswordSubmit -> forgotPassword(event.email)
            is AuthEvent.OtpSubmit -> verifyOtp(event.code)
            AuthEvent.ClearError -> _state.update { it.copy(error = null) }
        }
    }

    private fun login(email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            // TODO: wire to AuthRepository / API
            kotlinx.coroutines.delay(1000)
            _state.update { it.copy(isLoading = false, isAuthenticated = true) }
        }
    }

    private fun signUp(name: String, email: String, password: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            kotlinx.coroutines.delay(1000)
            _state.update { it.copy(isLoading = false, successMessage = "Check your email for a verification code") }
        }
    }

    private fun forgotPassword(email: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            kotlinx.coroutines.delay(800)
            _state.update { it.copy(isLoading = false, successMessage = "Password reset code sent to $email") }
        }
    }

    private fun verifyOtp(code: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            kotlinx.coroutines.delay(800)
            _state.update { it.copy(isLoading = false, isAuthenticated = true) }
        }
    }
}
