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
    val paymentSuccess: Boolean = false
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

    fun payNow() {
        if (_state.value.cartItems.isEmpty() || _state.value.isProcessing) return

        viewModelScope.launch {
            _state.update { currentState -> currentState.copy(isProcessing = true) }
            try {
                // Submit order to repository
                orderRepository.createOrder(
                    totalAmount = _state.value.totalAmount,
                    items = _state.value.cartItems
                )
                // Clear the cart
                cartRepository.clearCart()
                // Mark as success
                _state.update { currentState -> currentState.copy(isProcessing = false, paymentSuccess = true) }
            } catch (e: Exception) {
                _state.update { currentState -> currentState.copy(isProcessing = false) }
            }
        }
    }
}
