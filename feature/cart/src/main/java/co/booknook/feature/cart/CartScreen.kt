package co.booknook.feature.cart

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.booknook.core.domain.model.Book
import co.booknook.core.domain.model.CartItem

private val Cream = Color(0xFFF5F0E8)
private val DarkBrown = Color(0xFF1A1512)
private val WarmBrown = Color(0xFF8B7355)
private val SoftWhite = Color(0xFFFEFCF9)

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val couponCode: String = "",
    val couponDiscount: Long = 0,
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = true
) {
    val subtotal: Long get() = items.sumOf { it.priceKsh * it.quantity }
    val shipping: Long get() = if (items.isEmpty()) 0L else 200L
    val total: Long get() = subtotal + shipping - couponDiscount
}

@Composable
fun CartScreen(
    onCheckout: () -> Unit,
    onBookClick: (String) -> Unit,
    viewModel: CartViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var coupon by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(SoftWhite)) {
        LazyColumn(
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Your Stack (${state.items.size})",
                        color = DarkBrown,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = {}) {
                        Text("Edit", color = WarmBrown, fontSize = 14.sp)
                    }
                }
            }

            if (!state.isLoggedIn) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Person, contentDescription = null, tint = WarmBrown, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Please sign in", color = DarkBrown, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("Sign in to view your cart", color = WarmBrown, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            } else if (state.items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = WarmBrown, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("Your stack is empty", color = DarkBrown, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text("Add some books to get started", color = WarmBrown, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                }
            } else {
                // Cart items
                items(state.items, key = { it.bookId }) { item ->
                    CartItemRow(
                        item = item,
                        onQuantityChange = { qty -> viewModel.updateQuantity(item.bookId, qty) },
                        onRemove = { viewModel.removeItem(item.bookId) },
                        onClick = { onBookClick(item.bookId) }
                    )
                    HorizontalDivider(color = Cream, modifier = Modifier.padding(horizontal = 16.dp))
                }

                // Coupon
                item {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = coupon,
                            onValueChange = { coupon = it },
                            placeholder = { Text("Apply coupon code", color = WarmBrown.copy(alpha = 0.5f), fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = DarkBrown,
                                unfocusedBorderColor = Cream,
                                focusedContainerColor = Cream,
                                unfocusedContainerColor = Cream,
                                cursorColor = DarkBrown,
                                focusedTextColor = DarkBrown,
                                unfocusedTextColor = DarkBrown
                            )
                        )
                        Button(
                            onClick = {},
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBrown)
                        ) { Text("Apply", color = Cream) }
                    }
                }

                // Summary
                item {
                    Surface(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Cream
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            SummaryRow("Subtotal", state.subtotal)
                            SummaryRow("Shipping", state.shipping)
                            if (state.couponDiscount > 0) SummaryRow("Discount", -state.couponDiscount, tint = Color(0xFF2D6A4F))
                            HorizontalDivider(color = WarmBrown.copy(alpha = 0.2f))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total", color = DarkBrown, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text("KSh ${"%,d".format(state.total)}", color = DarkBrown, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }

        // Checkout button
        if (state.items.isNotEmpty()) {
            Button(
                onClick = onCheckout,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkBrown)
            ) {
                Text("Checkout", color = Cream, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun CartItemRow(item: CartItem, onQuantityChange: (Int) -> Unit, onRemove: () -> Unit, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.coverUrl,
            contentDescription = item.title,
            modifier = Modifier.size(70.dp).clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(item.title, color = DarkBrown, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("KSh ${"%,d".format(item.priceKsh)}", color = WarmBrown, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = { if (item.quantity > 1) onQuantityChange(item.quantity - 1) else onRemove() }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Clear, contentDescription = "Decrease", tint = DarkBrown, modifier = Modifier.size(18.dp))
            }
            Text("${item.quantity}", color = DarkBrown, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { onQuantityChange(item.quantity + 1) }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Outlined.Add, contentDescription = "Increase", tint = DarkBrown, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, amount: Long, tint: Color = WarmBrown) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = WarmBrown, fontSize = 14.sp)
        Text("KSh ${"%,d".format(kotlin.math.abs(amount))}", color = tint, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}


