package co.booknook.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.booknook.core.domain.model.Book
import co.booknook.core.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import co.booknook.core.domain.repository.CartRepository
import kotlinx.coroutines.Job

data class GenreCollection(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val bookCount: Int = 0
)

data class ExploreUiState(
    val searchQuery: String = "",
    val searchResults: List<Book> = emptyList(),
    val genres: List<GenreCollection> = defaultGenres(),
    val newArrivals: List<Book> = emptyList(),
    val isSearching: Boolean = false,
    val selectedGenre: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val cartSuccess: Boolean = false,
    val isLoggedIn: Boolean = false
)

private fun defaultGenres() = listOf(
    GenreCollection("thriller", "Keep me up all night"),
    GenreCollection("business", "Make me 1% better"),
    GenreCollection("fantasy", "Escape reality"),
    GenreCollection("romance", "Cry your eyes out"),
    GenreCollection("rare", "Vintage aesthetic"),
    GenreCollection("philosophy", "Deep thoughts")
)

@HiltViewModel
class ExploreViewModel @Inject constructor(
    private val bookRepository: co.booknook.core.domain.repository.BookRepository,
    private val cartRepository: CartRepository,
    private val preferencesDataSource: co.booknook.core.datastore.BookibaPreferencesDataSource
) : ViewModel() {

    private val _state = MutableStateFlow(ExploreUiState())
    val state: StateFlow<ExploreUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            preferencesDataSource.authToken.collect { token ->
                _state.update { it.copy(isLoggedIn = !token.isNullOrEmpty()) }
            }
        }
        observeSearch()
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        _state.update { it.copy(searchResults = emptyList(), isSearching = false) }
                    } else {
                        _state.update { it.copy(isSearching = true) }
                        try {
                            val results = bookRepository.searchBooks(query)
                            _state.update { it.copy(isSearching = false, searchResults = results) }
                        } catch (e: Exception) {
                            _state.update { it.copy(isSearching = false, error = e.message) }
                        }
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    fun onClearSearch() {
        searchQuery.value = ""
        _state.update { it.copy(searchQuery = "", searchResults = emptyList()) }
    }

    fun refresh() {
        // Since Explore is mostly static or handles search, we can just clear search or reload.
        // For now, it just toggles the loading state off immediately if we aren't searching.
        if (searchQuery.value.isNotBlank()) {
            searchQuery.value = searchQuery.value // Trigger re-search
        }
    }
}

