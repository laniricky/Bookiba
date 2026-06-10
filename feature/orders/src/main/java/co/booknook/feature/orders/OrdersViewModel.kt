package co.booknook.feature.orders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.booknook.core.domain.model.Order
import co.booknook.core.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrdersUiState(
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class OrdersViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OrdersUiState())
    val state: StateFlow<OrdersUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            orderRepository.getOrders().collect { orderList ->
                _state.update { it.copy(orders = orderList, isLoading = false) }
            }
        }
    }
}
