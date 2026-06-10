package co.booknook.feature.bookdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.booknook.core.domain.model.Book
import co.booknook.core.domain.repository.BookRepository
import co.booknook.core.domain.repository.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookDetailUiState(
    val book: Book? = null,
    val similarBooks: List<Book> = emptyList(),
    val isLoading: Boolean = true,
    val isWishlisted: Boolean = false,
    val cartSuccess: Boolean = false,
    val error: String? = null
)

sealed interface BookDetailEvent {
    data object AddToCart : BookDetailEvent
    data object ToggleWishlist : BookDetailEvent
    data object BuyNow : BookDetailEvent
    data object ResetCartSuccess : BookDetailEvent
}

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val cartRepository: CartRepository
) : ViewModel() {

    private val bookId: String = checkNotNull(savedStateHandle["bookId"])

    private val _state = MutableStateFlow(BookDetailUiState())
    val state: StateFlow<BookDetailUiState> = _state.asStateFlow()

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                bookRepository.getBookById(bookId)
                    .catch { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
                    .collect { book ->
                        _state.update { it.copy(isLoading = false, book = book) }
                    }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onEvent(event: BookDetailEvent) {
        when (event) {
            is BookDetailEvent.ToggleWishlist ->
                _state.update { it.copy(isWishlisted = !it.isWishlisted) }
            is BookDetailEvent.AddToCart -> {
                viewModelScope.launch {
                    val book = _state.value.book
                    if (book != null) {
                        cartRepository.addToCart(book)
                        _state.update { it.copy(cartSuccess = true) }
                    }
                }
            }
            is BookDetailEvent.ResetCartSuccess ->
                _state.update { it.copy(cartSuccess = false) }
            is BookDetailEvent.BuyNow -> { /* navigate to checkout */ }
        }
    }
}
