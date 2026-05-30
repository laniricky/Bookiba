package co.booknook.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.booknook.core.domain.model.Book
import co.booknook.core.domain.usecase.GetFeaturedBooksUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val featuredBooks: List<Book> = emptyList(),
    val staffPick: Book? = null,
    val newArrivals: List<Book> = emptyList(),
    val stories: List<StoryItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

data class StoryItem(
    val id: String,
    val label: String,
    val imageUrl: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getFeaturedBooksUseCase: GetFeaturedBooksUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        loadHome()
    }

    private fun loadHome() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                getFeaturedBooksUseCase()
                    .catch { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
                    .collect { books ->
                        _state.update {
                            it.copy(
                                isLoading = false,
                                featuredBooks = books,
                                staffPick = books.firstOrNull(),
                                newArrivals = books.drop(1).take(6),
                                stories = defaultStories(),
                                error = null
                            )
                        }
                    }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun refresh() = loadHome()

    private fun defaultStories() = listOf(
        StoryItem("1", "New"),
        StoryItem("2", "Staff\nPicks"),
        StoryItem("3", "Fiction"),
        StoryItem("4", "Philosophy"),
        StoryItem("5", "Vintage")
    )
}
