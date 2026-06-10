package co.booknook.feature.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.booknook.core.domain.repository.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartRepository: CartRepository
) : ViewModel() {

    val uiState: StateFlow<CartUiState> = cartRepository.getCartItems()
        .map { items -> CartUiState(items = items) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CartUiState(isLoading = true)
        )

    fun updateQuantity(bookId: String, qty: Int) {
        viewModelScope.launch {
            cartRepository.updateQuantity(bookId, qty)
        }
    }

    fun removeItem(bookId: String) {
        viewModelScope.launch {
            cartRepository.removeFromCart(bookId)
        }
    }
}
