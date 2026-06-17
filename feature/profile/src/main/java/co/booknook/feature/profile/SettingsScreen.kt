package co.booknook.feature.profile

import co.booknook.core.designsystem.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.deleteAccountSuccess) {
        if (state.deleteAccountSuccess) {
            onLogout()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account", color = DarkBrown, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete your account? This action is permanent and will remove all your data, including wishlists and reviews.", color = WarmBrown) },
            containerColor = SoftWhite,
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                ) {
                    Text("Delete", color = Cream)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DarkBrown)
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (state.error != null) {
        LaunchedEffect(state.error) {
            // Can be replaced with snackbar if a host state was passed in
            viewModel.clearError()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftWhite)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = DarkBrown)
            }
            Text("Settings", color = DarkBrown, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        // Preferences Group
        Text(
            text = "Preferences",
            color = WarmBrown,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Cream)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Dark Mode", color = DarkBrown, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text("Switch to dark theme", color = WarmBrown, fontSize = 12.sp)
                    }
                    Switch(
                        checked = state.isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Cream,
                            checkedTrackColor = DarkBrown,
                            uncheckedThumbColor = WarmBrown,
                            uncheckedTrackColor = Cream,
                            uncheckedBorderColor = WarmBrown
                        )
                    )
                }
                
                HorizontalDivider(color = SoftWhite, modifier = Modifier.padding(horizontal = 20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Notifications", color = DarkBrown, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text("Receive updates & offers", color = WarmBrown, fontSize = 12.sp)
                    }
                    Switch(
                        checked = state.notificationsEnabled,
                        onCheckedChange = { viewModel.toggleNotifications(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Cream,
                            checkedTrackColor = DarkBrown,
                            uncheckedThumbColor = WarmBrown,
                            uncheckedTrackColor = Cream,
                            uncheckedBorderColor = WarmBrown
                        )
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Account Group
        Text(
            text = "Account",
            color = WarmBrown,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Cream)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Delete Account", color = Color.Red.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Red, strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { showDeleteDialog = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        }
    }
}
