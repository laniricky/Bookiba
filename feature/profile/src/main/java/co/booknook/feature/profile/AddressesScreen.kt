package co.booknook.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import co.booknook.core.designsystem.theme.*
import co.booknook.core.domain.model.Address

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressesScreen(
    onBack: () -> Unit,
    viewModel: AddressesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SoftWhite,
        topBar = {
            TopAppBar(
                title = { Text("Addresses", color = DarkBrown, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = DarkBrown)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SoftWhite)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = DarkBrown,
                contentColor = Cream
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "Add Address")
            }
        }
    ) { padding ->
        if (state.isLoading && state.addresses.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = WarmBrown)
            }
        } else if (state.addresses.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No saved addresses yet.", color = WarmBrown, fontSize = 16.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.addresses) { address ->
                    AddressItemCard(
                        address = address,
                        onDelete = { viewModel.deleteAddress(address.id) },
                        onSetDefault = { viewModel.setAsDefault(address.id) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddAddressDialog(
                onDismiss = { showAddDialog = false },
                onAdd = { label, fullAddress, isDefault ->
                    viewModel.addAddress(label, fullAddress, isDefault)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
private fun AddressItemCard(
    address: Address,
    onDelete: () -> Unit,
    onSetDefault: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Cream,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = DarkBrown, modifier = Modifier.padding(top = 2.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(address.label, color = DarkBrown, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (address.isDefault) {
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(AccentGreen.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Default", color = AccentGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Text(address.fullAddress, color = WarmBrown, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
                
                if (!address.isDefault) {
                    Text(
                        text = "Set as Default",
                        color = DarkBrown,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp).clickable { onSetDefault() }
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun AddAddressDialog(
    onDismiss: () -> Unit,
    onAdd: (label: String, fullAddress: String, isDefault: Boolean) -> Unit
) {
    var label by remember { mutableStateOf("") }
    var fullAddress by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SoftWhite,
        title = { Text("Add New Address", color = DarkBrown, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Label (e.g. Home)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Cream,
                        unfocusedContainerColor = Cream,
                        focusedIndicatorColor = DarkBrown,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                OutlinedTextField(
                    value = fullAddress,
                    onValueChange = { fullAddress = it },
                    label = { Text("Full Address") },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Cream,
                        unfocusedContainerColor = Cream,
                        focusedIndicatorColor = DarkBrown,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it },
                        colors = CheckboxDefaults.colors(checkedColor = DarkBrown)
                    )
                    Text("Set as default address", color = DarkBrown)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (label.isNotBlank() && fullAddress.isNotBlank()) onAdd(label, fullAddress, isDefault) },
                colors = ButtonDefaults.buttonColors(containerColor = DarkBrown)
            ) {
                Text("Save", color = Cream)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = WarmBrown)
            }
        }
    )
}
