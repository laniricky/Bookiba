package co.booknook.feature.explore

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.booknook.core.domain.model.Book
import co.booknook.core.domain.repository.BookRepository
import co.booknook.core.domain.repository.CartRepository
import co.booknook.core.datastore.BookibaPreferencesDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoryViewerUiState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val cartSuccess: Boolean = false,
    val isLoggedIn: Boolean = false
)

@HiltViewModel
class StoryViewerViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val cartRepository: CartRepository,
    private val preferencesDataSource: BookibaPreferencesDataSource
) : ViewModel() {

    private val _state = MutableStateFlow(StoryViewerUiState())
    val state: StateFlow<StoryViewerUiState> = _state.asStateFlow()

    val queryTag: String = checkNotNull(savedStateHandle["queryTag"])
    val storyTitle: String = savedStateHandle["title"] ?: queryTag

    init {
        viewModelScope.launch {
            preferencesDataSource.authToken.collect { token ->
                _state.update { it.copy(isLoggedIn = !token.isNullOrEmpty()) }
            }
        }
        loadStory()
    }

    private fun loadStory() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                // Fetch books based on queryTag
                val books = bookRepository.searchBooks(queryTag)
                _state.update { it.copy(books = books, isLoading = false) }
            } catch (e: Exception) {
                _state.update { it.copy(error = e.message ?: "Failed to load story", isLoading = false) }
            }
        }
    }

    fun addToCart(book: Book) {
        viewModelScope.launch {
            cartRepository.addToCart(book)
            _state.update { it.copy(cartSuccess = true) }
        }
    }

    fun resetCartSuccess() {
        _state.update { it.copy(cartSuccess = false) }
    }
}
