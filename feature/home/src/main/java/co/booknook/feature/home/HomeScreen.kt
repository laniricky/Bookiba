package co.booknook.feature.home

import co.booknook.core.designsystem.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import coil.compose.AsyncImage
import co.booknook.core.domain.model.Book


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onBookClick: (String) -> Unit,
    onSearchClick: (String?) -> Unit,
    onNavigateToAuth: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }
    var discoverVisibleCount by remember { mutableIntStateOf(6) }

    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            isRefreshing = false
        }
    }

    LaunchedEffect(state.cartSuccess) {
        if (state.cartSuccess) {
            snackbarHostState.showSnackbar("Added to cart")
            viewModel.resetCartSuccess()
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            discoverVisibleCount = 6
            viewModel.refresh()
        },
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // â”€â”€ Top Bar â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item {
                HomeTopBar(onNotificationsClick = {})
            }

            // â”€â”€ Story Tray â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item {
                StoryTray(stories = state.stories, onClick = { onSearchClick(it) })
            }

            // â”€â”€ Hero Banners â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item {
                if (state.isLoading && state.banners.isEmpty()) {
                    co.booknook.core.designsystem.components.BannerSkeleton(modifier = Modifier.padding(vertical = 16.dp))
                } else if (state.banners.isNotEmpty()) {
                    val banners = state.banners
                    val pagerState = rememberPagerState(pageCount = { banners.size })
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    pageSpacing = 8.dp
                ) { page ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxSize(),
                        elevation = CardDefaults.cardElevation(4.dp)
                    ) {
                        AsyncImage(
                            model = banners[page].imageUrl,
                            contentDescription = banners[page].title ?: "Marketing Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                }
            }

            // â”€â”€ "Found Today" Section -> Trending in Nairobi â”€â”€â”€â”€â”€â”€â”€â”€â”€
            item {
                SectionHeader(title = "Trending in Nairobi Right Now", onSeeAll = { onSearchClick(null) })
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.featuredBooks) { book ->
                        FeaturedBookCard(book = book, onClick = { onBookClick(book.id) }, onAddToCart = { 
                            if (state.isLoggedIn) viewModel.addToCart(book) else onNavigateToAuth()
                        })
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Staff Pick ──────────────────────────────────────────────────────
            state.staffPick?.let { pick ->
                item {
                    SectionHeader(title = "Staff Pick", onSeeAll = { onSearchClick(null) })
                    StaffPickCard(book = pick, onClick = { onBookClick(pick.id) }, onAddToCart = { 
                        if (state.isLoggedIn) viewModel.addToCart(pick) else onNavigateToAuth()
                    })
                    Spacer(Modifier.height(24.dp))
                }
            }

            // ── New Arrivals ────────────────────────────────────────────────────
            item {
                SectionHeader(title = "New Arrivals", onSeeAll = { onSearchClick(null) })
            }
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(state.newArrivals) { book ->
                        SmallBookCard(book = book, onClick = { onBookClick(book.id) }, onAddToCart = { 
                            if (state.isLoggedIn) viewModel.addToCart(book) else onNavigateToAuth()
                        })
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // ── Discover – random 2×3 grid ──────────────────────────────────
            if (state.randomBooks.isNotEmpty()) {
                item {
                    SectionHeader(title = "Discover", onSeeAll = { onSearchClick(null) })
                }
                val chunks = state.randomBooks.take(discoverVisibleCount).chunked(2)
                items(chunks) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        row.forEach { book ->
                            GridBookCard(
                                book = book,
                                modifier = Modifier.weight(1f),
                                onClick = { onBookClick(book.id) },
                                onAddToCart = {
                                    if (state.isLoggedIn) viewModel.addToCart(book) else onNavigateToAuth()
                                }
                            )
                        }
                        // Fill the gap if the last row has only 1 book
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(12.dp))
                }
                if (discoverVisibleCount < state.randomBooks.size) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            OutlinedButton(
                                onClick = { discoverVisibleCount += 6 },
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "Load More",
                                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                } else {
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }

        // Loading
        if (state.isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                Text(
                    text = "Loading books...",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp
                )
            }
        }

        // Error with retry
        if (!state.isLoading && state.error != null) {
            Column(
                modifier = Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "⚠️ Couldn't load books",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = state.error ?: "Unknown error",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp
                )
                Button(
                    onClick = { viewModel.refresh() },
                    colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                ) {
                    Text("Retry", color = androidx.compose.material3.MaterialTheme.colorScheme.surface)
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(onNotificationsClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Bookiba",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Light
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = {}) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = "Wishlist",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                )
            }
            IconButton(onClick = {
                android.widget.Toast.makeText(context, "Coming soon", android.widget.Toast.LENGTH_SHORT).show()
            }) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun StoryTray(stories: List<co.booknook.core.domain.model.Editorial>, onClick: (String) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(stories) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { onClick(story.queryTag) }
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(androidx.compose.material3.MaterialTheme.colorScheme.onSurface, Color(0xFF5C3D2E))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = story.label.first().toString(),
                        color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = story.label,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, onSeeAll: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "See all",
            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.clickable(onClick = onSeeAll)
        )
    }
}

@Composable
private fun FeaturedBookCard(book: Book, onClick: () -> Unit, onAddToCart: () -> Unit) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = book.title,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.inventoryCount in 1..4) {
                    Text(
                        text = "ðŸ”¥ Only ${book.inventoryCount} left!",
                        color = Color(0xFFD62828),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                Text(
                    text = book.author,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                if (book.edition != null) {
                    Text(
                        text = "${book.edition} Edition",
                        color = AccentGreen,
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "KSh ${"%,d".format(book.priceKsh)}",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onAddToCart, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.ShoppingCart, contentDescription = "Add to Cart", tint = AccentGreen, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun StaffPickCard(book: Book, onClick: () -> Unit, onAddToCart: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(modifier = Modifier.height(160.dp)) {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .width(110.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = book.title,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = book.author,
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp
                    )
                }
                Row(modifier = Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "KSh ${"%,d".format(book.priceKsh)}",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onAddToCart, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.ShoppingCart, contentDescription = "Add to Cart", tint = AccentGreen, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallBookCard(book: Book, onClick: () -> Unit, onAddToCart: () -> Unit) {
    Column(
        modifier = Modifier
            .width(110.dp)
            .clickable(onClick = onClick),
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
        Text(
            text = book.title,
            color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (book.inventoryCount in 1..4) {
            Text(
                text = "Only ${book.inventoryCount} left",
                color = Color(0xFFD62828),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "KSh ${"%,d".format(book.priceKsh)}",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onAddToCart, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Outlined.ShoppingCart, contentDescription = "Add to Cart", tint = AccentGreen, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun GridBookCard(
    book: Book,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onAddToCart: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = book.title,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (book.inventoryCount in 1..4) {
                    Text(
                        text = "Only ${book.inventoryCount} left",
                        color = Color(0xFFD62828),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KSh ${"%,d".format(book.priceKsh)}",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onAddToCart, modifier = Modifier.size(24.dp)) {
                        Icon(
                            Icons.Outlined.ShoppingCart,
                            contentDescription = "Add to Cart",
                            tint = AccentGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
