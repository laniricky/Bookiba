package co.booknook.feature.wishlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import co.booknook.core.domain.model.Book

private val Cream = Color(0xFFF5F0E8)
private val DarkBrown = Color(0xFF1A1512)
private val WarmBrown = Color(0xFF8B7355)
private val SoftWhite = Color(0xFFFEFCF9)

// Sample state until wired to real VM
data class WishlistUiState(
    val books: List<Book> = emptyList(),
    val isLoading: Boolean = false
)

@Composable
fun WishlistScreen(
    onBookClick: (String) -> Unit,
    onRemove: (String) -> Unit,
    state: WishlistUiState = WishlistUiState()
) {
    Column(
        modifier = Modifier.fillMaxSize().background(SoftWhite)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Wishlist", color = DarkBrown, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text("${state.books.size} books", color = WarmBrown, fontSize = 14.sp)
        }

        if (state.books.isEmpty() && !state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📚", fontSize = 48.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Your wishlist is empty", color = DarkBrown, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("Save books you love to find them later", color = WarmBrown, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.books, key = { it.id }) { book ->
                    WishlistBookCard(
                        book = book,
                        onClick = { onBookClick(book.id) },
                        onRemove = { onRemove(book.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WishlistBookCard(book: Book, onClick: () -> Unit, onRemove: () -> Unit) {
    Box(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column {
            Box {
                AsyncImage(
                    model = book.coverUrl,
                    contentDescription = book.title,
                    modifier = Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                ) {
                    Icon(Icons.Filled.Favorite, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(book.title, color = DarkBrown, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("KSh ${"%,d".format(book.priceKsh)}", color = WarmBrown, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
