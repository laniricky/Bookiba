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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.MenuBook
private val Cream = Color(0xFFF5F0E8)
private val DarkBrown = Color(0xFF1A1512)
private val WarmBrown = Color(0xFF8B7355)
private val SoftWhite = Color(0xFFFEFCF9)
private val AccentGreen = Color(0xFF2D6A4F)
data class ShelfItem(val label: String, val count: Int, val icon: ImageVector)
data class ProfileState(
    val isLoggedIn: Boolean = false,
    val name: String = "",
    val bio: String = "",
    val ordersCount: Int = 0,
    val wishlistCount: Int = 0,
    val reviewsCount: Int = 0,
    val shelves: List<ShelfItem> = listOf(
        ShelfItem("Wishlist", 0, Icons.Outlined.FavoriteBorder),
        ShelfItem("Purchased", 0, Icons.Outlined.ShoppingCart),
        ShelfItem("Reading", 0, Icons.Outlined.MenuBook),
        ShelfItem("Favorites", 0, Icons.Outlined.StarBorder)
    ),
    val isLoading: Boolean = false,
    val error: String? = null
)

@Composable
fun ProfileScreen(
    onOrdersClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onLogout: () -> Unit,
    onNavigateToLogin: () -> Unit,
    onNavigateToSignup: () -> Unit,
    viewModel: ProfileViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    // Compute shelves from live counts
    val shelves = remember(state.ordersCount, state.wishlistCount, state.reviewsCount) {
        listOf(
            ShelfItem("Wishlist", state.wishlistCount, Icons.Outlined.FavoriteBorder),
            ShelfItem("Purchased", state.ordersCount, Icons.Outlined.ShoppingCart),
            ShelfItem("Reading", 0, Icons.Outlined.MenuBook),
            ShelfItem("Favorites", state.reviewsCount, Icons.Outlined.StarBorder)
        )
    }

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

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WarmBrown)
            }
            return
        }

        if (state.isLoggedIn) {
            // Avatar + info
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier.size(80.dp).clip(CircleShape).background(WarmBrown),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.name.firstOrNull()?.toString() ?: "U", color = Cream, fontSize = 32.sp, fontWeight = FontWeight.Bold)
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
                items(shelves) { shelf ->
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
        } else {
            GuestProfileContent(
                onNavigateToLogin = onNavigateToLogin,
                onNavigateToSignup = onNavigateToSignup
            )
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Cream, modifier = Modifier.padding(horizontal = 20.dp))
        }

        ProfileMenuItem(icon = Icons.Outlined.Info, label = "Help & Support", onClick = {})
        
        if (state.isLoggedIn) {
            ProfileMenuItem(
                icon = Icons.Outlined.ExitToApp,
                label = "Logout",
                onClick = {
                    viewModel.logout()
                    onLogout()
                },
                tint = Color.Red.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun GuestProfileContent(
    onNavigateToLogin: () -> Unit,
    onNavigateToSignup: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(80.dp).clip(CircleShape).background(Cream),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Person, contentDescription = null, tint = WarmBrown, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Unlock your rare book collection",
            color = DarkBrown,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Sign in to track orders, build your shelves, and save your favorite finds.",
            color = WarmBrown,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkBrown, contentColor = SoftWhite)
        ) {
            Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onNavigateToSignup,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkBrown),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBrown)
        ) {
            Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
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
            Icon(shelf.icon, contentDescription = null, tint = DarkBrown, modifier = Modifier.size(24.dp))
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
