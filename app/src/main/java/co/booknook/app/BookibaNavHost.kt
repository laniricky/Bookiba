package co.booknook.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import co.booknook.feature.auth.AuthFlow
import co.booknook.feature.bookdetails.BookDetailScreen
import co.booknook.feature.cart.CartScreen
import co.booknook.feature.checkout.CheckoutScreen
import co.booknook.feature.explore.ExploreScreen
import co.booknook.feature.home.HomeScreen
import co.booknook.feature.onboarding.OnboardingScreen
import co.booknook.feature.onboarding.SplashScreen
import co.booknook.feature.orders.OrdersScreen
import co.booknook.feature.profile.ProfileScreen
import co.booknook.feature.reels.ReelsScreen
import co.booknook.feature.wishlist.WishlistScreen

// ── Route constants ──────────────────────────────────────────────────────────
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val REELS = "reels"
    const val WISHLIST = "wishlist"
    const val CART = "cart"
    const val PROFILE = "profile"
    const val BOOK_DETAIL = "book/{bookId}"
    const val AUTH = "auth"
    const val CHECKOUT = "checkout"
    const val ORDERS = "orders"
    fun bookDetail(bookId: String) = "book/$bookId"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Routes.EXPLORE, "Explore", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem(Routes.REELS, "Reels", Icons.Filled.PlayArrow, Icons.Outlined.PlayArrow),
    BottomNavItem(Routes.WISHLIST, "Wishlist", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    BottomNavItem(Routes.CART, "Cart", Icons.Filled.ShoppingCart, Icons.Outlined.ShoppingCart),
    BottomNavItem(Routes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

private val bottomNavRoutes = bottomNavItems.map { it.route }.toSet()

private val DarkBrown = Color(0xFF1A1512)
private val Cream = Color(0xFFF5F0E8)
private val WarmBrown = Color(0xFF8B7355)
private val SoftWhite = Color(0xFFFEFCF9)

@Composable
fun BookibaNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val showBottomBar = bottomNavRoutes.any { currentRoute == it }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BookibaBottomBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onItemClick = { item ->
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        },
        containerColor = SoftWhite
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(onSplashFinished = {
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                })
            }

            composable(Routes.ONBOARDING) {
                OnboardingScreen(onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                })
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onBookClick = { bookId -> navController.navigate(Routes.bookDetail(bookId)) },
                    onSearchClick = { navController.navigate(Routes.EXPLORE) }
                )
            }

            composable(Routes.EXPLORE) {
                ExploreScreen(
                    onBookClick = { bookId -> navController.navigate(Routes.bookDetail(bookId)) },
                    onGenreClick = { /* navigate to genre */ }
                )
            }

            composable(Routes.REELS) {
                ReelsScreen(
                    onBookClick = { bookId -> navController.navigate(Routes.bookDetail(bookId)) }
                )
            }

            composable(Routes.WISHLIST) {
                WishlistScreen(
                    onBookClick = { bookId -> navController.navigate(Routes.bookDetail(bookId)) },
                    onRemove = { /* remove logic */ }
                )
            }

            composable(Routes.CART) {
                CartScreen(
                    onCheckout = { navController.navigate(Routes.CHECKOUT) },
                    onBookClick = { bookId -> navController.navigate(Routes.bookDetail(bookId)) }
                )
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onOrdersClick = { navController.navigate(Routes.ORDERS) },
                    onSettingsClick = { /* settings logic */ },
                    onLogout = { navController.navigate(Routes.AUTH) { popUpTo(0) } }
                )
            }

            composable(Routes.BOOK_DETAIL) {
                BookDetailScreen(
                    onBack = { navController.popBackStack() },
                    onAddToCart = { navController.navigate(Routes.CART) },
                    onBuyNow = { navController.navigate(Routes.CHECKOUT) }
                )
            }

            composable(Routes.AUTH) {
                AuthFlow(
                    onAuthenticated = { 
                        navController.navigate(Routes.HOME) { popUpTo(0) } 
                    },
                    onDismiss = { navController.popBackStack() }
                )
            }

            composable(Routes.CHECKOUT) {
                CheckoutScreen(
                    onBack = { navController.popBackStack() },
                    onSuccess = { 
                        navController.navigate(Routes.ORDERS) {
                            popUpTo(Routes.CART) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.ORDERS) {
                OrdersScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun BookibaBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    onItemClick: (BottomNavItem) -> Unit
) {
    NavigationBar(
        containerColor = SoftWhite,
        tonalElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route
            NavigationBarItem(
                selected = selected,
                onClick = { onItemClick(item) },
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = DarkBrown,
                    selectedTextColor = DarkBrown,
                    unselectedIconColor = WarmBrown.copy(alpha = 0.6f),
                    unselectedTextColor = WarmBrown.copy(alpha = 0.6f),
                    indicatorColor = Cream
                )
            )
        }
    }
}
