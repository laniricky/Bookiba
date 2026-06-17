package co.booknook.feature.wishlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.booknook.core.domain.model.Book
import co.booknook.core.domain.repository.WishlistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WishlistUiState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class WishlistViewModel @Inject constructor(
    private val wishlistRepository: WishlistRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WishlistUiState())
    val state: StateFlow<WishlistUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            wishlistRepository.getWishlist()
                .catch { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
                .collect { books ->
                    _state.update { it.copy(isLoading = false, books = books, error = null) }
                }
        }
    }

    fun removeFromWishlist(bookId: String) {
        viewModelScope.launch {
            wishlistRepository.removeFromWishlist(bookId)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true) }
            wishlistRepository.getWishlist()
                .catch { e -> _state.update { it.copy(isRefreshing = false, error = e.message) } }
                .collect { books ->
                    _state.update { it.copy(isRefreshing = false, books = books, error = null) }
                }
        }
    }
}
