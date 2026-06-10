package co.booknook.feature.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import coil.compose.AsyncImage
import co.booknook.core.domain.model.Order
import co.booknook.core.domain.model.OrderStatus

private val Cream = Color(0xFFF5F0E8)
private val DarkBrown = Color(0xFF1A1512)
private val WarmBrown = Color(0xFF8B7355)
private val SoftWhite = Color(0xFFFEFCF9)
private val AccentGreen = Color(0xFF2D6A4F)
@Composable
fun OrdersScreen(
    onBack: () -> Unit,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val orders = state.orders
    
    var selectedTab by remember { mutableStateOf("All") }
    val tabs = listOf("All", "Processing", "Shipped", "Delivered")

    val filteredOrders = if (selectedTab == "All") orders else orders.filter { it.status.label == selectedTab }

    Column(modifier = Modifier.fillMaxSize().background(SoftWhite)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = DarkBrown)
            }
            Text("Order History", color = DarkBrown, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        ScrollableTabRow(
            selectedTabIndex = tabs.indexOf(selectedTab),
            containerColor = SoftWhite,
            contentColor = DarkBrown,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[tabs.indexOf(selectedTab)]),
                    color = DarkBrown
                )
            },
            divider = {}
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == title,
                    onClick = { selectedTab = title },
                    text = { Text(title, fontWeight = if (selectedTab == title) FontWeight.Bold else FontWeight.Normal) },
                    selectedContentColor = DarkBrown,
                    unselectedContentColor = WarmBrown
                )
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.isLoading) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = WarmBrown)
                    }
                }
            } else if (filteredOrders.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No orders found.", color = WarmBrown)
                    }
                }
            } else {
                items(filteredOrders) { order ->
                    OrderCard(order)
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: Order) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Cream
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Order #${order.id}", color = DarkBrown, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                // Format the long dateMs string
                val dateStr = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(order.dateMs))
                Text(dateStr, color = WarmBrown, fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                order.items.take(3).forEach { book ->
                    AsyncImage(
                        model = book.coverUrl,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                if (order.items.size > 3) {
                    Box(
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)).background(SoftWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+${order.items.size - 3}", color = DarkBrown, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("KSh ${"%,d".format(order.totalAmount)}", color = DarkBrown, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                
                val statusColor = when (order.status) {
                    OrderStatus.PROCESSING -> WarmBrown
                    OrderStatus.SHIPPED -> Color(0xFFD97706)
                    OrderStatus.DELIVERED -> AccentGreen
                }
                
                Surface(color = statusColor.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                    Text(order.status.label, color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                }
            }
        }
    }
}
