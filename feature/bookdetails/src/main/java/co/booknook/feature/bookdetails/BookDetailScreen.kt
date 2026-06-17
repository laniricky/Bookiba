package co.booknook.feature.bookdetails

import co.booknook.core.designsystem.theme.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Star
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import co.booknook.core.domain.model.Book
import co.booknook.core.domain.model.Review

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    onBack: () -> Unit,
    onAddToCart: (String) -> Unit,
    onBuyNow: (String) -> Unit,
    onNavigateToAuth: () -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Cart feedback
    LaunchedEffect(state.cartSuccess) {
        if (state.cartSuccess) {
            snackbarHostState.showSnackbar("Added to cart")
            viewModel.onEvent(BookDetailEvent.ResetCartSuccess)
        }
    }

    // Review submission feedback
    LaunchedEffect(state.reviewSubmitSuccess) {
        if (state.reviewSubmitSuccess) {
            snackbarHostState.showSnackbar("Review submitted — thank you!")
            viewModel.onEvent(BookDetailEvent.ResetReviewSuccess)
        }
    }

    // Review bottom sheet
    if (state.showReviewSheet) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onEvent(BookDetailEvent.HideReviewSheet) },
            containerColor = SoftWhite,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            ReviewSubmitSheet(
                rating = state.pendingRating,
                comment = state.pendingComment,
                isSubmitting = state.isSubmittingReview,
                error = state.reviewSubmitError,
                onRatingChange = { viewModel.onEvent(BookDetailEvent.SetPendingRating(it)) },
                onCommentChange = { viewModel.onEvent(BookDetailEvent.SetPendingComment(it)) },
                onSubmit = { viewModel.onEvent(BookDetailEvent.SubmitReview) },
                onDismiss = { viewModel.onEvent(BookDetailEvent.HideReviewSheet) }
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(SoftWhite)) {
        state.book?.let { book ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                // ── Image Gallery ───────────────────────────────────────────
                item {
                    BookImageGallery(
                        bookId = book.id,
                        imageUrls = book.imageUrls.ifEmpty { listOf(book.coverUrl) },
                        onBack = onBack,
                        isWishlisted = state.isWishlisted,
                        onToggleWishlist = { viewModel.onEvent(BookDetailEvent.ToggleWishlist) }
                    )
                }

                // ── Availability Badge ──────────────────────────────────────
                item {
                    if (book.inventoryCount in 1..4) {
                        Surface(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFFEDE4D6)
                        ) {
                            Text(
                                text = "Only ${book.inventoryCount} left!",
                                color = WarmBrown,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // ── Title, Author & Price ───────────────────────────────────
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

                // ── Star Rating Summary ─────────────────────────────────────
                item {
                    if (book.reviewCount > 0) {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .clickable {
                                    if (state.isLoggedIn) {
                                        viewModel.onEvent(BookDetailEvent.ShowReviewSheet)
                                    } else onNavigateToAuth()
                                },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            StarRatingRow(rating = book.averageRating, size = 18.dp)
                            Text(
                                text = "${"%.1f".format(book.averageRating)} (${book.reviewCount} reviews)",
                                color = WarmBrown,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        TextButton(
                            onClick = {
                                if (state.isLoggedIn) viewModel.onEvent(BookDetailEvent.ShowReviewSheet)
                                else onNavigateToAuth()
                            },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Star,
                                contentDescription = null,
                                tint = AccentGreen,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Be the first to review", color = AccentGreen, fontSize = 13.sp)
                        }
                    }
                }

                // ── Edition Info Row ────────────────────────────────────────
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        book.edition?.let { InfoChip(label = it, icon = Icons.Outlined.DateRange) }
                        book.condition?.let { InfoChip(label = it, icon = Icons.Outlined.CheckCircle) }
                    }
                }

                // ── Divider ─────────────────────────────────────────────────
                item { HorizontalDivider(color = Cream, modifier = Modifier.padding(horizontal = 20.dp)) }

                // ── About Section ───────────────────────────────────────────
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

                // ── Reviews Section ─────────────────────────────────────────
                if (state.reviews.isNotEmpty()) {
                    item {
                        HorizontalDivider(color = Cream, modifier = Modifier.padding(horizontal = 20.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Customer Reviews", color = DarkBrown, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = {
                                if (state.isLoggedIn) viewModel.onEvent(BookDetailEvent.ShowReviewSheet)
                                else onNavigateToAuth()
                            }) {
                                Text("Write a review", color = AccentGreen, fontSize = 13.sp)
                            }
                        }
                    }

                    items(state.reviews.take(5)) { review ->
                        ReviewItem(review = review)
                    }
                }

                // ── Similar Books ───────────────────────────────────────────
                item {
                    if (state.similarBooks.isNotEmpty()) {
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
            }

            // ── Sticky Bottom CTA Bar ───────────────────────────────────────
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
                        if (state.isLoggedIn) viewModel.onEvent(BookDetailEvent.AddToCart)
                        else onNavigateToAuth()
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
                        if (state.isLoggedIn) {
                            viewModel.onEvent(BookDetailEvent.AddToCart)
                            onBuyNow(book.id)
                        } else onNavigateToAuth()
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
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
        )
    }
}

// ── Composable: Star Rating Row ─────────────────────────────────────────────

@Composable
fun StarRatingRow(
    rating: Double,
    size: androidx.compose.ui.unit.Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = null,
                tint = if (i <= rating) Color(0xFFF5A623) else Cream,
                modifier = Modifier.size(size)
            )
        }
    }
}

