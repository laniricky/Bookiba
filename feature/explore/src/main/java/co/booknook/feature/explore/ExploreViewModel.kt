package co.booknook.feature.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.booknook.core.domain.model.Book
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val isLoading: Boolean = false,
    val error: String? = null
)

private fun defaultGenres() = listOf(
    GenreCollection("dark_academia", "Dark Academia"),
    GenreCollection("vintage_classics", "Vintage Classics"),
    GenreCollection("african_lit", "African Literature"),
    GenreCollection("rare_finds", "Rare Finds"),
    GenreCollection("philosophy", "Philosophy"),
    GenreCollection("horror", "Horror"),
    GenreCollection("vintage_scifi", "Vintage Sci-Fi"),
    GenreCollection("annotated", "Annotated Copies")
)

@HiltViewModel
class ExploreViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(ExploreUiState())
    val state: StateFlow<ExploreUiState> = _state.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
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
                        // TODO: wire to actual search use case
                        _state.update { it.copy(isSearching = false) }
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
}
