package co.booknook.feature.checkout

import co.booknook.core.designsystem.theme.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OrderConfirmationScreen(
    orderId: String? = null,
    onContinueShopping: () -> Unit,
    onViewOrders: () -> Unit
) {
    // Animate the check icon in with a bounce
    var triggered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { triggered = true }
    val scale by animateFloatAsState(
        targetValue = if (triggered) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "check_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated check circle
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale)
                    .background(AccentGreen.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(56.dp)
                )
            }

            Text(
                "Order Placed! 🎉",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )

            orderId?.let {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.surface
                ) {
                    Text(
                        text = "Order #${it.take(8).uppercase()}",
                        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Text(
                text = "Thank you for your order. We'll send you updates as your books make their way to you.",
                color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onViewOrders,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
            ) {
                Text("Track My Order", color = androidx.compose.material3.MaterialTheme.colorScheme.surface, fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onContinueShopping,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
            ) {
                Text("Continue Shopping", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
