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
import androidx.compose.material.icons.outlined.DocumentScanner

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
        // ── Gamification Banner ───────────────────────────────────
        item(span = { GridItemSpan(2) }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable { /* Join challenge */ },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkBrown),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🏆 2026 Reading Challenge",
                            color = Cream,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Read 12 books this year. Join 4,200 readers!",
                            color = Cream.copy(alpha = 0.8f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Button(
                        onClick = { /* Join */ },
                        colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
                    ) {
                        Text("Join", color = Cream, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── Search Bar ────────────────────────────────────────────
        item(span = { GridItemSpan(2) }) {
            SearchBar(
                query = state.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
                onClear = viewModel::onClearSearch,
                onSearch = { focusManager.clearFocus() },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        if (state.searchQuery.isNotBlank()) {
            if (state.isSearching) {
                item(span = { GridItemSpan(2) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = WarmBrown)
                    }
                }
            } else if (state.searchResults.isEmpty()) {
                item(span = { GridItemSpan(2) }) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No results found for \"${state.searchQuery}\"", color = WarmBrown, fontSize = 15.sp)
                    }
                }
            } else {
                item(span = { GridItemSpan(2) }) {
                    Text(
                        text = "Search Results",
                        color = DarkBrown,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(state.searchResults, span = { GridItemSpan(2) }) { book ->
                    SearchResultCard(
                        book = book,
                        onClick = { onBookClick(book.id) }
                    )
                }
            }
        } else {
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
                        text = "Moods & Themes",
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

            // ── Mood Grid ────────────────────────────────────────────
            itemsIndexed(state.genres) { index, genre ->
                MoodCard(
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

            // ── CEO's Bookshelf ──────────────────────────────────────────
            item(span = { GridItemSpan(2) }) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "CEO's Bookshelf",
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
                    items(state.newArrivals.asReversed()) { book ->
                        Column(
                            modifier = Modifier
                                .width(130.dp)
                                .clickable { onBookClick(book.id) },
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AsyncImage(
                                model = book.coverUrl,
                                contentDescription = book.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(170.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Text(book.title, color = DarkBrown, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
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
}

@Composable
private fun SearchResultCard(book: Book, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Cream),
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
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = book.title,
                        color = DarkBrown,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = book.author,
                        color = WarmBrown,
                        fontSize = 13.sp
                    )
                }
                Text(
                    text = "KSh ${"%,d".format(book.priceKsh)}",
                    color = DarkBrown,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = WarmBrown)
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
                        .addOnFailureListener {
                            // Ignored or handled via snackbar in a real app
                        }
                }) {
                    Icon(imageVector = Icons.Outlined.DocumentScanner, contentDescription = "Scan Barcode", tint = WarmBrown)
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
            color = Cream,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(12.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