// ── Composable: Clickable Star Rating Input ─────────────────────────────────

@Composable
private fun StarRatingInput(
    rating: Int,
    onRatingChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (i in 1..5) {
            Icon(
                imageVector = if (i <= rating) Icons.Filled.Star else Icons.Outlined.Star,
                contentDescription = "Star $i",
                tint = if (i <= rating) Color(0xFFF5A623) else Cream,
                modifier = Modifier
                    .size(36.dp)
                    .clickable { onRatingChange(i) }
            )
        }
    }
}

// ── Composable: Review Item ─────────────────────────────────────────────────

@Composable
private fun ReviewItem(review: Review) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StarRatingRow(rating = review.rating.toDouble(), size = 14.dp)
            Text(
                text = review.createdAt.take(10),   // show just the date
                color = WarmBrown,
                fontSize = 11.sp
            )
        }
        review.comment?.let { comment ->
            Text(
                text = comment,
                color = DarkBrown,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        HorizontalDivider(color = Cream, modifier = Modifier.padding(top = 12.dp))
    }
}

// ── Composable: Review Submit Bottom Sheet ──────────────────────────────────

@Composable
private fun ReviewSubmitSheet(
    rating: Int,
    comment: String,
    isSubmitting: Boolean,
    error: String?,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Handle
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .clip(CircleShape)
                .background(Cream)
        )

        Text(
            text = "Write a Review",
            color = DarkBrown,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Tap a star to rate",
            color = WarmBrown,
            fontSize = 13.sp
        )

        StarRatingInput(rating = rating, onRatingChange = onRatingChange)

        OutlinedTextField(
            value = comment,
            onValueChange = onCommentChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Share your thoughts (optional)", color = WarmBrown.copy(alpha = 0.6f)) },
            minLines = 3,
            maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DarkBrown,
                unfocusedBorderColor = Cream,
                focusedTextColor = DarkBrown,
                unfocusedTextColor = DarkBrown
            ),
            shape = RoundedCornerShape(12.dp)
        )

        error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }

        Button(
            onClick = onSubmit,
            enabled = rating > 0 && !isSubmitting,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkBrown)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Cream, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                Text("Submit Review", color = Cream, fontWeight = FontWeight.SemiBold)
            }
        }

        TextButton(onClick = onDismiss) {
            Text("Cancel", color = WarmBrown)
        }
    }
}

// ── Composable: Image Gallery ───────────────────────────────────────────────

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
private fun BookImageGallery(
    bookId: String,
    imageUrls: List<String>,
    onBack: () -> Unit,
    isWishlisted: Boolean,
    onToggleWishlist: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { imageUrls.size })
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current

    Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
            var imageModifier: Modifier = Modifier.fillMaxSize()
            
            if (index == 0 && sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    imageModifier = Modifier
                        .sharedElement(
                            state = rememberSharedContentState(key = "cover_$bookId"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .then(imageModifier)
                }
            }
            
            AsyncImage(
                model = imageUrls[index],
                contentDescription = null,
                modifier = imageModifier,
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
            val context = androidx.compose.ui.platform.LocalContext.current
            IconButton(onClick = {
                val sendIntent = android.content.Intent().apply {
                    action = android.content.Intent.ACTION_SEND
                    putExtra(android.content.Intent.EXTRA_TEXT, "Check out this book on Bookiba!")
                    type = "text/plain"
                }
                context.startActivity(android.content.Intent.createChooser(sendIntent, null))
            }) {
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

// ── Composable: Info Chip ───────────────────────────────────────────────────

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
