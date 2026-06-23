package co.booknook.feature.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

@Composable
fun StoryViewerScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAuth: () -> Unit,
    viewModel: StoryViewerViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.cartSuccess) {
        if (state.cartSuccess) {
            snackbarHostState.showSnackbar("Added to cart")
            viewModel.resetCartSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.White
                )
            } else if (state.books.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = state.error ?: "No books found for this story.",
                        color = Color.White
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = onNavigateBack) {
                        Text("Go Back")
                    }
                }
            } else {
                val pagerState = rememberPagerState(pageCount = { state.books.size })
                val coroutineScope = rememberCoroutineScope()

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    val book = state.books[page]
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Background Cover
                        AsyncImage(
                            model = book.coverUrl,
                            contentDescription = book.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Dark Gradient Overlay for text readability
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.4f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.8f),
                                            Color.Black
                                        ),
                                        startY = 0f,
                                        endY = Float.POSITIVE_INFINITY
                                    )
                                )
                        )

                        // Tap areas for navigation
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        coroutineScope.launch {
                                            if (pagerState.currentPage > 0) {
                                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                            }
                                        }
                                    }
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        coroutineScope.launch {
                                            if (pagerState.currentPage < state.books.size - 1) {
                                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                            } else {
                                                onNavigateBack() // Close on last tap
                                            }
                                        }
                                    }
                            )
                        }

                        // Content (Text & Bottom Bar)
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(24.dp)
                        ) {
                            Text(
                                text = book.title,
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "By ${book.author}",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 18.sp
                            )
                            if (!book.description.isNullOrEmpty()) {
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    text = book.description!!,
                                    color = Color.White.copy(alpha = 0.9f),
                                    fontSize = 14.sp,
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis,
                                    lineHeight = 20.sp
                                )
                            }

                            Spacer(Modifier.height(32.dp))
                            
                            // Bottom Action Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Price",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "KSh ${"%,d".format(book.priceKsh)}",
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                
                                Button(
                                    onClick = { 
                                        if (state.isLoggedIn) {
                                            viewModel.addToCart(book)
                                        } else {
                                            onNavigateToAuth()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFE25A3C), // Accent color
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                                ) {
                                    Icon(Icons.Outlined.ShoppingCart, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Add to Cart", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Top Progress Indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    for (i in state.books.indices) {
                        val progress = if (i <= pagerState.currentPage) 1f else 0.3f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = progress))
                        )
                    }
                }

                // Close Button
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 28.dp, end = 16.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Story", tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
        }
    }
}
