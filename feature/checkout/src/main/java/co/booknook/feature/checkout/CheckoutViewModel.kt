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
import co.booknook.core.domain.model.Address
import co.booknook.core.domain.repository.AddressRepository

data class CheckoutUiState(
    val cartItems: List<CartItem> = emptyList(),
    val totalAmount: Long = 0L,
    val isProcessing: Boolean = false,
    val paymentSuccess: Boolean = false,
    val authorizationUrl: String? = null,
    val addresses: List<Address> = emptyList(),
    val selectedAddress: Address? = null,
    val shippingAddress: String = "", // Used for manual entry if no addresses
    val error: String? = null
)

@HiltViewModel
class CheckoutViewModel @Inject constructor(
    private val cartRepository: CartRepository,
    private val orderRepository: OrderRepository,
    private val addressRepository: AddressRepository
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
        viewModelScope.launch {
            addressRepository.getAddresses().collect { addresses ->
                val default = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
                _state.update { it.copy(addresses = addresses, selectedAddress = default) }
            }
        }
    }

    fun payNow(paymentMethod: String = "MPESA", phoneNumber: String = "", shippingAddress: String = "") {
        if (_state.value.cartItems.isEmpty() || _state.value.isProcessing) return

        if (paymentMethod == "MPESA" && phoneNumber.isBlank()) {
            _state.update { it.copy(error = "Please enter your M-Pesa phone number.") }
            return
        }
        val finalAddress = _state.value.selectedAddress?.fullAddress ?: shippingAddress
        
        if (finalAddress.isBlank()) {
            _state.update { it.copy(error = "Please enter a shipping address.") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isProcessing = true, error = null) }
            try {
                val authUrl = orderRepository.createOrder(
                    totalAmount = _state.value.totalAmount,
                    items = _state.value.cartItems,
                    paymentMethod = paymentMethod,
                    phoneNumber = phoneNumber,
                    shippingAddress = finalAddress
                )
                cartRepository.clearCart()
                _state.update { it.copy(isProcessing = false, paymentSuccess = true, authorizationUrl = authUrl) }
            } catch (e: Exception) {
                _state.update { it.copy(isProcessing = false, error = e.message ?: "Order failed. Please try again.") }
            }
        }
    }

    fun updateShippingAddress(address: String) {
        _state.update { it.copy(shippingAddress = address, selectedAddress = null) }
    }

    fun selectAddress(address: Address) {
        _state.update { it.copy(selectedAddress = address, shippingAddress = "") }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
