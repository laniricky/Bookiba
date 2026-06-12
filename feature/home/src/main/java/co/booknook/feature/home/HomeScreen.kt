package co.booknook.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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

private val Cream = Color(0xFFF5F0E8)
private val DarkBrown = Color(0xFF1A1512)
private val WarmBrown = Color(0xFF8B7355)
private val SoftWhite = Color(0xFFFEFCF9)
private val AccentGreen = Color(0xFF2D6A4F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onBookClick: (String) -> Unit,
    onSearchClick: () -> Unit,
    onNavigateToAuth: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var isRefreshing by remember { mutableStateOf(false) }

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
            viewModel.refresh()
        },
        modifier = Modifier
            .fillMaxSize()
            .background(SoftWhite)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // ── Top Bar ──────────────────────────────────────────────
            item {
                HomeTopBar(onNotificationsClick = {})
            }

            // ── Story Tray ───────────────────────────────────────────
            item {
                StoryTray(stories = state.stories)
            }

            // ── Hero Banners ──────────────────────────────────────────
            item {
                val banners = if (state.banners.isNotEmpty()) {
                    state.banners
                } else {
                    listOf(
                        co.booknook.core.domain.model.Banner("b1", "file:///C:/Users/rucom/.gemini/antigravity/brain/8e18155c-0fbc-4bae-8b27-be6e0cb044f0/banner_flash_sale_1781174890145.png", null, null),
                        co.booknook.core.domain.model.Banner("b2", "file:///C:/Users/rucom/.gemini/antigravity/brain/8e18155c-0fbc-4bae-8b27-be6e0cb044f0/banner_african_voices_1781174913954.png", null, null),
                        co.booknook.core.domain.model.Banner("b3", "file:///C:/Users/rucom/.gemini/antigravity/brain/8e18155c-0fbc-4bae-8b27-be6e0cb044f0/banner_staff_picks_1781174924594.png", null, null)
                    )
                }

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

            // ── "Found Today" Section -> Trending in Nairobi ─────────
            item {
                SectionHeader(title = "Trending in Nairobi Right Now", onSeeAll = onSearchClick)
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

            // ── Staff Pick ───────────────────────────────────────────
            state.staffPick?.let { pick ->
                item {
                    SectionHeader(title = "Staff Pick", onSeeAll = onSearchClick)
                    StaffPickCard(book = pick, onClick = { onBookClick(pick.id) }, onAddToCart = { 
                        if (state.isLoggedIn) viewModel.addToCart(pick) else onNavigateToAuth()
                    })
                    Spacer(Modifier.height(24.dp))
                }
            }

            // ── New Arrivals ─────────────────────────────────────────
            item {
                SectionHeader(title = "New Arrivals", onSeeAll = onSearchClick)
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
        }

        // Loading
        if (state.isLoading) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(color = WarmBrown)
                Text(
                    text = "Loading books…",
                    color = WarmBrown,
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
                    color = DarkBrown,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = state.error ?: "Unknown error",
                    color = WarmBrown,
                    fontSize = 12.sp
                )
                Button(
                    onClick = { viewModel.refresh() },
                    colors = ButtonDefaults.buttonColors(containerColor = WarmBrown)
                ) {
                    Text("Retry", color = Cream)
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(onNotificationsClick: () -> Unit) {
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
                color = DarkBrown,
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
                    tint = DarkBrown
                )
            }
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    imageVector = Icons.Outlined.Notifications,
                    contentDescription = "Notifications",
                    tint = DarkBrown
                )
            }
        }
    }
}

@Composable
private fun StoryTray(stories: List<StoryItem>) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(stories) { story ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(WarmBrown, Color(0xFF5C3D2E))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = story.label.first().toString(),
                        color = Cream,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = story.label,
                    color = DarkBrown,
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
            color = DarkBrown,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "See all",
            color = WarmBrown,
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
        colors = CardDefaults.cardColors(containerColor = Cream),
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
                    color = DarkBrown,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (book.title.length % 3 == 0) {
                    Text(
                        text = "🔥 Only ${(book.title.length % 5) + 1} left!",
                        color = Color(0xFFD62828),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                Text(
                    text = book.author,
                    color = WarmBrown,
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
                        color = DarkBrown,
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
        colors = CardDefaults.cardColors(containerColor = Cream),
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
                        color = DarkBrown,
                        fontSize = 17.sp,
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
                Row(modifier = Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "KSh ${"%,d".format(book.priceKsh)}",
                        color = DarkBrown,
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
            color = DarkBrown,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (book.title.length % 4 == 0) {
            Text(
                text = "⏱️ Sale ends soon",
                color = Color(0xFFD62828),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "KSh ${"%,d".format(book.priceKsh)}",
                color = WarmBrown,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = onAddToCart, modifier = Modifier.size(20.dp)) {
                Icon(Icons.Outlined.ShoppingCart, contentDescription = "Add to Cart", tint = AccentGreen, modifier = Modifier.size(16.dp))
            }
        }
    }
}
