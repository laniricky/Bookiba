package co.booknook.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Cream = Color(0xFFF5F0E8)
private val DarkBrown = Color(0xFF1A1512)
private val WarmBrown = Color(0xFF8B7355)
private val SoftWhite = Color(0xFFFEFCF9)
private val AccentGreen = Color(0xFF2D6A4F)

data class ShelfItem(val label: String, val count: Int, val emoji: String)
data class ProfileState(
    val name: String = "Amina",
    val bio: String = "Book lover · Vintage finder · Collecting stories.",
    val ordersCount: Int = 24,
    val wishlistCount: Int = 47,
    val reviewsCount: Int = 12,
    val shelves: List<ShelfItem> = listOf(
        ShelfItem("Wishlist", 47, "❤️"),
        ShelfItem("Purchased", 24, "📦"),
        ShelfItem("Reading", 5, "📖"),
        ShelfItem("Favorites", 12, "⭐")
    )
)

@Composable
fun ProfileScreen(
    onOrdersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit,
    state: ProfileState = ProfileState()
) {
    Column(modifier = Modifier.fillMaxSize().background(SoftWhite)) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Profile", color = DarkBrown, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = onSettingsClick) {
                Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = DarkBrown)
            }
        }

        // Avatar + info
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(80.dp).clip(CircleShape).background(WarmBrown),
                contentAlignment = Alignment.Center
            ) {
                Text(state.name.first().toString(), color = Cream, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            Text(state.name, color = DarkBrown, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(state.bio, color = WarmBrown, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))

            Spacer(Modifier.height(20.dp))

            // Stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(count = state.ordersCount, label = "Orders")
                VerticalDivider(modifier = Modifier.height(40.dp), color = Cream)
                StatItem(count = state.wishlistCount, label = "Wishlist")
                VerticalDivider(modifier = Modifier.height(40.dp), color = Cream)
                StatItem(count = state.reviewsCount, label = "Reviews")
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = Cream, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(16.dp))

        // My Shelves
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("My Shelves", color = DarkBrown, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("See all", color = WarmBrown, fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(220.dp)
        ) {
            items(state.shelves) { shelf ->
                ShelfCard(shelf = shelf)
            }
        }

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = Cream, modifier = Modifier.padding(horizontal = 20.dp))

        // Menu items
        ProfileMenuItem(icon = Icons.Outlined.List, label = "Order History", onClick = onOrdersClick)
        ProfileMenuItem(icon = Icons.Outlined.Star, label = "My Reviews", onClick = {})
        ProfileMenuItem(icon = Icons.Outlined.LocationOn, label = "Addresses", onClick = {})
        ProfileMenuItem(icon = Icons.Outlined.ShoppingCart, label = "Payment Methods", onClick = {})
        ProfileMenuItem(icon = Icons.Outlined.Info, label = "Help & Support", onClick = {})
        ProfileMenuItem(icon = Icons.Outlined.ExitToApp, label = "Logout", onClick = onLogout, tint = Color.Red.copy(alpha = 0.7f))
    }
}

@Composable
private fun StatItem(count: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(count.toString(), color = DarkBrown, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Text(label, color = WarmBrown, fontSize = 12.sp)
    }
}

@Composable
private fun ShelfCard(shelf: ShelfItem) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Cream,
        modifier = Modifier.fillMaxWidth().height(90.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(shelf.emoji, fontSize = 20.sp)
            Column {
                Text(shelf.label, color = DarkBrown, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("${shelf.count} books", color = WarmBrown, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = DarkBrown) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
        Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.ArrowForward, contentDescription = null, tint = WarmBrown.copy(alpha = 0.5f), modifier = Modifier.size(18.dp))
    }
}
