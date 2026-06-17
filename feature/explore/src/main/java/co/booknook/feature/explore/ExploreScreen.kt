package co.booknook.feature.explore

import co.booknook.core.designsystem.theme.*
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import coil.compose.AsyncImage
import co.booknook.core.domain.model.Book
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

// Warm palette for genre cards
private val genreGradients = listOf(
    listOf(Color(0xFF3D2B1F), Color(0xFF6B4226)),
    listOf(Color(0xFF1F2E3D), Color(0xFF2C4A6E)),
    listOf(Color(0xFF1A3320), Color(0xFF2D6A4F)),
    listOf(Color(0xFF3D1F1F), Color(0xFF7A3030)),
    listOf(Color(0xFF2E2D3D), Color(0xFF4A4870)),
    listOf(Color(0xFF3D3020), Color(0xFF6B5530)),
    listOf(Color(0xFF1F3D38), Color(0xFF2D6B62)),
    listOf(Color(0xFF3D2B35), Color(0xFF6B3D54))
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    onBookClick: (String) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val displayResults by viewModel.displayResults.collectAsState()
    val focusManager = LocalFocusManager.current

    var isRefreshing by remember { mutableStateOf(false) }
    val showSearchOverlay = state.isSearchFocused && state.searchQuery.isBlank() && state.searchHistory.isNotEmpty()

    LaunchedEffect(state.isLoading, state.isSearching) {
        if (!state.isLoading && !state.isSearching) isRefreshing = false
    }

    // Filter bottom sheet
    if (state.showFilterSheet) {
        FilterBottomSheet(
            filters = state.filters,
            availableConditions = state.availableConditions,
            onApply = { viewModel.onApplyFilters(it) },
            onDismiss = { viewModel.onHideFilterSheet() },
            onClear = { viewModel.onClearFilters() }
        )
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { isRefreshing = true; viewModel.refresh() },
        modifier = Modifier.fillMaxSize().background(androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // ── Gamification Banner ────────────────────────────────────────────
            item(span = { GridItemSpan(2) }) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .clickable { },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("🏆 2026 Reading Challenge", color = androidx.compose.material3.MaterialTheme.colorScheme.surface, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("Read 12 books this year. Join 4,200 readers!", color = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.8f), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                        Button(onClick = { }, colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)) {
                            Text("Join", color = androidx.compose.material3.MaterialTheme.colorScheme.surface, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Search Bar ────────────────────────────────────────────────────
            item(span = { GridItemSpan(2) }) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        SearchBarField(
                            query = state.searchQuery,
                            onQueryChange = viewModel::onSearchQueryChange,
                            onClear = viewModel::onClearSearch,
                            onFocusChange = viewModel::onSearchFocusChange,
                            onSearch = {
                                viewModel.onSearchSubmit(state.searchQuery)
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.weight(1f)
                        )
                        // Filter icon button
                        FilterIconButton(
                            active = state.filters.isActive,
                            onClick = { viewModel.onShowFilterSheet() }
                        )
                    }

                    // Active filter chips
                    if (state.filters.isActive) {
                        ActiveFilterChips(
                            filters = state.filters,
                            onRemoveGenre = { viewModel.onApplyFilters(state.filters.copy(selectedGenre = null)) },
                            onRemoveCondition = { viewModel.onApplyFilters(state.filters.copy(selectedCondition = null)) },
                            onRemovePriceRange = { viewModel.onApplyFilters(state.filters.copy(minPrice = null, maxPrice = null)) },
                            onClearAll = { viewModel.onClearFilters() }
                        )
                    }

                    // Search history overlay (focused, no query)
                    AnimatedVisibility(visible = showSearchOverlay) {
                        SearchHistoryPanel(
                            history = state.searchHistory,
                            onItemClick = { viewModel.onHistoryItemClick(it) },
                            onRemoveItem = { viewModel.onRemoveHistoryItem(it) },
                            onClearAll = { viewModel.onClearHistory() }
                        )
                    }
                    // Inline suggestions (while typing, before search fires)
                    AnimatedVisibility(visible = state.suggestions.isNotEmpty() && state.searchQuery.length >= 2 && !state.isSearching) {
                        SuggestionsPanel(
                            suggestions = state.suggestions,
                            onSuggestionClick = { term ->
                                viewModel.onSearchQueryChange(term)
                                viewModel.onSearchSubmit(term)
                                focusManager.clearFocus()
                            }
                        )
                    }
                }
            }

            // ── Search Results ─────────────────────────────────────────────────
            if (state.searchQuery.isNotBlank()) {
                if (state.isSearching) {
                    item(span = { GridItemSpan(2) }) {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                        }
                    }
                } else if (displayResults.isEmpty()) {
                    item(span = { GridItemSpan(2) }) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.SearchOff, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(48.dp))
                            Text("No results for \"${state.searchQuery}\"", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
                            if (state.filters.isActive) {
                                TextButton(onClick = { viewModel.onClearFilters() }) {
                                    Text("Clear filters", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                } else {
                    item(span = { GridItemSpan(2) }) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${displayResults.size} result${if (displayResults.size != 1) "s" else ""}", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            if (state.filters.isActive) {
                                Text("Filtered", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                            }
                        }
                    }
                    items(displayResults, span = { GridItemSpan(2) }) { book ->
                        SearchResultCard(book = book, onClick = { onBookClick(book.id) })
                    }
                }
            } else {
                // ── Collections header ─────────────────────────────────────────
                item(span = { GridItemSpan(2) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Moods & Themes", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("See all", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // ── Mood Grid ─────────────────────────────────────────────────
                itemsIndexed(state.genres) { index, genre ->
                    MoodCard(
                        genre = genre,
                        gradient = genreGradients[index % genreGradients.size],
                        onClick = { viewModel.onSearchQueryChange(genre.name) },
                        modifier = Modifier.padding(
                            start = if (index % 2 == 0) 16.dp else 6.dp,
                            end = if (index % 2 == 1) 16.dp else 6.dp,
                            bottom = 12.dp
                        )
                    )
                }

                // ── CEO's Bookshelf ───────────────────────────────────────────
                item(span = { GridItemSpan(2) }) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("CEO's Bookshelf", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("See all", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                item(span = { GridItemSpan(2) }) {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(state.newArrivals.asReversed()) { book ->
                            Column(modifier = Modifier.width(130.dp).clickable { onBookClick(book.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                AsyncImage(model = book.coverUrl, contentDescription = book.title, modifier = Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                                Text(book.title, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ── New Arrivals ──────────────────────────────────────────────
                item(span = { GridItemSpan(2) }) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("New Arrivals", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("See all", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }

                item(span = { GridItemSpan(2) }) {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(state.newArrivals) { book ->
                            Column(modifier = Modifier.width(110.dp).clickable { onBookClick(book.id) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                AsyncImage(model = book.coverUrl, contentDescription = book.title, modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
                                Text(book.title, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text("KSh ${"%,d".format(book.priceKsh)}", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

// ── Suggestions Panel ────────────────────────────────────────────────────────
@Composable
private fun SuggestionsPanel(
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            suggestions.forEach { suggestion ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
                    Text(suggestion, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

// ── Search History Panel ─────────────────────────────────────────────────────
@Composable
private fun SearchHistoryPanel(
    history: List<String>,
    onItemClick: (String) -> Unit,
    onRemoveItem: (String) -> Unit,
    onClearAll: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Searches", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onClearAll, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text("Clear all", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                }
            }
            history.take(6).forEach { term ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onItemClick(term) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.History, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                        Text(term, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 14.sp)
                    }
                    IconButton(onClick = { onRemoveItem(term) }, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Remove", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

// ── Active Filter Chips ──────────────────────────────────────────────────────
@Composable
private fun ActiveFilterChips(
    filters: SearchFilters,
    onRemoveGenre: () -> Unit,
    onRemoveCondition: () -> Unit,
    onRemovePriceRange: () -> Unit,
    onClearAll: () -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filters.selectedGenre?.let { genre ->
            item {
                FilterChip(
                    selected = true,
                    onClick = onRemoveGenre,
                    label = { Text(genre, fontSize = 12.sp) },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        selectedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        selectedTrailingIconColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
        filters.selectedCondition?.let { condition ->
            item {
                FilterChip(
                    selected = true,
                    onClick = onRemoveCondition,
                    label = { Text(condition, fontSize = 12.sp) },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        selectedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        selectedTrailingIconColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
        if (filters.minPrice != null || filters.maxPrice != null) {
            item {
                val label = when {
                    filters.minPrice != null && filters.maxPrice != null -> "KSh ${"%,d".format(filters.minPrice)} – ${"%,d".format(filters.maxPrice)}"
                    filters.minPrice != null -> "Min KSh ${"%,d".format(filters.minPrice)}"
                    else -> "Max KSh ${"%,d".format(filters.maxPrice)}"
                }
                FilterChip(
                    selected = true,
                    onClick = onRemovePriceRange,
                    label = { Text(label, fontSize = 12.sp) },
                    trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        selectedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        selectedTrailingIconColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    )
                )
            }
        }
    }
}

// ── Filter Icon Button ───────────────────────────────────────────────────────
@Composable
private fun FilterIconButton(active: Boolean, onClick: () -> Unit) {
    Box {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (active) androidx.compose.material3.MaterialTheme.colorScheme.onBackground else androidx.compose.material3.MaterialTheme.colorScheme.surface)
        ) {
            Icon(
                Icons.Outlined.Tune,
                contentDescription = "Filters",
                tint = if (active) androidx.compose.material3.MaterialTheme.colorScheme.surface else androidx.compose.material3.MaterialTheme.colorScheme.onSurface
            )
        }
        if (active) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFE57373))
                    .align(Alignment.TopEnd)
                    .offset(x = (-2).dp, y = 2.dp)
            )
        }
    }
}

// ── Filter Bottom Sheet ──────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    filters: SearchFilters,
    availableConditions: List<String>,
    onApply: (SearchFilters) -> Unit,
    onDismiss: () -> Unit,
    onClear: () -> Unit
) {
    var draft by remember { mutableStateOf(filters) }
    var minPriceText by remember { mutableStateOf(filters.minPrice?.toString() ?: "") }
    var maxPriceText by remember { mutableStateOf(filters.maxPrice?.toString() ?: "") }

    val genreOptions = listOf("Fiction", "Non-Fiction", "Thriller", "Business", "Fantasy", "Romance", "Philosophy", "Biography")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDefaults.DragHandle(color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Filter Results", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                TextButton(onClick = { draft = SearchFilters(); minPriceText = ""; maxPriceText = "" }) {
                    Text("Clear all", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                }
            }

            // Genre
            Text("Genre", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(genreOptions) { genre ->
                    FilterChip(
                        selected = draft.selectedGenre == genre,
                        onClick = { draft = if (draft.selectedGenre == genre) draft.copy(selectedGenre = null) else draft.copy(selectedGenre = genre) },
                        label = { Text(genre, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                            selectedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            // Condition
            Text("Condition", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(availableConditions) { condition ->
                    FilterChip(
                        selected = draft.selectedCondition == condition,
                        onClick = { draft = if (draft.selectedCondition == condition) draft.copy(selectedCondition = null) else draft.copy(selectedCondition = condition) },
                        label = { Text(condition, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                            selectedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            // Price Range
            Text("Price Range (KSh)", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = minPriceText,
                    onValueChange = { minPriceText = it.filter { c -> c.isDigit() } },
                    label = { Text("Min") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        focusedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        cursorColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                    )
                )
                OutlinedTextField(
                    value = maxPriceText,
                    onValueChange = { maxPriceText = it.filter { c -> c.isDigit() } },
                    label = { Text("Max") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        focusedLabelColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        cursorColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                    )
                )
            }

            // Apply button
            Button(
                onClick = {
                    onApply(draft.copy(
                        minPrice = minPriceText.toLongOrNull(),
                        maxPrice = maxPriceText.toLongOrNull()
                    ))
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
            ) {
                Text("Apply Filters", color = androidx.compose.material3.MaterialTheme.colorScheme.surface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ── Search Bar ───────────────────────────────────────────────────────────────
@Composable
private fun SearchBarField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scanner = remember {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .enableAutoZoom()
            .build()
        GmsBarcodeScanning.getClient(context, options)
    }

    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { onFocusChange(it.isFocused) },
        placeholder = {
            Text("Search books, authors, moods...", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 14.sp)
        },
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
        },
        trailingIcon = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                    }
                }
                IconButton(onClick = {
                    scanner.startScan()
                        .addOnSuccessListener { barcode ->
                            barcode.rawValue?.let { scannedValue ->
                                onQueryChange(scannedValue)
                                onSearch()
                            }
                        }
                        .addOnFailureListener { }
                }) {
                    Icon(imageVector = Icons.Outlined.DocumentScanner, contentDescription = "Scan Barcode", tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            unfocusedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            focusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            cursorColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() })
    )
}

@Composable
private fun SearchResultCard(book: Book, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(120.dp)) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier.padding(12.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(book.title, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(book.author, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("KSh ${"%,d".format(book.priceKsh)}", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                    book.condition?.let { cond ->
                        Surface(shape = RoundedCornerShape(8.dp), color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)) {
                            Text(cond, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoodCard(
    genre: GenreCollection,
    gradient: List<Color>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(gradient))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart
    ) {
        Text(
            text = genre.name,
            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(12.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
