package co.booknook.feature.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private val DarkBrown = Color(0xFF1A1512)
private val Cream = Color(0xFFF5F0E8)
private val WarmBrown = Color(0xFF8B7355)

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    var visible by remember { mutableStateOf(false) }

    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "splash_alpha"
    )

    LaunchedEffect(Unit) {
        visible = true
        delay(2500)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0D0A08),
                        DarkBrown,
                        Color(0xFF2A1F16)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(alpha)
                .padding(horizontal = 40.dp)
        ) {
            // Vector Logo
            androidx.compose.material3.Icon(
                painter = androidx.compose.ui.res.painterResource(id = co.booknook.core.designsystem.R.drawable.ic_bookiba_logo),
                contentDescription = "Bookiba Logo",
                tint = Color.Unspecified, // Uses the original colors from XML
                modifier = Modifier.size(80.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Logo text
            Text(
                text = "Bookiba",
                color = Cream,
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 8.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Hero tagline
            Text(
                text = "Find Stories\nThat Stay.",
                color = Cream,
                fontSize = 42.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 48.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Thrifted books.\nTimeless reads.",
                color = WarmBrown,
                fontSize = 15.sp,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }

        // Bottom branding
        Text(
            text = "bookiba.co.ke",
            color = WarmBrown.copy(alpha = 0.5f),
            fontSize = 11.sp,
            letterSpacing = 3.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .alpha(alpha)
                .padding(bottom = 40.dp)
        )
    }
}
