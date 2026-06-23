package co.booknook.feature.profile

import co.booknook.core.designsystem.theme.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.clickable
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
            title = { Text("Delete Account", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete your account? This action is permanent and will remove all your data, including wishlists and reviews.", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface) },
            containerColor = androidx.compose.material3.MaterialTheme.colorScheme.background,
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f))
                ) {
                    Text("Delete", color = androidx.compose.material3.MaterialTheme.colorScheme.surface)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
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
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
            }
            Text("Settings", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        if (state.isLoggedIn) {
            // Account Group
            SettingsGroupHeader("Account")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
            ) {
                Column {
                    SettingsRow(icon = Icons.Outlined.Person, title = "Edit Profile", subtitle = "Change name, photo, bio")
                    SettingsDivider()
                    SettingsRow(icon = Icons.Outlined.Email, title = "Change Email")
                    SettingsDivider()
                    SettingsRow(icon = Icons.Outlined.Lock, title = "Change Password")
                    SettingsDivider()
                    
                    // Logout
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { 
                            viewModel.logout()
                            onLogout() 
                        }.padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.ExitToApp, contentDescription = "Logout", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Logout", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    SettingsDivider()
                    
                    // Delete Account
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showDeleteDialog = true }.padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete Account", tint = Color.Red.copy(alpha = 0.8f))
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Delete Account", color = Color.Red.copy(alpha = 0.8f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                        if (state.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Red, strokeWidth = 2.dp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        } else {
            // Guest Account Prompt
            SettingsGroupHeader("Account")
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Sign In to Access", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Create an account to manage your profile, security settings, and synced preferences.", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
            Spacer(Modifier.height(32.dp))
        }

        // Preferences Group
        SettingsGroupHeader("Preferences")
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
        ) {
            Column {
                SettingsRow(icon = Icons.Outlined.Language, title = "Language", value = "English")
                SettingsDivider()
                SettingsRow(icon = Icons.Outlined.AttachMoney, title = "Currency Display", value = "KSh")
                SettingsDivider()
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.DarkMode, contentDescription = "Dark Mode", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Dark Mode", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text("Switch to dark theme", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                        }
                    }
                    Switch(
                        checked = state.isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                            checkedTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                            uncheckedThumbColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                            uncheckedTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                            uncheckedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
                
                if (state.isLoggedIn) {
                    SettingsDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Notifications, contentDescription = "Notifications", tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Notifications", color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                                Text("Receive updates & offers", color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                            }
                        }
                        Switch(
                            checked = state.notificationsEnabled,
                            onCheckedChange = { viewModel.toggleNotifications(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                checkedTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.onBackground,
                                uncheckedThumbColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                                uncheckedTrackColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                                uncheckedBorderColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // About Group
        SettingsGroupHeader("About")
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface)
        ) {
            Column {
                SettingsRow(icon = Icons.Outlined.Info, title = "App Version", value = "1.0.0")
                SettingsDivider()
                SettingsRow(icon = Icons.Outlined.Description, title = "Terms of Service")
                SettingsDivider()
                SettingsRow(icon = Icons.Outlined.PrivacyTip, title = "Privacy Policy")
                SettingsDivider()
                SettingsRow(icon = Icons.Outlined.HelpOutline, title = "Help & Support")
                SettingsDivider()
                SettingsRow(icon = Icons.Outlined.StarRate, title = "Rate the App")
            }
        }
        
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun SettingsGroupHeader(title: String) {
    Text(
        text = title,
        color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
}

@Composable
fun SettingsRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String? = null, value: String? = null, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = title, tint = androidx.compose.material3.MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, color = androidx.compose.material3.MaterialTheme.colorScheme.onBackground, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                if (subtitle != null) {
                    Text(subtitle, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                }
            }
        }
        if (value != null) {
            Text(value, color = androidx.compose.material3.MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        } else {
            Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = androidx.compose.material3.MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(color = androidx.compose.material3.MaterialTheme.colorScheme.background, modifier = Modifier.padding(horizontal = 20.dp))
}
