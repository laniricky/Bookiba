package co.booknook.app

import co.booknook.core.designsystem.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import co.booknook.feature.wishlist.WishlistViewModel
import co.booknook.feature.checkout.OrderConfirmationScreen

// â”€â”€ Route constants â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val EXPLORE = "explore?query={query}"
    fun explore(query: String? = null) = if (query != null) "explore?query=$query" else "explore"
    const val REELS = "reels"
    const val WISHLIST = "wishlist"
    const val CART = "cart"
    const val PROFILE = "profile"
    const val BOOK_DETAIL = "book/{bookId}"
    const val AUTH = "auth"
    const val CHECKOUT = "checkout"
    const val ORDER_CONFIRMATION = "order_confirmation"
    const val ORDERS = "orders"
    const val ADDRESSES = "addresses"
    const val SETTINGS = "settings"
    fun bookDetail(bookId: String) = "book/$bookId"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null,
    val iconResId: Int? = null
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Home", selectedIcon = Icons.Filled.Home, unselectedIcon = Icons.Outlined.Home),
    BottomNavItem(Routes.explore(), "Explore", selectedIcon = Icons.Filled.Search, unselectedIcon = Icons.Outlined.Search),
    BottomNavItem(Routes.REELS, "Reels", iconResId = co.booknook.app.R.drawable.ic_launcher_foreground),
    BottomNavItem(Routes.CART, "Cart", selectedIcon = Icons.Filled.ShoppingCart, unselectedIcon = Icons.Outlined.ShoppingCart),
    BottomNavItem(Routes.PROFILE, "Profile", selectedIcon = Icons.Filled.Person, unselectedIcon = Icons.Outlined.Person)
)

private val bottomNavRoutes = bottomNavItems.map { it.route }.toSet()


@Composable
fun BookibaNavHost(
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    val cartCount by viewModel.cartCount.collectAsStateWithLifecycle()

    val showBottomBar = bottomNavRoutes.any { currentRoute == it }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BookibaBottomBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    cartCount = cartCount,
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
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        @OptIn(androidx.compose.animation.ExperimentalSharedTransitionApi::class)
        androidx.compose.animation.SharedTransitionLayout {
            CompositionLocalProvider(
                LocalSharedTransitionScope provides this
            ) {
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
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                    HomeScreen(
                        onBookClick = { bookId -> navController.navigate(Routes.bookDetail(bookId)) },
                        onSearchClick = { query -> navController.navigate(Routes.explore(query)) },
                        onNavigateToAuth = { navController.navigate(Routes.AUTH) }
                    )
                }
            }

            composable(
                route = Routes.EXPLORE,
                arguments = listOf(androidx.navigation.navArgument("query") { nullable = true; defaultValue = null })
            ) {
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                    ExploreScreen(
                        onBookClick = { bookId -> navController.navigate(Routes.bookDetail(bookId)) }
                    )
                }
            }

            composable(Routes.REELS) {
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                    ReelsScreen(
                        onBookClick = { bookId -> navController.navigate(Routes.bookDetail(bookId)) }
                    )
                }
            }

            composable(Routes.WISHLIST) {
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                    val wishlistViewModel: WishlistViewModel = hiltViewModel()
                    WishlistScreen(
                        viewModel = wishlistViewModel,
                        onBookClick = { bookId -> navController.navigate(Routes.bookDetail(bookId)) }
                    )
                }
            }

            composable(Routes.CART) {
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                    CartScreen(
                        onCheckout = { navController.navigate(Routes.CHECKOUT) },
                        onBookClick = { bookId -> navController.navigate(Routes.bookDetail(bookId)) }
                    )
                }
            }

            composable(Routes.PROFILE) {
                ProfileScreen(
                    onOrdersClick = { navController.navigate(Routes.ORDERS) },
                    onAddressesClick = { navController.navigate(Routes.ADDRESSES) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                    onLogout = { navController.navigate(Routes.AUTH) { popUpTo(0) } },
                    onNavigateToLogin = { navController.navigate(Routes.AUTH) },
                    onNavigateToSignup = { navController.navigate(Routes.AUTH) }
                )
            }

            composable(Routes.BOOK_DETAIL) {
                CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) {
                    BookDetailScreen(
                        onBack = { navController.popBackStack() },
                        onAddToCart = { navController.navigate(Routes.CART) },
                        onBuyNow = { navController.navigate(Routes.CHECKOUT) },
                        onNavigateToAuth = { navController.navigate(Routes.AUTH) }
                    )
                }
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
                        navController.navigate(Routes.ORDER_CONFIRMATION) {
                            popUpTo(Routes.CART) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.ORDER_CONFIRMATION) {
                OrderConfirmationScreen(
                    onContinueShopping = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(0)
                        }
                    },
                    onViewOrders = {
                        navController.navigate(Routes.ORDERS) {
                            popUpTo(Routes.HOME)
                        }
                    }
                )
            }

            composable(Routes.ORDERS) {
                OrdersScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.ADDRESSES) {
                co.booknook.feature.profile.AddressesScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SETTINGS) {
                co.booknook.feature.profile.SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        navController.navigate(Routes.AUTH) { popUpTo(0) }
                    }
                )
            }
        }
            }
        }
    }
}

@Composable
private fun BookibaBottomBar(
    items: List<BottomNavItem>,
    currentRoute: String?,
    cartCount: Int,
    onItemClick: (BottomNavItem) -> Unit
) {
    val reelsItem = items.find { it.route == Routes.REELS }
    val otherItems = items.filter { it.route != Routes.REELS }
    val leftItems = otherItems.take(2)
    val rightItems = otherItems.drop(2)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        // â”€â”€ Nav bar surface â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .align(androidx.compose.ui.Alignment.BottomCenter)
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        ) {
            // Left items
            leftItems.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onItemClick(item) },
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon!! else item.unselectedIcon!!,
                            contentDescription = item.label,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    )
                )
            }

            // Centre spacer â€” reserves space for the floating Reels button
            NavigationBarItem(
                selected = false,
                onClick = {},
                icon = { Spacer(Modifier.size(56.dp)) },
                label = { Spacer(Modifier.height(0.dp)) },
                enabled = false,
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                )
            )

            // Right items
            rightItems.forEach { item ->
                val selected = currentRoute == item.route
                NavigationBarItem(
                    selected = selected,
                    onClick = { onItemClick(item) },
                    icon = {
                        if (item.route == Routes.CART && cartCount > 0) {
                            BadgedBox(
                                badge = {
                                    Badge(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
                                        Text(cartCount.toString())
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon!! else item.unselectedIcon!!,
                                    contentDescription = item.label,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = if (selected) item.selectedIcon!! else item.unselectedIcon!!,
                                contentDescription = item.label,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    },
                    label = { Text(item.label, style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                        unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
                    )
                )
            }
        }

        // â”€â”€ Floating Reels button â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        if (reelsItem != null) {
            val reelsSelected = currentRoute == reelsItem.route
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(androidx.compose.ui.Alignment.TopCenter)
            ) {
                Spacer(modifier = Modifier.weight(2f))
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = androidx.compose.ui.Alignment.TopCenter
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .offset(y = (-10).dp)
                            .size(64.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(if (reelsSelected) androidx.compose.material3.MaterialTheme.colorScheme.onBackground else androidx.compose.material3.MaterialTheme.colorScheme.onSurface)
                            .clickable { onItemClick(reelsItem) },
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = reelsItem.iconResId!!),
                            contentDescription = reelsItem.label,
                            tint = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                            modifier = Modifier.size(38.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(2f))
            }
        }
    }
}

