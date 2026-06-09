package co.booknook.feature.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import coil.compose.AsyncImage

private val Cream = Color(0xFFF5F0E8)
private val DarkBrown = Color(0xFF1A1512)
private val WarmBrown = Color(0xFF8B7355)
private val SoftWhite = Color(0xFFFEFCF9)

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
    onGenreClick: (String) -> Unit,
    viewModel: ExploreViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current

    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoading, state.isSearching) {
        if (!state.isLoading && !state.isSearching) {
            isRefreshing = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.refresh()
        },
        modifier = Modifier
            .fillMaxSize()
            .background(SoftWhite)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
        // ── Search Bar ────────────────────────────────────────────
        item(span = { GridItemSpan(2) }) {
            Spacer(Modifier.height(12.dp))
            SearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onClear = viewModel::onClearSearch,
                onSearch = { focusManager.clearFocus() },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // ── Collections header ────────────────────────────────────
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Collections",
                    color = DarkBrown,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "See all",
                    color = WarmBrown,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ── Genre Grid ────────────────────────────────────────────
        itemsIndexed(state.genres) { index, genre ->
            GenreCard(
                genre = genre,
                gradient = genreGradients[index % genreGradients.size],
                onClick = { 
                    viewModel.onSearchQueryChange(genre.name)
                    onGenreClick(genre.id) 
                },
                modifier = Modifier.padding(
                    start = if (index % 2 == 0) 16.dp else 6.dp,
                    end = if (index % 2 == 1) 16.dp else 6.dp,
                    bottom = 12.dp
                )
            )
        }

        // ── New Arrivals ──────────────────────────────────────────
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "New Arrivals",
                    color = DarkBrown,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("See all", color = WarmBrown, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        item(span = { GridItemSpan(2) }) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(state.newArrivals) { book ->
                    Column(
                        modifier = Modifier
                            .width(110.dp)
                            .clickable { onBookClick(book.id) },
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = book.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Text(book.title, color = DarkBrown, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("KSh ${"%,d".format(book.priceKsh)}", color = WarmBrown, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = {
            Text(
                "Search books, authors, moods...",
                color = WarmBrown.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(Icons.Outlined.Search, contentDescription = null, tint = WarmBrown)
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = WarmBrown)
                }
            }
        },
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = WarmBrown,
            unfocusedBorderColor = Cream,
            focusedContainerColor = Cream,
            unfocusedContainerColor = Cream,
            cursorColor = DarkBrown
        ),
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() })
    )
}

@Composable
private fun GenreCard(
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
            color = Cream,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(12.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
