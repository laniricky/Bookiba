package co.booknook.feature.bookdetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.booknook.core.domain.model.Book
import co.booknook.core.domain.model.Review
import co.booknook.core.domain.repository.BookRepository
import co.booknook.core.domain.repository.CartRepository
import co.booknook.core.domain.repository.ReviewRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BookDetailUiState(
    val book: Book? = null,
    val similarBooks: List<Book> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val isLoading: Boolean = true,
    val isWishlisted: Boolean = false,
    val cartSuccess: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    // Review submission state
    val pendingRating: Int = 0,
    val pendingComment: String = "",
    val isSubmittingReview: Boolean = false,
    val reviewSubmitSuccess: Boolean = false,
    val reviewSubmitError: String? = null,
    val showReviewSheet: Boolean = false
)

sealed interface BookDetailEvent {
    data object AddToCart : BookDetailEvent
    data object ToggleWishlist : BookDetailEvent
    data object BuyNow : BookDetailEvent
    data object ResetCartSuccess : BookDetailEvent
    data object ShowReviewSheet : BookDetailEvent
    data object HideReviewSheet : BookDetailEvent
    data class SetPendingRating(val rating: Int) : BookDetailEvent
    data class SetPendingComment(val comment: String) : BookDetailEvent
    data object SubmitReview : BookDetailEvent
    data object ResetReviewSuccess : BookDetailEvent
}

@HiltViewModel
class BookDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val cartRepository: CartRepository,
    private val reviewRepository: ReviewRepository,
    private val preferencesDataSource: co.booknook.core.datastore.BookibaPreferencesDataSource
) : ViewModel() {

    private val bookId: String = checkNotNull(savedStateHandle["bookId"])

    private val _state = MutableStateFlow(BookDetailUiState())
    val state: StateFlow<BookDetailUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesDataSource.authToken.collect { token ->
                _state.update { it.copy(isLoggedIn = !token.isNullOrEmpty()) }
            }
        }
        loadBook()
        loadReviews()
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

    private fun loadReviews() {
        viewModelScope.launch {
            reviewRepository.getReviews(bookId)
                .catch { /* silently ignore review load failure */ }
                .collect { reviews ->
                    _state.update { it.copy(reviews = reviews) }
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

            is BookDetailEvent.BuyNow -> { /* navigate to checkout — handled in NavHost */ }

            is BookDetailEvent.ShowReviewSheet ->
                _state.update { it.copy(showReviewSheet = true) }

            is BookDetailEvent.HideReviewSheet ->
                _state.update { it.copy(showReviewSheet = false, pendingRating = 0, pendingComment = "") }

            is BookDetailEvent.SetPendingRating ->
                _state.update { it.copy(pendingRating = event.rating) }

            is BookDetailEvent.SetPendingComment ->
                _state.update { it.copy(pendingComment = event.comment) }

            is BookDetailEvent.SubmitReview -> {
                val rating = _state.value.pendingRating
                if (rating == 0) return
                viewModelScope.launch {
                    _state.update { it.copy(isSubmittingReview = true, reviewSubmitError = null) }
                    val result = reviewRepository.submitReview(
                        bookId = bookId,
                        rating = rating,
                        comment = _state.value.pendingComment.trim().ifEmpty { null }
                    )
                    if (result.isSuccess) {
                        _state.update {
                            it.copy(
                                isSubmittingReview = false,
                                reviewSubmitSuccess = true,
                                showReviewSheet = false,
                                pendingRating = 0,
                                pendingComment = ""
                            )
                        }
                        // Reload reviews to show the new one
                        loadReviews()
                    } else {
                        _state.update {
                            it.copy(
                                isSubmittingReview = false,
                                reviewSubmitError = result.exceptionOrNull()?.message ?: "Failed to submit review"
                            )
                        }
                    }
                }
            }

            is BookDetailEvent.ResetReviewSuccess ->
                _state.update { it.copy(reviewSubmitSuccess = false) }
        }
    }
}
