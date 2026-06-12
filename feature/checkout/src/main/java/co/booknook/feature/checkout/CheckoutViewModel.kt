package co.booknook.feature.checkout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.booknook.core.domain.model.CartItem
import co.booknook.core.domain.repository.CartRepository
import co.booknook.core.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CheckoutUiState(
    val cartItems: List<CartItem> = emptyList(),
    val totalAmount: Long = 0L,
    val isProcessing: Boolean = false,
    val paymentSuccess: Boolean = false,
    val authorizationUrl: String? = null,
    val error: String? = null
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CheckoutUiState())
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            cartRepository.getCartItems().collect { items ->
                var total = 0L
                for (item in items) {
                    total += item.priceKsh * item.quantity
                }
                _state.update { currentState -> currentState.copy(cartItems = items, totalAmount = total) }
            }
        }
    }

    fun payNow(paymentMethod: String = "MPESA", phoneNumber: String = "") {
        if (_state.value.cartItems.isEmpty() || _state.value.isProcessing) return

        if (paymentMethod == "MPESA" && phoneNumber.isBlank()) {
            _state.update { it.copy(error = "Please enter your M-Pesa phone number.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, error = null) }
            try {
                val authUrl = orderRepository.createOrder(
                    totalAmount = _state.value.totalAmount,
                    items = _state.value.cartItems,
                    paymentMethod = paymentMethod,
                    phoneNumber = phoneNumber
                )
                cartRepository.clearCart()
                _state.update { it.copy(isProcessing = false, paymentSuccess = true, authorizationUrl = authUrl) }
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = e.message ?: "Order failed. Please try again.") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
