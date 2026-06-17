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
import androidx.lifecycle.SavedStateHandle
import co.booknook.core.datastore.BookibaPreferencesDataSource
import co.booknook.core.domain.repository.CartRepository
import kotlinx.coroutines.Job

data class GenreCollection(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val bookCount: Int = 0
)

data class SearchFilters(
    val selectedGenre: String? = null,
    val selectedCondition: String? = null,
    val minPrice: Long? = null,
    val maxPrice: Long? = null
) {
    val isActive: Boolean get() = selectedGenre != null || selectedCondition != null ||
            minPrice != null || maxPrice != null
}

data class ExploreUiState(
    val searchQuery: String = "",
    val searchResults: List<Book> = emptyList(),
    val filteredResults: List<Book> = emptyList(),
    val genres: List<GenreCollection> = defaultGenres(),
    val newArrivals: List<Book> = emptyList(),
    val isSearching: Boolean = false,
    val selectedGenre: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val cartSuccess: Boolean = false,
    val isLoggedIn: Boolean = false,
    val searchHistory: List<String> = emptyList(),
    val isSearchFocused: Boolean = false,
    val suggestions: List<String> = emptyList(),
    val filters: SearchFilters = SearchFilters(),
    val showFilterSheet: Boolean = false,
    val availableConditions: List<String> = listOf("New", "Like New", "Good", "Fair")
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
    private val savedStateHandle: SavedStateHandle,
    private val bookRepository: BookRepository,
    private val cartRepository: CartRepository,
    private val preferencesDataSource: BookibaPreferencesDataSource
) : ViewModel() {

    private val _state = MutableStateFlow(ExploreUiState())
    val state: StateFlow<ExploreUiState> = _state.asStateFlow()

    private val searchQueryFlow = MutableStateFlow("")

    init {
        viewModelScope.launch {
            preferencesDataSource.authToken.collect { token ->
                _state.update { it.copy(isLoggedIn = !token.isNullOrEmpty()) }
            }
        }
        viewModelScope.launch {
            preferencesDataSource.searchHistory.collect { history ->
                _state.update { it.copy(searchHistory = history) }
            }
        }
        observeSearch()
        savedStateHandle.get<String>("query")?.let { initialQuery ->
            if (initialQuery.isNotBlank()) {
                onSearchQueryChange(initialQuery)
            }
        }
    }

    @OptIn(FlowPreview::class)
    private fun observeSearch() {
        viewModelScope.launch {
            searchQueryFlow
                .debounce(300)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.isBlank()) {
                        _state.update { it.copy(searchResults = emptyList(), filteredResults = emptyList(), isSearching = false, suggestions = emptyList()) }
                    } else {
                        _state.update { it.copy(isSearching = true) }
                        try {
                            val results = bookRepository.searchBooks(query)
                            val filtered = applyFilters(results, _state.value.filters)
                            _state.update { it.copy(isSearching = false, searchResults = results, filteredResults = filtered, suggestions = emptyList()) }
                        } catch (e: Exception) {
                            _state.update { it.copy(isSearching = false, error = e.message) }
                        }
                    }
                }
        }
        // Suggestions flow: fires on shorter debounce, clears once real results arrive
        viewModelScope.launch {
            searchQueryFlow
                .debounce(150)
                .distinctUntilChanged()
                .collect { query ->
                    if (query.length >= 2 && !_state.value.isSearching) {
                        try {
                            val suggestions = bookRepository.getSuggestions(query)
                            _state.update { it.copy(suggestions = suggestions) }
                        } catch (_: Exception) { }
                    } else if (query.isBlank()) {
                        _state.update { it.copy(suggestions = emptyList()) }
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQueryFlow.value = query
        _state.update { it.copy(searchQuery = query) }
    }

    fun onClearSearch() {
        searchQueryFlow.value = ""
        _state.update { it.copy(searchQuery = "", searchResults = emptyList(), filteredResults = emptyList()) }
    }

    fun onSearchFocusChange(focused: Boolean) {
        _state.update { it.copy(isSearchFocused = focused) }
    }

    fun onSearchSubmit(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            preferencesDataSource.addSearchHistory(query)
        }
    }

    fun onRemoveHistoryItem(term: String) {
        viewModelScope.launch {
            preferencesDataSource.removeSearchHistory(term)
        }
    }

    fun onClearHistory() {
        viewModelScope.launch {
            preferencesDataSource.clearSearchHistory()
        }
    }

    fun onHistoryItemClick(term: String) {
        onSearchQueryChange(term)
        onSearchSubmit(term)
    }

    // ── Filter Actions ─────────────────────────────────────────────────────────
    fun onShowFilterSheet() = _state.update { it.copy(showFilterSheet = true) }
    fun onHideFilterSheet() = _state.update { it.copy(showFilterSheet = false) }

    fun onApplyFilters(filters: SearchFilters) {
        val filtered = applyFilters(_state.value.searchResults, filters)
        _state.update { it.copy(filters = filters, filteredResults = filtered, showFilterSheet = false) }
    }

    fun onClearFilters() {
        _state.update { it.copy(
            filters = SearchFilters(),
            filteredResults = _state.value.searchResults,
            showFilterSheet = false
        )}
    }

    private fun applyFilters(books: List<Book>, filters: SearchFilters): List<Book> {
        if (!filters.isActive) return books
        return books.filter { book ->
            val genreMatch = filters.selectedGenre == null ||
                    book.genre?.lowercase()?.contains(filters.selectedGenre.lowercase()) == true
            val conditionMatch = filters.selectedCondition == null ||
                    book.condition?.lowercase() == filters.selectedCondition.lowercase()
            val minPriceMatch = filters.minPrice == null || book.priceKsh >= filters.minPrice
            val maxPriceMatch = filters.maxPrice == null || book.priceKsh <= filters.maxPrice
            genreMatch && conditionMatch && minPriceMatch && maxPriceMatch
        }
    }

    val displayResults: StateFlow<List<Book>> = state
        .map { s -> if (s.filters.isActive) s.filteredResults else s.searchResults }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun refresh() {
        if (searchQueryFlow.value.isNotBlank()) {
            val q = searchQueryFlow.value
            searchQueryFlow.value = ""
            searchQueryFlow.value = q
        }
    }
}
