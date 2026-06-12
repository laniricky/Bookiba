package co.booknook.feature.bookdetails

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

@Composable
fun BookDetailScreen(
    onBack: () -> Unit,
    onAddToCart: (String) -> Unit,
    onBuyNow: (String) -> Unit,
    onNavigateToAuth: () -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.cartSuccess) {
        if (state.cartSuccess) {
            snackbarHostState.showSnackbar("Added to cart")
            viewModel.onEvent(BookDetailEvent.ResetCartSuccess)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(SoftWhite)) {
        state.book?.let { book ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // ── Image Gallery ─────────────────────────────────────
                item {
                    BookImageGallery(
                        imageUrls = book.imageUrls.ifEmpty { listOf(book.coverUrl) },
                        onBack = onBack,
                        isWishlisted = state.isWishlisted,
                        onToggleWishlist = { viewModel.onEvent(BookDetailEvent.ToggleWishlist) }
                    )
                }

                // ── Availability Badge ────────────────────────────────
                item {
                    Surface(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFEDE4D6)
                    ) {
                        Text(
                            text = "One copy available",
                            color = WarmBrown,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // ── Title & Price ─────────────────────────────────────
                item {
                    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                        Text(book.title, color = DarkBrown, fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 32.sp)
                        Text(book.author, color = WarmBrown, fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
                        Text(
                            text = "KSh ${"%,d".format(book.priceKsh)}",
                            color = DarkBrown,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                    }
                }

                // ── Edition Info Row ──────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        book.edition?.let {
                            InfoChip(label = it, icon = Icons.Outlined.DateRange)
                        }
                        book.condition?.let { InfoChip(label = it, icon = Icons.Outlined.CheckCircle) }
                    }
                }

                // ── Divider ──────────────────────────────────────────
                item { HorizontalDivider(color = Cream, modifier = Modifier.padding(horizontal = 20.dp)) }

                // ── About Section ─────────────────────────────────────
                item {
                    var expanded by remember { mutableStateOf(false) }
                    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                        Text("About the book", color = DarkBrown, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = book.description ?: "A timeless classic. This vintage edition is in good condition with minor cover wear and yellowed pages due to age.",
                            color = WarmBrown,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            maxLines = if (expanded) Int.MAX_VALUE else 4,
                            overflow = TextOverflow.Ellipsis,
                            fontStyle = FontStyle.Italic
                        )
                        TextButton(onClick = { expanded = !expanded }) {
                            Text(if (expanded) "show less" else "more", color = AccentGreen, fontSize = 13.sp)
                        }
                    }
                }

                // ── Similar Books placeholder ─────────────────────────
                item {
                    Text(
                        text = "You Might Also Like",
                        color = DarkBrown,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.similarBooks) { similar ->
                            AsyncImage(
                                model = similar.coverUrl,
                                contentDescription = similar.title,
                                modifier = Modifier
                                    .width(90.dp)
                                    .height(120.dp)
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }
            }

            // ── Sticky Bottom CTA Bar ─────────────────────────────────
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(SoftWhite)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { 
                        if (state.isLoggedIn) {
                            viewModel.onEvent(BookDetailEvent.AddToCart)
                        } else {
                            onNavigateToAuth()
                        }
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkBrown),
                    border = ButtonDefaults.outlinedButtonBorder
                ) {
                    Text("Add to Cart", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { 
                        if (state.isLoggedIn) onBuyNow(book.id) else onNavigateToAuth()
                    },
                    modifier = Modifier.weight(1f).height(50.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkBrown)
                ) {
                    Text("Buy Now", color = Cream, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        if (state.isLoading) {
            CircularProgressIndicator(color = WarmBrown, modifier = Modifier.align(Alignment.Center))
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp)
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun BookImageGallery(
    imageUrls: List<String>,
    onBack: () -> Unit,
    isWishlisted: Boolean,
    onToggleWishlist: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { imageUrls.size })

    Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
            AsyncImage(
                model = imageUrls[index],
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        // Gradient scrim
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .align(Alignment.TopCenter)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.3f), Color.Transparent)
                    )
                )
        )

        // Back button
        IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        // Actions
        Row(modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            val wishlistColor by animateColorAsState(
                targetValue = if (isWishlisted) Color.Red else Color.White,
                label = "wishlist_color"
            )
            IconButton(onClick = onToggleWishlist) {
                Icon(
                    imageVector = if (isWishlisted) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "Wishlist",
                    tint = wishlistColor
                )
            }
            IconButton(onClick = {}) {
                Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
            }
        }

        // Page dots
        Row(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            repeat(imageUrls.size) { index ->
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (pagerState.currentPage == index) Color.White else Color.White.copy(alpha = 0.4f))
                        .size(if (pagerState.currentPage == index) 8.dp else 5.dp)
                )
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(shape = RoundedCornerShape(8.dp), color = Cream) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = WarmBrown, modifier = Modifier.size(16.dp))
            Text(label, color = WarmBrown, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}
