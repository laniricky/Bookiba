package co.booknook.feature.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Cream = Color(0xFFF5F0E8)
private val DarkBrown = Color(0xFF1A1512)
private val WarmBrown = Color(0xFF8B7355)
private val SoftWhite = Color(0xFFFEFCF9)

data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val accent: String,
    val imageRes: Int
)

private val pages = listOf(
    OnboardingPage(
        title = "Discover\nhidden gems.",
        subtitle = "Carefully thrifted books\nwith stories to tell.",
        accent = "Browse thousands of curated rare finds",
        imageRes = R.drawable.img_onboarding_discover
    ),
    OnboardingPage(
        title = "Discover\nRare Books.",
        subtitle = "One-of-a-kind editions from\naround the world.",
        accent = "Every book has a story before you",
        imageRes = R.drawable.img_onboarding_rare
    ),
    OnboardingPage(
        title = "Save &\nBuild Shelves.",
        subtitle = "Create your own personal\nlibrary of saved books.",
        accent = "Wishlist, favorites, and reading shelves",
        imageRes = R.drawable.img_onboarding_shelves
    ),
    OnboardingPage(
        title = "Stay in\nthe Loop.",
        subtitle = "Get notified when rare books\nyou love drop in.",
        accent = "Never miss a rare find again",
        imageRes = R.drawable.img_onboarding_notify
    )
)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftWhite)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            OnboardingPageContent(page = pages[pageIndex], visible = visible)
        }

        // Skip button
        TextButton(
            onClick = onFinished,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 48.dp, start = 16.dp)
        ) {
            Text(
                text = "Skip",
                color = WarmBrown,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Top Logo
        Text(
            text = "Bookiba",
            color = DarkBrown,
            fontSize = 18.sp,
            letterSpacing = 6.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 56.dp)
        )

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Page indicator dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 32.dp)
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isSelected) DarkBrown else WarmBrown.copy(alpha = 0.3f))
                            .size(if (isSelected) 10.dp else 6.dp)
                    )
                }
            }

            // Next / Get Started button
            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    } else {
                        onFinished()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DarkBrown)
            ) {
                Text(
                    text = if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started",
                    color = Cream,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

@Composable
private fun OnboardingPageContent(page: OnboardingPage, visible: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF5F0E8),
                        Color(0xFFFEFCF9)
                    )
                )
            )
    ) {
        // Decorative circle with Illustration
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.TopCenter)
                .offset(y = 60.dp)
                .clip(CircleShape)
                .background(Color(0xFFEDE4D6)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(id = page.imageRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { it / 3 }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 36.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(Modifier.height(200.dp))

                // Accent label
                Text(
                    text = page.accent.uppercase(),
                    color = WarmBrown,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(Modifier.height(12.dp))

                // Main headline
                Text(
                    text = page.title,
                    color = DarkBrown,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 44.sp
                )

                Spacer(Modifier.height(16.dp))

                // Subtitle
                Text(
                    text = page.subtitle,
                    color = WarmBrown,
                    fontSize = 16.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 24.sp
                )
            }
        }
    }
}
