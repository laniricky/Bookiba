package co.booknook.core.designsystem.components

import co.booknook.core.designsystem.theme.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import coil.compose.AsyncImage
import co.booknook.core.domain.model.Book

/**
 * Vertical book card used in horizontal lists (Featured, New Arrivals).
 */
@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun BookCard(
    book: Book,
    onClick: () -> Unit,
    onAddToCart: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current

    Card(
        modifier = modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            var imageModifier: Modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))

            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    imageModifier = Modifier
                        .sharedElement(
                            state = rememberSharedContentState(key = "cover_${book.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .then(imageModifier)
                }
            }

            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                modifier = imageModifier,
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
                // Real low-stock badge — driven by server data, not fake heuristics
                if (book.inventoryCount in 1..4) {
                    Text(
                        text = "Only ${book.inventoryCount} left",
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
                Spacer(Modifier.height(4.dp))
                // Star rating row — only shown when there are reviews
                if (book.reviewCount > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = if (i <= book.averageRating) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = null,
                                tint = if (i <= book.averageRating) Color(0xFFF5A623) else androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        Text(
                            text = "${"%.1f".format(book.averageRating)}",
                            color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "KSh ${"%,d".format(book.priceKsh)}",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (onAddToCart != null) {
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
}

/**
 * Compact horizontal book card used in list/search results.
 */
@OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
@Composable
fun BookListCard(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.height(100.dp)) {
            var imageModifier: Modifier = Modifier
                .width(70.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp))

            if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                with(sharedTransitionScope) {
                    imageModifier = Modifier
                        .sharedElement(
                            state = rememberSharedContentState(key = "cover_${book.id}"),
                            animatedVisibilityScope = animatedVisibilityScope
                        )
                        .then(imageModifier)
                }
            }

            AsyncImage(
                model = book.coverUrl,
                contentDescription = book.title,
                modifier = imageModifier,
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
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(text = book.author, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    if (book.reviewCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            for (i in 1..5) {
                                Icon(
                                    imageVector = if (i <= book.averageRating) Icons.Filled.Star else Icons.Outlined.Star,
                                    contentDescription = null,
                                    tint = if (i <= book.averageRating) Color(0xFFF5A623) else androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                            Text(
                                text = "${"%,.1f".format(book.averageRating)} (${book.reviewCount})",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    text = "KSh ${"%,d".format(book.priceKsh)}",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
