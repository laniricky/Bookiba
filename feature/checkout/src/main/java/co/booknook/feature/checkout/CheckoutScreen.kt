package co.booknook.feature.checkout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Cream = Color(0xFFF5F0E8)
private val DarkBrown = Color(0xFF1A1512)
private val WarmBrown = Color(0xFF8B7355)
private val SoftWhite = Color(0xFFFEFCF9)

@Composable
fun CheckoutScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: CheckoutViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var selectedPayment by remember { mutableStateOf("MPESA") }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Show error in snackbar
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.paymentSuccess) {
        if (state.paymentSuccess) {
            state.authorizationUrl?.let { url ->
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
                context.startActivity(intent)
            }
            onSuccess()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SoftWhite
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().background(SoftWhite).padding(innerPadding)) {
            LazyColumn(contentPadding = PaddingValues(bottom = 100.dp)) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DarkBrown)
                        }
                        Text("Checkout", color = DarkBrown, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }

                item {
                    Text(
                        text = "Shipping Address",
                        color = DarkBrown,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = Cream
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Amina Doe", color = DarkBrown, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                Text("123 Vintage Lane\nNairobi, Kenya 00100", color = WarmBrown, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                            TextButton(onClick = {}) { Text("Change", color = DarkBrown) }
                        }
                    }
                }

                item {
                    Text(
                        text = "Payment Method",
                        color = DarkBrown,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 8.dp)
                    )
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        PaymentOptionRow(
                            title = "M-Pesa",
                            subtitle = "Pay via phone number",
                            icon = Icons.Outlined.Phone,
                            selected = selectedPayment == "MPESA",
                            onClick = { selectedPayment = "MPESA" }
                        )
                        PaymentOptionRow(
                            title = "Credit/Debit Card",
                            subtitle = "Visa, Mastercard",
                            icon = Icons.Outlined.ShoppingCart,
                            selected = selectedPayment == "CARD",
                            onClick = { selectedPayment = "CARD" }
                        )
                    }
                }
            }

            // Pay button
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                color = SoftWhite,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Payment", color = WarmBrown, fontSize = 15.sp)
                        Text("KSh ${"%,d".format(state.totalAmount)}", color = DarkBrown, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        onClick = { viewModel.payNow(selectedPayment) },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkBrown)
                    ) {
                        if (state.isProcessing) {
                            CircularProgressIndicator(color = Cream, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Text("Pay Now", color = Cream, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentOptionRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Cream,
        border = if (selected) androidx.compose.foundation.BorderStroke(2.dp, DarkBrown) else null
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = DarkBrown)
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = DarkBrown, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = WarmBrown, fontSize = 13.sp)
            }
            RadioButton(
                selected = selected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = DarkBrown, unselectedColor = WarmBrown)
            )
        }
    }
}

